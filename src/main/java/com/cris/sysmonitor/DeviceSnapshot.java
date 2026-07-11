package com.cris.sysmonitor;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "device_snapshot")
public class DeviceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ipAddress;
    private int usbCount;
    private int diskCount;
    private int comCount;
    private int audioCount;
    private int displayCount;
    private int crisCount;
    private String status;
    private LocalDateTime snapshotTime;

    public DeviceSnapshot() {}

    public DeviceSnapshot(String ipAddress, int usbCount, int diskCount,
                          int comCount, int audioCount, int displayCount,
                          int crisCount, String status, LocalDateTime snapshotTime) {
        this.ipAddress    = ipAddress;
        this.usbCount     = usbCount;
        this.diskCount    = diskCount;
        this.comCount     = comCount;
        this.audioCount   = audioCount;
        this.displayCount = displayCount;
        this.crisCount    = crisCount;
        this.status       = status;
        this.snapshotTime = snapshotTime;
    }

    // Getters
    public Long getId()               { return id; }
    public String getIpAddress()      { return ipAddress; }
    public int getUsbCount()          { return usbCount; }
    public int getDiskCount()         { return diskCount; }
    public int getComCount()          { return comCount; }
    public int getAudioCount()        { return audioCount; }
    public int getDisplayCount()      { return displayCount; }
    public int getCrisCount()         { return crisCount; }
    public String getStatus()         { return status; }
    public LocalDateTime getSnapshotTime() { return snapshotTime; }
}