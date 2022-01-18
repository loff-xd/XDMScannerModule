package com.loff.xdmscannermodule;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.zxing.Result;

public class MainActivity extends AppCompatActivity {

    FloatingActionButton scanBarcodeFab;
    FloatingActionButton cancelScanFab;
    CodeScannerView scannerView;
    CodeScanner codeScanner;
    SoundPool sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // SCAN BUTTON
        scanBarcodeFab = findViewById(R.id.fabScan);
        scanBarcodeFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openCameraInterface();
            }
        });

        // CANCEL BUTTON
        cancelScanFab = findViewById(R.id.fabCancel);
        cancelScanFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeCameraInterface();
            }
        });

        // BEEP
        sp = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        int soundID = sp.load(this, R.raw.genericbeep, 1);

        // CODE SCANNER
        scannerView = findViewById(R.id.codeScanner);
        codeScanner = new CodeScanner(this, scannerView);
        codeScanner.setDecodeCallback(new DecodeCallback() {
            @Override
            public void onDecoded(@NonNull Result result) {
                sp.play(soundID, 1, 1, 0,0,1);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, result.getText(), Toast.LENGTH_SHORT).show();
                        closeCameraInterface();
                    }
                });
            }
        });

    }

    private void openCameraInterface() {
        scannerView.setVisibility(View.VISIBLE);
        cancelScanFab.setVisibility(View.VISIBLE);
        scanBarcodeFab.setVisibility(View.INVISIBLE);
        codeScanner.startPreview();
    }

    private void closeCameraInterface() {
        scannerView.setVisibility(View.GONE);
        cancelScanFab.setVisibility(View.INVISIBLE);
        scanBarcodeFab.setVisibility(View.VISIBLE);
        codeScanner.stopPreview();
    }
}