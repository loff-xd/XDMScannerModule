package com.loff.xdmscannermodule;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;

public class MenuActivity extends AppCompatActivity {

    Button btnBeginScanning;
    Button btnSync;
    TextView txtStatusText;
    SwipeRefreshLayout refreshLayout;
    ArrayAdapter<String> xdManifestArrayAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.MANAGE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MenuActivity.this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 5);
        }

        Backend.xdtMobileJsonFile = new File(this.getExternalFilesDir(null), getString(R.string.xdt_data_file));
        Backend.xdtMobileJsonTempFile = new File(this.getExternalFilesDir(null), getString(R.string.xdt_data_temp_file));

        btnBeginScanning = findViewById(R.id.btn_begin_scanning);
        btnSync = findViewById(R.id.btn_sync);
        txtStatusText = findViewById(R.id.txt_status_text);
        refreshLayout = findViewById(R.id.refresh_layout);


        // BEGIN SCANNING BUTTON
        btnBeginScanning.setOnClickListener(view -> {
            if (!refreshLayout.isRefreshing()) startActivity(new Intent(MenuActivity.this, ScannerActivity.class));
        });
        // PULL TO REFRESH
        refreshLayout.setOnRefreshListener(this::interfaceUpdate);

        // LOAD BACKEND
        doBackendLoad();

        // SYNC BUTTON
        btnSync.setOnClickListener(view -> startActivity(new Intent(MenuActivity.this, SyncActivity.class)));
    }

    @Override
    public void onStop(){
        super.onStop();
        Log.v("MenuActivity", "ONSTOP SAVE");
        if (Backend.manifests.size() > 0) {
            Backend.exportJsonAsync(getApplicationContext());
        }
    }

    @Override
    public void onRestart(){
        super.onRestart();
        interfaceUpdate();
    }

    private void doBackendLoad(){
        if (Backend.manifests.size() == 0) {
            refreshLayout.setRefreshing(true);
            new Thread(() -> {
                Log.v("MenuActivity", "backend_load");
                boolean loadSuccess = Backend.importJsonFile();

                if (loadSuccess) {
                    runOnUiThread(this::interfaceUpdate);
                } else {
                    runOnUiThread(() -> {
                        txtStatusText.setText(R.string.jsonImportError);
                        refreshLayout.setRefreshing(false);
                        btnBeginScanning.setEnabled(false);
                    });
                }
            }).start();
        } else {
            interfaceUpdate();
        }
    }

    public void interfaceUpdate(){
        refreshLayout.setRefreshing(true);
        Log.v("MenuActivity", "interface_update");

        if (Backend.selectedManifest != null && Backend.manifest_list.size() != 0){
            xdManifestArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, Backend.manifest_list);
            btnBeginScanning.setEnabled(true);

            int scannedCount = 0;
            int hrCount = 0;
            for (int i = 0; i < Backend.selectedManifest.ssccList.size(); i++) {
                if (Backend.selectedManifest.ssccList.get(i).scanned) {
                    scannedCount++;
                }
                if (Backend.selectedManifest.ssccList.get(i).highRisk) {
                    hrCount++;
                }
            }


                        for (int i = 0; i < Backend.manifest_list.size(); i++) {
                            if (Backend.selectedManifest.manifestID.equals(Backend.manifest_list.get(i))) {
                                break;
                            }
                        }

            String sb = "Manifest: " + Backend.selectedManifest.manifestID +
                    "\n\n\nTotal SSCCs: " + Backend.selectedManifest.ssccList.size() +
                    "\n\n    - Scanned: " + scannedCount +
                    "\n    - High-Risk: " + hrCount;

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
}