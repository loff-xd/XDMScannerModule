package com.loff.xdmscannermodule;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;

public class SyncActivity extends AppCompatActivity {

    TextView syncStatus;
    TextView ipText;
    String statusText = "";
    int PORT = 7700;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);

        syncStatus = findViewById(R.id.text_sync_status);
        ipText = findViewById(R.id.text_ip_addr);

        // NETCODE
        new Thread(new netExchanger()).start();
    }

    class netExchanger implements Runnable{
        @Override
        public void run() {
            statusUpdate("\nWaiting for connection.");
            ServerSocket serverSocket;

            Context context = getApplicationContext();
            WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            @SuppressWarnings("deprecation") String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
            statusUpdate("\nIP: " + ip);
            statusUpdate("\nPort: " + PORT);
            ipText.setText(ip);

            try {
                serverSocket = new ServerSocket(PORT);
                Socket socket = serverSocket.accept();

                BufferedReader data_in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Writer data_out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                // TIMESTAMP IN
                String timestamp_in = data_in.readLine();

                // DATA IN
                String data = data_in.readLine();
                statusUpdate("\nRecveived bytes: " + data.length());

                // TIMESTAMP OUT
                String timestamp_out = "0";
                for (int i=0; i<Backend.manifests.size(); i++) {
                    if (Long.parseLong(Backend.manifests.get(i).lastModified) > Long.parseLong(timestamp_out)) {
                        timestamp_out = Backend.manifests.get(i).lastModified;
                    }
                }
                data_out.append(timestamp_out);
                data_out.append("\n");
                data_out.flush();

                // DATA OUT
                data_out.append(Backend.exportJson());
                data_out.append("\n");
                data_out.flush();
                Log.d("DATA_OUT", "Sent bytes: " + Backend.exportJson().length());

                // DATA CLOSE
                socket.close();
                serverSocket.close();

                statusUpdate("\nProcessing...");

                Log.d("SYNC", timestamp_in + " > " + timestamp_out);

                if (Long.parseLong(timestamp_in) > Long.parseLong(timestamp_out)) {
                    if (Backend.importJson(data)) {
                        Log.d("SYNC", "DOING UPDATE");
                        statusUpdate("\nSuccessfuly updated.");
                        Backend.exportJsonFile();
                        closeActivity();
                    } else {
                        statusUpdate("\nJSON import failed.");
                    }
                } else {
                    Log.d("SYNC", "UP TO DATE");
                    statusUpdate("\nNo need for update.");
                }

            } catch (IOException e) {
                e.printStackTrace();
                statusUpdate("\nSync failed.");
            }
        }

    }

    private void statusUpdate(String update){
        runOnUiThread(() -> {
            statusText += update;
            syncStatus.setText(statusText);
        });

    }

    private void closeActivity() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.finish();
    }
}