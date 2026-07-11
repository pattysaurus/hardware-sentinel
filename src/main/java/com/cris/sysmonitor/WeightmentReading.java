package com.cris.sysmonitor;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "weightment_reading")
public class WeightmentReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double weight;
    private String unit;
    private String source;   // machine ID / port, filled when real device arrives
    private LocalDateTime readingTime;

    public WeightmentReading() {}

    public WeightmentReading(Double weight, String unit, String source, LocalDateTime readingTime) {
        this.weight      = weight;
        this.unit        = unit;
        this.source      = source;
        this.readingTime = readingTime;
    }

    // Getters
    public Long getId()                  { return id; }
    public Double getWeight()            { return weight; }
    public String getUnit()              { return unit; }
    public String getSource()            { return source; }
    public LocalDateTime getReadingTime(){ return readingTime; }
}
