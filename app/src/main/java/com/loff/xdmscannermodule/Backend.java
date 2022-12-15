package com.loff.xdmscannermodule;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.ExistingWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;


public class Backend {
    public static ArrayList<XDManifest> manifests = new ArrayList<>();
    public static ArrayList<String> manifest_list;
    public static XDManifest selectedManifest;
    public static File xdtMobileJsonFile = null;
    public static File xdtMobileJsonTempFile = null;

    public static class saveWorker extends Worker {
        public saveWorker(
                Context context,
                WorkerParameters params) {
            super(context, params);
        }

        @NonNull
        @Override
        public Result doWork() {
            return exportJsonFile();
        }
    }

    public static class SSCC implements Serializable {
        String ssccID;
        Boolean scanned = false;
        Boolean unknown = false;
        String description = "";
        Boolean highRisk = false;
        String scannedInManifest;
        String dilStatus = "";
        String dilComment = "";
        ArrayList<Article> articles = new ArrayList<>();
    }

    public static class Article implements Serializable {
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

    public static void exportJsonAsync(Context context) {
        WorkManager workmanager = WorkManager.getInstance(context);
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(saveWorker.class).build();
        workmanager.enqueueUniqueWork("fileSave", ExistingWorkPolicy.REPLACE, request);
    }

    public static ListenableWorker.Result exportJsonFile() {
        Writer writer;
        try {
            Log.v("BACKEND", "WRITE NEW DATA TO TEMP FILE");
            writer = new BufferedWriter(new FileWriter(xdtMobileJsonTempFile));
            writer.write(exportJson());
            writer.close();

            boolean deleteResult = true;
            if (xdtMobileJsonFile.exists()) {
                deleteResult = xdtMobileJsonFile.delete();
                Log.v("BACKEND", "REPLACE FAILSAFE WITH NEW DATA");
            }
            boolean renameResult = xdtMobileJsonTempFile.renameTo(xdtMobileJsonFile);
            Log.v("BACKEND", "NEW DATA SAVED");

            if (deleteResult && renameResult) {
                return ListenableWorker.Result.success();
            } else {
                return ListenableWorker.Result.failure();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ListenableWorker.Result.failure();
        }
    }

    @NonNull
    public static String exportJson() {
        // WRITE SELECTED TO ARRAY
        //syncSelectedManifestToDB();

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
                            .put("ScannedInManifest", manifests.get(i).ssccList.get(j).scannedInManifest)
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

    public static boolean importJson(String json_string_in) {

        try {
            JSONObject jsonIn = new JSONObject(json_string_in);
            JSONArray jmanifests = jsonIn.getJSONArray("Manifests");
            ArrayList<XDManifest> manifestsOld = manifests;
            manifests.clear();

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

                    // SORT THE NEW ARRAY
                    newManifest.ssccList.sort((sscc, t1) -> {
                        int ssccLF = Integer.parseInt(sscc.ssccID.substring(sscc.ssccID.length() - 4));
                        int t1LF = Integer.parseInt(t1.ssccID.substring(t1.ssccID.length() - 4));
                        return ssccLF - t1LF;
                    });

                }
                manifests.add(newManifest);
            }

            manifests.sort((t1, t2) -> {
                int t1LF = Integer.parseInt(t1.manifestID);
                int t2LF = Integer.parseInt(t2.manifestID);
                return t1LF - t2LF;
            });

            manifest_list = new ArrayList<>();
            for (int i = 0; i < manifests.size(); i++) {
                manifest_list.add(manifests.get(i).manifestID);
            }

            selectedManifest = manifests.get(manifests.size() - 1);

            if (manifests.size() == 1){
                if (manifests.get(0).manifestID.equals(manifestsOld.get(0).manifestID)){
                    manifests.clear();
                    manifests = manifestsOld;
                    manifestsOld.clear();
                    System.out.println("KEEP OLD MANI");
                }
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean importJsonFile() {
        if (fsCheck()) {

            StringBuilder json_str_in = new StringBuilder();
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(xdtMobileJsonFile));
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    json_str_in.append(line);
                }

                String result = json_str_in.toString();
                return importJson(result);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }

    @SuppressWarnings("BusyWait")
    public static boolean fsCheck() {

        if (xdtMobileJsonFile != null && xdtMobileJsonTempFile != null) {

            int attempts = 0;
            while (xdtMobileJsonTempFile.exists()) {
                if (attempts > 5) {
                    //noinspection ResultOfMethodCallIgnored
                    xdtMobileJsonTempFile.delete();
                    return xdtMobileJsonFile.exists();
                }
                attempts++;
                Log.v("BACKEND", "WAIT FOR FILE LOCK");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            return xdtMobileJsonFile.exists();

        } else {
            return false;
        }

    }


}
