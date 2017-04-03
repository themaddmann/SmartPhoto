package com.brianmannresearch.smartphoto;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.IOException;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String foldername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // get foldername from previous activity
        Bundle extras = getIntent().getExtras();
        if (extras != null){
            foldername = extras.getString("foldername");
        }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // open folder to collect all of the photos
        File imagesFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), foldername);
        File[] files = imagesFolder.listFiles();
        ExifInterface exif;
        // collect the GPS coordinates of each photo and add them to the map as pins
        // set the pins icon to the actual (scaled) image itself
        for (int j = 0; j < files.length; j++) {
            try {
                exif = new ExifInterface(files[j].getAbsolutePath());
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                Bitmap currentBitmap = BitmapFactory.decodeFile(files[j].getAbsolutePath());
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(currentBitmap, currentBitmap.getWidth()/25, currentBitmap.getHeight()/25, true);
                String time = exif.getAttribute(ExifInterface.TAG_DATETIME);
                String lat = getGeoCoordinates(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
                double latitude = Double.valueOf(lat);
                String lon = getGeoCoordinates(exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));
                double longitude = Double.valueOf(lon);
                switch (orientation){
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        scaledBitmap = rotateBitmap(scaledBitmap, 90);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        scaledBitmap = rotateBitmap(scaledBitmap, 180);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        scaledBitmap = rotateBitmap(scaledBitmap, 270);
                        break;
                }
                if (exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF).matches("S")){
                    latitude *= -1;
                }
                if (exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF).matches("W")){
                    longitude *= -1;
                }
                LatLng coordinate = new LatLng(latitude, longitude);
                mMap.addMarker(new MarkerOptions()
                        .position(coordinate)
                        .title(time)
                        .icon(BitmapDescriptorFactory.fromBitmap(scaledBitmap))
                        .flat(true));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinate, 16));
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    // correctly orients the bitmap of the image
    private static Bitmap rotateBitmap(Bitmap bitmap, int degrees){
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    // converts the string of gps coordinates from deg/min/sec to degrees
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
