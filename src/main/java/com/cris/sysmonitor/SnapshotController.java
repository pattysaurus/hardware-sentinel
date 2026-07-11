package com.cris.sysmonitor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/snapshots")
public class SnapshotController {

    @Autowired
    private DeviceSnapshotRepository repository;

    // Returns last 50 snapshots, newest first
    @GetMapping
    public List<DeviceSnapshot> getSnapshots() {
        return repository.findAllByOrderBySnapshotTimeDesc(PageRequest.of(0, 50));
    }
}