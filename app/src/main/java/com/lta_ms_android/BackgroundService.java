package com.lta_ms_android;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.lta_ms_android.s3.Database;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;

import static com.lta_ms_android.MainActivity.LOCATION_MANAGER;
import static com.lta_ms_android.MainActivity.MobileUUID;
import static com.lta_ms_android.MainActivity.adapter_log;
import static com.lta_ms_android.MainActivity.list_string_logs;
import static com.lta_ms_android.MainActivity.sensor_records;
import static com.lta_ms_android.MainActivity.transportLabel;

public class BackgroundService extends Service{
    private final String TAG = this.getClass().getSimpleName();

    private Runnable writeData;
    private final Handler sensorSave = new Handler();
    SensorManager SENSOR_MANAGER = null;
    JobScheduler jobScheduler = null;

    private static final int
        sensor_freq = SensorManager.SENSOR_DELAY_FASTEST,
        save_freq = 60*1000,
        upload_freq = 60*1000;

    private static final int LOCATION_INTERVAL = 1000;
    private static final float LOCATION_DISTANCE = 0;
    LocationListener[] locationListeners = new LocationListener[]{
        new LocationListener(LocationManager.GPS_PROVIDER),
        new LocationListener(LocationManager.NETWORK_PROVIDER)
    };
    private class LocationListener implements android.location.LocationListener{
        Location lastLocation;
        LocationListener(String provider) {
            Log.i(TAG,"LocationListener " + provider);lastLocation = new Location(provider);
        }
        @Override
        public void onLocationChanged(Location location) {
            long timeMilli = System.currentTimeMillis();
            //Log.i(TAG, "onLocationChanged");
            try {
                String sensor_name = "location";
                JSONObject record = new JSONObject()
                    .put("Timestamp", timeMilli)
                    .put("Mode", transportLabel)
                    .put("Latitude", location.getLatitude())
                    .put("Longitude", location.getLongitude())
                    .put("Altitude", location.getAltitude());
                if (sensor_records.has(sensor_name)){
                    sensor_records.getJSONArray(sensor_name)
                        .put(record);
                }
                else{
                    sensor_records.put(
                        sensor_name,
                        new JSONArray().put(record)
                    );
                }
            }
            catch (JSONException json_ex){
                json_ex.printStackTrace();
            }
            lastLocation.set(location);
        }
        @Override
        public void onProviderDisabled(String provider){Log.i(TAG, "onProviderDisabled: "+provider);}
        @Override
        public void onProviderEnabled(String provider){Log.i(TAG, "onProviderEnabled: "+provider);}
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras){Log.i(TAG, "onStatusChanged: "+provider);}
    }

    private SensorEventListener ACC_LISTENER = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            long timeMilli = System.currentTimeMillis();
            try{
                String sensor_name = "accelerometer";
                JSONObject record = new JSONObject()
                    .put("Timestamp", timeMilli)
                    .put("Mode", transportLabel)
                    .put("X", event.values[0])
                    .put("Y", event.values[1])
                    .put("Z", event.values[2]);
                if (sensor_records.has(sensor_name)){
                    sensor_records.getJSONArray(sensor_name)
                        .put(record);
                }
                else{
                    sensor_records.put(
                        sensor_name,
                        new JSONArray().put(record)
                    );
                }
            }
            catch (JSONException json_ex){
                json_ex.printStackTrace();
            }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {}
    };
    private SensorEventListener LIN_ACC_LISTENER = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            long timeMilli = System.currentTimeMillis();
            try{
                String sensor_name = "linear_acceleration";
                JSONObject record = new JSONObject()
                    .put("Timestamp", timeMilli)
                    .put("Mode", transportLabel)
                    .put("X", event.values[0])
                    .put("Y", event.values[1])
                    .put("Z", event.values[2]);
                if (sensor_records.has(sensor_name)){
                    sensor_records.getJSONArray(sensor_name)
                        .put(record);
                }
                else{
                    sensor_records.put(
                        sensor_name,
                        new JSONArray().put(record)
                    );
                }
            }
            catch (JSONException json_ex){
                json_ex.printStackTrace();
            }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {}
    };
    private SensorEventListener BAR_LISTENER = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            long timeMilli = System.currentTimeMillis();
            try{
                String sensor_name = "barometer";
                JSONObject record = new JSONObject()
                        .put("Timestamp", timeMilli)
                        .put("Mode", transportLabel)
                        .put("Pressure", event.values[0]);
                if (sensor_records.has(sensor_name)){
                    sensor_records.getJSONArray(sensor_name)
                        .put(record);
                }
                else{
                    sensor_records.put(
                        sensor_name,
                        new JSONArray().put(record)
                    );
                }
            }
            catch (JSONException json_ex){
                json_ex.printStackTrace();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };
    private SensorEventListener GYR_LISTENER = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            long timeMilli = System.currentTimeMillis();
            try{
                String sensor_name = "gyroscope";
                JSONObject record = new JSONObject()
                    .put("Timestamp", timeMilli)
                    .put("Mode", transportLabel)
                    .put("X", event.values[0])
                    .put("Y", event.values[1])
                    .put("Z", event.values[2]);
                if (sensor_records.has(sensor_name)){
                    sensor_records.getJSONArray(sensor_name)
                        .put(record);
                }
                else{
                    sensor_records.put(
                        sensor_name,
                        new JSONArray().put(record)
                    );
                }
            }
            catch (JSONException json_ex){
                json_ex.printStackTrace();
            }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {}
    };
    private SensorEventListener ROT_LISTENER = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            long timeMilli = System.currentTimeMillis();
            try{
                String sensor_name = "rotation_vector";
                JSONObject record = new JSONObject()
                    .put("Timestamp", timeMilli)
                    .put("Mode", transportLabel)
                    .put("X", event.values[0])
                    .put("Y", event.values[1])
                    .put("Z", event.values[2])
                    .put("scalar", event.values[3]);
                if (sensor_records.has(sensor_name)){
                    sensor_records.getJSONArray(sensor_name)
                        .put(record);
                }
                else{
                    sensor_records.put(
                        sensor_name,
                        new JSONArray().put(record)
                    );
                }
            }
            catch (JSONException json_ex){
                json_ex.printStackTrace();
            }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {}
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onCreate(){
        super.onCreate();
        list_string_logs = new ArrayList<>();
        try{
            sensor_records = new JSONObject()
                .put("accelerometer", new JSONArray())
                .put("barometer", new JSONArray())
                .put("linear_acceleration", new JSONArray())
                .put("gyroscope", new JSONArray())
                .put("rotation_vector", new JSONArray())
                .put("location", new JSONArray());
        }
        catch (JSONException json_ex){
            json_ex.printStackTrace();
        }

        String CHANNEL_ID = "com.lta_ms_android";
        String CHANNEL_NAME = "Background Service";

        Intent notificationIntent = new Intent(
                this,
                MainActivity.class
        );

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                0
        );

        Notification notification;
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O){
            Log.d(TAG, "App Version smaller than Build Version");
            notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.icon_lta_circle_white_blue)
                .setContentTitle("LTA Mobility Sensing in Background")
                .setContentText("Touch to open application")
                .setContentIntent(pendingIntent)
                .build();
        }
        else{
            Log.d(TAG, "App Version equal or greater than Build Version");
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setLightColor(Color.BLUE);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(channel);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this,CHANNEL_ID);
            notification = notificationBuilder
                .setOngoing(true)
                .setSmallIcon(R.drawable.icon_lta_circle_white_blue)
                .setContentTitle("LTA Mobility Sensing in Background")
                .setContentText("Touch to open application")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        }
        startForeground(1337, notification);
    }
    @Override
    public void onDestroy(){
        Log.i(TAG, "Background service destroy");
        super.onDestroy();
        kill_all_service();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        adapter_log = new ArrayAdapter<>(
                getApplicationContext(),
                R.layout.activity_listview,
                list_string_logs
        );
        init_sensor(); init_location_service(); init_scheduler();
        init_write_schedule();
        return START_STICKY;
    }

    private void init_sensor(){
        SENSOR_MANAGER = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        assert SENSOR_MANAGER != null;
        get_available_sensor();
        MainActivity.getInstance().update_logs_view();
        PackageManager PM = this.getPackageManager();
        if(PM.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)){
            SENSOR_MANAGER.registerListener(
                ACC_LISTENER,
                SENSOR_MANAGER.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                sensor_freq
            );
            SENSOR_MANAGER.registerListener(
                LIN_ACC_LISTENER,
                SENSOR_MANAGER.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                sensor_freq
            );
        }
        if(PM.hasSystemFeature(PackageManager.FEATURE_SENSOR_BAROMETER)){
            SENSOR_MANAGER.registerListener(
                BAR_LISTENER,
                SENSOR_MANAGER.getDefaultSensor(Sensor.TYPE_PRESSURE),
                sensor_freq
            );
        }
        if (PM.hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE)){
            SENSOR_MANAGER.registerListener(
                GYR_LISTENER,
                SENSOR_MANAGER.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                sensor_freq
            );
            SENSOR_MANAGER.registerListener(
                ROT_LISTENER,
                SENSOR_MANAGER.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                sensor_freq
            );
        }
    }
    private void init_location_service(){
        if (LOCATION_MANAGER == null)
            LOCATION_MANAGER = (LocationManager) getApplication().getSystemService(Context.LOCATION_SERVICE);
        if(
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ){
            LOCATION_MANAGER.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                LOCATION_INTERVAL,
                LOCATION_DISTANCE,
                locationListeners[1]
            );
            LOCATION_MANAGER.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                LOCATION_INTERVAL,
                LOCATION_DISTANCE,
                locationListeners[0]
            );
        }
    }
    private void init_write_schedule(){
        writeData = new Runnable() {
            @Override
            public void run() {
                long current_millis = System.currentTimeMillis();
                save_data(current_millis);
                sensorSave.postDelayed(this, save_freq);
            }
        };
        sensorSave.postDelayed(writeData, save_freq);
    }
    private void init_scheduler(){
        jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        JobInfo upload_data_job = new JobInfo.Builder(
            1001,
            new ComponentName(this, Database.class)
        )
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
            .setPersisted(true)
            .setPeriodic(upload_freq, upload_freq)
//            .setMinimumLatency(upload_freq)
//            .setOverrideDeadline(3*upload_freq)
            .build();
        if(jobScheduler != null){
            jobScheduler.schedule(upload_data_job);
            Log.i(TAG, "JobSchedule: Upload data job is scheduled");
        }
    }
    private void save_data(long current_millis){
        JSONObject saving = sensor_records; // Copy JSONObject
        sensor_records = new JSONObject(); // Replace with empty JSONObject
        // Write to JSON File
        try{
            write_file(saving.toString(2), current_millis);
        }
        catch (JSONException json_ex){
            json_ex.printStackTrace();
            write_file(saving.toString(), current_millis);
        }
    }
    private void write_file(String data, long current_millis){
        if (MobileUUID==null){
            Log.e(TAG, "Mobile UUID cannot be Null");
            MobileUUID = MainActivity.getInstance().get_MobileUUID();
        }
        File sdcard = Environment.getExternalStorageDirectory();
        File msp_folder = new File(sdcard.getAbsolutePath() + "/msp_data/");
        Log.i(TAG, msp_folder.toString());
        if (!msp_folder.exists()) Log.i(TAG, "Create folder "+msp_folder.mkdir());

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH_mm_ss-SSS", Locale.UK);
        dateFormat.setTimeZone(TimeZone.getDefault());
        String datetime = dateFormat.format(current_millis);
        File records_file = new File(msp_folder, MobileUUID+"_"+datetime+".json");
        try{
            FileOutputStream out = new FileOutputStream(records_file);
            out.write(data.getBytes()); out.close();
        }
        catch (IOException e){
            e.printStackTrace();
            list_string_logs.add(datetime+" Save records failed");
        }
        finally {
            Log.i(TAG, "File saved: "+records_file.toString());
            list_string_logs.add(datetime+" Save records successful");
        }
        MainActivity.getInstance().update_logs_view();
    }
    private void kill_all_service(){
        save_data(System.currentTimeMillis());
        try {
            SENSOR_MANAGER.unregisterListener(ACC_LISTENER);
            SENSOR_MANAGER.unregisterListener(LIN_ACC_LISTENER);
            SENSOR_MANAGER.unregisterListener(BAR_LISTENER);
            SENSOR_MANAGER.unregisterListener(GYR_LISTENER);
            SENSOR_MANAGER.unregisterListener(ROT_LISTENER);
            LOCATION_MANAGER.removeUpdates(locationListeners[0]);
            LOCATION_MANAGER.removeUpdates(locationListeners[1]);
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
        jobScheduler.cancelAll();
        sensorSave.removeCallbacks(writeData);
    }
    private void get_available_sensor(){
        SensorManager SENSOR_MANAGER = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        assert SENSOR_MANAGER != null;
        for (Sensor sen: SENSOR_MANAGER.getSensorList(Sensor.TYPE_ALL)){
            String sensor_type = null;
            switch (sen.getType()){
                case Sensor.TYPE_ACCELEROMETER:
                    sensor_type = "Accelerometer";
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    sensor_type = "Magnetic Field";
                    break;
                case Sensor.TYPE_ORIENTATION:
                    sensor_type = "Orientation";
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    sensor_type = "Gyroscope";
                    break;
                case Sensor.TYPE_LIGHT:
                    sensor_type = "Light";
                    break;
                case Sensor.TYPE_PRESSURE:
                    sensor_type = "Barometer";
                    break;
                case Sensor.TYPE_TEMPERATURE: //7
                    sensor_type = "Thermometer";
                    break;
                case Sensor.TYPE_PROXIMITY: //8
                    sensor_type = "Proximity";
                    break;
                case Sensor.TYPE_GRAVITY://9
                    sensor_type = "Gravity";
                    break;
                case Sensor.TYPE_LINEAR_ACCELERATION://10
                    sensor_type = "Linear Acceleration";
                    break;
                case Sensor.TYPE_ROTATION_VECTOR://11
                    sensor_type = "Rotation Vector";
                    break;
                case Sensor.TYPE_RELATIVE_HUMIDITY://12
                    sensor_type = "Relative Humidity";
                    break;
                case Sensor.TYPE_AMBIENT_TEMPERATURE://13
                    sensor_type = "Ambient Thermometer";
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED://14
                    sensor_type = "UnCal Magnetic Field";
                    break;
                case Sensor.TYPE_GAME_ROTATION_VECTOR://15
                    sensor_type = "Game Rotation Vector";
                    break;
                case Sensor.TYPE_GYROSCOPE_UNCALIBRATED://16
                    sensor_type = "UnCal Gyroscope";
                    break;
                case Sensor.TYPE_SIGNIFICANT_MOTION://17
                    sensor_type = "Significant Motion";
                    break;
                case Sensor.TYPE_STEP_DETECTOR://18
                    sensor_type = "Step Detector";
                    break;
                case Sensor.TYPE_STEP_COUNTER://19
                    sensor_type = "Step Counter";
                    break;
                case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR://20
                    sensor_type = "GeoMagnetic Rotation Vector";
                    break;
                case Sensor.TYPE_HEART_RATE://21
                    sensor_type = "Heart Rate";
                    break;
                case Sensor.TYPE_STATIONARY_DETECT://20
                    sensor_type = "Stationary Detect";
                    break;
                default:
                    sensor_type = sen.getName();
                    Log.e(TAG, sen.toString());
            }
            list_string_logs.add(sensor_type+" exist on the phone");
        }
    }
}
