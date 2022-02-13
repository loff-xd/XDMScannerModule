package com.loff.xdmscannermodule;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.util.concurrent.Executor;

public class MenuActivity extends AppCompatActivity {

    Button btnBeginScanning;
    Button btnSync;
    TextView txtStatusText;
    SwipeRefreshLayout refreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.MANAGE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MenuActivity.this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 5);
        }

        Backend.xdtMobileJsonFile = new File(this.getExternalFilesDir(null), getString(R.string.xdt_data_file));

        btnBeginScanning = findViewById(R.id.btn_begin_scanning);
        btnSync = findViewById(R.id.btn_sync);
        txtStatusText = findViewById(R.id.txt_status_text);
        refreshLayout = findViewById(R.id.refresh_layout);

        // BEGIN SCANNING BUTTON
        btnBeginScanning.setOnClickListener(view -> startActivity(new Intent(MenuActivity.this, ScannerActivity.class)));

        // POPULATE TEXT FIELD
        doBackendLoad();

        // PULL TO REFRESH
        refreshLayout.setOnRefreshListener(this::doBackendLoad);

        // SYNC BUTTON
        btnSync.setOnClickListener(view -> startActivity(new Intent(MenuActivity.this, SyncActivity.class)));
    }

    @Override
    protected void onResume() {
        doBackendLoad();
        super.onResume();
    }

    private void doBackendLoad(){
        Executor IORunner = getMainExecutor();
        IORunner.execute(() -> {
            refreshLayout.setRefreshing(true);

            if (Backend.importJsonFile()) {
                int scannedCount = 0;
                int unknownCount = 0;
                int hrCount = 0;
                for (int i=0; i<Backend.selectedManifest.ssccList.size(); i++){
                    if (Backend.selectedManifest.ssccList.get(i).scanned) { scannedCount++; }
                    if (Backend.selectedManifest.ssccList.get(i).unknown) { unknownCount++; }
                    if (Backend.selectedManifest.ssccList.get(i).highRisk) { hrCount++; }
                }

                String sb = "Target Manifest: " +
                        Backend.selectedManifest.manifestID +
                        "\n\nSSCCs: " +
                        Backend.selectedManifest.ssccList.size() +
                        " (Scanned: " + scannedCount + ")" +
                        " (Extras: " + unknownCount + ")" +
                        "\n\nHigh-Risk SSCCs: " + hrCount;
                txtStatusText.setText(sb);
                btnBeginScanning.setEnabled(true);
                if (scannedCount > 0) {btnBeginScanning.setText(R.string.resume_scanning);}
            } else {
                btnBeginScanning.setEnabled(false);
                txtStatusText.setText(R.string.jsonImportError);
            }

            refreshLayout.setRefreshing(false);
        });
    }
}