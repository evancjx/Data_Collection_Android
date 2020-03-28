package com.lta_ms_android;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.json.JSONObject;

import java.util.List;

import static com.lta_ms_android.utilities.helper.get_MobileUUID;

public class MainActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getSimpleName();
    private Context ctx;
    @SuppressLint("StaticFieldLeak")
    private static MainActivity instance;

    // User Interface
    Vibrator vibrator;

    SharedPreferences settings;
    TextView tv_Username;

    // Background Services
    Intent backgroundService;
    public static String transportLabel = "";
    public static JSONObject sensor_records = null;
    public static final int REQUEST_LOCATION_STORAGE_ACCESS = 0;
    @SuppressLint("StaticFieldLeak")
    public static ListView list_view_log; public static ArrayAdapter adapter_log;
    public static List<String> list_string_logs;
    public static LocationManager LOCATION_MANAGER = null;
    public static String data_path = Environment.getExternalStorageDirectory().getAbsoluteFile() + "/msp_data/";
    public static String MobileUUID = null, username = null;

    public static MainActivity getInstance() {return instance;}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); instance=this; ctx=this;
        setContentView(R.layout.activity_main);

        MobileUUID = get_MobileUUID(getApplicationContext());
        Log.e(TAG, "MobileUUID "+MobileUUID);
        settings = PreferenceManager.getDefaultSharedPreferences(this);

        check_permissions(); setup_UI();
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (list_string_logs != null && list_string_logs.size()>1){
            list_view_log.setAdapter(adapter_log);
            list_view_log.setDivider(null);
        }
        Log.i(TAG, "onResume");
        if(isServiceRunning(BackgroundService.class))
            Log.i(TAG, "onResume: Background running");
        else
            Log.i(TAG, "onResume: Background not running");
    }
    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        if(isServiceRunning(BackgroundService.class))
            Log.i(TAG, "onPause: Background running");
        else
            Log.i(TAG, "onPause: Background not running");
    }
    @Override
    public void onBackPressed(){moveTaskToBack(true); Log.i(TAG, "onBackPressed");}
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isServiceRunning(BackgroundService.class)) stopService(backgroundService);
        Log.i(TAG, "onDestroy");
        if(isServiceRunning(BackgroundService.class))
            Log.i(TAG, "onDestroy: Background running");
        else
            Log.i(TAG, "onDestroy: Background not running");
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "onRequest result called.");
        if (requestCode == REQUEST_LOCATION_STORAGE_ACCESS) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0) {// permission was granted
                for (int i=0; i<grantResults.length; i++){
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED)
                        Log.i(TAG, permissions[i] + " permission has now been granted.");
                    else
                        Log.i(TAG, permissions[i] + " permission was NOT granted.");
                }
            }
            else // permission denied,    Disable this feature or close the app.
                Log.i(TAG, "permissions was NOT granted.");
            check_username();
        }
        else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        Intent settingIntent;
        //noinspection SimplifiableIfStatement
        if (item.getItemId() == R.id.action_settings) {
            settingIntent = new Intent(this, Config.class);
            startActivityForResult(settingIntent, 0);
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult" + resultCode);
        if (resultCode == Activity.RESULT_OK)
            switch (requestCode){
                case 0:
                    if (check_username()){
                        String temp = settings.getString("username", null);
                        tv_Username.setText(temp);
                        username=temp;
                    }
                    else
                        Log.e(TAG, "onActivityResult: username error!");
                    break;
                default:
                    Log.e(TAG, "onActivityResult: error!");
            }
        else if (requestCode == Activity.RESULT_CANCELED){
            showToast("No changes made");
        }
    }

    private void setup_UI() {
        TextView tv_MobileUUID;
        tv_Username = findViewById(R.id.tv_Username);
        tv_MobileUUID = findViewById(R.id.tv_MobileUUID);
        username = settings.getString("username", "");
        tv_Username.setText(username);
        tv_MobileUUID.setText(MobileUUID);

        list_view_log = findViewById(R.id.lv_log);

        final ToggleButton
            btnTrain = findViewById(R.id.btn_train),
            btnBus = findViewById(R.id.btn_bus),
            btnCar = findViewById(R.id.btn_car),
            btnIdle = findViewById(R.id.btn_stationary),
            btnWalking = findViewById(R.id.btn_walking);

        btnTrain.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                btnBus.setChecked(false);
                btnCar.setChecked(false);
                btnWalking.setChecked(false);
                btnIdle.setChecked(false);
                transportLabel = "Train";
                Toast.makeText(getApplicationContext(), transportLabel, Toast.LENGTH_LONG).show();
            } else {
                transportLabel = "";
            }
        });
        btnBus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                btnTrain.setChecked(false);
                btnCar.setChecked(false);
                btnWalking.setChecked(false);
                btnIdle.setChecked(false);
                transportLabel = "Bus";
                Toast.makeText(getApplicationContext(), transportLabel, Toast.LENGTH_LONG).show();
            } else {
                transportLabel = "";
            }
        });
        btnCar.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                btnTrain.setChecked(false);
                btnBus.setChecked(false);
                btnWalking.setChecked(false);
                btnIdle.setChecked(false);
                transportLabel = "Car";
                Toast.makeText(getApplicationContext(), transportLabel, Toast.LENGTH_LONG).show();
            } else {
                transportLabel = "";
            }
        });
        btnIdle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                btnTrain.setChecked(false);
                btnBus.setChecked(false);
                btnCar.setChecked(false);
                btnWalking.setChecked(false);
                transportLabel = "Stationary";
                Toast.makeText(getApplicationContext(), transportLabel, Toast.LENGTH_LONG).show();
            } else {
                transportLabel = "";
            }
        });
        btnWalking.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                btnTrain.setChecked(false);
                btnBus.setChecked(false);
                btnCar.setChecked(false);
                btnIdle.setChecked(false);
                transportLabel = "Walking";
                Toast.makeText(getApplicationContext(), transportLabel, Toast.LENGTH_LONG).show();
            } else {
                transportLabel = "";
            }
        });
    }
    private void check_permissions(){
        if (
            ActivityCompat.checkSelfPermission(
                MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
                &&
            ActivityCompat.checkSelfPermission(
                MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (
                ActivityCompat.shouldShowRequestPermissionRationale(
                    MainActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ){
                Toast.makeText(
                        MainActivity.this,
                        "Access Fine Location permission allows us to collect GPS data. Please allow this permission in App Settings.",
                        Toast.LENGTH_LONG
                ).show();
            }
            if (
                ActivityCompat.shouldShowRequestPermissionRationale(
                    MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) {
                Toast.makeText(
                    MainActivity.this,
                    "Write External Storage permission allows us to create files. Please allow this permission in App Settings.",
                    Toast.LENGTH_LONG
                ).show();
            }
            ActivityCompat.requestPermissions(
                MainActivity.this,
                new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                },
                MainActivity.REQUEST_LOCATION_STORAGE_ACCESS
            );
        }
        else check_username();
    }
    private void start_background_service(){
        backgroundService = new Intent(MainActivity.this, BackgroundService.class);
        if(!isServiceRunning(BackgroundService.class))startService(backgroundService);
//        Intent simpleJobIntentService = new Intent(this, SimpleJobIntentService.class);
//        simpleJobIntentService.putExtra("maxCountValue",1000);
//        SimpleJobIntentService.enqueueWork(this, simpleJobIntentService);
    }
    private boolean isServiceRunning(Class<?> serviceClass){
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        assert manager != null;
        for (ActivityManager.RunningServiceInfo service: manager.getRunningServices(Integer.MAX_VALUE))
            if (serviceClass.getName().equals(service.service.getClassName()))
                return true;

        return false;
    }

    private boolean check_username(){
        if (settings.getString("username", null)==null){
            showToast("Please enter username");
            return false;
        }
        else {
            start_background_service();
            return true;
        }
    }

    public void update_logs_view(){
        // Remove old elements if the log list is more than 10
        if (list_string_logs.size() > 15)
            list_string_logs.subList(15, list_string_logs.size()).clear();
        // Place the log on the list view
        list_view_log.setAdapter(adapter_log);
        list_view_log.setDivider(null);
    }
    public void showToast(final String msg){
        runOnUiThread(() -> Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show());
    }
}
