package com.loff.xdmscannermodule;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MenuActivity extends AppCompatActivity {

    Button btnBeginScanning;
    TextView txtStatusText;
    SwipeRefreshLayout refreshLayout;

     public static Backend xbackend = new Backend();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        btnBeginScanning = findViewById(R.id.btn_begin_scanning);
        txtStatusText = findViewById(R.id.txt_status_text);
        refreshLayout = findViewById(R.id.refresh_layout);

        // BEGIN SCANNING BUTTON
        btnBeginScanning.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MenuActivity.this, ScannerActivity.class));
            }
        });

        // POPULATE TEXT FIELD
        if (xbackend.importJson()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Target Manifest: ");
            sb.append(xbackend.xdManifest.manifestID);
            sb.append("\n\nSSCCs: ");
            sb.append(xbackend.xdManifest.ssccList.size());
            txtStatusText.setText(sb.toString());  //TODO
        } else {
            txtStatusText.setText(R.string.jsonImportError);
        }


        // PULL TO REFRESH
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                doBackendLoad();
            }
        });
    }

    private void doBackendLoad(){
        refreshLayout.setRefreshing(false);
    }
}