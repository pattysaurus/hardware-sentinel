package com.cris.sysmonitor;

import com.fazecast.jSerialComm.SerialPort;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.UsbDevice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class SystemService {

    @Autowired
    private CrisDeviceService crisDeviceService;

    // ── USB ───────────────────────────────────────────────────────────────────

    public List<DeviceInfo> getUsbDevices() {
        List<DeviceInfo> devices = new ArrayList<DeviceInfo>();
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        for (UsbDevice usb : hal.getUsbDevices(true)) {
            collectDevices(usb, devices);
        }
        return devices;
    }

    private void collectDevices(UsbDevice device, List<DeviceInfo> devices) {
        String name = device.getName();
        if (!shouldFilter(name)) {
            String serial  = device.getSerialNumber();
            String details = "Vendor: " + device.getVendor() +
                    (serial.isEmpty() ? "" : " | Serial: " + serial);
            devices.add(new DeviceInfo(name, "USB", "Connected", details, classifyDevice(name)));
        }
        for (UsbDevice child : device.getConnectedDevices()) {
            collectDevices(child, devices);
        }
    }

    private boolean shouldFilter(String name) {
        String lower = name.toLowerCase();
        return lower.contains("root hub") || lower.contains("usb composite device");
    }

    private String classifyDevice(String name) {
        String lower = name.toLowerCase();
        if (lower.contains("keyboard"))                                          return "Keyboard";
        if (lower.contains("mouse"))                                             return "Mouse";
        if (lower.contains("webcam") || lower.contains("camera") || lower.contains("uvc")) return "Camera";
        if (lower.contains("bluetooth"))                                         return "Bluetooth";
        if (lower.contains("audio") || lower.contains("sound") || lower.contains("headset")) return "Audio";
        if (lower.contains("storage") || lower.contains("flash"))               return "Storage";
        if (lower.contains("hub"))                                               return "Hub";
        if (lower.contains("adapter"))                                           return "Adapter";
        if (lower.contains("printer"))                                           return "Printer";
        if (lower.contains("host controller"))                                   return "Controller";
        return "USB Device";
    }

    // ── Disks ─────────────────────────────────────────────────────────────────

    public List<DeviceInfo> getDiskDrives() {
        List<DeviceInfo> drives = new ArrayList<DeviceInfo>();
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        hal.getDiskStores().forEach(disk -> drives.add(new DeviceInfo(
                disk.getName(), "Disk", "Mounted",
                "Model: " + disk.getModel() + " | Size: " + (disk.getSize() / (1024 * 1024 * 1024)) + " GB",
                "Storage"
        )));
        return drives;
    }

    // ── COM Ports ─────────────────────────────────────────────────────────────

    /**
     * Returns COM ports visible via WMI, excluding any port identified as a CRIS device.
     * CRIS devices are shown in their own dedicated dashboard section instead.
     */
    public List<DeviceInfo> getComPorts() {
        // Build set of CRIS port names to exclude
        List<String> crisPortNames = new ArrayList<String>();
        for (SerialPort port : SerialPort.getCommPorts()) {
            String desc = port.getDescriptivePortName();
            if (desc != null && desc.contains("USB Serial Device")) {
                crisPortNames.add(port.getSystemPortName()); // e.g. "COM3"
            }
        }

        List<DeviceInfo> ports = new ArrayList<DeviceInfo>();
        try {
            ProcessBuilder pb = new ProcessBuilder("powershell", "-Command",
                    "Get-WMIObject Win32_SerialPort | ForEach-Object { $_.Name + '|' + $_.Description + '|' + $_.DeviceID }");
            pb.redirectErrorStream(true);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(pb.start().getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\|", -1);
                String name = parts.length > 0 ? parts[0].trim() : "Unknown";
                String desc = parts.length > 1 ? parts[1].trim() : "";
                String id   = parts.length > 2 ? parts[2].trim() : "";

                // Skip CRIS devices — they appear in the CRIS DEVICES section
                if (crisPortNames.contains(id)) continue;

                ports.add(new DeviceInfo(name, "COM Port", "Active",
                        "ID: " + id + (desc.isEmpty() ? "" : " | " + desc), "Serial Port"));
            }
        } catch (Exception ignored) {}
        return ports;
    }

    // ── Audio ─────────────────────────────────────────────────────────────────

    public List<DeviceInfo> getAudioDevices() {
        List<DeviceInfo> devices = new ArrayList<DeviceInfo>();
        try {
            ProcessBuilder pb = new ProcessBuilder("powershell", "-Command",
                    "Get-WMIObject Win32_SoundDevice | ForEach-Object { $_.Name + '|' + $_.Manufacturer + '|' + $_.Status }");
            pb.redirectErrorStream(true);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(pb.start().getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\|", -1);
                String name = parts.length > 0 ? parts[0].trim() : "Unknown";
                String mfr  = parts.length > 1 ? parts[1].trim() : "";
                String stat = parts.length > 2 ? parts[2].trim() : "Unknown";
                devices.add(new DeviceInfo(name, "Audio", stat, "Manufacturer: " + mfr, "Audio"));
            }
        } catch (Exception ignored) {}
        return devices;
    }

    // ── Display ───────────────────────────────────────────────────────────────

    public List<DeviceInfo> getDisplayDevices() {
        List<DeviceInfo> devices = new ArrayList<DeviceInfo>();
        try {
            ProcessBuilder pb = new ProcessBuilder("powershell", "-Command",
                    "Get-WMIObject Win32_DesktopMonitor | ForEach-Object { $_.Name + '|' + $_.MonitorManufacturer + '|' + $_.Status }");
            pb.redirectErrorStream(true);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(pb.start().getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\|", -1);
                String name = parts.length > 0 ? parts[0].trim() : "Unknown";
                String mfr  = parts.length > 1 ? parts[1].trim() : "";
                String stat = parts.length > 2 ? parts[2].trim() : "Unknown";
                devices.add(new DeviceInfo(name, "Display", stat, "Manufacturer: " + mfr, "Display"));
            }
        } catch (Exception ignored) {}
        return devices;
    }
}
