package com.loff.xdmscannermodule;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class ScannerActivity extends AppCompatActivity {

    private FloatingActionButton scanBarcodeFab;
    private FloatingActionButton cancelScanFab;
    private CodeScannerView scannerView;
    private CodeScanner codeScanner;

    private RecyclerView recyclerView;
    private RecyclerView.Adapter RVadapter;
    private RecyclerView.LayoutManager RVlayoutManager;

    Backend xbackend = MenuActivity.xbackend;
    ArrayList<SSCCCard> ssccCards;

    // BEEP
    MediaPlayer soundIDbeep;
    MediaPlayer soundIDwarn;
    MediaPlayer soundIDerror;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        // BEEPS
        soundIDbeep = MediaPlayer.create(this, R.raw.genericbeep);
        soundIDwarn = MediaPlayer.create(this, R.raw.warnbeep);
        soundIDerror = MediaPlayer.create(this, R.raw.errorbeep);

        // SCAN BUTTON
        scanBarcodeFab = findViewById(R.id.fabScan);
        scanBarcodeFab.setOnClickListener(view -> openCameraInterface());

        // CANCEL BUTTON
        cancelScanFab = findViewById(R.id.fabCancel);
        cancelScanFab.setOnClickListener(view -> closeCameraInterface());

        // CODE SCANNER
        scannerView = findViewById(R.id.codeScanner);
        codeScanner = new CodeScanner(this, scannerView);
        codeScanner.setDecodeCallback(result -> runOnUiThread(() -> {
            checkBarcode(result.getText());
            closeCameraInterface();
        }));

        // CREATE DATA LIST
        ssccCards = new ArrayList<SSCCCard>(); //TODO POPULATE
        ssccCards.add(new SSCCCard(R.drawable.ic_unknown, "40935803498533345", "1 Item", true));
        ssccCards.add(new SSCCCard(R.drawable.ic_scanned, "04983579438759438", "5 Items", false));

        recyclerView = findViewById(R.id.rcv_data_container);
        recyclerView.setHasFixedSize(true);
        RVlayoutManager = new LinearLayoutManager(this);
        RVadapter = new SSCCAdapter(ssccCards);

        recyclerView.setLayoutManager(RVlayoutManager);
        recyclerView.setAdapter(RVadapter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeCameraInterface();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeCameraInterface();

        soundIDbeep.release();
        soundIDerror.release();
        soundIDwarn.release();
        soundIDbeep = null;
        soundIDerror = null;
        soundIDwarn = null;
    }

    private void openCameraInterface() {
        // CHECK PERMS AND ASK IF MISSING
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {

            codeScanner.startPreview();
            scannerView.setVisibility(View.VISIBLE);
            cancelScanFab.setVisibility(View.VISIBLE);
            scanBarcodeFab.setVisibility(View.INVISIBLE);

        } else {
            ActivityCompat.requestPermissions(ScannerActivity.this, new String[] {Manifest.permission.CAMERA}, 5);
        }
    }

    private void closeCameraInterface() {
        scannerView.setVisibility(View.GONE);
        cancelScanFab.setVisibility(View.INVISIBLE);
        scanBarcodeFab.setVisibility(View.VISIBLE);
        codeScanner.stopPreview();
    }

    private void checkBarcode(String barcode) {
        System.out.println(barcode);
        soundIDbeep.start();
    }
}