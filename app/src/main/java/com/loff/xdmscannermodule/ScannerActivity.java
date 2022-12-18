package com.loff.xdmscannermodule;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
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
import android.view.MotionEvent;
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
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.Objects;
import java.util.concurrent.ExecutionException;


public class ScannerActivity extends AppCompatActivity {

    private FloatingActionButton scanBarcodeFab;
    private FloatingActionButton cancelScanFab;
    private FloatingActionButton torchFab;

    private ConstraintLayout articleDialogLayout;
    private View darkOverlay;
    private FloatingActionButton enterBarcodeFab;
    private TextView tbText;

    private BarcodeScanner barcodeScanner;
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private CameraControl cameraControl;
    private boolean torch = false;

    private SurfaceView surfaceView;
    private SurfaceHolder holder;
    private ListAdapter adapter;
    private RecyclerView.LayoutManager layoutManager;
    private String ssccViewItemChanged = "";

    // BEEPS + HAPTICS
    private MediaPlayer soundIDbeep;
    private MediaPlayer soundIDwarn;
    private MediaPlayer soundIDerror;
    private Vibrator vibrator;
    private final int H_NORMAL = 0;
    private final int H_WARN = 1;
    private final int H_ERROR = 2;
    private final VibrationEffect normalPattern = VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE);

    private int progressCounter = 0;
    private final Size accuracy = new Size(1440, 2560);
    // private final Size speed = new Size(1080, 1920);

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        // LAYOUT
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        layoutManager = new LinearLayoutManager(this);

        // HAPTICS
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

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
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_CODE_128).build();
        barcodeScanner = BarcodeScanning.getClient(options);

        surfaceView = findViewById(R.id.overlay);
        surfaceView.setZOrderOnTop(true);
        holder = surfaceView.getHolder();

        // TAP TO REFOCUS
        previewView.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                MeteringPointFactory mf = previewView.getMeteringPointFactory();
                MeteringPoint focusPoint = mf.createPoint(view.getWidth()/2f, view.getHeight()/2f);
                cameraControl.startFocusAndMetering(new FocusMeteringAction.Builder(focusPoint).build());
                return true;
            } else {
                return false;
            }
        });

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
        adapter = new ListAdapter(this, Backend.selectedManifest.ssccList, sscc -> {
            Intent intent = new Intent(ScannerActivity.this, SSCCViewActivity.class);
            intent.putExtra("SSCC", sscc.ssccID);
            startActivity(intent);
            ssccViewItemChanged = sscc.ssccID;
        });
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(layoutManager);
        tbText = findViewById(R.id.tb_text);
    }

    private void startCamera(ProcessCameraProvider cameraProvider) {
        // Camera setup
        cameraProvider.unbindAll();
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        Preview preview = new Preview.Builder().setTargetResolution(accuracy).build(); // CHANGE FOR SHOOTING MODES

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Image analysis setup
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).setImageQueueDepth(1).build(); // CHANGE FOR SHOOTING MODES

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
        Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
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
        Backend.saveData(getApplicationContext());
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

        Paint paint = new Paint();
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
            boolean matchFound = false;
            if (barcode.length() > 16) {
                for (int i = 0; i < Backend.selectedManifest.ssccList.size(); i++) {
                    if (barcode.contains(Backend.selectedManifest.ssccList.get(i).ssccID)) {
                        // ALREADY SCANNED

                        if (Backend.selectedManifest.ssccList.get(i).scanned) {
                            matchFound = true;
                            soundIDerror.start();
                            doHaptics(H_ERROR);

                            runOnUiThread(() -> new AlertDialog.Builder(dialogContext)
                                    .setMessage("SSCC already scanned.")
                                    .setPositiveButton("Ok", null)
                                    .show());
                            break;
                        }

                        Backend.selectedManifest.ssccList.get(i).scanned = true;

                        final int scrollPos = i;
                        runOnUiThread(() -> {
                            layoutManager.scrollToPosition(scrollPos);
                            adapter.notifyItemChanged(scrollPos);
                            // MANUAL RIPPLE TODO
                        });

                        // IS HIGH RISK
                        if (Backend.selectedManifest.ssccList.get(i).highRisk) {
                            soundIDwarn.start();
                            doHaptics(H_WARN);

                            runOnUiThread(() -> new AlertDialog.Builder(dialogContext)
                                    .setMessage("This is a high-risk carton.\n Please action accordingly.")
                                    .setPositiveButton("OK", null)
                                    .show());
                        } else {
                            // NORMAL SCAN
                            soundIDbeep.start();
                            doHaptics(H_NORMAL);
                        }
                        matchFound = true;
                        break;
                    }

                }

                if (!matchFound) {
                    // UNKNOWN CARTON IS SSCC
                    soundIDerror.start();
                    doHaptics(H_ERROR);

                    runOnUiThread(() -> new AlertDialog.Builder(dialogContext)
                            .setMessage("SSCC: " + barcode + " is not in the selected manifest.\n\nPlease check it is for your store and isolate for further action.")
                            .setPositiveButton("OK", null)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show());

                }

            } else {

                // UNKNOWN CARTON NOT SSCC
                soundIDerror.start();
                doHaptics(H_ERROR);

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
            Backend.saveData(getApplicationContext());
            progressCounter = 0;
        } else {
            progressCounter++;
        }
    }

    // REFRESH SSCC LIST
    private void doDataRefresh(){
        new Thread(() -> {
            int scannedTally = 0;
            for (int i=0; i < Backend.selectedManifest.ssccList.size(); i++) {
                if (Backend.selectedManifest.ssccList.get(i).scanned) {scannedTally += 1;} // UPDATE SCAN TALLY
            }
            Backend.selectedManifest.lastModified = String.valueOf(System.currentTimeMillis()); // UPDATE LASTMODIFIED

            int finalScannedTally = scannedTally;
            runOnUiThread(() -> {
                if (!ssccViewItemChanged.equals("")) {
                    for (int i = 0; i < Backend.selectedManifest.ssccList.size(); i++) {
                        if (ssccViewItemChanged.equals(Backend.selectedManifest.ssccList.get(i).ssccID)) {
                            adapter.notifyItemChanged(i);
                        }
                    }
                }
                tbText.setText(String.format("MANIFEST: %s - %s/%s", Backend.selectedManifest.manifestID, finalScannedTally, Backend.selectedManifest.ssccList.size()));
            });

        }).start();
    }

    private void doSaveClose(){
        Backend.selectedManifest.lastModified = String.valueOf(System.currentTimeMillis());
        this.finish();
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
}