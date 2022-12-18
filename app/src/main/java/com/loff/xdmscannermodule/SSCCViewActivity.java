package com.loff.xdmscannermodule;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class SSCCViewActivity extends AppCompatActivity {

    private SwitchCompat switchMissing;
    private static Backend.SSCC sscc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ssccview);
        String ssccID = getIntent().getStringExtra("SSCC");

        TextView ssccTitle = findViewById(R.id.tb_ssccTitle);
        TextView ssccView = findViewById(R.id.tv_sscc_view);
        Button backButton = findViewById(R.id.btn_sscc_back);
        switchMissing = findViewById(R.id.sw_mark_missing);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < Backend.selectedManifest.ssccList.size(); i++) {
            if (ssccID.equals(Backend.selectedManifest.ssccList.get(i).ssccID)) {
                sscc = Backend.selectedManifest.ssccList.get(i);
            }
        }
        ssccTitle.setText(sscc.ssccID);

        if (sscc.dilStatus.equals("missing")) {
            switchMissing.setChecked(true);
        }

        if (sscc.highRisk) {
            sb.append("  == HIGH RISK CARTON ==\n\n");
        }
        for (int j = 0; j < sscc.articles.size(); j++) {
            sb.append("  ").append(sscc.articles.get(j).QTY).append(" x  ");
            sb.append(sscc.articles.get(j).code).append(" - [");
            sb.append(sscc.articles.get(j).desc).append("]\n\n");
        }


        backButton.setOnClickListener(v -> doClose());
        switchMissing.setOnClickListener(v -> doSwitchToggle(sscc.ssccID));

        ssccView.setText(sb.toString());

    }

    private void doSwitchToggle(String ssccID) {
        for (int i = 0; i < Backend.selectedManifest.ssccList.size(); i++) {
            if (ssccID.equals(Backend.selectedManifest.ssccList.get(i).ssccID)) {
                if (switchMissing.isChecked()) {
                    Backend.selectedManifest.ssccList.get(i).dilStatus = "missing";
                } else {
                    Backend.selectedManifest.ssccList.get(i).dilStatus = "";
                }

            }
        }
    }

    private void doClose() {
        Backend.saveData(getApplicationContext());
        this.finish();
    }
}