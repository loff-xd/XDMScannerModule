package com.loff.xdmscannermodule;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SyncActivity extends AppCompatActivity {

    TextView syncStatus;
    String statusText = "";
    String SERVER_IP = "";
    int PORT = 0;

    CodeScannerView scannerView;
    CodeScanner codeScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);

        syncStatus = findViewById(R.id.text_sync_status);
        statusUpdate(String.valueOf(System.currentTimeMillis()));

        // CODE SCANNER
        scannerView = findViewById(R.id.qrScanner);
        codeScanner = new CodeScanner(this, scannerView);
        codeScanner.setDecodeCallback(result -> runOnUiThread(() -> {
            decodeQR(result.getText());
            codeScanner.stopPreview();
        }));

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            codeScanner.startPreview();

        } else {
            ActivityCompat.requestPermissions(SyncActivity.this, new String[] {Manifest.permission.CAMERA}, 5);
            this.finish();
        }
    }

    public void decodeQR(String barcode) {
        String[] connectionDetails = barcode.split(";");

        SERVER_IP = connectionDetails[0];
        PORT = Integer.parseInt(connectionDetails[1]);

        new Thread(new netExchanger()).start();
    }

    class netExchanger implements Runnable{
        @Override
        public void run() {
            statusUpdate("Sync Started.");
            Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(SERVER_IP, PORT), 10000);
                BufferedReader data_in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String data = data_in.readLine();
                statusUpdate("\nRecveived bytes: " + data.length());
                statusUpdate("\nProcessing...");
                if (Backend.importJson(data)) {
                    statusUpdate("\nSuccessfuly updated.");
                    Backend.exportJson();
                    closeActivity();
                } else {
                    statusUpdate("\nJSON import failed");
                }

            } catch (IOException e) {
                e.printStackTrace();
                statusUpdate("Sync failed");
            }
        }

    }

    private void statusUpdate(String update){
        runOnUiThread(() -> {
            statusText += update;
            syncStatus.setText(statusText);
        });

    }

    private void closeActivity() {
        codeScanner.releaseResources();
        codeScanner = null;
        this.finish();
    }
}