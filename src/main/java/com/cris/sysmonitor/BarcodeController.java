package com.cris.sysmonitor;

import org.springframework.web.bind.annotation.*;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/barcode")
public class BarcodeController {

    @PostMapping("/print")
    public Map<String, String> printBarcode(@RequestBody Map<String, Object> payload) {
        Map<String, String> response = new HashMap<String, String>();
        try {
            // Build ZPL label
            String zpl = buildZpl(payload);

            // Find Zebra printer
            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
            PrintService[] services = PrintServiceLookup.lookupPrintServices(flavor, null);

            PrintService selected = null;
            for (int i = 0; i < services.length; i++) {
                String name = services[i].getName().toUpperCase();
                if (name.contains("ZDESIGNER") || name.contains("ZEBRA") ||
                        name.contains("ZPL") || name.contains("BARCODE")) {
                    selected = services[i];
                    break;
                }
            }

            if (selected == null) {
                response.put("status", "error");
                response.put("message", "Zebra printer not found. Check driver installation.");
                return response;
            }

            // Send raw ZPL bytes
            byte[] zplBytes = zpl.getBytes("UTF-8");
            Doc doc = new SimpleDoc(zplBytes, flavor, null);
            DocPrintJob job = selected.createPrintJob();
            job.print(doc, new HashPrintRequestAttributeSet());

            System.out.println("[Barcode] ZPL sent to: " + selected.getName());
            response.put("status", "success");
            response.put("message", "Label printed on " + selected.getName());

        } catch (Exception e) {
            System.err.println("[Barcode] Error: " + e.getMessage());
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return response;
    }

    @GetMapping("/printers")
    public Map<String, Object> listPrinters() {
        Map<String, Object> response = new HashMap<String, Object>();
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        String[] names = new String[services.length];
        for (int i = 0; i < services.length; i++) {
            names[i] = services[i].getName();
        }
        response.put("printers", names);
        response.put("count", services.length);
        return response;
    }

    private String buildZpl(Map<String, Object> p) {
        String Pwbltno     = get(p, "Pwbltno");
        String ConsignorName = get(p, "ConsignorName");
        String ConsignorAdd  = get(p, "ConsignorAdd");
        String ConsineeName  = get(p, "ConsineeName");
        String ConsigneeAdd  = get(p, "ConsigneeAdd");
        String nop           = get(p, "nop");
        String totalweight   = get(p, "totalweight");
        String SrcStn        = get(p, "SrcStn");
        String DstnStn       = get(p, "DstnStn");
        String bktype        = get(p, "bktype");
        String FormNo        = get(p, "FormNo");
        String BookingDt     = get(p, "BookingDt");
        String railway       = get(p, "railway");
        String strViastn     = get(p, "strViastn");
        String strScalecd    = get(p, "strScalecd");
        String strTrntype    = get(p, "strTrntype");
        String majcommd      = get(p, "majcommd");
        String stritmnoDwt   = get(p, "stritmnoDwt");

        // Parse first item from stritmnoDwt (itmno~weight~recheckwt~itemType~chrgwt~pkg_desc)
        String itmno = "", weight = "", recheckwt = "";
        String[] items = stritmnoDwt.split("@");
        if (items.length > 0) {
            String[] parts = items[0].split("~");
            itmno     = parts.length > 0 ? parts[0] : "";
            weight    = parts.length > 1 ? parts[1] : "";
            recheckwt = parts.length > 2 ? parts[2] : "";
        }

        // DataMatrix barcode data (matches original BCClass format)
        String barcodeData = Pwbltno + itmno + SrcStn + DstnStn + nop +
                totalweight + weight + bktype + recheckwt + FormNo;

        StringBuilder zpl = new StringBuilder();
        zpl.append("^XA\n");
        zpl.append("^PW800\n");           // print width 800 dots (4" at 200dpi)
        zpl.append("^LL0800\n");          // label length 800 dots

        // Header: booking type + form no
        zpl.append("^FO110,10^ADN,25,15^FD").append(bktype).append(" - ").append(FormNo).append("^FS\n");

        // Railway zone (large)
        zpl.append("^FO210,30^ADN,36,20^FD").append(railway).append("^FS\n");

        // Train type
        zpl.append("^FO200,80^ADN,18,10^FDTrain Type: ").append(strTrntype).append("^FS\n");

        // Scale box
        zpl.append("^FO210,100^GB40,40,2^FS\n");
        zpl.append("^FO215,105^ADN,18,10^FDSCALE^FS\n");
        zpl.append("^FO215,120^ADN,25,15^FD").append(strScalecd).append("^FS\n");

        // From / To
        zpl.append("^FO10,110^ADN,20,12^FDFrom: ").append(ConsignorName).append("^FS\n");
        zpl.append("^FO10,130^ADN,18,10^FD").append(ConsignorAdd).append("^FS\n");
        zpl.append("^FO10,150^ADN,20,12^FDTo: ").append(ConsineeName).append("^FS\n");
        zpl.append("^FO10,170^ADN,18,10^FD").append(ConsigneeAdd).append("^FS\n");

        // Package and weight info
        zpl.append("^FO10,195^ADN,18,10^FDPkg: ").append(nop).append("/").append(itmno)
                .append("  Wt: ").append(totalweight).append("/").append(weight)
                .append(" ").append(recheckwt).append("^FS\n");

        // Railway mark (PWB number)
        zpl.append("^FO10,215^ADN,25,15^FD").append(Pwbltno, 0, Math.min(5, Pwbltno.length()))
                .append("-").append(Pwbltno.length() > 5 ? Pwbltno.substring(5) : "")
                .append("/").append(BookingDt).append("^FS\n");

        // Via
        zpl.append("^FO10,240^ADN,15,10^FDVIA: ").append(strViastn).append("^FS\n");

        // Source -> Destination (large)
        zpl.append("^FO10,260^ADN,30,18^FD").append(SrcStn).append(" --> ").append(DstnStn).append("^FS\n");

        // Commodity
        zpl.append("^FO10,295^ADN,15,10^FD").append(majcommd).append("^FS\n");

        // DataMatrix barcode — bottom right (large)
        zpl.append("^FO530,10^BXN,5,200^FD").append(barcodeData).append("^FS\n");

        // DataMatrix barcode — top left (large)
        zpl.append("^FO10,10^BXN,5,200^FD").append(barcodeData).append("^FS\n");

        // DataMatrix barcode — small copies
        zpl.append("^FO130,10^BXN,3,200^FD").append(barcodeData).append("^FS\n");
        zpl.append("^FO160,100^BXN,3,200^FD").append(barcodeData).append("^FS\n");
        zpl.append("^FO10,210^BXN,3,200^FD").append(barcodeData).append("^FS\n");

        zpl.append("^XZ\n");
        return zpl.toString();
    }

    private String get(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }
}