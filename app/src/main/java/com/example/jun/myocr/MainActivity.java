package com.example.jun.myocr;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;

import Decoder.BASE64Encoder;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int TAKE_PHOTO = 1;
    public static final int CHOOSE_PHOTO = 2;
    /**
     * 打开系统相机
     */
    private Button mBtCamera;
    private ImageView mIvShowPic;
    private Uri imageUri;

    private TextView show_text;
    /**
     * 打开系统相册
     */
    private Button mBtChooseFromAlbum;
    private ProgressDialog progressDialog;

    // private AlertDialog alertDialog;
    //  alertDialog=new AlertDialog.Builder(this)
    //                .setTitle("title")
//                .setMessage("message")
//                .setPositiveButton("cancel", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.dismiss();
//                    }
//                })
//                .create();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


//
        initView();

    }

    private void initView() {
        mBtCamera = (Button) findViewById(R.id.bt_camera);
        mBtCamera.setOnClickListener(this);
        mIvShowPic = (ImageView) findViewById(R.id.iv_show_pic);
        mBtChooseFromAlbum = (Button) findViewById(R.id.bt_choose_from_album);
        mBtChooseFromAlbum.setOnClickListener(this);
        show_text = findViewById(R.id.show_text);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("提示");
        progressDialog.setMessage("获取网络数据中...");
        progressDialog.setCancelable(false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
            case R.id.bt_camera:
//                Calendar calendar = Calendar.getInstance();
//                String imageName = "IMG" + calendar.get(Calendar.YEAR) + ""
//                        + (calendar.get(Calendar.MONTH) + 1) + ""
//                        + calendar.get(Calendar.DATE) + ""
//                        + calendar.get(Calendar.HOUR) + ""
//                        + calendar.get(Calendar.MINUTE) + ""
//                        + calendar.get(Calendar.SECOND) + ".jpg";
                String imageName = "imageName.jpg";
                File file = new File(getExternalCacheDir(), imageName);
                try {
                    if (file.exists()) {
                        file.delete();
                    }
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (Build.VERSION.SDK_INT >= 24) {
                    imageUri = FileProvider.getUriForFile(this,
                            "com.example.provider.image", file);

                } else {
                    imageUri = Uri.fromFile(file);
                }
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(intent, TAKE_PHOTO);
                break;
            case R.id.bt_choose_from_album:

                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                } else {
                    openAlbum();
                }
                break;
        }
    }

    private void openAlbum() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");

        startActivityForResult(intent, CHOOSE_PHOTO);
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openAlbum();
                } else {
                    Toast.makeText(this, "You denied the permission", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    try {
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                        mIvShowPic.setImageBitmap(bitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case CHOOSE_PHOTO:
                if (resultCode == RESULT_OK) {
                    if (Build.VERSION.SDK_INT >= 19) {
                        handleImageOnKitKat(data);
                    } else {
                        handleImageBeforeKitKat(data);
                    }
                }
                break;
            default:
                break;
        }
    }

    private void getOrcData(InputStream inputStream) {
        if (inputStream == null) {
            return;
        } else {
            show_text.setText("");
        }
        try {
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            //关闭流
            inputStream.close();
            BASE64Encoder encoder = new BASE64Encoder();
            String encode = encoder.encode(data);
            progressDialog.show();
            getPerSonData(encode);
            Log.e("encode", "1----------------------------" + encode);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getPerSonData(final String encode) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.e("test", "2----------------------------");
                    JSONObject jsonObject1 = new JSONObject();
                    jsonObject1.put("image", encode);

                    JSONObject jsonObject2 = new JSONObject();
                    jsonObject2.put("side", "face");

                    jsonObject1.put("configure", jsonObject2.toString());
                    Log.e("jsonObject1", "" + jsonObject1.toString());
                    URL url = new URL("http://dm-51.data.aliyun.com/rest/160601/ocr/ocr_idcard.json");

                    //这里就用第三方开源库okhttp，，httpURlConnction太磨叽了
                    Log.e("aa", "aaaaaaaaaaaa");
                    OkHttpClient client = new OkHttpClient();
                    RequestBody requestBody = FormBody.create(MediaType.parse("application/json;charset=utf-8"), jsonObject1.toString());
                    Request request = new Request.Builder()
                            .url(url)
                            .header("accept", "*/*")
                            .header("connection", "Keep-Alive")
                            .header("Content-Type", "text/html;charset=utf-8")
                            .header("Authorization", "APPCODE 41056833fb5d44c8826d72b5de3c472a")
                            .method("POST", requestBody)
                            .build();
                    Response response = client.newCall(request).execute();
                    closeDialog();
                    if (response != null) {
                        String string = response.body().string();
                        Log.e("aa", "aaaaaaaaaaaa");
                        Log.e("aa", "aaaaaaaaaaaa" + string);
                        showText(string);
                    }else {

                    }

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void closeDialog() {
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void showText(final String string) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                show_text.setText(string);
            }
        });
    }

    @SuppressLint("NewApi")
    private void handleImageOnKitKat(Intent data) {
        String imagePath = null;
        Uri uri = data.getData();
        Log.d("TAG", "handleImageOnKitKat: uri is " + uri);
        if (DocumentsContract.isDocumentUri(this, uri)) {
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            imagePath = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            imagePath = uri.getPath();
        }
        displayImage(imagePath);
    }

    private void handleImageBeforeKitKat(Intent data) {
        Uri uri = data.getData();
        String imagePath = getImagePath(uri, null);
        displayImage(imagePath);
    }

    private String getImagePath(Uri uri, String selection) {
        String path = null;
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    private void displayImage(String imagePath) {
        if (imagePath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            mIvShowPic.setImageBitmap(bitmap);
            File file = new File(imagePath);
            try {
                InputStream inputStream = new FileInputStream(file);
                getOrcData(inputStream);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "failed to get image", Toast.LENGTH_SHORT).show();
        }
    }

}
