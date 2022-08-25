package com.loff.xdmscannermodule;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.InputType;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
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

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;


public class ScannerActivity extends AppCompatActivity {

    private FloatingActionButton scanBarcodeFab;
    private FloatingActionButton cancelScanFab;
    private FloatingActionButton torchFab;

    private ConstraintLayout articleDialogLayout;
    private View darkOverlay;
    private FloatingActionButton enterBarcodeFab;
    TextView tbText;

    BarcodeScannerOptions options;
    BarcodeScanner barcodeScanner;
    PreviewView previewView;
    ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    Camera camera;
    CameraControl cameraControl;
    boolean torch = false;

    SurfaceView surfaceView;
    SurfaceHolder holder;
    Paint paint;

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

        // CANCEL BUTTON
        torchFab = findViewById(R.id.fabTorch);
        torchFab.setOnClickListener(view -> toggleTorch());

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

        surfaceView = findViewById(R.id.overlay);
        surfaceView.setZOrderOnTop(true);
        holder = surfaceView.getHolder();

        // CODE SCANNER PREVIEW
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                holder.setFormat(PixelFormat.TRANSPARENT);
                drawOverlay();
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
            }
        });

        // ARTICLE WINDOW
        Button scanButton = findViewById(R.id.btn_scan_gtin);
        Button cancelButton = findViewById(R.id.extra_sscc_btn_cancel);
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
        Preview preview = new Preview.Builder().setTargetResolution(new Size(1080, 1920)).build();

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
                                if (Objects.requireNonNull(barcode.getDisplayValue()).startsWith("00")) {
                                    checkBarcode(barcode.getDisplayValue());
                                    closeCameraInterface();
                                }
                            }
                        })
                        .addOnCompleteListener(task -> imageProxy.close());
            }
        });

        // Begin + bind
        camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
        cameraControl = camera.getCameraControl();
        cameraControl.enableTorch(torch);
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
            surfaceView.setVisibility(View.VISIBLE);
            cancelScanFab.setVisibility(View.VISIBLE);
            torchFab.setVisibility(View.VISIBLE);
            scanBarcodeFab.setVisibility(View.INVISIBLE);
            enterBarcodeFab.setVisibility(View.INVISIBLE);

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

    public void drawOverlay() {
        Canvas canvas = holder.lockCanvas();

        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#FF0000"));
        paint.setStrokeWidth(6);

        int crosshairSize = 35;
        int centre_x = canvas.getWidth() / 2;
        int centre_y = canvas.getHeight() / 2;

        canvas.drawLine(centre_x - crosshairSize, centre_y, centre_x + crosshairSize, centre_y, paint);
        canvas.drawLine(centre_x, centre_y - crosshairSize, centre_x, centre_y + crosshairSize, paint);

        holder.unlockCanvasAndPost(canvas);
    }

    private void barcodeMaunalEntry() {
        AlertDialog.Builder manualDialog = new AlertDialog.Builder(this);
        manualDialog.setTitle("Enter carton SSCC:\n(Including the leading 00!)");

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
        surfaceView.setVisibility(View.GONE);
        cancelScanFab.setVisibility(View.INVISIBLE);
        torchFab.setVisibility(View.INVISIBLE);
        scanBarcodeFab.setVisibility(View.VISIBLE);
        enterBarcodeFab.setVisibility(View.VISIBLE);
    }

    private void checkBarcode(String barcode) {
        Context dialogContext = this;
        new Thread(() -> {
            Context context = getApplicationContext();
            boolean matchFound = false;
            if (barcode.length() > 16) {
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
                                    .setPositiveButton("OK", null)
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
                            //.setMessage("SSCC: " + barcode + " is not in the manifest.\n\nIf it belongs to your store, would you like to add as missing carton?")
                            .setMessage("SSCC: " + barcode + " is not in the selected manifest.\n\nPlease check it is for your store and isolate for further action.")
                            //.setPositiveButton("Yes", (dialogInterface, i) -> showUnknownSSCCDialog(barcode))
                            //.setNegativeButton("No", null)
                            .setPositiveButton("OK", null)
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
                                .setMessage("Barcode: " + barcode + " isn't a SSCC")
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

    private void toggleTorch(){
        torch = !torch;
        cameraControl.enableTorch(torch);
    }

    private void doHaptics(int type) {
        switch (type){

            case H_WARN:
                new Thread(warnHaptic).start();
                break;
            case H_ERROR:
                new Thread(errorHaptic).start();
                break;
            case H_NORMAL:
            default:
                new Thread(normalHaptic).start();
                break;
        }
    }

    private void setBG(int color) {
        runOnUiThread(() -> findViewById(R.id.tx_statusBar).setBackgroundColor(color));
    }
}