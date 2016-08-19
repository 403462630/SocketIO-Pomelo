package com.pomelo.test;

import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.netease.pomelo.DataCallBack;
import com.netease.pomelo.DataEvent;
import com.netease.pomelo.DataListener;
import com.netease.pomelo.PomeloClient;

import com.netease.pomelo.PomeloClient2;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private TextView textView;
    PomeloClient pomeloClient;
    PomeloClient2 pomeloClient2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.tv_text);
        pomeloClient2 = new PomeloClient2("139.224.30.254", 4010);
        findViewById(R.id.bt1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pomeloClient2.request("test.entryHandler.entry", new DataCallBack(){
                    public void responseData(final JSONObject msg){
                        //handle data here
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText(msg.toString());
                            }
                        });
                    }
                });

            }
        });

        findViewById(R.id.bt2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if (pomeloClient == null) {
                //    pomeloClient = new PomeloClient("139.224.30.254", 4010);
                //    pomeloClient.on("test.entryHandler.entry", new DataListener() {
                //        @Override
                //        public void receiveData(DataEvent event) {
                //            Log.i("MainActivity", event.getMessage().toString());
                //        }
                //    });
                //    pomeloClient.init();
                //} else {
                    //pomeloClient.
                //}
                pomeloClient2.connect();
            }
        });

        findViewById(R.id.bt3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pomeloClient2.disconnect();

            }
        });
    }
}
