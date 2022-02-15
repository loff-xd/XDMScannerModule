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
    String statusText = "";
    int PORT = 7700;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);

        syncStatus = findViewById(R.id.text_sync_status);
        statusUpdate(String.valueOf(System.currentTimeMillis()));

        // NETCODE
        new Thread(new netExchanger()).start();
    }

    class netExchanger implements Runnable{
        @Override
        public void run() {
            statusUpdate("\nSync Started.");
            ServerSocket serverSocket;

            Context context = getApplicationContext();
            WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
            statusUpdate("\n" + ip);
            statusUpdate("\nPort: " + PORT);

            try {
                serverSocket = new ServerSocket(PORT);
                Socket socket = serverSocket.accept();

                BufferedReader data_in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Writer data_out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                // DATA IN - LEN
                String len = data_in.readLine();
                Log.d("DATA_IN", "Received bytes: " + len);

                // DATA IN
                String data = data_in.readLine();


                statusUpdate("Recveived bytes: " + data.length());

                // DATA OUT
                data_out.append(Backend.exportJson());
                data_out.append("\n");
                data_out.flush();
                Log.d("DATA_OUT", "Sent bytes: " + Backend.exportJson().length());

                socket.close();
                serverSocket.close();

                statusUpdate("\nProcessing...");
                if (Backend.importJson(data)) {
                    statusUpdate("\nSuccessfuly updated.");
                    Backend.exportJsonFile();
                    closeActivity();
                } else {
                    statusUpdate("\nJSON import failed");
                }

            } catch (IOException e) {
                e.printStackTrace();
                statusUpdate("\nSync failed");
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
        this.finish();
    }
}