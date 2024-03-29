package com.loff.xdmscannermodule;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

interface syncCallback {
    void callback();
}

public class MenuActivity extends AppCompatActivity implements syncCallback {

    Button btnBeginScanning;
    TextView txtStatusText;
    SwipeRefreshLayout refreshLayout;
    Updater updater;
    AlertDialog.Builder dialog;
    Thread netModule;
    String ip = "";

    public void callback(){
        Backend.saveData(getApplicationContext());
        runOnUiThread(this::interfaceUpdate);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.MANAGE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MenuActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 5);
        }

        btnBeginScanning = findViewById(R.id.btn_begin_scanning);
        txtStatusText = findViewById(R.id.txt_status_text);
        refreshLayout = findViewById(R.id.refresh_layout);


        // BEGIN SCANNING BUTTON
        btnBeginScanning.setOnClickListener(view -> {
            if (!refreshLayout.isRefreshing())
                startActivity(new Intent(MenuActivity.this, ScannerActivity.class));
        });

        // LOAD BACKEND
        doBackendLoad();

        // SYNC
        WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        //noinspection deprecation
        ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());

        // APP UPDATER
        updater = new Updater();
        new Thread(() -> {
            if (updater.doUpdateCheck()) showUpdatePrompt();
            else
                runOnUiThread(() -> Toast.makeText(this, "Application up-to-date", Toast.LENGTH_SHORT).show());
        }).start();

        // PULL TO REFRESH
        refreshLayout.setOnRefreshListener(this::interfaceUpdate);
    }

    @Override
    public void onStart(){
        super.onStart();
        netModule = new Thread(new netExchanger(this));
        netModule.start();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.v("MenuActivity", "ONSTOP SAVE");
        Backend.saveData(getApplicationContext());
        netModule.interrupt();
    }

    @Override
    public void onRestart() {
        super.onRestart();
        interfaceUpdate();
    }

    private void doBackendLoad() {
        if (Backend.selectedManifest == null) {
            refreshLayout.setRefreshing(true);
            new Thread(() -> {
                Log.v("MenuActivity", "backend_load");
                boolean loadSuccess = Backend.loadData(getApplicationContext());

                if (loadSuccess) {
                    runOnUiThread(this::interfaceUpdate);
                } else {
                    runOnUiThread(() -> {
                        String sb = getString(R.string.jsonImportError) + "\n\nIP: " + ip;
                        txtStatusText.setText(sb);
                        refreshLayout.setRefreshing(false);
                        btnBeginScanning.setEnabled(false);
                    });
                }
            }).start();
        } else {
            interfaceUpdate();
        }
    }

    public void interfaceUpdate() {
        refreshLayout.setRefreshing(true);
        Log.v("MenuActivity", "interface_update");

        if (Backend.selectedManifest != null) {
            btnBeginScanning.setEnabled(true);

            int scannedCount = 0;
            int hrCount = 0;
            int missingCount = 0;
            for (int i = 0; i < Backend.selectedManifest.ssccList.size(); i++) {
                if (Backend.selectedManifest.ssccList.get(i).scanned) {
                    scannedCount++;
                }
                if (Backend.selectedManifest.ssccList.get(i).highRisk) {
                    hrCount++;
                }
                if (Backend.selectedManifest.ssccList.get(i).dilStatus.equals("missing")) {
                    missingCount++;
                }
            }


            String sb = "Manifest: " + Backend.selectedManifest.manifestID +
                    "\n\n\nTotal SSCCs: " + Backend.selectedManifest.ssccList.size() +
                    "\n\n    - Scanned: " + scannedCount +
                    "\n    - High-Risk: " + hrCount +
                    "\n    - Missing: " + missingCount +
                    "\n\n\n IP: " + ip;

            txtStatusText.setText(sb);
            if (scannedCount > 0) {
                btnBeginScanning.setText(R.string.resume_scanning);
            } else {
                btnBeginScanning.setText(R.string.begin_scanning);
            }

        } else {
            btnBeginScanning.setEnabled(false);
        }

        refreshLayout.setRefreshing(false);
    }

    public void showUpdatePrompt() {
        runOnUiThread(() -> {
            dialog = new AlertDialog.Builder(this);
            dialog.setTitle("Application update avaliable");
            dialog.setMessage("Changelog:\n" + updater.changelog + "\n\nDo you wish to install?");
            dialog.setPositiveButton("Yes", (dialog1, which) -> new Thread(() -> {
                runOnUiThread(() -> Toast.makeText(this, "Update Requested, Please wait...", Toast.LENGTH_LONG).show());
                updater.installUpdate(getApplicationContext());
            }).start());
            dialog.setNegativeButton("Later", null);
            dialog.create().show();
        });
    }

    static class netExchanger implements Runnable {

        int PORT = 7700;
        ServerSocket serverSocket;
        Socket socket;
        syncCallback c;

        public netExchanger(syncCallback c){
            this.c = c;
        }

        @Override
        public void run() {

            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(PORT));

                socket = serverSocket.accept();

                BufferedReader data_in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Writer data_out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                // SYNC_VER IN
                String client_sync_version = data_in.readLine();
                Log.d("SYNC_VER", client_sync_version);

                // DATA IN
                String data = data_in.readLine();

                if (client_sync_version.equals("v2")) {

                    // DATA OUT
                    data_out.append(Backend.exportJson());
                    data_out.append("\n");
                    data_out.flush();
                    Log.d("DATA_OUT", "Sent");

                    Backend.parseReceivedJson(data);

                    c.callback();

                }
                restart();

            } catch (SocketException ignore) {
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void restart() {
            try {
                if (socket != null) {
                    socket.close();
                }
                serverSocket.close();
                run();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}