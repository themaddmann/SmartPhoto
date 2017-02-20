package com.brianmannresearch.smartphoto;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class GalleryActivity extends AppCompatActivity implements View.OnClickListener {

    private static int CAMERA_INTENT = 1;

    private File imagesFolder, pictureFile;
    private int currentimage;
    private Bitmap currentBitmap = null;
    private TextView exifData;
    private ImageView defaultImage;
    private Button returnButton, deleteButton, mapButton, uploadButton, resumeButton;
    private ProgressDialog dialog = null;

    private ArrayList<String> imagesPath;
    private File[] files;
    private String[] filepath;
    private String foldername, upLoadServerUrl;
    private StringBuilder builder;
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

        defaultImage = (ImageView) findViewById(R.id.selectedImage);

        exifData = (TextView) findViewById(R.id.ExifData);

        upLoadServerUrl = "http://ndssl.000webhostapp.com/photos/uploadphoto.php";

        returnButton = (Button) findViewById(R.id.returnButton);
        deleteButton = (Button) findViewById(R.id.deleteButton);
        mapButton = (Button) findViewById(R.id.mapButton);
        uploadButton = (Button) findViewById(R.id.bUpload);
        resumeButton = (Button) findViewById(R.id.continueButton);

        returnButton.setOnClickListener(this);
        deleteButton.setOnClickListener(this);
        uploadButton.setOnClickListener(this);
        mapButton.setOnClickListener(this);
        resumeButton.setOnClickListener(this);

        defaultImage.setOnTouchListener(new OnSwipeTouchListener(GalleryActivity.this){
            public void onSwipeRight(){
                setBitmap("right");
            }
            public void onSwipeLeft(){
                setBitmap("left");
            }
        });

        imagesFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), foldername);
        if (imagesFolder.listFiles().length == 0){
            showEmptyAlert();
            isempty = true;
        }else {
            imagesPath = new ArrayList<>();
            files = imagesFolder.listFiles();
            for (int j = 0; j < files.length; j++) {
                imagesPath.add(files[j].toString());
            }

            currentimage = 0;
            currentBitmap = BitmapFactory.decodeFile(imagesPath.get(currentimage));

            ExifInterface exif = null;
            try {
                pictureFile = new File(imagesPath.get(currentimage));
                exif = new ExifInterface(pictureFile.getAbsolutePath());
                filepath = pictureFile.getAbsolutePath().split("/");
            }catch (IOException e){
                e.printStackTrace();
            }

            int orientation = ExifInterface.ORIENTATION_NORMAL;

            if (exif != null) {
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                builder = new StringBuilder();
                builder.append("Filename: ").append(filepath[filepath.length - 1]).append("\n");
                builder.append("Date & Time: ").append(exif.getAttribute(ExifInterface.TAG_DATETIME)).append("\n");
                String lat = getGeoCoordinates(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
                String lon = getGeoCoordinates(exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));
                builder.append("GPS Latitude: ").append(lat).append(" ").append(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF)).append("\n");
                builder.append("GPS Longitude: ").append(lon).append(" ").append(exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF));
                exifData.setText(builder.toString());
            }

            switch (orientation){
                case ExifInterface.ORIENTATION_ROTATE_90:
                    currentBitmap = rotateBitmap(currentBitmap, 90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    currentBitmap = rotateBitmap(currentBitmap, 180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    currentBitmap = rotateBitmap(currentBitmap, 270);
                    break;
            }

            defaultImage.setImageBitmap(currentBitmap);
        }
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

    private static Bitmap rotateBitmap(Bitmap bitmap, int degrees){
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private void setBitmap(String direction){
        if ("left".matches(direction)) {
            if ((currentimage) < imagesPath.size()-1) {
                currentBitmap = BitmapFactory.decodeFile(imagesPath.get(++currentimage));
            }else{
                currentBitmap = BitmapFactory.decodeFile(imagesPath.get(currentimage));
            }

            ExifInterface exif = null;
            try {
                pictureFile = new File(imagesPath.get(currentimage));
                exif = new ExifInterface(pictureFile.getAbsolutePath());
                filepath = pictureFile.getAbsolutePath().split("/");
            } catch (IOException e) {
                e.printStackTrace();
            }

            int orientation = ExifInterface.ORIENTATION_NORMAL;

            if (exif != null) {
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                builder = new StringBuilder();
                builder.append("Filename: ").append(filepath[filepath.length - 1]).append("\n");
                builder.append("Date & Time: ").append(exif.getAttribute(ExifInterface.TAG_DATETIME)).append("\n");
                String lat = getGeoCoordinates(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
                String lon = getGeoCoordinates(exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));
                builder.append("GPS Latitude: ").append(lat).append(" ").append(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF)).append("\n");
                builder.append("GPS Longitude: ").append(lon).append(" ").append(exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF));
                exifData.setText(builder.toString());
            }

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    currentBitmap = rotateBitmap(currentBitmap, 90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    currentBitmap = rotateBitmap(currentBitmap, 180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    currentBitmap = rotateBitmap(currentBitmap, 270);
                    break;
            }
            defaultImage.setImageBitmap(currentBitmap);
        }else if ("right".matches(direction)){
            if (currentimage > 0) {
                currentBitmap = BitmapFactory.decodeFile(imagesPath.get(--currentimage));

            }else{
                currentBitmap = BitmapFactory.decodeFile(imagesPath.get(currentimage));
            }

            ExifInterface exif = null;
            try {
                pictureFile = new File(imagesPath.get(currentimage));
                exif = new ExifInterface(pictureFile.getAbsolutePath());
                filepath = pictureFile.getAbsolutePath().split("/");
            } catch (IOException e) {
                e.printStackTrace();
            }

            int orientation = ExifInterface.ORIENTATION_NORMAL;

            if (exif != null) {
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                builder = new StringBuilder();
                builder.append("Filename: ").append(filepath[filepath.length - 1]).append("\n");
                builder.append("Date & Time: ").append(exif.getAttribute(ExifInterface.TAG_DATETIME)).append("\n");
                String lat = getGeoCoordinates(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
                String lon = getGeoCoordinates(exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));
                builder.append("GPS Latitude: ").append(lat).append(" ").append(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF)).append("\n");
                builder.append("GPS Longitude: ").append(lon).append(" ").append(exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF));
                exifData.setText(builder.toString());
            }

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    currentBitmap = rotateBitmap(currentBitmap, 90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    currentBitmap = rotateBitmap(currentBitmap, 180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    currentBitmap = rotateBitmap(currentBitmap, 270);
                    break;
            }
            defaultImage.setImageBitmap(currentBitmap);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
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
            case R.id.deleteButton:
                if (!isempty) {
                    showDeleteAlert();
                }
                break;
            case R.id.mapButton:
                Intent mapsIntent = new Intent(GalleryActivity.this, MapsActivity.class);
                mapsIntent.putExtra("foldername", foldername);
                startActivity(mapsIntent);
                break;
            case R.id.bUpload:
                if(imagesFolder.listFiles().length == 0) {
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
        }
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        String dir = fileOrDirectory.getAbsolutePath();
        fileOrDirectory.delete();
        callBroadCast(dir);
    }

    private void showDeleteAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        alertDialog.setTitle("Delete");
        alertDialog.setMessage("Are you sure you want to delete this photo?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        deleteRecursive(pictureFile);
                        Intent intent = getIntent();
                        finish();
                        startActivity(intent);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // do nothing
                    }
                });
        final AlertDialog alert = alertDialog.create();
        alert.show();
    }

    private void callBroadCast(String dir) {
        MediaScannerConnection.scanFile(this, new String[]{dir}, null, new MediaScannerConnection.OnScanCompletedListener() {
            @Override
            public void onScanCompleted(String path, Uri uri) {
                Toast.makeText(GalleryActivity.this, "Scanned", Toast.LENGTH_LONG).show();
                Log.e("ExternalStorage", "Scanned " + path + ":");
                Log.e("ExternalStorage", "-> uri=" + uri);
            }
        });
    }

    private String getGeoCoordinates(String loc){
        String[] degMinSec = loc.split(",");
        String[] deg = degMinSec[0].split("/");
        String[] min = degMinSec[1].split("/");
        String[] sec = degMinSec[2].split("/");
        double degree = Double.parseDouble(deg[0])/Double.parseDouble(deg[1]);
        double minute = Double.parseDouble(min[0])/Double.parseDouble(min[1]);
        double second = Double.parseDouble(sec[0])/Double.parseDouble(sec[1]);
        double degrees = degree + minute/60 + second/3600;
        return String.valueOf(degrees);
    }
}
