package com.loff.xdmscannermodule;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class SSCCViewActivity extends AppCompatActivity {

    private TextView ssccTitle;
    private TextView ssccView;
    private Button backButton;
    private SwitchCompat switchMissing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ssccview);

        Backend.SSCC sscc = (Backend.SSCC) getIntent().getSerializableExtra("SSCC");

        ssccTitle = findViewById(R.id.tb_ssccTitle);
        ssccView = findViewById(R.id.tv_sscc_view);
        backButton = findViewById(R.id.btn_sscc_back);
        switchMissing = findViewById(R.id.sw_mark_missing);

        ssccTitle.setText(sscc.ssccID);
        backButton.setOnClickListener(v -> doClose());

        StringBuilder sb = new StringBuilder();
        if (sscc.highRisk) {
            sb.append("== HIGH RISK CARTON ==\n\n");
        }
        for (int i = 0; i < sscc.articles.size(); i++) {
            sb.append(sscc.articles.get(i).QTY).append("x ");
            sb.append(sscc.articles.get(i).code).append(" - [");
            sb.append(sscc.articles.get(i).desc).append("]\n\n");
        }
        ssccView.setText(sb.toString());

    }

    private void doClose(){
        this.finish();
    }
}