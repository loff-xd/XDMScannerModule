package com.loff.xdmscannermodule;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.Image;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;


public class ScannerActivity extends AppCompatActivity {

    private FloatingActionButton scanBarcodeFab;
    private FloatingActionButton cancelScanFab;

    private ConstraintLayout articleDialogLayout;
    private View darkOverlay;
    private EditText textGTIN;
    private EditText textQTY;
    private TextView textArticleList;
    private Button articleSaveButton;
    private Button addButton;
    private CheckBox checkHighRisk;
    private FloatingActionButton enterBarcodeFab;
    TextView tbText;

    BarcodeScannerOptions options;
    BarcodeScanner barcodeScanner;
    PreviewView previewView;
    ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

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

    private int progressCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        // HAPTICS
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        HapticRunner = getMainExecutor();

        // SCAN BUTTON
        scanBarcodeFab = findViewById(R.id.fabScan);
        scanBarcodeFab.setOnClickListener(view -> openCameraInterface());

        // CANCEL BUTTON
        cancelScanFab = findViewById(R.id.fabCancel);
        cancelScanFab.setOnClickListener(view -> closeCameraInterface());

        // MANUAL ENTRY BUTTON
        enterBarcodeFab = findViewById(R.id.fabEnterCode);
        enterBarcodeFab.setOnClickListener(view -> barcodeMaunalEntry());

        // SAVE BUTTON
        Button saveButton = findViewById(R.id.btn_save);
        saveButton.setOnClickListener(view -> doSaveClose());

        // OVERLAY
        articleDialogLayout = findViewById(R.id.container_new_article);
        darkOverlay = findViewById(R.id.view_darken_overlay);

        // CODE SCANNER
        previewView = findViewById(R.id.previewView);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        options = new BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_CODE_128).build();
        barcodeScanner = BarcodeScanning.getClient(options);

        // ARTICLE WINDOW
        textQTY = findViewById(R.id.entry_article_qty);
        textArticleList = findViewById(R.id.text_article_list);
        Button scanButton = findViewById(R.id.btn_scan_gtin);
        Button cancelButton = findViewById(R.id.extra_sscc_btn_cancel);
        articleSaveButton = findViewById(R.id.extra_sscc_button_save);
        addButton = findViewById(R.id.btn_add_article);
        checkHighRisk = findViewById(R.id.cb_HR);
        textGTIN = findViewById(R.id.entry_article_gtin);

        scanButton.setOnClickListener(view -> openCameraInterface());

        // CANCEL BUTTON + DISMISSAL
        cancelButton.setOnClickListener(view12 -> {
            articleDialogLayout.setVisibility(View.GONE);
            darkOverlay.setVisibility(View.GONE);
        });

        // CREATE DATA LIST
        dataList = findViewById(R.id.tv_dataList);
        tbText = findViewById(R.id.tb_text);
        statusText = findViewById(R.id.tx_statusBar);
    }

    private void startCamera(ProcessCameraProvider cameraProvider) {
        // Camera setup
        cameraProvider.unbindAll();
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        Preview preview = new Preview.Builder().build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Image analysis setup
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();

        imageAnalysis.setAnalyzer(getMainExecutor(), imageProxy -> {
            // Barcode scanning
            @SuppressLint("UnsafeOptInUsageError") Image frame = imageProxy.getImage();
            if (frame != null) {
                InputImage image = InputImage.fromMediaImage(frame, imageProxy.getImageInfo().getRotationDegrees());
                barcodeScanner.process(image)
                        .addOnSuccessListener(barcodes -> {
                            for (Barcode barcode: barcodes) {
                                Log.v("RESULT: ", barcode.getRawValue());
                                closeCameraInterface();
                            }
                        })
                        .addOnCompleteListener(task -> imageProxy.close());
            }
        });

        // Begin + bind
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }


    @Override
    protected void onStart(){
        super.onStart();
        soundIDbeep = MediaPlayer.create(this, R.raw.genericbeep);
        soundIDwarn = MediaPlayer.create(this, R.raw.warnbeep);
        soundIDerror = MediaPlayer.create(this, R.raw.errorbeep);
        doDataRefresh();
    }

    @Override
    public void onStop(){
        super.onStop();
        Log.v("ScannerActivity", "ONSTOP RELEASE + SAVE");
        closeCameraInterface();
        soundIDbeep.release();
        soundIDerror.release();
        soundIDwarn.release();
        soundIDbeep = null;
        soundIDerror = null;
        soundIDwarn = null;
        if (Backend.manifests.size() > 0) {
            Backend.exportJsonAsync(getApplicationContext());
        }
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

    private void openCameraInterface() {
        // CHECK PERMS AND ASK IF MISSING
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            previewView.setVisibility(View.VISIBLE);
            cancelScanFab.setVisibility(View.VISIBLE);
            scanBarcodeFab.setVisibility(View.INVISIBLE);

            cameraProviderFuture.addListener(() -> {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    startCamera(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }, ContextCompat.getMainExecutor(this));

        } else {
            ActivityCompat.requestPermissions(ScannerActivity.this, new String[] {Manifest.permission.CAMERA}, 5);
        }
    }

    private void barcodeMaunalEntry() {
        AlertDialog.Builder manualDialog = new AlertDialog.Builder(this);
        manualDialog.setTitle("Enter carton SSCC:\n(Including leading 00)");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        manualDialog.setView(input);

        manualDialog.setPositiveButton("OK", (dialogInterface, i) -> checkBarcode(input.getText().toString()));
        manualDialog.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.cancel());

        manualDialog.show();
    }

    private void closeCameraInterface() {
        try {
            ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
            cameraProvider.unbindAll();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        previewView.setVisibility(View.GONE);
        cancelScanFab.setVisibility(View.INVISIBLE);
        scanBarcodeFab.setVisibility(View.VISIBLE);
    }

    private void checkBarcode(String barcode) {
        Context dialogContext = this;
        new Thread(() -> {
            Context context = getApplicationContext();
            boolean matchFound = false;
            if (barcode.startsWith("00") && barcode.length() > 16) {
                for (int i = 0; i < Backend.selectedManifest.ssccList.size(); i++) {
                    if (barcode.contains(Backend.selectedManifest.ssccList.get(i).ssccID)) {
                        // ALREADY SCANNED
                        if (Backend.selectedManifest.ssccList.get(i).scanned) {
                            matchFound = true;
                            soundIDerror.start();
                            doHaptics(H_ERROR);
                            setBG(ContextCompat.getColor(context, R.color.fail_red));

                            runOnUiThread(() -> new AlertDialog.Builder(dialogContext)
                                    .setMessage("SSCC already scanned.")
                                    .setPositiveButton("Ok", null)
                                    .show());
                            break;
                        }

                        Backend.selectedManifest.ssccList.get(i).scanned = true;
                        setBG(ContextCompat.getColor(context, R.color.success_green));

                        // IS HIGH RISK
                        if (Backend.selectedManifest.ssccList.get(i).highRisk) {
                            soundIDwarn.start();
                            doHaptics(H_WARN);
                            statusText.setText(String.format("%s: HIGH-RISK", barcode));

                            runOnUiThread(() -> new AlertDialog.Builder(dialogContext)
                                    .setMessage("This is a high-risk carton.\n Please action accordingly.")
                                    .setPositiveButton("Ok", null)
                                    .show());
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

                if (!matchFound) {
                    // UNKNOWN CARTON IS SSCC
                    soundIDerror.start();
                    doHaptics(H_ERROR);
                    setBG(ContextCompat.getColor(context, R.color.fail_red));
                    statusText.setText(String.format("%s: UNKNOWN", barcode));

                    runOnUiThread(() -> new AlertDialog.Builder(dialogContext)
                            .setMessage("SSCC: " + barcode + " is not in the manifest.\n\nIf it belongs to your store, would you like to add as missing carton?")
                            .setPositiveButton("Yes", (dialogInterface, i) -> showUnknownSSCCDialog(barcode))
                            .setNegativeButton("No", null)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show());

                }

            } else {

                // UNKNOWN CARTON NOT SSCC
                soundIDerror.start();
                doHaptics(H_ERROR);
                setBG(ContextCompat.getColor(context, R.color.fail_red));
                statusText.setText(String.format("%s: UNKNOWN", barcode));

                runOnUiThread(() -> new AlertDialog.Builder(dialogContext)
                                .setMessage("SSCC: " + barcode + " isn't a SSCC")
                                .setNegativeButton("OK", null)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show());
            }

            doDataRefresh();
        }).start();

        // SAVE PROGRESS EVERY 3 CARTONS
        if (progressCounter > 2) {
            Backend.exportJsonAsync(getApplicationContext());
            progressCounter = 0;
        } else {
            progressCounter++;
        }
    }

    // REFRESH SSCC LIST
    private void doDataRefresh(){
        new Thread(() -> {
            int scannedTally = 0;
            StringBuilder dataText = new StringBuilder();
            for (int i=0; i < Backend.selectedManifest.ssccList.size(); i++) {
                dataText.append(Backend.selectedManifest.ssccList.get(i).ssccID.substring(Backend.selectedManifest.ssccList.get(i).ssccID.length() - 4));
                if (Backend.selectedManifest.ssccList.get(i).scanned){dataText.append(" [███] [ "); scannedTally+=1;} else {dataText.append(" [░░░] [ ");}

                dataText.append(fixedLengthString(Backend.selectedManifest.ssccList.get(i).ssccID));
                dataText.append(" ]\n");
                dataText.append("     - ").append(Backend.selectedManifest.ssccList.get(i).description);
                if (Backend.selectedManifest.ssccList.get(i).highRisk) {dataText.append("\n     - ").append("HIGH-RISK");}
                dataText.append("\n\n");
            }
            Backend.selectedManifest.lastModified = String.valueOf(System.currentTimeMillis());

            int finalScannedTally = scannedTally;
            String dataListText = dataText.toString();
            runOnUiThread(() -> {
                dataList.setText(dataListText);
                tbText.setText(String.format("MANIFEST: %s - %s/%s", Backend.selectedManifest.manifestID, finalScannedTally, Backend.selectedManifest.ssccList.size()));
            });

        }).start();
    }

    private void doSaveClose(){
        Backend.selectedManifest.lastModified = String.valueOf(System.currentTimeMillis());
        this.finish();
    }

    private void showUnknownSSCCDialog(String barcode) {
        // SHOW VIS
        articleDialogLayout.setVisibility(View.VISIBLE);
        darkOverlay.setVisibility(View.VISIBLE);
        enterBarcodeFab.setVisibility(View.GONE);
        textQTY.setText("1");



        // SSCC CREATION
        ArrayList<Backend.Article> articleList = new ArrayList<>();

        addButton.setOnClickListener(view -> {
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

        articleSaveButton.setOnClickListener(view -> {
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
            Backend.syncSelectedManifestToDB();
            doDataRefresh();

            articleDialogLayout.setVisibility(View.GONE);
            darkOverlay.setVisibility(View.GONE);
            enterBarcodeFab.setVisibility(View.VISIBLE);
        });
    }

    private static String fixedLengthString(String string) {
        // FORMAT SSCC TO 18 DIGITS
        return String.format("%1$"+ 18 + "s", string);
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

    private void doHaptics(int type) {
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