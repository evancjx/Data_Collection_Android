package com.lta_ms_android.s3;

import android.annotation.SuppressLint;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.provider.Settings;
import android.util.Log;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.lta_ms_android.MainActivity;

import java.io.File;

import static com.lta_ms_android.MainActivity.data_path;
import static com.lta_ms_android.MainActivity.MobileUUID;

import static com.lta_ms_android.MainActivity.list_string_logs;
import static com.lta_ms_android.MainActivity.username;
import static com.lta_ms_android.access.access.MGQ.*;

public class Database extends JobService {
    private final String TAG = this.getClass().getSimpleName();
    private AmazonS3Client s3Client;

    private static final String des_folder = "msp-waypoints-data/new-files/";

    @SuppressLint("HardwareIds")
    @Override
    public void onCreate(){
        super.onCreate();
        MobileUUID = Settings.Secure.getString(
            getApplicationContext().getContentResolver(),
            Settings.Secure.ANDROID_ID
        );
        s3Client = new AmazonS3Client(
            new BasicAWSCredentials(AK, SK)
        );
        s3Client.setRegion(Region.getRegion(Regions.AP_SOUTHEAST_1));
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        Log.d(TAG, "onStartJob: " + params.getJobId());

        Runnable uploadTask = () -> {
            try{
                for (File f: new File (data_path).listFiles()) upload(f.getName());
            }
            catch (Exception ex){
                ex.printStackTrace();
            }
            jobFinished(params, true); //do not need to reschedule this Job
        };
        new Thread(uploadTask).start();
        return true;
    }
    @Override
    public boolean onStopJob(JobParameters jobParameters) {return false;}

    private void upload(String filename){
        if(filename == null) return;
        File file = new File(data_path +filename);
        if(!file.exists()) return;
        TransferUtility transferUtility = TransferUtility.builder()
            .context(getApplicationContext())
            .s3Client(s3Client)
            .build();

        TransferObserver uploadObserver = transferUtility.upload(
            BK,
            username+"/"+MobileUUID+ "/"+filename, //this is the path and name
            file //path to the file locally
        );

        // Attach a listener to the observer to get state update and progress notifications
        uploadObserver.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                if (TransferState.COMPLETED == state) {
                    // Handle a completed upload.
                    Log.d(TAG, "Completed: " + uploadObserver.getKey());
                    list_string_logs.add(uploadObserver.getKey().split("/")[2]+" uploaded");
                    MainActivity.getInstance().update_logs_view();

                    File file = new File(
                        data_path+uploadObserver.getKey().split("/")[2]
                    );
                    if(!file.delete() && file.exists()){
                        try{
                            if(!file.getCanonicalFile().delete() && file.exists())
                                getApplicationContext().deleteFile(file.getName());
                        }
                        catch (Exception ex){
                            ex.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                int percentDone = (int)((float) bytesCurrent / (float) bytesTotal) * 100;

                Log.d(TAG, "onProgressChanged ID:" + id + " bytesCurrent: " + bytesCurrent
                        + " bytesTotal: " + bytesTotal + " " + percentDone + "%");
            }

            @Override
            public void onError(int id, Exception ex) {
                // Handle errors
                Log.d(TAG, "Error| ID:"+id+" "+ex.getMessage());
            }
        });

        // If you prefer to poll for the data, instead of attaching a
        // listener, check for the state and progress in the observer.
//        if (TransferState.COMPLETED == uploadObserver.getState()) {
//            // Handle a completed upload.
//            Log.d(TAG, "Completed@");
//            return;
//        }
//
//        Log.d(TAG, "Bytes Transferred: " + uploadObserver.getBytesTransferred());
//        Log.d(TAG, "Bytes Total: " + uploadObserver.getBytesTotal());
    }
}
