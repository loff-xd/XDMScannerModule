package com.loff.xdmscannermodule;
import android.media.MediaCodec;
import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;

public class Backend {

    public XDManifest xdManifest;
    public static File xdtMobileFolder = new File(Environment.getExternalStorageDirectory(), "XDTMobile");
    public static File xdtMobileJsonFile = new File(xdtMobileFolder, "xdt_mobile.json");

    public static class SSCC{
        String ssccID;
        Boolean scanned = false;
        Boolean surplus = false;
        String description;
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
            json_out.put("ManifestID", xdManifest.manifestID);

            JSONArray json_sscc_array = new JSONArray();

            for (int i = 0; i < xdManifest.ssccList.size(); i++){
                json_sscc_array.put(new JSONObject()
                .put("ssccID", xdManifest.ssccList.get(i).ssccID)
                .put("scanned", xdManifest.ssccList.get(i).scanned)
                .put("surplus", xdManifest.ssccList.get(i).surplus)
                .put("description", xdManifest.ssccList.get(i).description)
                );
            }

            json_out.put("SSCCs", json_sscc_array);

        } catch (JSONException e){
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

    public void writeFile(){

    }

    public boolean fsCheck(){
        if (!xdtMobileFolder.exists()){
            return xdtMobileFolder.mkdirs();
        } else {
            return xdtMobileJsonFile.exists();

        }
    }


}
