package com.util.imageselector;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

// 커밋테스트.
public class MainActivity extends AppCompatActivity {

    final static int PERMISSION_REQUEST_CODE = 1001;
    Button changeBtn;
    ImageView imageview;
    TextView targetNumHaeder;
    TextView displayTargetNumber;
    int position = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permissionCheck();
        setContentView(R.layout.activity_main);
        changeBtn = findViewById(R.id.changeBtn);
        imageview = findViewById(R.id.selectedImage);
        targetNumHaeder = findViewById(R.id.targetText);
        displayTargetNumber = findViewById(R.id.displayTargetNumber);

        changeBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View view) {
                String targetNumber = pickTargetNum();
                Uri uri1 = pickRandomImage();
                if(uri1 != null) {
                    Bitmap bm = null;
                    try {
                        bm = MediaStore.Images.Media.getBitmap(getContentResolver(),
                                Uri.parse("file://" + uri1));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    ExifInterface exif = null;
                    try {
                        exif = new ExifInterface(String.valueOf(uri1));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_UNDEFINED);
                    Bitmap bmRotated = rotateBitmap(bm, orientation);
                    imageview.setImageBitmap(bmRotated);
                    imageview.setVisibility(view.GONE);
                    targetNumHaeder.setText(targetNumber);
                    targetNumHaeder.setVisibility(View.INVISIBLE);
                    displayTargetNumber.setText(targetNumber);
                    displayTargetNumber.setVisibility(View.VISIBLE);
                }
            }
        });

        displayTargetNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                targetNumHaeder.setVisibility(View.VISIBLE);
                displayTargetNumber.setVisibility(View.GONE);
                imageview.setVisibility(View.VISIBLE);
            }
        });

    }

    private void permissionCheck(){
        if(Build.VERSION.SDK_INT >= 23){
            int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            ArrayList<String> arrayPermission = new ArrayList<String>();
            if (permissionCheck != PackageManager.PERMISSION_GRANTED){
                arrayPermission.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if(arrayPermission.size() > 0){
                String strArray[] = new String[arrayPermission.size()];
                strArray = arrayPermission.toArray(strArray);
                ActivityCompat.requestPermissions(this, strArray, PERMISSION_REQUEST_CODE);
            } else{
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        switch (requestCode){
            case PERMISSION_REQUEST_CODE: {
                if (grantResults.length < 1) {
                    Toast.makeText(this, "권한 획득 실패", Toast.LENGTH_SHORT).show();
                    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                    return;
                }
                for (int i= 0; i< grantResults.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "거부되었습니다.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                }
                Toast.makeText(this, "승인되었습니다.", Toast.LENGTH_SHORT).show();
            }
            break;
        }
        super.onRequestPermissionsResult(requestCode, permissions,grantResults);
    }

    private Uri pickRandomImage() {
        Uri targetUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String targetDir = Environment.getExternalStorageDirectory().toString() + "/DCIM/SELECT";
        File targetDirFile = new File(targetDir);
        if(!targetDirFile.toString().equals(targetDir)){
            Log.d("체크","지정 폴더 없음" + targetDirFile);
            Toast.makeText(this,
                    "선택용 폴더가 존재하지 않습니다.\n 갤러리앱에서 SELECT(모두 대문자) 폴더(앨범)를\n 만들고 뽑을 그림을 넣어주세요.",
                    Toast.LENGTH_LONG).show();
            return null;
        }
        Log.d("체크","저장용 폴더경로 : " + targetDirFile);
        targetUri = targetUri.buildUpon().appendQueryParameter("bucketId",
                String.valueOf(targetDir.toLowerCase().hashCode())).build();
        Cursor c = getContentResolver().query(
                targetUri,
                new String[]{MediaStore.Images.ImageColumns._ID, MediaStore.Images.ImageColumns.DATA},
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " = ? ",
                new String[]{"SELECT"}, null);
        Uri uri = null;

        if(c != null){
            int total = c.getCount();
            Log.d("난수체크", "랜덤전 : " + position);
            while (true) {
                int newPosition = (int) (Math.random() * total);
                if(position != newPosition) {
                    position = newPosition;
                    Log.d("난수체크", "랜덤후(대입확인) : " + position);
                    break;
                }
            }
            if (total > 1) {
                if (c.moveToPosition(position)) {
                    @SuppressLint("Range") String data = c.getString
                            (c.getColumnIndex(MediaStore.Images.ImageColumns.DATA));
                    //@SuppressLint("Range") long id = c.getLong
                    // (c.getColumnIndex(MediaStore.Images.ImageColumns._ID));
                    uri = Uri.parse(data);
                    Log.d("체크","선택된 파일 : " + uri.toString());
                }
            }else {
                Toast.makeText(this,
                        "지정된 폴더(SELECT)에 최소 2장 이상의 그림을 넣어주세요",
                        Toast.LENGTH_LONG).show();
            }
            c.close();
        }
        return uri;
    }

    private String pickTargetNum() {
            String targetNum = getKeyForRandomNum(8);
            Log.d("체크","생성된 타겟넘버 : " + targetNum);
            return targetNum;
    }

    public String getKeyForRandomNum(int keyLength) {
        Random random=new Random();
        String key = Integer.toString( random.nextInt(keyLength) + 1);
        for (int i = 1; i < keyLength; i++) {
            key+= Integer.toString(random.nextInt(keyLength + 1));
        }
        return key;
    }


    public static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }
        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                    bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return bmRotated;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }
}
