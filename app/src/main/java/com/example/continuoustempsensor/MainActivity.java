package com.example.continuoustempsensor;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.security.keystore.KeyGenParameterSpec;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.security.crypto.MasterKeys;

import com.blure.complexview.ComplexView;
import com.blure.complexview.Shadow;
import com.example.continuoustempsensor.ui.main.NotificationReceiver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final String TAG = "MainActUUID";
    public static boolean spark = false;
    public static int notifFreq = -1;
    private int tempFreq = notifFreq;
    public static String name;
    int timecount = 0;
    String unit = " °F";
    BluetoothAdapter mBlueAdapter;
    ArrayList<String> time = new ArrayList<>();
    ArrayList<Float> tempVals = new ArrayList<>();
    ArrayList<Float> tipperVals = new ArrayList<>();
    TextView tempTextView;
    public static LineChart mChart;
    public static String address;
    SimpleDateFormat format = new SimpleDateFormat("h:mm a");
    public static SimpleDateFormat date = new SimpleDateFormat("EEE.yyyy.MM.dd");
    public static String jsonDate = date.format(Calendar.getInstance().getTime());
    String temperature;
    private final Fragment fragment2 = new fragment_tab2();
    private Fragment fragment3 = new fragment_tab3();
    final FragmentManager fm = getSupportFragmentManager();
    Fragment active;
    boolean plotData = false;
    ImageView btSym;
    TextView btStat;
    public static ImageView notif;
    Boolean good;
    Boolean bad;
    Boolean warning;
    Boolean veryBad;
    ComplexView shadow, ring, white;
    ViewGroup vg;
    public static Boolean firstNotif = true;
    Boolean firstNormalNotif = true;
    protected LocationManager locationManager;
    AndroidService mService;
    public static boolean notifChecked = true;
    public final static String UNIT_CHANGE = "TEMP_UNIT_CHANGED";
    boolean unitcels = false;
    private ArrayList<Float> graphVals = new ArrayList<>();
    boolean onStopBooleanUrgent = false;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((AndroidService.LocalBinder) service).getService();
            mService.startConnection(address);
            fragment3 = new fragment_tab3();
            fm.beginTransaction().add(R.id.container3, fragment3, "3").hide(fragment3).addToBackStack(null).commit();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            spark = false;
            btStat.setText("Not Connected");
            btSym.setBackgroundResource(R.drawable.ic_b2);
        }
    };

    @SuppressLint("ShowToast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vg = (ViewGroup) getLayoutInflater().inflate(R.layout.activity_main, null);
        setContentView(vg);
        shadow = findViewById(R.id.complex);
        ring = findViewById(R.id.ring);
        white = findViewById(R.id.white);
        tempTextView = findViewById(R.id.temp);
        btSym = findViewById(R.id.btSym);
        btStat = findViewById(R.id.btStat);
        notif = findViewById(R.id.notif);

//        if (!fileCreated()) {
        if (true) { // TODO: change back to previous line when done
            try {
                new FileOutputStream(this.getFilesDir() + "/notif.json");
                new FileOutputStream(this.getFilesDir() + "/temp.json");
                new FileOutputStream(this.getFilesDir() + "/tippers.json");
                saveFileCreation();
                notif.setImageResource(R.drawable.bell);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            File file;
            FileReader fileReader;
            BufferedReader bufferedReader;
            String FILE_NAME = "notif.json";
            file = new File(this.getFilesDir(), FILE_NAME);
            try {
                fileReader = new FileReader(file);
                bufferedReader = new BufferedReader(fileReader);
                StringBuilder stringBuilder = new StringBuilder();
                String line = bufferedReader.readLine();
                while (line != null) {
                    stringBuilder.append(line).append("\n");
                    line = bufferedReader.readLine();
                }
                bufferedReader.close();
                String response = stringBuilder.toString();
                JSONObject jsonObject = new JSONObject(response);
                JSONArray names = jsonObject.names();
                if (names != null) {
                    notif.setImageResource(R.drawable.bell2);
                } else {
                    notif.setImageResource(R.drawable.bell);
                }
            } catch (IOException | JSONException e) {
                notif.setImageResource(R.drawable.bell);
                e.printStackTrace();
            }
        }

        Bundle bundle = getIntent().getExtras();

        mBlueAdapter = BluetoothAdapter.getDefaultAdapter();
        mChart = findViewById(R.id.sparkView);
        mChart.setVisibility(View.VISIBLE);
        mChart.setDescription(null);
        mChart.setTouchEnabled(true);
        mChart.setExtraBottomOffset(10f);
        mChart.setBackgroundColor(Color.TRANSPARENT);
        mChart.setHighlightPerTapEnabled(true);
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(false);
        mChart.setPinchZoom(true);
        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);
        mChart.setData(data);
        mChart.setMaxHighlightDistance(20);

        XAxis xl = mChart.getXAxis();
        xl.setTextColor(Color.BLACK);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);
        xl.setDrawGridLines(false);
        xl.setPosition(XAxis.XAxisPosition.BOTTOM);
        xl.setLabelCount(4, true);

        LimitLine limitLine = new LimitLine(100.4f, null);
        limitLine.setLineColor(Color.RED);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setDrawLimitLinesBehindData(true);
        leftAxis.removeAllLimitLines();
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setDrawGridLines(false);
        leftAxis.setEnabled(true);
        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);

        mChart.getLegend().setEnabled(false);
        mChart.setDrawBorders(false);
        mChart.invalidate();

        String saveUnit = restoreTempUnit(this);
        if (saveUnit.equals(" °C")) {
            unitcels = true;
        }

        if (bundle != null) {
            address = bundle.getString("address");
            name = bundle.getString("name");
            Intent intent = new Intent(this, AndroidService.class);
            bindService(intent, connection, Context.BIND_AUTO_CREATE);
            savePrefsData(this, name, address);
        } else if (mBlueAdapter.isEnabled()) {
            address = restoreAddressData();
            name = restoreNameData();
            Intent intent = new Intent(this, AndroidService.class);
            bindService(intent, connection, Context.BIND_AUTO_CREATE);
        }

        // can delete once TIPPERS is confirmed
        FileReader tippersFileReader;
        BufferedReader tippersBufferedReader;
        String FILE_NAME2 = "tippers.json";
        File tippersFile = new File(this.getFilesDir(), FILE_NAME2);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        assert locationManager != null;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        double latitude = 0;
        double longitude = 0;
        if  (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        }
        try {
            tippersFileReader = new FileReader(tippersFile);
            tippersBufferedReader = new BufferedReader(tippersFileReader);
            String tippersLine = tippersBufferedReader.readLine();
            String array1 = null;
            if (tippersLine != null) {
                StringBuilder tippersString = new StringBuilder();
                while (tippersLine != null) {
                    tippersString.append(tippersLine).append("\n");
                    tippersLine = tippersBufferedReader.readLine();
                }
                tippersBufferedReader.close();
                String tippersResponse = tippersString.toString();
                JSONArray firstArray = new JSONArray(tippersResponse);
                array1 = firstArray.toString();
            }
            JSONObject tippersData = new JSONObject();
            SimpleDateFormat sdf = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmXXX");
            }
            String timestamp = sdf.format(Calendar.getInstance().getTime());
            tippersData.put("timestamp", timestamp);
            JSONObject read = new JSONObject();
            read.put("val", "98.6");
            read.put("unit", unit);
            tippersData.put("data", read);
            tippersData.put("Latitude", latitude);
            tippersData.put("Longitude", longitude);
            JSONArray appendArray = new JSONArray();
            appendArray.put(tippersData);
            String array2 = appendArray.toString();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode tree1;
            if (array1 != null) {
                tree1 = mapper.readTree(array1);
                JsonNode tree2 = mapper.readTree(array2);
                ((ArrayNode) tree1).addAll((ArrayNode) tree2);
            } else {
                tree1 = mapper.readTree(array2);
            }
            String tippersJSON = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tree1);
            FileWriter fileWriter = new FileWriter(tippersFile, false);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(tippersJSON);
            bufferedWriter.close();
            // delete below when done
            ComponentName componentName = new ComponentName(getApplicationContext(), TippersJobService.class);
            JobInfo jobInfo = new JobInfo.Builder(110, componentName)
                    .setPersisted(false)
                    .setRequiresCharging(false)
                    .setOverrideDeadline(5000)
                    .build();
            JobScheduler jobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
            assert jobScheduler != null;
            jobScheduler.schedule(jobInfo);
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }

//        File file;
//        String FILE_NAME = "temp.json";
//        file = new File(this.getFilesDir(), FILE_NAME);
//        ArrayList<String> timeArray = new ArrayList<>();
//        timeArray.add("1:30 PM");
//        timeArray.add("1:35 PM");
//        timeArray.add("1:40 PM");
//        timeArray.add("1:45 PM");
//        timeArray.add("1:50 PM");
//        timeArray.add("1:55 PM");
//
//        ArrayList<String> dayArray = new ArrayList<>();
//        dayArray.add("Sun.2021.09.19");
//        dayArray.add("Mon.2021.09.20");
//        dayArray.add("Tue.2021.09.21");
//        dayArray.add("Wed.2021.09.22");
//        dayArray.add("Thu.2021.09.23");
//        dayArray.add("Fri.2021.09.24");
//        dayArray.add("Sat.2021.09.25");
//        try {
//            JSONObject jsonObject = new JSONObject();
//            for (int d = 0; d < dayArray.size(); d++) {
//                JSONObject obj = new JSONObject();
//                for (int t = 0; t < timeArray.size(); t++) {
//                    String index = String.valueOf(t);
//                    String key = "time" + index;
//                    JSONObject reading = new JSONObject();
//                    reading.put("temperature", 97.9 + Math.random() * (103.5 - 97.9));
//                    reading.put("hour", timeArray.get(t));
//                    reading.put("unit", unit);
//                    obj.put(key, reading);
//                    jsonObject.put(dayArray.get(d), obj);
//                }
//            }
//            String userString = jsonObject.toString();
//            FileWriter fileWriter = new FileWriter(file, false);
//            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
//            bufferedWriter.write(userString);
//            bufferedWriter.close();
//        } catch (JSONException | IOException e) {
//            e.printStackTrace();
//        }

        notif.setOnClickListener(v -> {
            Intent notifActivityIntent = new Intent(getApplicationContext(), notifActivity.class);
            startActivity(notifActivityIntent);
        });

        btSym.setOnClickListener(v -> {
            Intent connectActivity = new Intent(getApplicationContext(), ConnectionActivity.class);
            startActivity(connectActivity);
        });

        btStat.setOnClickListener(v -> {
            Intent connectActivity = new Intent(getApplicationContext(), ConnectionActivity.class);
            startActivity(connectActivity);
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNav);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.home:
                    if (active != null) {
                        fm.beginTransaction().hide(active).commit();
                    }
                    tempTextView.setVisibility(View.VISIBLE);
                    mChart.setVisibility(View.VISIBLE);
                    shadow.setVisibility(View.VISIBLE);
                    ring.setVisibility(View.VISIBLE);
                    white.setVisibility(View.VISIBLE);
                    btStat.setVisibility(View.VISIBLE);
                    btSym.setVisibility(View.VISIBLE);
                    notif.setVisibility(View.VISIBLE);
                    return true;

                case R.id.Bt:
                    if (active != null) {
                        fm.beginTransaction().hide(active).show(fragment3).commit();
                    } else {
                        fm.beginTransaction().show(fragment3).commit();
                    }
                    tempTextView.setVisibility(View.INVISIBLE);
                    mChart.setVisibility(View.INVISIBLE);
                    shadow.setVisibility(View.INVISIBLE);
                    ring.setVisibility(View.INVISIBLE);
                    white.setVisibility(View.INVISIBLE);
                    btStat.setVisibility(View.INVISIBLE);
                    btSym.setVisibility(View.INVISIBLE);
                    notif.setVisibility(View.INVISIBLE);
                    active = fragment3;
                    return true;

                case R.id.profile:
                    if (active != null) {
                        fm.beginTransaction().hide(active).show(fragment2).commit();
                    } else {
                        fm.beginTransaction().show(fragment2).commit();
                    }
                    tempTextView.setVisibility(View.INVISIBLE);
                    mChart.setVisibility(View.INVISIBLE);
                    shadow.setVisibility(View.INVISIBLE);
                    ring.setVisibility(View.INVISIBLE);
                    white.setVisibility(View.INVISIBLE);
                    btStat.setVisibility(View.INVISIBLE);
                    btSym.setVisibility(View.INVISIBLE);
                    notif.setVisibility(View.INVISIBLE);
                    active = fragment2;
                    return true;
            }
            return false;
        });

        fm.beginTransaction().add(R.id.container3, fragment3, "3").hide(fragment3).addToBackStack(null).commit();
        fm.beginTransaction().add(R.id.container2, fragment2, "2").hide(fragment2).addToBackStack(null).commit();
        bottomNavigationView.setSelectedItemId(R.id.home);
    }

    private void tempDisplay(int num) {
        float[] radii = {250, 250, 250, 250, 250, 250, 250, 250}; // TODO: find a way to not hard-code radius of display
        if (num == 0) {
            shadow.setShadow(new Shadow(4, 100, "#00B0F0", GradientDrawable.RECTANGLE, radii, Shadow.Position.CENTER)); // blue
            ring.setColor(Color.parseColor("#00B0F0"));
            temperature = "--";
            tempTextView.setText(temperature);
            tempTextView.setTextSize(44);
        } else if (num == 1) {
            shadow.setShadow(new Shadow(4, 100, "#00B050", GradientDrawable.RECTANGLE, radii, Shadow.Position.CENTER)); // green
            ring.setColor(Color.parseColor("#00B050"));
            tempTextView.setText(temperature + unit);
            tempTextView.setTextSize(44);
            tempTextView.setTextColor(Color.parseColor("#000000"));
        } else if (num == 2) {
            shadow.setShadow(new Shadow(4, 100, "#FFD500", GradientDrawable.RECTANGLE, radii, Shadow.Position.CENTER)); // yellow
            ring.setColor(Color.parseColor("#FFD500"));
            tempTextView.setText(temperature + unit);
            tempTextView.setTextSize(44);
            tempTextView.setTextColor(Color.parseColor("#000000"));
        } else if (num == 3) {
            shadow.setShadow(new Shadow(4, 100, "#FB710B", GradientDrawable.RECTANGLE, radii, Shadow.Position.CENTER)); // orange
            ring.setColor(Color.parseColor("#FB710B"));
            tempTextView.setText(temperature + unit);
            tempTextView.setTextSize(40);
            tempTextView.setTextColor(Color.parseColor("#000000"));
        } else {
            shadow.setShadow(new Shadow(4, 100, "#FF0000", GradientDrawable.RECTANGLE, radii, Shadow.Position.CENTER)); // red
            ring.setColor(Color.parseColor("#FF0000"));
            tempTextView.setText(temperature + unit);
            tempTextView.setTextSize(40);
            tempTextView.setTextColor(Color.parseColor("#000000"));
        }
    }

    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (AndroidService.ACTION_GATT_CONNECTED.equals(action)) {
                spark = true;
                btStat.setText("Connected");
                btSym.setBackgroundResource(R.drawable.ic_b1);
                Intent frag3intent = new Intent("BLUETOOTH_IS_CONNECTED");
                sendBroadcast(frag3intent);
            } else if (AndroidService.ACTION_GATT_DISCONNECTED.equals(action)) {
                spark = false;
                btStat.setText("Not Connected");
                btSym.setBackgroundResource(R.drawable.ic_b2);
                Intent frag3intent = new Intent("BLUETOOTH_IS_DISCONNECTED");
                sendBroadcast(frag3intent);
            } else if (AndroidService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                List<BluetoothGattService> gattServices = mService.getSupportedGattServices();
                loopGatt(gattServices);
            } else if (AndroidService.ACTION_DATA_AVAILABLE.equals(action)) {
                String numericVal = intent.getStringExtra(AndroidService.EXTRA_DATA);
                try {
                    updateUI(numericVal);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "Temperature: " + numericVal);
            } else if (UNIT_CHANGE.equals(action)) {
                if (temperature != null && !temperature.isEmpty() && !temperature.equals("--")) {
                    unit = intent.getStringExtra("unit");
                    float convertedTemp;
                    if (unit.equals(" °F")) { // this means convert to fahrenheit
                        convertedTemp = (float) ((Float.parseFloat(temperature) * 9.0 / 5) + 32);
                        unitcels = false;
                    } else { // convert to celsius
                        convertedTemp = (float) ((Float.parseFloat(temperature) - 32) * 5 / 9.0);
                        unitcels = true;
                    }
                    DecimalFormat df = new DecimalFormat("#.#");
                    temperature = df.format(convertedTemp);
                    onResume();
                }
                adjustGraph(unit);
            }
        }
    };

    private void adjustGraph(String tempunit) {

        ArrayList<Float> alltemps = new ArrayList<>();
        float x;
        DecimalFormat df = new DecimalFormat("#.#");
        for (int k = 0; k < graphVals.size(); k++) {
            if (tempunit.equals(" °F")) {
                x = (graphVals.get(k)*9)/5 + 32;
                alltemps.add(Float.parseFloat(df.format(x)));
            } else {
                x = (graphVals.get(k) - 32) * 5/9;
                alltemps.add(Float.parseFloat(df.format(x)));
            }
        }

        mChart.clear();
        mChart = findViewById(R.id.sparkView);
        mChart.setVisibility(View.VISIBLE);
        mChart.setDescription(null);
        mChart.setTouchEnabled(true);
        mChart.setExtraBottomOffset(10f);
        mChart.setBackgroundColor(Color.TRANSPARENT);
        mChart.setHighlightPerTapEnabled(true);
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(false);
        mChart.setPinchZoom(true);
        mChart.setDoubleTapToZoomEnabled(false);
        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);
        mChart.setData(data);
        mChart.setMaxHighlightDistance(20);

        XAxis xl = mChart.getXAxis();
        xl.setTextColor(Color.BLACK);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);
        xl.setDrawGridLines(false);
        xl.setPosition(XAxis.XAxisPosition.BOTTOM);
        xl.setLabelCount(4, true);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setDrawLimitLinesBehindData(true);
        leftAxis.removeAllLimitLines();
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setDrawGridLines(false);
        leftAxis.setEnabled(true);
        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);

        mChart.getLegend().setEnabled(false);
        mChart.setDrawBorders(false);
        mChart.invalidate();
        mChart.setVisibility(View.INVISIBLE);

        LineData lineData = mChart.getData();
        if (lineData != null) {
            MyLineDataSet mySet = (MyLineDataSet) data.getDataSetByIndex(0);
            if (mySet == null) {
                mySet = createSet();
                data.addDataSet(mySet);
            }
            ArrayList<String> xTime = time;
            for (int r = 0; r < alltemps.size(); r++) {
                float yVals = alltemps.get(r);
                data.addEntry(new Entry(mySet.getEntryCount(), yVals), 0);
                xl.setValueFormatter(new IndexAxisValueFormatter(xTime));
                xl.setGranularityEnabled(true);
                xl.setGranularity(1f);
                xl.setSpaceMax(0.3f);
                lineData.notifyDataChanged();
                mChart.notifyDataSetChanged();
                mChart.setVisibleXRangeMaximum(6);
                mChart.moveViewToX(lineData.getEntryCount());
            }
        }
    }

    private void loopGatt(List<BluetoothGattService> gattServices) {
        if (gattServices == null) {
            return;
        }
        String uuid;
        for (BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString();
            if (uuid.equals("e761d2af-1c15-4fa7-af80-b5729020b340")) {
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    UUID characteristicUUID = gattCharacteristic.getUuid();
                    String charUUID = String.valueOf(characteristicUUID);
                    if (charUUID.equals("0000bead-0000-1000-8000-00805f9b34fb")) {
                        final int charaProp = gattCharacteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mService.setCharacteristicNotification(gattCharacteristic, true);
                        }
                    }
                }
            }
        }
    }

    private void updateUI(String tempString) throws FileNotFoundException {
        float sensorVal = Float.parseFloat(tempString);
        tempVals.add(sensorVal);
        tipperVals.add(sensorVal);
        int N = tempVals.size();
        int M = tipperVals.size();
        if (M >= 125) { // 21 minutes for tippers
            String tippersTemp = grubbs(tipperVals, M);
            if (!tippersTemp.equals("NaN")) {
                SimpleDateFormat sdf = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmXXX");
                }
                String timestamp = sdf.format(Calendar.getInstance().getTime());
                unit = restoreTempUnit(MainActivity.this);
                writeTippersJSON(tippersTemp, timestamp, unit);
                ComponentName componentName = new ComponentName(getApplicationContext(), TippersJobService.class);
                JobInfo jobInfo = new JobInfo.Builder(110, componentName)
                        .setPersisted(false)
                        .setRequiresCharging(false)
                        .setOverrideDeadline(5000)
                        .build();
                JobScheduler jobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
                assert jobScheduler != null;
                jobScheduler.schedule(jobInfo);
            }
            new FileOutputStream(this.getFilesDir() + "/tippers.json");
            tipperVals.clear();
        }

        if (N >= 2) {
            temperature = String.valueOf((tempVals.get(0) + tempVals.get(1)) / 2); // 6 minutes for app
//            temperature = grubbs(tempVals, N);
            if (!temperature.equals("NaN")) {
                float notifTemp = Float.parseFloat(temperature);
                if (unitcels) {
                    double newTemp = (Double.parseDouble(temperature) - 32) * 5 / 9.0;
                    DecimalFormat df = new DecimalFormat("#.#");
                    temperature = String.valueOf(Float.parseFloat(df.format(newTemp)));
                }
                booleanUpdate(temperature);
                plotData = true;
                new Thread(() -> {
                    while (plotData) {
                        runOnUiThread(() -> {
                            String clock = format.format(Calendar.getInstance().getTime());
                            time.add(clock);
                            addEntry(temperature);
                            plotData = false;
                            unit = restoreTempUnit(MainActivity.this);
                            writeJSON(temperature, clock, timecount, unit);
                            timecount++;
                            onResume();
                        });
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
//                float medianTemp = 101;
                saveTempVal(notifTemp, getApplicationContext());
                if (notifTemp > 100.3) { // more urgent -- red
                    if (firstNotif) {// send first notif
                        if (onStopBooleanUrgent) {
                            onStopBooleanUrgent = false;
                            scheduleUrgentJob();
                        } else {
                            onStopBooleanUrgent = true;
                            NotificationReceiver.sendNotification(this, "URGENT_NOTIFY");
                        }
                        firstNotif = false;
                    }
                } else if (fragment_tab3.restoreNotifEnable()) {
                    if (firstNormalNotif) { // not urgent normal notification -- temp greater than 0 but less than 100.3
                        firstNormalNotif = false;
                        scheduleNormalJob();
                        notif.setImageResource(R.drawable.bell2);
                    } else if (notifFreq != tempFreq) {
                        cancelJob(1);
                        scheduleNormalJob();
                    }
                }
            }
            tempVals.clear();
        }
    }

    private String grubbs(ArrayList<Float> Vals, int sampleSize) {
        String returnVal = "NaN";

        ArrayList<Float> MovingAverage = new ArrayList<>();
        for (int window = 0; window < (sampleSize - 5); window++) {
            ArrayList<Float> subArray = new ArrayList<>(Vals.subList(window, window + 5));
            int sum = 0;
            for (int idx = 0; idx<subArray.size(); idx++) {
                sum += subArray.get(idx);
            }
            double avg = sum/5f;
            MovingAverage.add((float) avg);
        }

        int arraySize = MovingAverage.size();
        ArrayList<Integer> removeIdx = new ArrayList<>();

        if (arraySize == 30) {
            Collections.sort(MovingAverage);
            double median = (MovingAverage.get(15) + MovingAverage.get(16)) / 2.0;
            ArrayList<Float> madVals = new ArrayList<>();
            for (int i = 0; i < arraySize; i++) {
                madVals.add((float) Math.abs(MovingAverage.get(i) - median));
            }
            Collections.sort(madVals);
            double MAD = (madVals.get(15) + madVals.get(16)) / 2.0;
            for (int i = 0; i < arraySize; i++) {
                double M = 0.675*(MovingAverage.get(i) - median) / MAD;
                if (Math.abs(M) > 3.5) {
                    removeIdx.add(i);
                }
            }

            Collections.sort(removeIdx, Collections.reverseOrder());
            for (int rIdx : removeIdx) {
                MovingAverage.remove(rIdx);
            }

            if (!MovingAverage.isEmpty()) {
                Collections.sort(MovingAverage);
                double medianTemp;
                if (sampleSize % 2 == 0) {
                    medianTemp = (MovingAverage.get(MovingAverage.size() / 2) + MovingAverage.get((MovingAverage.size() / 2) - 1)) / 2.0;
                } else {
                    medianTemp = (MovingAverage.get(MovingAverage.size() / 2)) / 1.0;
                }
                DecimalFormat df = new DecimalFormat("#.#");
                returnVal = df.format(medianTemp);
            }
        } else if (arraySize != 0){
            ArrayList<Float> GrubbTest = new ArrayList<>();
            double total = 0;
            for (int i = 0; i < arraySize; i++) {
                total += Vals.get(i);
            }
            double mean = total / arraySize;
            double total2 = 0;
            for (int i = 0; i < arraySize; i++) {
                total2 += Math.pow((Vals.get(i) - mean), 2);
            }
            double std = Math.sqrt(total2 / (arraySize - 1));
            double cv = std / mean;
            boolean legit;
            if (cv > 0.2) {
                for (int i = 0; i < arraySize; i++) {
                    double Gstat = Math.abs(Vals.get(i) - mean) / std;
                    if (Gstat < 3.4451) {
                        GrubbTest.add(Vals.get(i));
                    }
                }
                legit = !GrubbTest.isEmpty();
                if (legit) {
                    Collections.sort(GrubbTest);
                    double medianTemp;
                    if (sampleSize % 2 == 0) {
                        medianTemp = (GrubbTest.get(GrubbTest.size() / 2) + GrubbTest.get((GrubbTest.size() / 2) - 1)) / 2.0;
                    } else {
                        medianTemp = (GrubbTest.get(GrubbTest.size() / 2)) / 1.0;
                    }
                    DecimalFormat df = new DecimalFormat("#.#");
                    returnVal = df.format(medianTemp);
                }
            }
        }
        return returnVal;
    }

    public void scheduleUrgentJob(){
        ComponentName componentName = new ComponentName(this, urgentNotifJob.class);
        JobInfo info = new JobInfo.Builder(123, componentName)
                .setPersisted(false) // will continue job id device reboots
                .setPeriodic(15*60*1000) //15 min minimum
                .setBackoffCriteria(TimeUnit.MINUTES.toMillis(5), JobInfo.BACKOFF_POLICY_LINEAR)
                .setRequiresCharging(false)
                .build();

        JobScheduler scheduler = (JobScheduler)getSystemService(JOB_SCHEDULER_SERVICE);
        int resultCode = scheduler.schedule(info);
        if (resultCode == JobScheduler.RESULT_SUCCESS){
            Log.d(TAG, "Job scheduled");
        } else{
            Log.d(TAG, "Job scheduling failed");
        }
    }

    public void scheduleNormalJob(){ // TODO: test whether programmatic change of notification timing works
        ComponentName componentName = new ComponentName(this, normalNotifJob.class);
        if (notifFreq == -1) {
            if (fragment_tab3.restoreNotifFreq() == null) {
                notifFreq = 30;
            } else {
                String[] dropdownTimes = this.getResources().getStringArray(R.array.dropdown_times);
                String freq = dropdownTimes[fragment_tab3.restoreNotifIndex()];
                if (freq.contains("min")) {
                    notifFreq = 30;
                } else if (freq.contains("hour")) {
                    notifFreq = Integer.parseInt(freq.replace("hours", ""))*60;
                }
            }
        }

        tempFreq = notifFreq;

        JobInfo info = new JobInfo.Builder(456, componentName)
                .setPersisted(true) // will continue job id device reboots
                .setPeriodic(notifFreq*60*1000) //30 min minimum
                .setBackoffCriteria(TimeUnit.MINUTES.toMillis(1), JobInfo.BACKOFF_POLICY_LINEAR)
                .setRequiresCharging(false)
                .build();

        JobScheduler scheduler = (JobScheduler)getSystemService(JOB_SCHEDULER_SERVICE);
        int resultCode = scheduler.schedule(info);
        if (resultCode == JobScheduler.RESULT_SUCCESS){
            Log.d(TAG, "Job scheduled");
        } else{
            Log.d(TAG, "Job scheduling failed");
        }
    }

    public void cancelJob(int reqCode){
        JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        assert scheduler != null;
        if (reqCode == 1) {
            scheduler.cancel(456);
        } else {
            scheduler.cancel(123);
            firstNotif = true;
        }
        Log.d(TAG, "Job cancelled");
    }

    private void writeJSON(String temperature, String clock, int i, String unit) {
        File file;
        FileReader fileReader;
        BufferedReader bufferedReader;
        String FILE_NAME = "temp.json";
        file = new File(this.getFilesDir(), FILE_NAME);
        try {
            fileReader = new FileReader(file);
            bufferedReader = new BufferedReader(fileReader);
            StringBuilder stringBuilder = new StringBuilder();
            String line = bufferedReader.readLine();
            while (line != null) {
                stringBuilder.append(line).append("\n");
                line = bufferedReader.readLine();
            }
            bufferedReader.close();
            String response = stringBuilder.toString();
            JSONObject jsonObject = new JSONObject(response);
            String index = String.valueOf(i);
            String key = "time" + index;
            JSONObject reading = new JSONObject();
            reading.put("temperature", temperature);
            reading.put("hour", clock);
            reading.put("unit", unit);
            JSONObject obj = new JSONObject();
            obj.put(key, reading);
            jsonObject.put(jsonDate, obj);
            String userString = jsonObject.toString();
            FileWriter fileWriter = new FileWriter(file, false);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(userString);
            bufferedWriter.close();
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    private void writeTippersJSON(String tippersTemp, String timestamp, String tempUnit) {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        assert locationManager != null;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if  (location != null) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            File tippersFile;
            FileReader tippersFileReader;
            BufferedReader tippersBufferedReader;
            String FILE_NAME = "tippers.json";
            tippersFile = new File(this.getFilesDir(), FILE_NAME);
            try {
                tippersFileReader = new FileReader(tippersFile);
                tippersBufferedReader = new BufferedReader(tippersFileReader);
                String tippersLine = tippersBufferedReader.readLine();
                String array1 = null;
                if (tippersLine != null) {
                    StringBuilder tippersString = new StringBuilder();
                    while (tippersLine != null) {
                        tippersString.append(tippersLine).append("\n");
                        tippersLine = tippersBufferedReader.readLine();
                    }
                    tippersBufferedReader.close();
                    String tippersResponse = tippersString.toString();
                    JSONArray firstArray = new JSONArray(tippersResponse);
                    array1 = firstArray.toString();
                }
                JSONObject tippersData = new JSONObject();
                tippersData.put("ID", retrieveID());
                tippersData.put("timestamp", timestamp);
                tippersData.put("Latitude", latitude);
                tippersData.put("Longitude", longitude);
                JSONObject read = new JSONObject();
                read.put("val", tippersTemp);
                read.put("unit", tempUnit);
                tippersData.put("data", read);
                JSONArray appendArray = new JSONArray();
                appendArray.put(tippersData);
                String array2 = appendArray.toString();
                ObjectMapper mapper = new ObjectMapper();
                JsonNode tree1;
                if (array1 != null) {
                    tree1 = mapper.readTree(array1);
                    JsonNode tree2 = mapper.readTree(array2);
                    ((ArrayNode) tree1).addAll((ArrayNode) tree2);
                } else {
                    tree1 = mapper.readTree(array2);
                }
                String tippersJSON = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tree1);
                FileWriter fileWriter = new FileWriter(tippersFile, false);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                bufferedWriter.write(tippersJSON);
                bufferedWriter.close();
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void addEntry(String temperature) {
        LineData data = mChart.getData();
        if (data != null) {
            MyLineDataSet mySet = (MyLineDataSet) data.getDataSetByIndex(0);
            if (mySet == null) {
                mySet = createSet();
                data.addDataSet(mySet);
            }
            float y = Float.parseFloat(temperature);
            graphVals.add(y);
            data.addEntry(new Entry(mySet.getEntryCount(), y), 0);
            XAxis xl = mChart.getXAxis();
            xl.setValueFormatter(new IndexAxisValueFormatter(time)); // TODO: x-axis is still wack
            xl.setGranularityEnabled(true);
            xl.setGranularity(1f);
            xl.setSpaceMax(0.3f);
            data.notifyDataChanged();
            mChart.notifyDataSetChanged();
            mChart.setVisibleXRangeMaximum(6);
            mChart.moveViewToX(data.getEntryCount());
        }
    }

    private MyLineDataSet createSet() {
        MyLineDataSet mySet = new MyLineDataSet(null, null);
        mySet.setDrawCircles(true);
        mySet.setCircleRadius(4f);
        mySet.setDrawValues(false);
        mySet.setFillAlpha(65);
        mySet.setCircleHoleColor(Color.WHITE);
        mySet.setAxisDependency(YAxis.AxisDependency.LEFT);
        mySet.setLineWidth(3f);
        mySet.setMode(LineDataSet.Mode.LINEAR);
        int colorId;
        if (good) {
            colorId = R.color.green;
            mySet.setCircleColor(Color.parseColor("#009e48"));
        } else if (warning) {
            colorId = R.color.yellow;
            mySet.setCircleColor(Color.parseColor("#d6b300"));
        } else if (bad) {
            colorId = R.color.orange;
            mySet.setCircleColor(Color.parseColor("#d4600b"));
        } else if (veryBad) {
            colorId = R.color.red;
            mySet.setCircleColor(Color.parseColor("#d10000"));
        } else {
            colorId = R.color.blue;
            mySet.setCircleColor(Color.parseColor("#0099d1"));
        }
        mySet.setColors(ContextCompat.getColor(this, colorId));
        return mySet;
    }

    public static void savePrefsData(Context context, String name, String address) {
        SharedPreferences preferences = context.getSharedPreferences("devicePrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("device", name);
        editor.putString("address", address);
        editor.apply();
    }

    private String restoreNameData() {
        SharedPreferences pref = getApplicationContext().getSharedPreferences("devicePrefs", MODE_PRIVATE);
        return pref.getString("device", null);
    }

    private String restoreAddressData() {
        SharedPreferences pref = getApplicationContext().getSharedPreferences("devicePrefs", MODE_PRIVATE);
        return pref.getString("address", null);
    }

    public static void saveIdx(int idx, Context context) {
        SharedPreferences preferences = context.getSharedPreferences("jsonIdx", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("idx", idx);
        editor.apply();
    }

    public static String restoreIdx(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("jsonIdx", MODE_PRIVATE);
        int idx = preferences.getInt("idx", 0) + 1;
        return "Notif " + idx;
    }

    public static void saveTempUnit(String tempUnit, Context context) {
        SharedPreferences prefs = context.getSharedPreferences("MainUnitPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("tempUnit", tempUnit);
        editor.apply();
    }

    public static void saveTempVal(float medianTemp, Context context) {
        SharedPreferences prefs = context.getSharedPreferences("MainUnitPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat("tempVal", medianTemp);
        editor.apply();
    }

    public static float restoreTempVal(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("MainUnitPrefs", MODE_PRIVATE);
        return prefs.getFloat("tempVal", 0);
    }

    public static String restoreTempUnit(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("MainUnitPrefs", MODE_PRIVATE);
        return prefs.getString("tempUnit", " °F");
    }

    private String retrieveID() {
        SharedPreferences pref = getApplicationContext().getSharedPreferences("IDprefs", MODE_PRIVATE);
        return pref.getString("uniqueID", "-1");
    }

    private void saveFileCreation() {
        SharedPreferences preferences = getApplicationContext().getSharedPreferences("filePrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("files", true);
        editor.apply();
    }

    private boolean fileCreated() {
        SharedPreferences pref = getApplicationContext().getSharedPreferences("filePrefs", MODE_PRIVATE);
        return pref.getBoolean("files", false);
    }

    private void booleanUpdate(String val) {
        if (val != null && !val.isEmpty()) {
            float y = Float.parseFloat(temperature);
            good = false;
            bad = false;
            warning = false;
            veryBad = false;
            if (unit.equals(" °F")) {
                if (y <= 99.9) {
                    good = true;
                } else if (y < 100.4 && y >= 100) {
                    warning = true;
                } else if (y >= 100.4 && y <= 102.9) {
                    bad = true;
                } else {
                    veryBad = true;
                }
            } else {
                if (y <= 37.7) {
                    good = true;
                } else if (y < 38 && y >= 37.8) {
                    warning = true;
                } else if (y >= 38 && y <= 39.4) {
                    bad = true;
                } else {
                    veryBad = true;
                }
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        boolean status = intent.getBooleanExtra("status", false);
        if (status) {
            cancelJob(0);
            Log.d(TAG, "Urgent Identified");
            firstNotif = true;
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AndroidService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(AndroidService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(AndroidService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(AndroidService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UNIT_CHANGE);
        return intentFilter;
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
        if (spark) {
            btStat.setText("Connected");
            btSym.setBackgroundResource(R.drawable.ic_b1);
        } else {
            btStat.setText("Not Connected");
            btSym.setBackgroundResource(R.drawable.ic_b2);
        }
        int num;
        if (temperature != null && !temperature.isEmpty() && !temperature.equals("--")) {
            float y = Float.parseFloat(temperature);
            if (unit.equals(" °F")) {
                if (y <= 99.9) {
                    num = 1;
                } else if (y < 100.4 && y >= 100) {
                    num = 2;
                } else if (y >= 100.4 && y <= 102.9) {
                    num = 3;
                } else {
                    num = 4;
                }
            } else {
                if (y <= 37.7) {
                    num = 1;
                } else if (y <= 38 && y >= 37.8) {
                    num = 2;
                } else if (y > 38 && y <= 39.4) {
                    num = 3;
                } else {
                    num = 4;
                }
            }
        } else {
            num = 0;
        }
        tempDisplay(num);

        File file;
        FileReader fileReader;
        BufferedReader bufferedReader;
        String FILE_NAME = "notif.json";
        file = new File(this.getFilesDir(), FILE_NAME);
        try {
            fileReader = new FileReader(file);
            bufferedReader = new BufferedReader(fileReader);
            StringBuilder stringBuilder = new StringBuilder();
            String line = bufferedReader.readLine();
            while (line != null) {
                stringBuilder.append(line).append("\n");
                line = bufferedReader.readLine();
            }
            bufferedReader.close();
            String response = stringBuilder.toString();
            JSONObject jsonObject = new JSONObject(response);
            JSONArray names = jsonObject.names();
            if (names != null) {
                notif.setImageResource(R.drawable.bell2);
            } else {
                notif.setImageResource(R.drawable.bell);
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(gattUpdateReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (onStopBooleanUrgent) {
            if (unit.equals(" °F")) {
                if (Float.parseFloat(temperature) > 100.3) {
                    scheduleUrgentJob();
                    onStopBooleanUrgent = false;
                }
            } else {
                if (Float.parseFloat(temperature) >= 38) {
                    scheduleUrgentJob();
                    onStopBooleanUrgent = false;
                }
            }
            Toast.makeText(this, "onStop", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLocationChanged(Location location) {}

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}
}