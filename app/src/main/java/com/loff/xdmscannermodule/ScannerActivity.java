package com.loff.xdmscannermodule;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;


public class ScannerActivity extends AppCompatActivity {

    private FloatingActionButton scanBarcodeFab;
    private FloatingActionButton cancelScanFab;
    private CodeScannerView scannerView;
    private CodeScanner codeScanner;

    private TextView dataList;
    private TextView statusText;

    // BEEP
    MediaPlayer soundIDbeep;
    MediaPlayer soundIDwarn;
    MediaPlayer soundIDerror;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        Backend.xdtMobileJsonFile = new File(this.getExternalFilesDir(null), getString(R.string.xdt_data_file));

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

        // SAVE BUTTON
        Button saveButton = findViewById(R.id.btn_save);
        saveButton.setOnClickListener(view -> doSaveClose());

        // CODE SCANNER
        scannerView = findViewById(R.id.codeScanner);
        codeScanner = new CodeScanner(this, scannerView);
        codeScanner.setDecodeCallback(result -> runOnUiThread(() -> {
            checkBarcode(result.getText());
            closeCameraInterface();
        }));

        // CREATE DATA LIST
        dataList = findViewById(R.id.tv_dataList);

        TextView tbText = findViewById(R.id.tb_text);
        tbText.setText(String.format("Manifest %s", Backend.xdManifest.manifestID));

        statusText = findViewById(R.id.tx_statusBar);
        doDataRefresh();
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
        boolean matchFound = false;
        statusText.setText(barcode);
        for (int i=0; i < Backend.xdManifest.ssccList.size(); i++) {
            if (Backend.xdManifest.ssccList.get(i).ssccID.contains(barcode)) {
                Backend.xdManifest.ssccList.get(i).scanned = true;
                if (Backend.xdManifest.ssccList.get(i).highRisk) {
                    soundIDwarn.start();
                    new AlertDialog.Builder(this)
                            .setMessage("This is a high-risk carton.")
                            .setPositiveButton("Ok", null)
                            .show();
                } else {
                    soundIDbeep.start();
                }
                matchFound = true;
                break;
            }
        }

        if (!matchFound){
            soundIDerror.start();
            // TODO PROMPT NEW Entry
            new AlertDialog.Builder(this)
                    .setMessage("SSCC: " + barcode + " is not in the manifest.\nAdd as missing carton?")
                    .setPositiveButton("No", null)
                    .setNegativeButton("No", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }

        doDataRefresh();

    }

    private void doDataRefresh(){
        StringBuilder dataText = new StringBuilder();
        for (int i=0; i < Backend.xdManifest.ssccList.size(); i++) {
            if (Backend.xdManifest.ssccList.get(i).scanned){dataText.append("[███] ");} else {dataText.append("[░░░] ");}
            dataText.append(Backend.xdManifest.ssccList.get(i).ssccID.substring(Backend.xdManifest.ssccList.get(i).ssccID.length() - 4));
            dataText.append(" - ").append(fixedLengthString(Backend.xdManifest.ssccList.get(i).ssccID, 18));
            dataText.append("\n");
            dataText.append("              - ").append(Backend.xdManifest.ssccList.get(i).description);
            if (Backend.xdManifest.ssccList.get(i).highRisk) {dataText.append("\n              - ").append("High Risk");}
            dataText.append("\n\n");
        }

        dataList.setText(dataText.toString());
    }

    private void doSaveClose(){
        Backend.exportJson();
        this.finish();
    }

    public static String fixedLengthString(String string, int length) {
        return String.format("%1$"+length+ "s", string);
    }
}