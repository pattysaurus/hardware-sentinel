package com.cris.sysmonitor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SystemController {

    @Autowired
    private SystemService systemService;

    @GetMapping("/api/usb")
    @ResponseBody
    public List<DeviceInfo> getUsbDevices() {
        return systemService.getUsbDevices();
    }

    @GetMapping("/api/disks")
    @ResponseBody
    public List<DeviceInfo> getDiskDrives() {
        return systemService.getDiskDrives();
    }

    @GetMapping("/api/ports")
    @ResponseBody
    public List<DeviceInfo> getComPorts() {
        return systemService.getComPorts();
    }

    @GetMapping("/api/audio")
    @ResponseBody
    public List<DeviceInfo> getAudioDevices() {
        return systemService.getAudioDevices();
    }

    @GetMapping("/api/display")
    @ResponseBody
    public List<DeviceInfo> getDisplayDevices() {
        return systemService.getDisplayDevices();
    }
}
