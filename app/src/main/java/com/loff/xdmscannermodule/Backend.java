package com.loff.xdmscannermodule;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;


public class Backend {
    private static ArrayList<XDManifest> manifests = new ArrayList<>();
    public static XDManifest selectedManifest = null;


    public static class SSCC {
        String ssccID;
        Boolean scanned = false;
        Boolean unknown = false;
        String description = "";
        Boolean highRisk = false;
        String dilStatus = "";
        String dilComment = "";
        ArrayList<Article> articles = new ArrayList<>();
    }

    public static class Article {
        String code;
        String desc;
        String GTIN;
        int QTY;
        Boolean highRisk = false;
        String dilStatus = "";
        String dilComment = "";
        int dilQTY = 0;
    }

    public static class XDManifest {
        String manifestID;
        String manifestDate;
        String lastModified;
        ArrayList<SSCC> ssccList = new ArrayList<>();
    }

    @NonNull
    public static String exportJson() {
        Log.d("OPERATION", "exportJson()");

        // WRITE TO JSON
        try {
            JSONObject jsonOutput = new JSONObject();
            JSONArray newManifests = new JSONArray();

            for (int i = 0; i < manifests.size(); i++) {

                JSONObject newManifest = new JSONObject();
                JSONArray newManifestSSCCs = new JSONArray();
                newManifest.put("Manifest ID", manifests.get(i).manifestID);
                newManifest.put("Import Date", manifests.get(i).manifestDate);
                newManifest.put("Last Modified", manifests.get(i).lastModified);

                for (int j = 0; j < manifests.get(i).ssccList.size(); j++) {

                    // ARTICLE LIST GEN
                    JSONArray newSSCCArticles = new JSONArray();
                    for (int k = 0; k < manifests.get(i).ssccList.get(j).articles.size(); k++) {

                        newSSCCArticles.put(new JSONObject()
                                .put("Code", manifests.get(i).ssccList.get(j).articles.get(k).code)
                                .put("Desc", manifests.get(i).ssccList.get(j).articles.get(k).desc)
                                .put("GTIN", manifests.get(i).ssccList.get(j).articles.get(k).GTIN)
                                .put("QTY", manifests.get(i).ssccList.get(j).articles.get(k).QTY)
                                .put("is_HR", manifests.get(i).ssccList.get(j).articles.get(k).highRisk)
                                .put("DIL Status", manifests.get(i).ssccList.get(j).articles.get(k).dilStatus)
                                .put("DIL Comment", manifests.get(i).ssccList.get(j).articles.get(k).dilComment)
                                .put("DIL Qty", manifests.get(i).ssccList.get(j).articles.get(k).dilQTY)
                        );

                    }

                    // SSCC GEN
                    newManifestSSCCs.put(new JSONObject()
                            .put("SSCC", manifests.get(i).ssccList.get(j).ssccID)
                            .put("Scanned", manifests.get(i).ssccList.get(j).scanned)
                            .put("Unknown", manifests.get(i).ssccList.get(j).unknown)
                            .put("is_HR", manifests.get(i).ssccList.get(j).highRisk)
                            .put("DIL Status", manifests.get(i).ssccList.get(j).dilStatus)
                            .put("DIL Comment", manifests.get(i).ssccList.get(j).dilComment)
                            .put("Articles", newSSCCArticles)
                    );
                }

                newManifest.put("SSCCs", newManifestSSCCs);
                newManifests.put(newManifest);
            }

            jsonOutput.put("Manifests", newManifests);

            return jsonOutput.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static void parseReceivedJson(String json_string_in) {

        try {
            Log.d("OPERATION", "parseReceivedJson()");
            JSONObject jsonIn = new JSONObject(json_string_in);
            JSONArray jmanifests = jsonIn.getJSONArray("Manifests");

            ArrayList<XDManifest> newManifests = new ArrayList<>();


            for (int i = 0; i < jmanifests.length(); i++) {
                XDManifest newManifest = new XDManifest();
                JSONObject jManifest = jmanifests.getJSONObject(i);

                newManifest.manifestID = jManifest.getString("Manifest ID");
                newManifest.manifestDate = jManifest.getString("Import Date");
                if (jManifest.has("Last Modified")) {
                    newManifest.lastModified = jManifest.getString("Last Modified");
                } else {
                    newManifest.lastModified = String.valueOf(System.currentTimeMillis());
                }
                JSONArray jSSCCs = jManifest.getJSONArray("SSCCs");

                for (int j = 0; j < jSSCCs.length(); j++) {
                    // SSCC READ
                    SSCC newSSCC = new SSCC();
                    JSONObject jSSCC = jSSCCs.getJSONObject(j);

                    newSSCC.ssccID = jSSCC.getString("SSCC");
                    newSSCC.highRisk = jSSCC.getBoolean("is_HR");
                    if (jSSCC.has("Scanned")) {
                        newSSCC.scanned = jSSCC.getBoolean("Scanned");
                    }
                    if (jSSCC.has("Unknown")) {
                        newSSCC.unknown = jSSCC.getBoolean("Unknown");
                    }
                    if (jSSCC.has("DIL Status")) {
                        newSSCC.dilStatus = jSSCC.getString("DIL Status");
                        newSSCC.dilComment = jSSCC.getString("DIL Comment");
                    }

                    // SSCC ARTICLES READ
                    JSONArray articles = jSSCC.getJSONArray("Articles");
                    for (int k = 0; k < articles.length(); k++) {
                        Article newArticle = new Article();
                        JSONObject article = articles.getJSONObject(k);

                        newArticle.code = article.getString("Code");
                        newArticle.desc = article.getString("Desc");
                        newArticle.GTIN = article.getString("GTIN");
                        newArticle.QTY = article.getInt("QTY");
                        newArticle.highRisk = article.getBoolean("is_HR");
                        if (article.has("DIL Status")) {
                            newArticle.dilStatus = article.getString("DIL Status");
                            newArticle.dilComment = article.getString("DIL Comment");
                            newArticle.dilQTY = article.getInt("DIL Qty");
                        }

                        newSSCC.articles.add(newArticle);
                    }

                    if (newSSCC.articles.size() > 1) {
                        newSSCC.description = "MULTI - LINES: " + newSSCC.articles.size();
                    } else {
                        newSSCC.description = newSSCC.articles.get(0).desc;
                    }

                    newManifest.ssccList.add(newSSCC);

                }

                newManifest.ssccList.sort((sscc, t1) -> {
                    int ssccLF = Integer.parseInt(sscc.ssccID.substring(sscc.ssccID.length() - 4));
                    int t1LF = Integer.parseInt(t1.ssccID.substring(t1.ssccID.length() - 4));
                    return ssccLF - t1LF;
                });

                newManifests.add(newManifest);
            }

            if (newManifests.size() == 1 && manifests.size() == 1){
                if (newManifests.get(0).manifestID.equals(manifests.get(0).manifestID)){
                    newManifests.clear();
                    System.out.println("KEEPING OLD MANIFEST");
                } else {
                    manifests = newManifests;
                }
            } else {
                manifests = newManifests;
            }

            selectedManifest = manifests.get(manifests.size() - 1);



        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean loadData(@NonNull Context context) {
        Log.d("OPERATION", "loadData()");
        SharedPreferences sharedPreferences = context.getSharedPreferences("sp", Context.MODE_PRIVATE);
        Gson gson = new Gson();

        String json = sharedPreferences.getString("manifests", null);
        Type type = new TypeToken<ArrayList<XDManifest>>() {}.getType();

        manifests = gson.fromJson(json, type);

        if (manifests == null || manifests.size() == 0) {
            manifests = new ArrayList<>();
            return false;
        }

        selectedManifest = manifests.get(manifests.size() - 1);

        return true;
    }

    public static void saveData(@NonNull Context context) {
        Log.d("OPERATION", "saveData()");
        SharedPreferences sharedPreferences = context.getSharedPreferences("sp", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Gson gson = new Gson();
        String json = gson.toJson(manifests);
        editor.putString("manifests", json);
        editor.apply();
    }

}
