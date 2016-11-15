package com.brianmannresearch.smartphoto;

import android.app.Dialog;
import android.content.Context;
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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class GalleryActivity extends AppCompatActivity implements View.OnClickListener {

    File imagesFolder, pictureFile;
    int tripid, currentimage;
    Bitmap currentBitmap = null;
    TextView exifData;
    ImageView defaultImage;
    Button returnButton, viewButton, deleteButton;

    ArrayList<String> imagesPath;
    File[] files;
    String[] filepath;
    StringBuilder builder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        Bundle extras = getIntent().getExtras();
        if (extras != null){
            tripid = extras.getInt("tripid");
        }

        defaultImage = (ImageView) findViewById(R.id.selectedImage);

        exifData = (TextView) findViewById(R.id.ExifData);

        returnButton = (Button) findViewById(R.id.returnButton);
        viewButton = (Button) findViewById(R.id.viewButton);
        deleteButton = (Button) findViewById(R.id.deleteButton);

        returnButton.setOnClickListener(this);
        viewButton.setOnClickListener(this);
        deleteButton.setOnClickListener(this);

        defaultImage.setOnTouchListener(new OnSwipeTouchListener(GalleryActivity.this){
            public void onSwipeRight(){
                setBitmap("right");
            }
            public void onSwipeLeft(){
                setBitmap("left");
            }
        });

        imagesFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Trip_" + tripid);
        if (!imagesFolder.exists() && !imagesFolder.isDirectory()) {
            showExistsAlert();
        }else if (imagesFolder.exists() && imagesFolder.isDirectory() && imagesFolder.listFiles().length == 0){
            showEmptyAlert();
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
                pictureFile = new File(imagesPath.get(0));
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
                builder.append("Trip ID: ").append(tripid).append("\n");
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

    private void showExistsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setMessage("This trip does not exist!")
                .setCancelable(false)
                .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                });
        final AlertDialog alert = alertDialog.create();
        alert.show();
    }

    private void showEmptyAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setMessage("This trip is empty!")
                .setCancelable(false)
                .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
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
                builder.append("Trip ID: ").append(tripid).append("\n");
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
                builder.append("Trip ID: ").append(tripid).append("\n");
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
            case R.id.returnButton:
                finish();
                break;
            case R.id.viewButton:
                showTripAlert();
                break;
            case R.id.deleteButton:
                showDeleteAlert();
                break;
        }

    }

    private void showTripAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();

        alertDialog.setMessage("What trip number do you want to view?")
                .setCancelable(false)
                .setView(inflater.inflate(R.layout.trip_dialog, null))
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Dialog f = (Dialog) dialogInterface;
                        EditText text = (EditText) f.findViewById(R.id.tripID);
                        tripid = Integer.parseInt(text.getText().toString());
                        imagesFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Trip_" + tripid);
                        if (!imagesFolder.exists() && !imagesFolder.isDirectory()) {
                            showExistsAlert();
                        }else if (imagesFolder.exists() && imagesFolder.isDirectory() && imagesFolder.listFiles().length == 0){
                            showEmptyAlert();
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
                                pictureFile = new File(imagesPath.get(0));
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
                                builder.append("Trip ID: ").append(tripid).append("\n");
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
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // do nothing
                    }
                });
        final AlertDialog alert = alertDialog.create();
        alert.show();
    }

    private void showDeleteAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        alertDialog.setTitle("Delete");
        alertDialog.setMessage("Are you sure you want to delete this photo?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        deleteandscan(getApplicationContext(), pictureFile.getAbsolutePath(),pictureFile);
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

    private void deleteandscan(final Context context, String path, final File FileorDirectory) {
        String fpath = path.substring(path.lastIndexOf("/") + 1);
        Log.i("fpath", fpath);
        try{
            MediaScannerConnection.scanFile(context, new String[]{Environment
                            .getExternalStorageDirectory().toString()
                            + "/images/"
                            + fpath}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        @Override
                        public void onScanCompleted(String s, Uri uri) {
                            if (uri != null){
                                context.getContentResolver().delete(uri, null, null);
                            }
                            FileorDirectory.delete();
                            System.out.println("file deleted: " + FileorDirectory.getPath());
                            Log.i("ExternalStorage", "Scanned " + s + ":");
                            Log.i("ExternalStorage", "-> uri=" + uri);
                        }
                    });
        }catch (Exception e){
            e.printStackTrace();
        }
        Intent intent = getIntent();
        finish();
        startActivity(intent);
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
