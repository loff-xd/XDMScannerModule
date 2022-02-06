package com.loff.xdmscannermodule;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Executor;


public class ScannerActivity extends AppCompatActivity {

    private FloatingActionButton scanBarcodeFab;
    private FloatingActionButton cancelScanFab;
    private CodeScannerView scannerView;
    private CodeScanner codeScanner;

    private TextView dataList;
    private TextView statusText;
    private View darkenOverlay;

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
        darkenOverlay = findViewById(R.id.view_darken_overlay);

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
        tbText.setText(String.format("MANIFEST: %s", Backend.xdManifest.manifestID));

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
        for (int i=0; i < Backend.xdManifest.ssccList.size(); i++) {
            if (Backend.xdManifest.ssccList.get(i).ssccID.contains(barcode)) {
                // ALREADY SCANNED
                if (Backend.xdManifest.ssccList.get(i).scanned) {
                    matchFound = true;
                    soundIDerror.start();
                    doHaptics(H_ERROR);
                    new AlertDialog.Builder(this)
                            .setMessage("SSCC already scanned.")
                            .setPositiveButton("Ok", null)
                            .show();
                    break;
                }

                Backend.xdManifest.ssccList.get(i).scanned = true;

                // IS HIGH RISK
                if (Backend.xdManifest.ssccList.get(i).highRisk) {
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
        for (int i=0; i < Backend.xdManifest.ssccList.size(); i++) {
            dataText.append(Backend.xdManifest.ssccList.get(i).ssccID.substring(Backend.xdManifest.ssccList.get(i).ssccID.length() - 4));
            if (Backend.xdManifest.ssccList.get(i).scanned){dataText.append(" [███] [ ");} else {dataText.append(" [░░░] [ ");}

            dataText.append(fixedLengthString(Backend.xdManifest.ssccList.get(i).ssccID, 18));
            dataText.append(" ]\n");
            dataText.append("     - ").append(Backend.xdManifest.ssccList.get(i).description);
            if (Backend.xdManifest.ssccList.get(i).highRisk) {dataText.append("\n     - ").append("HIGH-RISK");}
            dataText.append("\n\n");
        }

        dataList.setText(dataText.toString());
    }

    private void doSaveClose(){
        Backend.exportJson();
        this.finish();
    }

    private void showUnknownSSCCDialog(String barcode) {
        // CREATE WINDOW
        darkenOverlay.setVisibility(View.VISIBLE);
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.missing_carton_window, null);
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        final PopupWindow popupWindow = new PopupWindow(view, width, height, true);
        popupWindow.setElevation(10);
        popupWindow.setOutsideTouchable(false);
        popupWindow.setAnimationStyle(R.style.Animation_AppCompat_Dialog);
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);

        // GTIN SCANNER + SUPPORT ELEMENTS
        EditText textGTIN = view.findViewById(R.id.entry_article_gtin);

        CodeScannerView gtinScannerView = view.findViewById(R.id.codescanner_gtin);
        CodeScanner gtinCodeScanner = new CodeScanner(this, gtinScannerView);

        // TEXT ELEMENTS
        EditText textQTY = view.findViewById(R.id.entry_article_qty);
        TextView textArticleList = view.findViewById(R.id.text_article_list);
        textQTY.setText("1");

        // BUTTONS
        Button scanButton = view.findViewById(R.id.btn_scan_gtin);
        Button cancelButton = view.findViewById(R.id.extra_sscc_btn_cancel);
        Button saveButton = view.findViewById(R.id.extra_sscc_button_save);
        Button addButton = view.findViewById(R.id.btn_add_article);
        CheckBox checkHighRisk = view.findViewById(R.id.cb_HR);

        // CODE SCANNER
        gtinCodeScanner.setDecodeCallback(result -> runOnUiThread(() -> {
            textGTIN.setText(result.getText());
            gtinCodeScanner.stopPreview();
            gtinScannerView.setVisibility(View.GONE);
            scanButton.setText(R.string.scan);
            doHaptics(H_NORMAL);
        }));

        // SCAN BUTTON
        scanButton.setOnClickListener(view1 -> {
            if (gtinCodeScanner.isPreviewActive()) {
                gtinCodeScanner.stopPreview();
                gtinScannerView.setVisibility(View.GONE);
                scanButton.setText(R.string.scan);
            } else {
                gtinScannerView.setVisibility(View.VISIBLE);
                gtinCodeScanner.startPreview();
                scanButton.setText(R.string.stop);
            }
        });

        // CANCEL BUTTON + DISMISSAL
        cancelButton.setOnClickListener(view12 -> {
            if (gtinCodeScanner.isPreviewActive()) {
                gtinCodeScanner.stopPreview();
                gtinScannerView.setVisibility(View.GONE);
            }
            darkenOverlay.setVisibility(View.GONE);
            popupWindow.dismiss();
        });

        popupWindow.setOnDismissListener(() -> {
            if (gtinCodeScanner.isPreviewActive()) {
                gtinCodeScanner.stopPreview();
                gtinScannerView.setVisibility(View.GONE);
            }
            darkenOverlay.setVisibility(View.GONE);
            popupWindow.dismiss();
        });

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

        saveButton.setOnClickListener(view13 -> {
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
            Backend.xdManifest.ssccList.add(newSSCC);
            doDataRefresh();

            if (gtinCodeScanner.isPreviewActive()) {
                gtinCodeScanner.stopPreview();
                gtinScannerView.setVisibility(View.GONE);
            }
            darkenOverlay.setVisibility(View.GONE);
            popupWindow.dismiss();
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
}