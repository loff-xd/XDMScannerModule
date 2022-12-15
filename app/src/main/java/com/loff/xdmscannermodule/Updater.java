package com.loff.xdmscannermodule;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.core.content.FileProvider;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

@SuppressWarnings("deprecation")
public class Updater {

    static int version = 1200;  // TODO APPLICATION VERSION
    String dl_url = "";
    String changelog = "";

    public boolean doUpdateCheck(){

            try {
                GitHub gitHub = GitHub.connectAnonymously();

                GHRepository repo = gitHub.getRepository("loff-xd/XDMScannerModule");
                int latestVersion = Integer.parseInt(repo.getLatestRelease().getTagName().replaceAll("\\.",""));
                Log.v("UPDATER", "current: " + version + ", latest: " + latestVersion);

                // Check for version
                if (version < latestVersion) {
                    // Fetch assets JSON from github
                    URL assetsURL = new URL(repo.getLatestRelease().getAssetsUrl());
                    changelog = repo.getLatestRelease().getBody();
                    URLConnection request = assetsURL.openConnection();
                    request.connect();

                    JSONArray ja = new JSONArray(IOUtils.toString(assetsURL));
                    for (int i = 0; i < ja.length(); i++){
                        JSONObject obj = ja.getJSONObject(i);
                        dl_url = obj.getString("browser_download_url");
                    }

                    // FETCH UPDATE
                    Log.v("UPDATER", dl_url);
                    return !dl_url.equals("");
                }
                return false;

            } catch (IOException | JSONException e) {
                e.printStackTrace();
                return false;
            }

    }

    public void installUpdate(Context context) {
        File updateFile = new File(context.getExternalFilesDir(null), context.getString(R.string.xdt_update_file));
        if (updateFile.exists()){
            //noinspection ResultOfMethodCallIgnored
            updateFile.delete();
        }

        try {
            ReadableByteChannel rbc = Channels.newChannel(new URL(dl_url).openStream());
            FileOutputStream fos = new FileOutputStream(updateFile);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

            Log.v("UPDATER", "Got update");

            // STAGE UPDATE
            Uri uri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", updateFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            context.startActivity(intent);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
