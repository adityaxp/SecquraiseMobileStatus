package com.aditya.secquraisemobilestatus;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.Random;
import java.util.concurrent.TimeUnit;


public class MobileStatusActivity extends AppCompatActivity {

    private Toolbar mobileStatusToolBar;
    private static final int REQUEST_CODE = 1;
    private TelephonyManager telephonyManager;
    private TextView imeiTextView, deviceNameTextView, connectionStatusTextView, chargingStatusTextView, batteryPercentageTextView, locationTextView, timeStampTextView;
    private ConnectivityManager connectivityManager;
    private Boolean connectionFlag = false, chargingFlag = false;
    private IntentFilter ifilter;
    private Intent batteryStatus;
    private int chargingStatus, batteryLevel, batteryScale;
    private float batteryPercentage;
    private FusedLocationProviderClient fusedLocationClient;
    private Bitmap photo;
    private ImageView capturedPhotoImageView;
    private Button captureImageButton, uploadDataButton;
    private Handler updateMobileStatusHandler;
    private String imageURL, locationLatitude, locationLongitude, timeStamp, imei;


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mobile_status);

        mobileStatusToolBar = findViewById(R.id.mobileStatusToolBar);
        imeiTextView = (TextView) findViewById(R.id.imeiTextView);
        deviceNameTextView = (TextView) findViewById(R.id.deviceNameTextView);
        connectionStatusTextView = (TextView) findViewById(R.id.connectionStatusTextView);
        chargingStatusTextView = (TextView) findViewById(R.id.chargingStatusTextView);
        locationTextView = (TextView) findViewById(R.id.locationTextView);
        capturedPhotoImageView = (ImageView) findViewById(R.id.capturedPhotoImageView);
        captureImageButton = (Button) findViewById(R.id.captureImageButton);
        timeStampTextView = (TextView) findViewById(R.id.timeStampTextView);
        batteryPercentageTextView = (TextView) findViewById(R.id.batteryPercentageTextView);
        uploadDataButton = (Button) findViewById(R.id.uploadDataButton);
        updateMobileStatusHandler = new Handler();


        setSupportActionBar(mobileStatusToolBar);
        setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mobileStatusToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        verifyPermissions();

        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        deviceNameTextView.setText(deviceNameTextView.getText() + " " + Build.MODEL);

        try {
            imei =  telephonyManager.getImei().toString();
            imeiTextView.setText(imeiTextView.getText() + " \n" + imei);
        } catch (Exception e) {
            imei = "unspecified";
            Toast.makeText(this, "IMEI retrieval failed", Toast.LENGTH_SHORT).show();
        }

        setMobileStatus();

        // 15 min delay
        UpdateMobileStatusRunnableThread updateMobileStatusRunnableThread = new UpdateMobileStatusRunnableThread(900000);
        new Thread(updateMobileStatusRunnableThread).start();


        uploadDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(getConnectionStatus()){
                    uploadData();
                }else{
                    uploadDataButton.setEnabled(false);
                    new AlertDialog.Builder(MobileStatusActivity.this)
                            .setTitle("No internet connection")
                            .setMessage("Data will be automatically uploaded after internet connection is established.")
                            .setCancelable(false)
                            .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    AutoUploadDataWhenConnectionEstablishedRunnableThread autoUploadDataWhenConnectionEstablishedRunnableThread = new AutoUploadDataWhenConnectionEstablishedRunnableThread();
                                    new Thread(autoUploadDataWhenConnectionEstablishedRunnableThread).start();
                                }
                            }).setNegativeButton("Cancel upload", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            uploadDataButton.setEnabled(true);

                        }
                    }).show();
                }

            }
        });


    }

    public void setMobileStatus(){
        clearMobileStatus();
        if (getConnectionStatus()) {
            connectionStatusTextView.setText(connectionStatusTextView.getText() + " \n" + "Connection established");
        } else {
            connectionStatusTextView.setText(connectionStatusTextView.getText() + " \n" + "No Internet");
        }

        if (getChargingStatus()) {
            chargingStatusTextView.setText(chargingStatusTextView.getText() + " \n" + "Charging");
        } else {
            chargingStatusTextView.setText(chargingStatusTextView.getText() + " \n" + "Not Charging");
        }

        batteryPercentageTextView.setText(batteryPercentageTextView.getText() + " \n" + String.valueOf((int) getBatteryPercentage()) + "%");

        setLocation();

        captureImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureImage(v);
            }
        });

        timeStamp = String.valueOf(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
        timeStampTextView.setText(timeStampTextView.getText() + " \n" + timeStamp);

    }

    public void clearMobileStatus(){
        connectionStatusTextView.setText("Connection status:");
        chargingStatusTextView.setText("Charging status:");
        batteryPercentageTextView.setText("Battery percentage:");
        locationTextView.setText("Location:");
        timeStampTextView.setText("Time Stamp:");
    }

    public Boolean getConnectionStatus(){
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        connectionFlag = (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED);
        return connectionFlag;
    }

    public Boolean getChargingStatus(){
        ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        batteryStatus = this.registerReceiver(null, ifilter);
        chargingStatus = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        chargingFlag = chargingStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
                chargingStatus == BatteryManager.BATTERY_STATUS_FULL;
        return chargingFlag;
    }

    public float getBatteryPercentage(){
        batteryLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        batteryScale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        batteryPercentage = batteryLevel * 100 / (float) batteryScale;
        return batteryPercentage;
    }

    public void setLocation(){
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            }
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                locationLatitude = String.valueOf(location.getLatitude());
                                locationLongitude = String.valueOf(location.getLongitude());
                                locationTextView.setText(locationTextView.getText() + " \n" + "Latitude: " + location.getLatitude() + " \n" + "Longitude: " + location.getLongitude());
                            }else{
                                Toast.makeText(MobileStatusActivity.this, "Failed to retrieve location", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }catch (Exception e){}
    }

    public void uploadImage(Bitmap bitmap){
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] imageData = byteArrayOutputStream.toByteArray();

        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child(randomString());
        UploadTask uploadTask = storageReference.putBytes(imageData);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
               imageURL = taskSnapshot.getStorage().getDownloadUrl().toString();
            }
        });
    }

    public void uploadData(){
        CustomLoadingDialog customLoadingDialog = new CustomLoadingDialog(MobileStatusActivity.this, "Uploading Data...");
        if(photo != null){
            uploadImage(photo);
            customLoadingDialog.customLoadingDialogShow();
            new CountDownTimer(2000, 100) {
                @Override
                public void onTick(long millisUntilFinished) {
                }
                @Override
                public void onFinish() {
                    MobileStatusData mobileStatusData = new MobileStatusData(imei, getConnectionStatus().toString(), getChargingStatus().toString(), String.valueOf(getBatteryPercentage()), locationLatitude, locationLongitude, imageURL, timeStamp);
                    FirebaseDatabase.getInstance("https://secquraisemobilestatus-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference().child(Build.MODEL).child(timeStamp).setValue(mobileStatusData).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Toast.makeText(MobileStatusActivity.this, "Upload Complete", Toast.LENGTH_SHORT).show();
                            customLoadingDialog.customLoadingDialogDismiss();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(MobileStatusActivity.this, "Upload Failed", Toast.LENGTH_SHORT).show();
                            customLoadingDialog.customLoadingDialogDismiss();
                        }
                    });
                }
            }.start();


        }else{
            Toast.makeText(MobileStatusActivity.this, "Please capture an image", Toast.LENGTH_SHORT).show();
            customLoadingDialog.customLoadingDialogDismiss();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.refresh, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case R.id.refresh:
                setMobileStatus();
                return true;
            default:
                return false;
        }
    }

        @Override
    public void onBackPressed() {
        finish();
    }

    public void captureImage(View view){
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, 2);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 2) {
            if (resultCode == RESULT_OK && data != null) {
                photo = (Bitmap) data.getExtras().get("data");
                capturedPhotoImageView.setImageBitmap(photo);
            } else {
                Toast.makeText(MobileStatusActivity.this, "Please capture an image", Toast.LENGTH_SHORT).show();
            }

        }

    }

    public String randomString(){
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder randomString = new StringBuilder();
        Random rnd = new Random();
        while (randomString.length() < 10) { // length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            randomString.append(SALTCHARS.charAt(index));
        }
        return randomString.toString();
    }

    private void verifyPermissions(){
        String[] permissions = {Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CAMERA};

        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                permissions[0]) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this.getApplicationContext(),
                permissions[1]) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this.getApplicationContext(),
                permissions[2]) == PackageManager.PERMISSION_GRANTED){
            Log.i("Permission: ", "Access Granted");
        }else{
            ActivityCompat.requestPermissions(MobileStatusActivity.this, permissions, REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        verifyPermissions();
    }

    class UpdateMobileStatusRunnableThread  implements Runnable {

        int updateTime;

        UpdateMobileStatusRunnableThread(int updateTime){
            this.updateTime = updateTime;
        }

        @Override
        public void run() {
            while (true){
                try {
                    Thread.sleep(updateTime);
                }catch (Exception e){}
                updateMobileStatusHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        setMobileStatus();
                    }
                });
            }
        }
    }

    class AutoUploadDataWhenConnectionEstablishedRunnableThread  implements Runnable {


        AutoUploadDataWhenConnectionEstablishedRunnableThread(){}

        @Override
        public void run() {
                try {
                   ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                   boolean connectionFlag = (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                            connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED);
                   while (!connectionFlag){
                       connectionFlag = (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                               connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED);
                   }
                }catch (Exception e){}
                updateMobileStatusHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        uploadData();
                        uploadDataButton.setEnabled(true);
                    }
                });
        }
    }
}