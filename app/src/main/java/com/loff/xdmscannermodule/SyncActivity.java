package com.loff.xdmscannermodule;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class SyncActivity extends AppCompatActivity {

    TextView syncStatus;
    TextView ipText;
    Button cancelButton;
    String statusText = "";
    int PORT = 7700;
    ServerSocket serverSocket;
    Socket socket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);

        syncStatus = findViewById(R.id.text_sync_status);
        ipText = findViewById(R.id.text_ip_addr);
        cancelButton = findViewById(R.id.btn_cancel_sync);

        cancelButton.setOnClickListener(view -> closeActivity());

        // NETCODE
        new Thread(new netExchanger()).start();
    }

    class netExchanger implements Runnable{
        @Override
        public void run() {
            statusUpdate("Waiting for connection.");

            Context context = getApplicationContext();
            WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            @SuppressWarnings("deprecation") String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
            ipText.setText(ip);

            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(PORT));

                socket = serverSocket.accept();
                statusUpdate("\nConnection success!");
                setBG(ContextCompat.getColor(context, R.color.wait_yellow));

                BufferedReader data_in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Writer data_out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                // SYNC_VER IN
                String client_sync_version = data_in.readLine();
                Log.d("SYNC_VER", client_sync_version);

                // DATA IN
                String data = data_in.readLine();
                statusUpdate("\nRecveived bytes: " + data.length());

                if (client_sync_version.equals("v2")) {

                    // DATA OUT
                    data_out.append(Backend.exportJson());
                    data_out.append("\n");
                    data_out.flush();
                    Log.d("DATA_OUT", "\nSent bytes: " + Backend.exportJson().length());

                    // DATA CLOSE
                    socket.close();
                    serverSocket.close();

                    statusUpdate("\nProcessing...");

                    if (Backend.importJson(data)) {
                        Log.d("SYNC", "DOING UPDATE");
                        statusUpdate("\nSuccessfuly updated.");
                        setBG(ContextCompat.getColor(context, R.color.success_green));
                        Backend.exportJsonFile();
                    } else {
                        statusUpdate("\nJSON import failed.");
                        setBG(ContextCompat.getColor(context, R.color.fail_red));
                    }

                    closeActivity();

                } else {

                    // DATA CLOSE
                    socket.close();
                    serverSocket.close();

                    statusUpdate("\n\nUnsupported app version!\nPlease update X-Dock Manager");
                    setBG(ContextCompat.getColor(context, R.color.fail_red));
                }

            } catch (SocketException ignore) {} catch (IOException e) {
                e.printStackTrace();
                statusUpdate("\n\nSync failed.");
                setBG(ContextCompat.getColor(context, R.color.fail_red));
            }
        }

    }

    private void setBG(int color) {
        runOnUiThread(() -> findViewById(R.id.text_ip_addr).setBackgroundColor(color));
    }

    private void statusUpdate(String update){
        runOnUiThread(() -> {
            statusText += update;
            syncStatus.setText(statusText);
        });

    }

    private void closeActivity() {
        try {
            if (socket != null) {socket.close();}
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        setResult(Activity.RESULT_OK);
        this.finish();
    }
}