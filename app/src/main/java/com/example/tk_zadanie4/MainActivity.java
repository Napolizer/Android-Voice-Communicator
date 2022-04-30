package com.example.tk_zadanie4;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final Pattern IP_ADDRESS
            = Pattern.compile(
            "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
                    + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
                    + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                    + "|[1-9][0-9]|[0-9]))");

    private ConstraintLayout layout;
    private TableLayout tableLayout;
    private EditText textYourIPAddress;
    private TextView textYourPortNumber;
    private Button startCall;
    private EditText externalIpAddress;
    private EditText externalPortNumber;
    private SeekBar seekBarSampleRate;

    private Context context;
    private WifiManager wifiManager;

    private DatagramSocket server;

    private final int[] sampleRates = {
            2000,
            5000,
            11000,
            22000,
            44100,
            48000,
            96000,
            192000,
            320000,
            640000
    };

    protected void initComponents() {
        layout = findViewById(R.id.Layout);
        tableLayout = findViewById(R.id.TableLayout);
        textYourIPAddress = findViewById(R.id.TextYourIPAddress);
        textYourPortNumber = findViewById(R.id.TextYourPortNumber);
        startCall = findViewById(R.id.ButtonStartCall);
        externalIpAddress = findViewById(R.id.EditTextExternalIpAddress);
        externalPortNumber = findViewById(R.id.EditTextExternalPortNumber);
        seekBarSampleRate = findViewById(R.id.SeekBarSampleRate);

        context = getApplicationContext();
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        try {
            createServer();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected MainActivity getMainActivity() {
        return this;
    }

    protected String getIpAddress() {
        return Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
    }

    protected String getPortNumber() {
        return String.valueOf(server.getLocalPort());
    }

    protected boolean isDeviceConnected() {
        return wifiManager.isWifiEnabled();
    }

    protected void createServer() throws SocketException, UnknownHostException {
        server = new DatagramSocket(0, InetAddress.getByName(getIpAddress()));
    }

    protected boolean validateUserInput() {
        try {
            Matcher matcher = IP_ADDRESS.matcher(externalIpAddress.getText().toString());
            if (!matcher.matches()) {
                throw new RuntimeException();
            }
            Integer.parseInt(externalPortNumber.getText().toString());
            return true;
        } catch(RuntimeException e) {
            AlertDialog.Builder invalidInputDialog = new AlertDialog.Builder(this);
            invalidInputDialog.setMessage("Port Number or Ip Address is incorrect");
            invalidInputDialog.setTitle("Invalid Input");
            invalidInputDialog.setCancelable(false);

            invalidInputDialog.setPositiveButton("OK", (dialogInterface, i) -> {});
            invalidInputDialog.create().show();
        }
        return false;
    }

    protected boolean doesServerExists(String ip, int port) {
        try {
            return InetAddress.getByName(ip).isReachable(2000);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initComponents();

        if (isDeviceConnected()) {
            textYourIPAddress.setText(getIpAddress());
        } else {
            AlertDialog.Builder deviceNotConnectedDialog = new AlertDialog.Builder(this);
            deviceNotConnectedDialog.setMessage("Device is not connected to internet");
            deviceNotConnectedDialog.setTitle("No Connection");
            deviceNotConnectedDialog.setCancelable(false);

            deviceNotConnectedDialog.setPositiveButton("OK", (dialogInterface, i) -> {
                moveTaskToBack(true);
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
            });


            deviceNotConnectedDialog.create().show();
        }

        textYourPortNumber.setText(getPortNumber());

        startCall.setOnClickListener(view -> {
            if (validateUserInput()) {
                Call.Builder builder = Call.getBuilder();

                builder.parentActivity = getMainActivity();
                builder.externalIpAddress = externalIpAddress.getText().toString();
                builder.externalPortNumber = Integer.parseInt(externalPortNumber.getText().toString());
                builder.server = server;
                builder.sampleRate = 8000;
                builder.sampleInterval = 20;
                builder.sampleSize = 2;

                Call call;
                try {
                    call = builder.build();
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                }

                call.startCall();
            }
        });

        seekBarSampleRate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private Toast message;

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                if (message != null) {
                    message.cancel();
                }
                message = Toast.makeText(MainActivity.this, String.valueOf(sampleRates[seekBar.getProgress()]), Toast.LENGTH_SHORT);
                message.show();
            }
        });
    }

    public Context getContext() {
        return context;
    }
}