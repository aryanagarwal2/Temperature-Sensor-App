package com.example.continuoustempsensor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ConnectionActivity extends AppCompatActivity implements BtAdapter.OnDeviceListener {

    Button back;
    public String sensor;
    @SuppressLint("StaticFieldLeak")
    public static Context context;
    TextView status;
    TextView connect;
    Button rename;
    Dialog myDialog;
    Button find;
    RecyclerView list;
    BtAdapter btAdapter;
    private static final int REQUEST_CODE = 1;
    List<BtDevice> mData;
    List<String> listOfAddress = new ArrayList<>();
    private BluetoothAdapter mBlueAdapter;
    private static final int REQUEST_ENABLE_BT = 0;
    Toast toast;
    String correct;
    public String addy;
    public String daStatus;
    Button yes;
    Button no;
    AndroidService mService;
    private BluetoothLeScanner bluetoothLeScanner;
    private boolean scanning = false;
    private Handler handler = new Handler();
    private static final long SCAN_PERIOD = 20000;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((AndroidService.LocalBinder) service).getService();
            mService.startConnection(addy);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            MainActivity.spark = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_connection);
        back = findViewById(R.id.back);
        find = findViewById(R.id.pairedBtn);
        myDialog = new Dialog(this);
        myDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        rename = findViewById(R.id.rename);
        status = findViewById(R.id.status);
        sensor = restoreNameData(this);
        mBlueAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!MainActivity.spark || !mBlueAdapter.isEnabled()) {
            daStatus = "Not Connected";
            status.setText(daStatus);
        } else if ((MainActivity.name != null) && (sensor == null)) {
            sensor = MainActivity.name;
            daStatus = "Connected to " + sensor;
            status.setText(daStatus);
        } else if (sensor != null) {
            daStatus = "Connected to " + sensor;
            status.setText(daStatus);
        } else {
            daStatus = "Not Connected";
            status.setText(daStatus);
        }
        bluetoothLeScanner = mBlueAdapter.getBluetoothLeScanner();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
        }
        list = findViewById(R.id.list);
        mData = new ArrayList<>();
        btAdapter = new BtAdapter(this, mData, this);
        list.setAdapter(btAdapter);
        list.setLayoutManager(new LinearLayoutManager(this));

        back.setOnClickListener(v -> onBackPressed());

        find.setOnClickListener(v -> {
            if (!mBlueAdapter.isEnabled()) {
                find.setText("Find Devices");
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, REQUEST_ENABLE_BT);
            } else {
                if (!scanning) { ;
                    handler.postDelayed(() -> {
                        scanning = false;
                        bluetoothLeScanner.stopScan(leScanCallback);
                        find.setText("Find Devices");
                    }, SCAN_PERIOD);
                    scanning = true;
                    bluetoothLeScanner.startScan(leScanCallback);
                    toast = Toast.makeText(getBaseContext(), "Make sure your device is on", Toast.LENGTH_SHORT);
                    setToast();
                    find.setText("Cancel");
                } else {
                    scanning = false;
                    bluetoothLeScanner.stopScan(leScanCallback);
                    find.setText("Find Devices");
                }
            }
        });

        rename.setOnClickListener(v -> {
            if (MainActivity.spark && mBlueAdapter.isEnabled()) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
                alertDialog.setTitle("Rename Sensor");
                final EditText userInput = new EditText(this);
                alertDialog.setView(userInput);
                alertDialog.setCancelable(false).setPositiveButton("Rename", (dialog, which) -> {
                    sensor = userInput.getText().toString();
                    daStatus = "Connected to " + sensor;
                    status.setText(daStatus);
                    dialog.cancel();
                    saveNameData(this, sensor, addy);
                }).setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
                alertDialog.show();
            } else {
                toast = Toast.makeText(getBaseContext(), "Please connect to a device", Toast.LENGTH_SHORT);
                setToast();
            }
        });
    }

    @SuppressLint("ShowToast")
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                handler.postDelayed(() -> {
                    scanning = false;
                    bluetoothLeScanner.stopScan(leScanCallback);
                }, SCAN_PERIOD);
                scanning = true;
                bluetoothLeScanner.startScan(leScanCallback);
                find.setText("Cancel");
                toast = Toast.makeText(getBaseContext(), "Make sure your device is on", Toast.LENGTH_SHORT);
                setToast();
            } else {
                toast = Toast.makeText(this, "Unable to turn on Bluetooth", Toast.LENGTH_SHORT);
                setToast();
            }
        }
    }

    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = mBlueAdapter.getRemoteDevice(result.getDevice().getAddress());
            String deviceAddress = result.getDevice().getAddress();
            if (device.getName() != null) {
                if (!listOfAddress.contains(deviceAddress)) {
                    mData.add(new BtDevice(device.getName() + ":" + deviceAddress));
                    listOfAddress.add(deviceAddress);
                }
            }
            Set<BtDevice> hashSet = new LinkedHashSet<>(mData);
            mData.clear();
            mData.addAll(hashSet);
            btAdapter.notifyDataSetChanged();
            btAdapter = new BtAdapter(getApplicationContext(), mData, (BtAdapter.OnDeviceListener) context);
            list.setAdapter(btAdapter);
            list.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        }
    };

    public void setToast() {
        toast.setGravity(Gravity.BOTTOM, 0, 100);
        toast.show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onDeviceClick(int position) {
        correct = mData.get(position).getDevice();
        addy = mData.get(position).getAddress();
        ShowPopUp();
    }

    public void ShowPopUp() {
        myDialog.setContentView(R.layout.popup);
        yes = myDialog.findViewById(R.id.ok);
        no = myDialog.findViewById(R.id.no);
        connect = myDialog.findViewById(R.id.connection);
        connect.setText("Connect to " + correct + "?");
        no.setOnClickListener(v -> myDialog.dismiss());
        yes.setOnClickListener(v -> {
            Intent intent = new Intent(this, AndroidService.class);
            bindService(intent, connection, Context.BIND_AUTO_CREATE);
            MainActivity.address = addy;
            scanning = false;
            bluetoothLeScanner.stopScan(leScanCallback);
            myDialog.dismiss();
            find.setText("Find Devices");
        });
        myDialog.show();
    }

    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (AndroidService.ACTION_GATT_CONNECTED.equals(action)) {
                MainActivity.spark = true;
                daStatus = "Connected to " + correct;
                status.setText(daStatus);
                sensor = correct;
                saveNameData(context, sensor, addy);
                MainActivity.savePrefsData(context, sensor, addy);
                Intent frag3intent = new Intent("BLUETOOTH_IS_CONNECTED");
                sendBroadcast(frag3intent);
            } else if (AndroidService.ACTION_GATT_DISCONNECTED.equals(action)) {
                MainActivity.spark = false;
                daStatus = "Not Connected";
                status.setText(daStatus);
                Intent frag3intent = new Intent("BLUETOOTH_IS_DISCONNECTED");
                sendBroadcast(frag3intent);
            }
        }
    };

    public static void saveNameData(Context context, String sensor, String addy) {
        SharedPreferences preferences = context.getSharedPreferences("connectPref", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("rename", sensor);
        editor.putString("address", addy);
        editor.apply();
    }

    public static String restoreNameData(Context context) {
        SharedPreferences pref = context.getSharedPreferences("connectPref", Context.MODE_PRIVATE);
        return pref.getString("rename", null);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AndroidService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(AndroidService.ACTION_GATT_DISCONNECTED);
        return intentFilter;
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onPause() {
        super.onPause();
        Toast.makeText(this, "onPause", Toast.LENGTH_SHORT).show();
        unregisterReceiver(gattUpdateReceiver);
    }
}