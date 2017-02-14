package com.brianmannresearch.smartphoto;

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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class GalleryActivity extends AppCompatActivity implements View.OnClickListener {

    private File imagesFolder, pictureFile;
    private int currentimage;
    private Bitmap currentBitmap = null;
    private TextView exifData;
    private ImageView defaultImage;
    private Button returnButton, deleteButton, mapButton;

    private ArrayList<String> imagesPath;
    private File[] files;
    private String[] filepath;
    private String foldername;
    private StringBuilder builder;
    private Boolean isempty = false;

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

        returnButton = (Button) findViewById(R.id.returnButton);
        deleteButton = (Button) findViewById(R.id.deleteButton);
        mapButton = (Button) findViewById(R.id.mapButton);

        returnButton.setOnClickListener(this);
        deleteButton.setOnClickListener(this);
        mapButton.setOnClickListener(this);

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
