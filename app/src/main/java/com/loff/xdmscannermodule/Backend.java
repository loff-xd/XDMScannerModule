package com.loff.xdmscannermodule;
import android.os.Environment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;

public class Backend {

    public XDManifest xdManifest;
    public static File xdtMobileFolder = new File(Environment.getExternalStorageDirectory(), "XDTMobile");
    public static File xdtMobileJsonFile = new File(xdtMobileFolder, "xdt_mobile.json");

    public static class SSCC{
        String ssccID;
        Boolean scanned = false;
        Boolean unknown = false;
        String description = "";
        Boolean highRisk = false;
    }

    public static class XDManifest {
        String manifestID;
        String manifestDate;
        ArrayList<SSCC> ssccList = new ArrayList<SSCC>();
    }

    public void exportJson(XDManifest xdManifest){
        try {
            JSONObject json_out = new JSONObject();

            JSONObject json_manifest = new JSONObject();
            json_manifest.put("Manifest ID", xdManifest.manifestID);

            JSONArray json_sscc_array = new JSONArray();

            for (int i = 0; i < xdManifest.ssccList.size(); i++){
                json_sscc_array.put(new JSONObject()
                .put("SSCC", xdManifest.ssccList.get(i).ssccID)
                .put("Scanned", xdManifest.ssccList.get(i).scanned)
                .put("Unknown", xdManifest.ssccList.get(i).unknown)
                .put("Description", xdManifest.ssccList.get(i).description)
                );
            }

            json_manifest.put("SSCCs", json_sscc_array);
            json_out.put("Manifest", json_manifest);

            Writer writer = new BufferedWriter(new FileWriter(xdtMobileJsonFile));
            writer.write(json_out.toString());
            writer.close();

        } catch (Exception e){
            System.out.println(Arrays.toString(e.getStackTrace()));
        }
    }

    public boolean importJson(){
        if (fsCheck()) {
            StringBuilder json_str_in = new StringBuilder();
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(xdtMobileJsonFile));
                String line;

                while ((line = bufferedReader.readLine()) != null){
                    json_str_in.append(line);
                }

                String result = json_str_in.toString();
                xdManifest = new XDManifest();

                JSONObject Jmanifest = new JSONObject(result).getJSONObject("Manifest");
                xdManifest.manifestID = Jmanifest.getString("Manifest ID");
                xdManifest.manifestDate = Jmanifest.getString("Import Date");
                JSONArray jSSCCs = Jmanifest.getJSONArray("SSCCs");

                for (int i=0; i < jSSCCs.length(); i++){
                    SSCC newSSCC = new SSCC();
                    JSONObject jSSCC = jSSCCs.getJSONObject(i);
                    newSSCC.ssccID = jSSCC.getString("SSCC");
                    newSSCC.highRisk = jSSCC.getBoolean("is_HR");
                    newSSCC.description = jSSCC.getJSONArray("Articles").length() + " Lines";
                    xdManifest.ssccList.add(newSSCC);
                }



            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

            return true;

        } else {
            return false;
        }
    }

    public boolean fsCheck(){
        if (!xdtMobileFolder.exists()){
            return xdtMobileFolder.mkdirs();
        } else {
            return xdtMobileJsonFile.exists();

        }
    }


}
