package com.loff.xdmscannermodule;

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
import java.util.Collections;
import java.util.Comparator;

public class Backend {
    public static XDManifest xdManifest;
    public static File xdtMobileJsonFile = null;

    public static class SSCC {
        String ssccID;
        Boolean scanned = false;
        Boolean unknown = false;
        String description = "";
        Boolean highRisk = false;
        ArrayList<Article> articles = new ArrayList<>();
    }

    public static class Article {
        String code;
        String desc;
        String GTIN;
        int QTY;
        Boolean highRisk = false;
    }

    public static class XDManifest {
        String manifestID;
        String manifestDate;
        ArrayList<SSCC> ssccList = new ArrayList<>();
    }

    public static void exportJson(){
        try {
            JSONObject json_out = new JSONObject();

            JSONObject json_manifest = new JSONObject();
            json_manifest.put("Manifest ID", xdManifest.manifestID);
            json_manifest.put("Import Date", xdManifest.manifestDate);

            JSONArray json_sscc_array = new JSONArray();

            for (int i = 0; i < xdManifest.ssccList.size(); i++){

                // ARTICLE LIST GEN
                JSONArray json_article_array = new JSONArray();
                for (int j=0; j < xdManifest.ssccList.get(i).articles.size(); j++){

                    json_article_array.put(new JSONObject()
                    .put("Code", xdManifest.ssccList.get(i).articles.get(j).code)
                    .put("Desc", xdManifest.ssccList.get(i).articles.get(j).desc)
                    .put("GTIN", xdManifest.ssccList.get(i).articles.get(j).GTIN)
                    .put("QTY", xdManifest.ssccList.get(i).articles.get(j).QTY)
                    .put("is_HR", xdManifest.ssccList.get(i).articles.get(j).highRisk)
                    );

                }

                // SSCC GEN
                json_sscc_array.put(new JSONObject()
                .put("SSCC", xdManifest.ssccList.get(i).ssccID)
                .put("Scanned", xdManifest.ssccList.get(i).scanned)
                .put("Unknown", xdManifest.ssccList.get(i).unknown)
                .put("is_HR", xdManifest.ssccList.get(i).highRisk)
                .put("Articles", json_article_array)
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

    public static boolean importJson(){
        if (fsCheck()) {
            StringBuilder json_str_in = new StringBuilder();
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(xdtMobileJsonFile));
                String line;

                while ((line = bufferedReader.readLine()) != null){
                    json_str_in.append(line);
                }

                String result = json_str_in.toString();
                xdManifest = null;
                xdManifest = new XDManifest();

                JSONObject Jmanifest = new JSONObject(result).getJSONObject("Manifest");
                xdManifest.manifestID = Jmanifest.getString("Manifest ID");
                xdManifest.manifestDate = Jmanifest.getString("Import Date");
                JSONArray jSSCCs = Jmanifest.getJSONArray("SSCCs");

                for (int i=0; i < jSSCCs.length(); i++){
                    // SSCC READ
                    SSCC newSSCC = new SSCC();
                    JSONObject jSSCC = jSSCCs.getJSONObject(i);

                    newSSCC.ssccID = jSSCC.getString("SSCC");
                    newSSCC.highRisk = jSSCC.getBoolean("is_HR");
                    if (jSSCC.has("Scanned")) { newSSCC.scanned = jSSCC.getBoolean("Scanned"); }
                    if (jSSCC.has("Unknown")) { newSSCC.unknown = jSSCC.getBoolean("Unknown"); }

                    // SSCC ARTICLES READ
                    JSONArray articles = jSSCC.getJSONArray("Articles");
                    for (int j=0; j < articles.length(); j++){
                        Article newArticle = new Article();
                        JSONObject article = articles.getJSONObject(j);

                        newArticle.code = article.getString("Code");
                        newArticle.desc = article.getString("Desc");
                        newArticle.GTIN = article.getString("GTIN");
                        newArticle.QTY = article.getInt("QTY");
                        newArticle.highRisk = article.getBoolean("is_HR");

                        newSSCC.articles.add(newArticle);
                    }

                    if (newSSCC.articles.size() > 1) {
                        newSSCC.description = "MULTI - LINES: " + newSSCC.articles.size();
                    } else {
                        newSSCC.description = newSSCC.articles.get(0).desc;
                    }

                    xdManifest.ssccList.add(newSSCC);
                }

                // SORT THE NEW ARRAY
                xdManifest.ssccList.sort((sscc, t1) -> {
                    int ssccLF = Integer.parseInt(sscc.ssccID.substring(sscc.ssccID.length() - 4));
                    int t1LF = Integer.parseInt(t1.ssccID.substring(t1.ssccID.length() - 4));
                    return ssccLF - t1LF;
                });


            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

            return true;

        } else {
            return false;
        }
    }

    public static boolean fsCheck(){
        if (xdtMobileJsonFile != null) {
            return xdtMobileJsonFile.exists();
        } else {
            return false;
        }
    }


}
