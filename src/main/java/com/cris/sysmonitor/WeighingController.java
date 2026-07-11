package com.cris.sysmonitor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/weighing")
public class WeighingController {

    @Autowired
    private WeighingMachineService weighingService;

    // Returns latest reading from every active COM port that is sending data
    @GetMapping("/latest")
    public ResponseEntity<List<WeighingMachineService.WeighingReading>> getLatest() {
        return ResponseEntity.ok(weighingService.getLatestReadings());
    }

    // Returns all available COM port names — useful for mentor to identify the right one
    @GetMapping("/ports")
    public ResponseEntity<List<String>> getPorts() {
        return ResponseEntity.ok(weighingService.getAvailablePorts());
    }
}