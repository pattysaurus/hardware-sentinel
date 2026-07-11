package com.cris.sysmonitor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cris")
public class CrisDeviceController {

    @Autowired
    private CrisDeviceService crisDeviceService;

    /**
     * GET /api/cris/devices
     * Returns all currently connected CRIS QR devices.
     */
    @GetMapping("/devices")
    @ResponseBody
    public List<CrisDeviceInfo> getDevices() {
        return crisDeviceService.getConnectedDevices();
    }

    /**
     * POST /api/cris/command
     * Body: { "port": "COM3", "type": "display-qrcode", "params": { "text": "...", "width": 100, ... } }
     * Relays the command to the device and returns the device's response.
     */
    @PostMapping("/command")
    public ResponseEntity<String> sendCommand(@RequestBody CrisCommandRequest request) {
        if (request.getPort() == null || request.getPort().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("{\"error\":\"Port name is required\"}");
        }

        String command = crisDeviceService.buildCommand(request.getType(), request.getParams());
        if (command == null) {
            return ResponseEntity.badRequest()
                    .body("{\"error\":\"Unknown command type: " + request.getType() + "\"}");
        }

        String response = crisDeviceService.sendCommand(request.getPort(), command);
        return ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .body(response);
    }
}
