package com.cris.sysmonitor;

import com.fazecast.jSerialComm.SerialPort;
import org.springframework.stereotype.Service;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CrisDeviceService {

    private static final int    BAUD_RATE         = 115200;
    private static final int    READ_TIMEOUT_MS   = 2000;
    private static final int    RESPONSE_WAIT_MS  = 600;
    private static final String CRIS_DEVICE_LABEL = "USB Serial Device";

    // ── Device Discovery ─────────────────────────────────────────────────────

    public List<CrisDeviceInfo> getConnectedDevices() {
        List<CrisDeviceInfo> devices = new ArrayList<CrisDeviceInfo>();

        // QR display devices via serial port
        for (SerialPort port : SerialPort.getCommPorts()) {
            String desc = port.getDescriptivePortName();
            if (desc != null && desc.contains(CRIS_DEVICE_LABEL)) {
                devices.add(new CrisDeviceInfo(
                        port.getSystemPortName(),
                        desc,
                        "Connected",
                        "QR_DISPLAY"
                ));
            }
        }

        // Barcode printer via Windows print queue
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        for (PrintService svc : services) {
            String name = svc.getName().toUpperCase();
            if (name.contains("ZDESIGNER") || name.contains("ZEBRA") ||
                    name.contains("ZPL") || name.contains("BARCODE")) {
                devices.add(new CrisDeviceInfo(
                        svc.getName(),
                        "Zebra ZPL Barcode Printer",
                        "Connected",
                        "BARCODE_PRINTER"
                ));
            }
        }

        return devices;
    }

    public boolean isCrisDevice(String portName) {
        for (SerialPort port : SerialPort.getCommPorts()) {
            if (port.getSystemPortName().equals(portName)) {
                String desc = port.getDescriptivePortName();
                return desc != null && desc.contains(CRIS_DEVICE_LABEL);
            }
        }
        return false;
    }

    // ── Command Relay ─────────────────────────────────────────────────────────

    public synchronized String sendCommand(String portName, String jsonCommand) {
        SerialPort port = findPort(portName);
        if (port == null) {
            return "{\"error\":\"Port not found: " + portName + "\"}";
        }

        port.setComPortParameters(BAUD_RATE, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, READ_TIMEOUT_MS, 0);

        if (!port.openPort()) {
            return "{\"error\":\"Could not open port " + portName + ". Is it already in use?\"}";
        }

        try {
            byte[] cmd = (jsonCommand + "\n").getBytes(StandardCharsets.UTF_8);
            port.writeBytes(cmd, cmd.length);

            Thread.sleep(RESPONSE_WAIT_MS);

            int available = port.bytesAvailable();
            if (available > 0) {
                byte[] buffer = new byte[available];
                int read = port.readBytes(buffer, buffer.length);
                return new String(buffer, 0, read, StandardCharsets.UTF_8).trim();
            }

            return "{\"status\":\"sent\",\"note\":\"no response from device\"}";

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "{\"error\":\"Command interrupted\"}";
        } catch (Exception e) {
            return "{\"error\":\"" + e.getMessage() + "\"}";
        } finally {
            port.closePort();
        }
    }

    // ── Command Builder ───────────────────────────────────────────────────────

    public String buildCommand(String type, Map<String, Object> params) {
        if (type == null) return null;
        switch (type) {
            case "firmware":
                return "{\"cmd\":\"firmware\"}";

            case "version":
                return "{\"cmd\":\"version\"}";

            case "serialno":
                return "{\"cmd\":\"serialno\"}";

            case "cleardisplay":
                return "{\"cmd\":\"cleardisplay\"}";

            case "display-string": {
                int    row      = getInt(params, "row", 20);
                int    col      = getInt(params, "col", 8);
                int    fontSize = getInt(params, "fontSize", 24);
                String text     = getString(params, "text", "");
                return "{\"cmd\":\"display-string: " + row + "#" + col + "#" + fontSize + "#" + text + "\"}";
            }

            case "foreground-color": {
                String color = getString(params, "color", "#FFFF");
                return "{\"cmd\":\"foreground-color: " + color + "\"}";
            }

            case "background-color": {
                String color = getString(params, "color", "#0000");
                return "{\"cmd\":\"background-color: " + color + "\"}";
            }

            case "display-qrcode": {
                int    row   = getInt(params, "row", 10);
                int    col   = getInt(params, "col", 10);
                int    width = getInt(params, "width", 100);
                String text  = getString(params, "text", "");
                return "{\"cmd\":\"display-qrcode:" + row + "#" + col + "#" + width + "#" + text + "\"}";
            }

            default:
                return null;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private SerialPort findPort(String portName) {
        for (SerialPort port : SerialPort.getCommPorts()) {
            if (port.getSystemPortName().equals(portName)) return port;
        }
        return null;
    }

    private int getInt(Map<String, Object> params, String key, int defaultVal) {
        if (params == null || !params.containsKey(key)) return defaultVal;
        Object val = params.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return defaultVal; }
    }

    private String getString(Map<String, Object> params, String key, String defaultVal) {
        if (params == null || !params.containsKey(key)) return defaultVal;
        Object val = params.get(key);
        return val != null ? val.toString() : defaultVal;
    }
}