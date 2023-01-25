package com.aditya.secquraisemobilestatus;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

public class CustomLoadingDialog {


    Activity activity;
    AlertDialog alertDialog;
    ImageView loadingImageView;
    TextView progressTitleTextView;
    String loadingText;

    public CustomLoadingDialog(Activity myActivity,String loadingText) {
        activity = myActivity;
        this.loadingText = loadingText;
    }

    void customLoadingDialogShow(){
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        View view = activity.getLayoutInflater().inflate(R.layout.custom_progress_dialog, null);

        progressTitleTextView = view.findViewById(R.id.progressTitleTextView);
        loadingImageView = view.findViewById(R.id.loadingImageView);

        Glide.with(activity).load(R.drawable.dualballloading).into(loadingImageView);
        progressTitleTextView.setText(loadingText);

        builder.setView(view);
        builder.setCancelable(false);

        alertDialog = builder.create();
        alertDialog.show();
    }

    void  customLoadingDialogDismiss(){
        alertDialog.dismiss();
    }
}
