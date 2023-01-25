package com.aditya.secquraisemobilestatus;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*
        * Implementation of a simple splash screen using
        * open-source Glide library
        * */

        ImageView imageView = findViewById(R.id.splashScreenImageView);
        Glide.with(this).load(R.drawable.splashscreen).into(imageView);

        new CountDownTimer(3000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }
            @Override
            public void onFinish() {
                Intent intent = new Intent(MainActivity.this, MobileStatusActivity.class);
                startActivity(intent);
                finish();
            }
        }.start();
    }

}