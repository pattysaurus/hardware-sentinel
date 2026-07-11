package com.cris.sysmonitor;

public class CrisDeviceInfo {

    private String portName;
    private String description;
    private String status;
    private String type;        // "QR_DISPLAY" or "BARCODE_PRINTER"

    public CrisDeviceInfo(String portName, String description, String status, String type) {
        this.portName    = portName;
        this.description = description;
        this.status      = status;
        this.type        = type;
    }

    public String getPortName()    { return portName; }
    public String getDescription() { return description; }
    public String getStatus()      { return status; }
    public String getType()        { return type; }
}