package com.cris.sysmonitor;

import java.util.Map;

public class CrisCommandRequest {

    private String port;              // e.g. "COM3"
    private String type;              // e.g. "cleardisplay", "display-string", "display-qrcode"
    private Map<String, Object> params; // command-specific parameters

    public String getPort()                  { return port; }
    public void setPort(String port)         { this.port = port; }

    public String getType()                  { return type; }
    public void setType(String type)         { this.type = type; }

    public Map<String, Object> getParams()              { return params; }
    public void setParams(Map<String, Object> params)   { this.params = params; }
}
