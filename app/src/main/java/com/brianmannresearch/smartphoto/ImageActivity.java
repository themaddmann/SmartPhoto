package com.brianmannresearch.smartphoto;

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
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class ImageActivity extends AppCompatActivity implements View.OnClickListener{

    private CustomPagerAdapter mCustomPagerAdapter;
    private ViewPager mViewPager;
    private String foldername;
    private Button deleteButton;
    private File imagesFolder;
    private File[] files;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        // get data from previous activity
        Bundle extras = getIntent().getExtras();
        if (extras != null){
            foldername = extras.getString("foldername");
        }

        // open folder of trip
        imagesFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), foldername);
        files = imagesFolder.listFiles();

        // key to easier image viewing
        mCustomPagerAdapter = new CustomPagerAdapter(this, foldername);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mCustomPagerAdapter);
        deleteButton = (Button) findViewById(R.id.deleteButton);

        deleteButton.setOnClickListener(this);
    }

    private void showDeleteAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        alertDialog.setTitle("Delete");
        alertDialog.setMessage("Are you sure you want to delete this photo?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // on deletion of photo, exit activity
                        // simplest way to avoid nullpointerexception, which caused app to crash
                        int index = mViewPager.getCurrentItem();
                        File currfile = files[index];
                        deleteRecursive(currfile);
                        Intent data = new Intent();
                        String text = "Finished";
                        data.setData(Uri.parse(text));
                        setResult(RESULT_OK, data);
                        finish();
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

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        String dir = fileOrDirectory.getAbsolutePath();
        fileOrDirectory.delete();
        callBroadCast(dir);
    }

    // function to rescan the folder after deletion
    // keeps app up-to-date with whether or not a given file exists in the public directory
    private void callBroadCast(String dir) {
        MediaScannerConnection.scanFile(this, new String[]{dir}, null, new MediaScannerConnection.OnScanCompletedListener() {
            @Override
            public void onScanCompleted(String path, Uri uri) {
                Toast.makeText(ImageActivity.this, "Scanned", Toast.LENGTH_LONG).show();
                Log.e("ExternalStorage", "Scanned " + path + ":");
                Log.e("ExternalStorage", "-> uri=" + uri);
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.deleteButton:
                showDeleteAlert();
                break;
        }
    }
}

// tutorials exist on online to better understand how this feature works
class CustomPagerAdapter extends PagerAdapter{

    private LayoutInflater mLayoutInflater;
    private File imagesFolder;
    private File[] files;

    CustomPagerAdapter(Context context, String foldername) {
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        imagesFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), foldername);
        files = imagesFolder.listFiles();
    }

    private static Bitmap rotateBitmap(Bitmap bitmap, int degrees){
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    @Override
    public int getCount() {
        return files.length;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View itemView = mLayoutInflater.inflate(R.layout.pager_item, container, false);

        ImageView imageView = (ImageView) itemView.findViewById(R.id.imageView);
        TextView ExifData = (TextView) itemView.findViewById(R.id.ExifData);
        Bitmap bitmap = null;
        try{
            ExifInterface exif = new ExifInterface(files[position].getAbsolutePath());
            String filepath[] = files[position].toString().split("/");
            String builder = "Filename: " + filepath[filepath.length-1] + "\n" +
                    "Date & Time: " + exif.getAttribute(ExifInterface.TAG_DATETIME) + "\n" +
                    "GPS Latitude: " + getGeoCoordinates(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)) + " " + exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF) + "\n" +
                    "GPS Longitude: " + getGeoCoordinates(exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)) + " " + exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);
            ExifData.setText(builder);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            bitmap = BitmapFactory.decodeFile(files[position].getAbsolutePath());
            switch (orientation){
                case ExifInterface.ORIENTATION_ROTATE_90:
                    bitmap = rotateBitmap(bitmap, 90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    bitmap = rotateBitmap(bitmap, 180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    bitmap = rotateBitmap(bitmap, 270);
                    break;
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        imageView.setImageBitmap(bitmap);

        container.addView(itemView);

        return itemView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((LinearLayout) object);
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