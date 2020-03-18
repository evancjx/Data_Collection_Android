package com.lta_ms_android.s3;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
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

import static com.lta_ms_android.MainActivity.MobileUUID;
import static com.lta_ms_android.MainActivity.data_path;
import static com.lta_ms_android.MainActivity.list_string_logs;
import static com.lta_ms_android.MainActivity.username;
import static com.lta_ms_android.access.access.MGQ.AK;
import static com.lta_ms_android.access.access.MGQ.BK;
import static com.lta_ms_android.access.access.MGQ.SK;
import static com.lta_ms_android.utilities.helper.get_MobileUUID;

public class Database extends JobService {
    private final String TAG = this.getClass().getSimpleName();
    private static AmazonS3Client s3Client;
    private static TransferUtility transferUtility;

    private static final String des_folder = "msp-waypoints-data/new-files/";

    @Override
    public void onCreate(){
        super.onCreate();
        MobileUUID = get_MobileUUID(getApplicationContext());
        transferUtility = getTransferUtility(getApplicationContext());
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

    /**
     * Gets an instance of a S3 client which is constructed using the given
     * Context.
     *
     * @return A default S3 client.
     */
    private static AmazonS3Client getS3Client(){
        if (s3Client == null){
            s3Client = new AmazonS3Client(
                new BasicAWSCredentials(AK, SK),
                Region.getRegion(Regions.AP_SOUTHEAST_1)
            );
        }
        return s3Client;
    }
    /**
     * Gets an instance of the TransferUtility which is constructed using the
     * given Context
     *
     * @param context An Context instance.
     * @return a TransferUtility instance
     */
    private static TransferUtility getTransferUtility(Context context) {
        if (transferUtility == null) {
            //transferUtility = new TransferUtility(
            //    getS3Client(context.getApplicationContext()),
            //    context.getApplicationContext()
            //);
            transferUtility = TransferUtility.builder()
                .context(context)
                .s3Client(getS3Client())
                .build();
        }
        return transferUtility;
    }

    private void upload(String filename){
        if(filename==null || username==null) return;
        File file = new File(data_path +filename);
        if(!file.exists()) return;
        TransferUtility transferUtility = getTransferUtility(getApplicationContext());

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
                    Log.d(TAG, "Completed: "+uploadObserver.getKey());
                    String filename = uploadObserver.getKey().split("/")[2];
                    list_string_logs.add(0, filename+" uploaded");
                    MainActivity.getInstance().update_logs_view();
                    delete_file(filename);
                }
            }
            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                Log.d(
                    TAG,
                    String.format(
                        "[onProgressChanged] id: %d, total: %d, current: %d, percent: %.2f %%",
                        id, bytesTotal, bytesCurrent, ((float)bytesCurrent/(float) bytesTotal)*100
                    )
                );
            }
            @Override
            public void onError(int id, Exception ex) {// Handle errors
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
    private void delete_file(String filename){
        File file = new File(data_path+filename);
        if(!file.delete() && file.exists()){
            try{
                if(!file.getCanonicalFile().delete() && file.exists())
                    getApplicationContext().deleteFile(file.getName());
            }
            catch (Exception ex){
                ex.printStackTrace();
            }
        }
        if (!file.exists())
            Log.d(
                TAG,
                file.getAbsolutePath()+" Deleted"
            );
    }
}