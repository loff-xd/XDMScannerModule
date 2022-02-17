package com.loff.xdmscannermodule;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Executor;


public class ScannerActivity extends AppCompatActivity {

    private FloatingActionButton scanBarcodeFab;
    private FloatingActionButton cancelScanFab;

    private ConstraintLayout articleDialogLayout;
    private View darkOverlay;
    private boolean articleDialogIsVisible = false;
    private EditText textGTIN;
    private EditText textQTY;
    private TextView textArticleList;
    private Button articleSaveButton;
    private Button addButton;
    private CheckBox checkHighRisk;
    CodeScanner codeScanner;
    CodeScannerView scannerView;

    private TextView dataList;
    private TextView statusText;

    // BEEPS + HAPTICS
    MediaPlayer soundIDbeep;
    MediaPlayer soundIDwarn;
    MediaPlayer soundIDerror;
    Vibrator vibrator;
    Executor HapticRunner;
    final int H_NORMAL = 0;
    final int H_WARN = 1;
    final int H_ERROR = 2;
    final VibrationEffect normalPattern = VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        Backend.xdtMobileJsonFile = new File(this.getExternalFilesDir(null), getString(R.string.xdt_data_file));

        // BEEPS + HAPTICS
        soundIDbeep = MediaPlayer.create(this, R.raw.genericbeep);
        soundIDwarn = MediaPlayer.create(this, R.raw.warnbeep);
        soundIDerror = MediaPlayer.create(this, R.raw.errorbeep);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        HapticRunner = getMainExecutor();

        // SCAN BUTTON
        scanBarcodeFab = findViewById(R.id.fabScan);
        scanBarcodeFab.setOnClickListener(view -> openCameraInterface());

        // CANCEL BUTTON
        cancelScanFab = findViewById(R.id.fabCancel);
        cancelScanFab.setOnClickListener(view -> closeCameraInterface());

        // SAVE BUTTON
        Button saveButton = findViewById(R.id.btn_save);
        saveButton.setOnClickListener(view -> doSaveClose());

        // OVERLAY
        articleDialogLayout = findViewById(R.id.container_new_article);
        darkOverlay = findViewById(R.id.view_darken_overlay);

        // CODE SCANNER
        scannerView = findViewById(R.id.codeScanner);
        codeScanner = new CodeScanner(this, scannerView);
        codeScanner.setDecodeCallback(result -> runOnUiThread(() -> {
            if (articleDialogIsVisible){
                textGTIN.setText(result.toString());
            } else {
                checkBarcode(result.getText());
            }
            closeCameraInterface();
        }));

        // ARTICLE WINDOW
        textQTY = findViewById(R.id.entry_article_qty);
        textArticleList = findViewById(R.id.text_article_list);
        Button scanButton = findViewById(R.id.btn_scan_gtin);
        Button cancelButton = findViewById(R.id.extra_sscc_btn_cancel);
        articleSaveButton = findViewById(R.id.extra_sscc_button_save);
        addButton = findViewById(R.id.btn_add_article);
        checkHighRisk = findViewById(R.id.cb_HR);
        textGTIN = findViewById(R.id.entry_article_gtin);

        scanButton.setOnClickListener(view1 -> openCameraInterface());

        // CANCEL BUTTON + DISMISSAL
        cancelButton.setOnClickListener(view12 -> {
            articleDialogIsVisible = false;
            articleDialogLayout.setVisibility(View.GONE);
            darkOverlay.setVisibility(View.GONE);
        });

        // CREATE DATA LIST
        dataList = findViewById(R.id.tv_dataList);
        TextView tbText = findViewById(R.id.tb_text);
        tbText.setText(String.format("MANIFEST: %s", Backend.selectedManifest.manifestID));
        statusText = findViewById(R.id.tx_statusBar);
        doDataRefresh();
    }

    @Override
    protected void onStop() {
        super.onStop();
        closeCameraInterface();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, id) -> doSaveClose())
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        closeCameraInterface();
        soundIDbeep.release();
        soundIDerror.release();
        soundIDwarn.release();
        soundIDbeep = null;
        soundIDerror = null;
        soundIDwarn = null;
        super.onDestroy();
    }

    private void openCameraInterface() {
        // CHECK PERMS AND ASK IF MISSING
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            scannerView.setVisibility(View.VISIBLE);
            cancelScanFab.setVisibility(View.VISIBLE);
            scanBarcodeFab.setVisibility(View.INVISIBLE);
            codeScanner.startPreview();

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
        Context context = getApplicationContext();
        boolean matchFound = false;
        for (int i=0; i < Backend.selectedManifest.ssccList.size(); i++) {
            if (barcode.contains(Backend.selectedManifest.ssccList.get(i).ssccID)) {
                // ALREADY SCANNED
                if (Backend.selectedManifest.ssccList.get(i).scanned) {
                    matchFound = true;
                    soundIDerror.start();
                    doHaptics(H_ERROR);
                    setBG(ContextCompat.getColor(context, R.color.fail_red));
                    new AlertDialog.Builder(this)
                            .setMessage("SSCC already scanned.")
                            .setPositiveButton("Ok", null)
                            .show();
                    break;
                }

                Backend.selectedManifest.ssccList.get(i).scanned = true;
                setBG(ContextCompat.getColor(context, R.color.success_green));

                // IS HIGH RISK
                if (Backend.selectedManifest.ssccList.get(i).highRisk) {
                    soundIDwarn.start();
                    doHaptics(H_WARN);
                    statusText.setText(String.format("%s: HIGH-RISK", barcode));
                    new AlertDialog.Builder(this)
                            .setMessage("This is a high-risk carton.\n Please action accordingly.")
                            .setPositiveButton("Ok", null)
                            .show();
                } else {
                    // NORMAL SCAN
                    soundIDbeep.start();
                    doHaptics(H_NORMAL);
                    statusText.setText(String.format("%s: OK", barcode));
                }
                matchFound = true;
                break;
            }
        }

        // UNKNOWN CARTON
        if (!matchFound){
            soundIDerror.start();
            doHaptics(H_ERROR);
            setBG(ContextCompat.getColor(context, R.color.fail_red));
            statusText.setText(String.format("%s: UNKNOWN", barcode));
            new AlertDialog.Builder(this)
                    .setMessage("SSCC: " + barcode + " is not in the manifest.\nAdd as missing carton?")
                    .setPositiveButton("Yes", (dialogInterface, i) -> showUnknownSSCCDialog(barcode))
                    .setNegativeButton("No", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }

        doDataRefresh();

    }

    // REFRESH SSCC LIST
    private void doDataRefresh(){
        StringBuilder dataText = new StringBuilder();
        for (int i=0; i < Backend.selectedManifest.ssccList.size(); i++) {
            dataText.append(Backend.selectedManifest.ssccList.get(i).ssccID.substring(Backend.selectedManifest.ssccList.get(i).ssccID.length() - 4));
            if (Backend.selectedManifest.ssccList.get(i).scanned){dataText.append(" [███] [ ");} else {dataText.append(" [░░░] [ ");}

            dataText.append(fixedLengthString(Backend.selectedManifest.ssccList.get(i).ssccID, 18));
            dataText.append(" ]\n");
            dataText.append("     - ").append(Backend.selectedManifest.ssccList.get(i).description);
            if (Backend.selectedManifest.ssccList.get(i).highRisk) {dataText.append("\n     - ").append("HIGH-RISK");}
            dataText.append("\n\n");
        }
        Backend.selectedManifest.lastModified = String.valueOf(System.currentTimeMillis());
        dataList.setText(dataText.toString());
    }

    private void doSaveClose(){
        Backend.selectedManifest.lastModified = String.valueOf(System.currentTimeMillis());
        Backend.exportJsonFile();
        this.finish();
    }

    private void showUnknownSSCCDialog(String barcode) {
        // SHOW VIS
        articleDialogIsVisible = true;
        articleDialogLayout.setVisibility(View.VISIBLE);
        darkOverlay.setVisibility(View.VISIBLE);
        textQTY.setText("1");



        // SSCC CREATION
        ArrayList<Backend.Article> articleList = new ArrayList<>();

        addButton.setOnClickListener(view14 -> {
            if(!(textGTIN.getText().toString().equals("") || textQTY.getText().toString().equals(""))) {
                // NEW ARTICLE
                Backend.Article newArticle = new Backend.Article();
                newArticle.GTIN = textGTIN.getText().toString();
                newArticle.QTY = Integer.parseInt(textQTY.getText().toString());
                newArticle.highRisk = checkHighRisk.isChecked();
                newArticle.code = "";
                newArticle.desc = "";
                articleList.add(newArticle);

                // RESET FIELDS
                textGTIN.setText("");
                textQTY.setText("1");
                checkHighRisk.setChecked(false);

                // UPDATE LIST
                StringBuilder articleListOutput = new StringBuilder();
                for (int i = 0; i < articleList.size(); i++) {
                    articleListOutput.append("\n").append(articleList.get(i).QTY).append("x ").append(articleList.get(i).GTIN);
                    if (articleList.get(i).highRisk) {
                        articleListOutput.append("\n  - HIGH-RISK");
                    }
                }
                textArticleList.setText(articleListOutput.toString());
            }
        });

        articleSaveButton.setOnClickListener(view13 -> {
            Backend.SSCC newSSCC = new Backend.SSCC();
            newSSCC.ssccID = barcode;
            newSSCC.unknown = true;
            newSSCC.scanned = true;
            newSSCC.description = "Manually Added";
            newSSCC.highRisk = false;
            for (int i=0; i<articleList.size(); i++){
                if (articleList.get(i).highRisk) { newSSCC.highRisk = true; }
            }
            newSSCC.articles = articleList;
            Backend.selectedManifest.ssccList.add(newSSCC);
            doDataRefresh();

            if (codeScanner.isPreviewActive()) {
                closeCameraInterface();
            }
            articleDialogIsVisible = false;
            articleDialogLayout.setVisibility(View.GONE);
            darkOverlay.setVisibility(View.GONE);
        });
    }

    public static String fixedLengthString(String string, int length) {
        return String.format("%1$"+length+ "s", string);
    }

    Runnable normalHaptic = () -> vibrator.vibrate(normalPattern);
    Runnable warnHaptic = () -> {
        try {
            vibrator.vibrate(normalPattern);
            Thread.sleep(300);
            vibrator.vibrate(normalPattern);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    };
    Runnable errorHaptic = () -> {
        try {
            vibrator.vibrate(normalPattern);
            Thread.sleep(200);
            vibrator.vibrate(normalPattern);
            Thread.sleep(200);
            vibrator.vibrate(normalPattern);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    };

    public void doHaptics(int type) {
        switch (type){

            case H_WARN:
                HapticRunner.execute(warnHaptic);
                break;
            case H_ERROR:
                HapticRunner.execute(errorHaptic);
                break;
            case H_NORMAL:
            default:
                HapticRunner.execute(normalHaptic);
                break;
        }
    }

    private void setBG(int color) {
        runOnUiThread(() -> findViewById(R.id.tx_statusBar).setBackgroundColor(color));
    }
}