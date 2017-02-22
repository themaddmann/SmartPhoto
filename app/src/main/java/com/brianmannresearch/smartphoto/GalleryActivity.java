package com.brianmannresearch.smartphoto;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;

import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class GalleryActivity extends AppCompatActivity implements View.OnClickListener {

    private static int CAMERA_INTENT = 1, VIEW_INTENT = 2;

    private File imagesFolder;
    private Button returnButton, mapButton, uploadButton, resumeButton, imagesButton;
    private TextView tripinfo;
    private ProgressDialog dialog = null;

    private File[] files;
    private String foldername, upLoadServerUrl;
    private Boolean isempty = false;
    private int serverResponseCode = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        Bundle extras = getIntent().getExtras();
        if (extras != null){
            foldername = extras.getString("foldername");
        }

        upLoadServerUrl = "http://ndssl.000webhostapp.com/photos/uploadphoto.php";

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

        imagesFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), foldername);
        if (imagesFolder.listFiles().length == 0) {
            isempty = true;
            showEmptyAlert();
        }

        String sb = foldername + '\n' +
                "Number of photos in this trip: " + imagesFolder.listFiles().length;
        tripinfo.setText(sb);
    }

    private int uploadPhoto(String sourceFileUri) {
        HttpURLConnection conn;
        DataOutputStream dos;
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1024 * 1024;
        File sourceFile = new File(sourceFileUri);

        if (!sourceFile.isFile()) {
            dialog.dismiss();

            Log.e("uploadFile", "Source File does not exist: " + sourceFileUri);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(GalleryActivity.this, "File does not exist", Toast.LENGTH_LONG).show();
                }
            });
            return 0;
        } else {
            try {
                // open a URL connection to the server
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL(upLoadServerUrl);

                // open a HTTP connection to the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // allow inputs
                conn.setDoOutput(true); // allow outputs
                conn.setUseCaches(false); // prevent a cached copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("uploaded_file", sourceFileUri);

                dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=foldername;id=" + foldername + "\r\n");
                dos.writeBytes("\r\n");

                dos.writeBytes(foldername);
                dos.writeBytes("\r\n");
                dos.writeBytes("--" + boundary + "\r\n");

                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=uploaded_file;filename=" + sourceFileUri + "\r\n");
                dos.writeBytes("\r\n");

                // create a buffer of max size
                bytesAvailable = fileInputStream.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                // send multipart form data
                dos.writeBytes("\r\n");
                dos.writeBytes("--" + boundary + "--\r\n");

                // responses from the server
                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();
                Log.i("uploadFile", "HTTP Response is: " + serverResponseMessage + ": " + serverResponseCode);

                if (serverResponseCode == 200) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {Toast.makeText(GalleryActivity.this, "Upload complete...", Toast.LENGTH_LONG).show();}
                    });
                }

                // close the streams
                fileInputStream.close();
                dos.flush();
                dos.close();
            } catch (MalformedURLException ex) {
                dialog.dismiss();
                ex.printStackTrace();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(GalleryActivity.this, "Malformed URL", Toast.LENGTH_LONG).show();
                    }
                });
                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {
                dialog.dismiss();
                e.printStackTrace();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(GalleryActivity.this, "Exception detected", Toast.LENGTH_LONG).show();
                    }
                });
                Log.e("Upload file exception", "Exception: " + e.getMessage(), e);
            }
            dialog.dismiss();
            return serverResponseCode;
        }
    }

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
            case R.id.viewImagesButton:
                if (!isempty) {
                    Intent imagesIntent = new Intent(GalleryActivity.this, ImageActivity.class);
                    imagesIntent.putExtra("foldername", foldername);
                    startActivityForResult(imagesIntent, VIEW_INTENT);
                }
                break;
            case R.id.continueButton:
                Intent cameraIntent = new Intent(GalleryActivity.this, CameraActivity.class);
                cameraIntent.putExtra("folder", foldername);
                cameraIntent.putExtra("mode", "continue");
                startActivityForResult(cameraIntent, CAMERA_INTENT);
                break;
            case R.id.returnButton:
                Intent data = new Intent();
                String text = "Finished";
                data.setData(Uri.parse(text));
                setResult(RESULT_OK, data);
                finish();
                break;
            case R.id.mapButton:
                if (!isempty) {
                    Intent mapsIntent = new Intent(GalleryActivity.this, MapsActivity.class);
                    mapsIntent.putExtra("foldername", foldername);
                    startActivity(mapsIntent);
                }
                break;
            case R.id.bUpload:
                if(isempty) {
                    Toast.makeText(GalleryActivity.this, "Please add some photos to this trip before uploading", Toast.LENGTH_LONG).show();
                }else {
                    dialog = ProgressDialog.show(GalleryActivity.this, "", "Uploading...", true);
                    files = imagesFolder.listFiles();
                    for (int i = 0; i < files.length; i++) {
                        final String uploadname = files[i].toString();
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
                        }
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
