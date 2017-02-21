package com.brianmannresearch.smartphoto;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Environment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

public class ImageActivity extends AppCompatActivity {

    private CustomPagerAdapter mCustomPagerAdapter;
    private ViewPager mViewPager;
    private String foldername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        Bundle extras = getIntent().getExtras();
        if (extras != null){
            foldername = extras.getString("foldername");
        }

        mCustomPagerAdapter = new CustomPagerAdapter(this, foldername);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mCustomPagerAdapter);
    }
}

class CustomPagerAdapter extends PagerAdapter implements View.OnClickListener{

    private LayoutInflater mLayoutInflater;
    private File imagesFolder;
    private File[] files;
    private TextView ExifData;

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
        Button deleteButton = (Button) itemView.findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(this);
        ExifData = (TextView) itemView.findViewById(R.id.ExifData);
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

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.deleteButton:

                break;
        }
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