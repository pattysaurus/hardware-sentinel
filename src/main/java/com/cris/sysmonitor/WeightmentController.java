package com.cris.sysmonitor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/weightment")
public class WeightmentController {

    @Autowired
    private WeightmentReadingRepository repository;

    // Called by the weighment machine (or stub) to push a reading
    @PostMapping("/data")
    public Map<String, String> receiveReading(@RequestBody Map<String, Object> payload) {
        try {
            double weight = Double.parseDouble(payload.getOrDefault("weight", "0").toString());
            String unit   = payload.getOrDefault("unit", "kg").toString();
            String source = payload.getOrDefault("source", "unknown").toString();

            WeightmentReading reading = new WeightmentReading(
                    weight, unit, source, LocalDateTime.now()
            );
            repository.save(reading);

            Map<String, String> res = new HashMap<String, String>();
            res.put("status", "saved");
            return res;

        } catch (Exception e) {
            Map<String, String> err = new HashMap<String, String>();
            err.put("status", "error");
            err.put("message", e.getMessage());
            return err;
        }
    }

    // Dashboard polls this for the latest reading
    @GetMapping("/latest")
    public List<WeightmentReading> getLatest() {
        return repository.findAllByOrderByReadingTimeDesc(PageRequest.of(0, 20));
    }
}
