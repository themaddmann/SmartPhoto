package com.brianmannresearch.smartphoto;

import android.content.DialogInterface;
import android.content.Intent;

import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import net.gotev.uploadservice.MultipartUploadRequest;

import java.io.File;
import java.util.UUID;

public class GalleryActivity extends AppCompatActivity implements View.OnClickListener {

    private static int CAMERA_INTENT = 1, VIEW_INTENT = 2;

    private File imagesFolder;
    private Button returnButton, mapButton, uploadButton, resumeButton, imagesButton;
    private TextView tripinfo;

    private File[] files;
    private String foldername, upLoadServerUrl;
    private Boolean isempty = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        // get data from previous activity
        Bundle extras = getIntent().getExtras();
        if (extras != null){
            foldername = extras.getString("foldername");
        }

        upLoadServerUrl = "http://sslab.nd.edu/photos/upload.php";

        // setup buttons for layout
        returnButton = (Button) findViewById(R.id.returnButton);
        mapButton = (Button) findViewById(R.id.mapButton);
        uploadButton = (Button) findViewById(R.id.bUpload);
        resumeButton = (Button) findViewById(R.id.continueButton);
        imagesButton = (Button) findViewById(R.id.viewImagesButton);

        tripinfo = (TextView) findViewById(R.id.tripinfo);

        returnButton.setOnClickListener(this);
        uploadButton.setOnClickListener(this);
        mapButton.setOnClickListener(this);
        resumeButton.setOnClickListener(this);
        imagesButton.setOnClickListener(this);

        // open folder and check if any images are in the trip
        // if not, show alert message
        imagesFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), foldername);
        if (imagesFolder.listFiles().length == 0) {
            isempty = true;
            showEmptyAlert();
        }

        // check size of trip
        String sb = foldername + '\n' +
                "Number of photos in this trip: " + imagesFolder.listFiles().length;
        tripinfo.setText(sb);
    }

    // upload function
    private void uploadMultipart(String uploadname){

        String [] split = uploadname.split("/");
        String name = split[split.length-1];

        try{
            String uploadId = UUID.randomUUID().toString();

            new MultipartUploadRequest(this, uploadId, upLoadServerUrl)
                    .addFileToUpload(uploadname, "image")
                    .addParameter("name", name)
                    .setMaxRetries(2)
                    .startUpload();
        }catch (Exception exc){
            Toast.makeText(this, exc.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // alert the user that no photos exist in this trip
    private void showEmptyAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setMessage("This trip is empty!")
                .setCancelable(false)
                .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
        final AlertDialog alert = alertDialog.create();
        alert.show();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            // launch image activity
            case R.id.viewImagesButton:
                if (!isempty) {
                    Intent imagesIntent = new Intent(GalleryActivity.this, ImageActivity.class);
                    imagesIntent.putExtra("foldername", foldername);
                    startActivityForResult(imagesIntent, VIEW_INTENT);
                }
                break;
            // continue current trip
            case R.id.continueButton:
                Intent cameraIntent = new Intent(GalleryActivity.this, CameraActivity.class);
                cameraIntent.putExtra("folder", foldername);
                cameraIntent.putExtra("mode", "continue");
                startActivityForResult(cameraIntent, CAMERA_INTENT);
                break;
            // return to main activity
            case R.id.returnButton:
                Intent data = new Intent();
                String text = "Finished";
                data.setData(Uri.parse(text));
                setResult(RESULT_OK, data);
                finish();
                break;
            // launch map activity
            case R.id.mapButton:
                if (!isempty) {
                    Intent mapsIntent = new Intent(GalleryActivity.this, MapsActivity.class);
                    mapsIntent.putExtra("foldername", foldername);
                    startActivity(mapsIntent);
                }
                break;
            // upload photos to server if applicable
            case R.id.bUpload:
                if(isempty) {
                    Toast.makeText(GalleryActivity.this, "Please add some photos to this trip before uploading", Toast.LENGTH_LONG).show();
                }else {
                    //dialog = ProgressDialog.show(GalleryActivity.this, "", "Uploading...", true);
                    files = imagesFolder.listFiles();
                    for (int i = 0; i < files.length; i++) {
                        final String uploadname = files[i].toString();
                        uploadMultipart(uploadname);
                        /*
                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                uploadPhoto(uploadname);
                            }
                        });
                        thread.start();
                        try {
                            thread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }*/
                    }
                }
                break;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_INTENT && resultCode == RESULT_OK) {
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        }else if (requestCode == VIEW_INTENT && resultCode == RESULT_OK){
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        }
    }
}
