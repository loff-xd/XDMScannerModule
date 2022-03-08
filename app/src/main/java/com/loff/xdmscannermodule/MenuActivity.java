package com.loff.xdmscannermodule;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;

public class MenuActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, View.OnTouchListener {

    Button btnBeginScanning;
    Button btnSync;
    TextView txtStatusText;
    SwipeRefreshLayout refreshLayout;
    Spinner manifestSpinner;
    boolean userAction = false;
    ArrayAdapter<String> xdManifestArrayAdapter;

    final int REQ_CODE_SCANNER = 101;


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
        btnBeginScanning.setOnClickListener(view -> startActivityForResult(new Intent(MenuActivity.this, ScannerActivity.class), REQ_CODE_SCANNER));

        // MANIFEST SELECTOR
        manifestSpinner = findViewById(R.id.spinner_mainfest_selector);

        // PULL TO REFRESH
        refreshLayout.setOnRefreshListener(this::doBackendLoad);

        // LOAD BACKEND
        doBackendLoad();

        // SYNC BUTTON
        btnSync.setOnClickListener(view -> startActivityForResult(new Intent(MenuActivity.this, SyncActivity.class), REQ_CODE_SCANNER));

        // SPINNER ONCLICK
        manifestSpinner.setOnTouchListener(this);
        manifestSpinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onPause(){
        Backend.exportJsonFile();
        Log.v("ACTION", "ONPAUSE SAVE");
        super.onPause();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        userAction = true;
        return false;
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        if (userAction) {
            Backend.changeManifest(Backend.manifest_list.get(i));
            runOnUiThread(this::interfaceUpdate);
            userAction = false;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQ_CODE_SCANNER) {
            runOnUiThread(this::interfaceUpdate);
        }
    }

    private void doBackendLoad(){
        refreshLayout.setRefreshing(true);
        new Thread(() -> {
            Log.v("ACTION", "backend_load");
            boolean loadSuccess = Backend.importJsonFile();

            if (loadSuccess) {
                runOnUiThread(this::interfaceUpdate);
            } else {
                runOnUiThread(() -> { txtStatusText.setText(R.string.jsonImportError);
                    refreshLayout.setRefreshing(false);
                    btnBeginScanning.setEnabled(false);
                });
            }

        }).start();
    }

    public void interfaceUpdate(){
        refreshLayout.setRefreshing(true);
        Log.v("ACTION", "interface_update");
        xdManifestArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, Backend.manifest_list);
        manifestSpinner.setAdapter(xdManifestArrayAdapter);

        if (Backend.selectedManifest != null && Backend.manifest_list.size() != 0){
            btnBeginScanning.setEnabled(true);

            int scannedCount = 0;
            int unknownCount = 0;
            int hrCount = 0;
            for (int i = 0; i < Backend.selectedManifest.ssccList.size(); i++) {
                if (Backend.selectedManifest.ssccList.get(i).scanned) {
                    scannedCount++;
                }
                if (Backend.selectedManifest.ssccList.get(i).unknown) {
                    unknownCount++;
                }
                if (Backend.selectedManifest.ssccList.get(i).highRisk) {
                    hrCount++;
                }
            }


                        for (int i = 0; i < Backend.manifest_list.size(); i++) {
                            if (Backend.selectedManifest.manifestID.equals(Backend.manifest_list.get(i))) {
                                manifestSpinner.setSelection(i);
                                break;
                            }
                        }

            String sb = "SSCCs: " +
                    Backend.selectedManifest.ssccList.size() +
                    " (Scanned: " + scannedCount + ")" +
                    " (Extras: " + unknownCount + ")" +
                    "\n\nHigh-Risk SSCCs: " + hrCount +
                    "\n\nTotal Manifests: " + Backend.manifests.size();

            txtStatusText.setText(sb);
            if (scannedCount > 0) {
                btnBeginScanning.setText(R.string.resume_scanning);
            } else {
                btnBeginScanning.setText(R.string.begin_scanning);
            }

        } else {
            btnBeginScanning.setEnabled(false);
        }

        runOnUiThread(() -> refreshLayout.setRefreshing(false));
    }
}