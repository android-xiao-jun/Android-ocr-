package com.example.jun.myocr;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class WecomeActivity extends AppCompatActivity {
    public Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                Intent intent = new Intent(getBaseContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wecome);
        Message msg = Message.obtain();
        handler.sendEmptyMessageDelayed(0, 2000);
    }
}
