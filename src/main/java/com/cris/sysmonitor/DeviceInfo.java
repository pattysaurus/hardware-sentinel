package com.cris.sysmonitor;

public class DeviceInfo {

    private String name;
    private String type;
    private String status;
    private String details;
    private String category;

    public DeviceInfo(String name, String type, String status, String details, String category) {
        this.name     = name;
        this.type     = type;
        this.status   = status;
        this.details  = details;
        this.category = category;
    }

    public String getName()     { return name; }
    public String getType()     { return type; }
    public String getStatus()   { return status; }
    public String getDetails()  { return details; }
    public String getCategory() { return category; }
}
