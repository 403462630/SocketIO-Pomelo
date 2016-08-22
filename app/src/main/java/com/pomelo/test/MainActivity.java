package com.pomelo.test;

import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.baidao.pomelo.Callback;
import com.baidao.pomelo.PomeloClient;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private TextView textView;
    PomeloClient pomeloClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.tv_text);
        pomeloClient = new PomeloClient("139.224.30.254", 4010);
        findViewById(R.id.bt1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //pomeloClient2.request("test.entryHandler.entry", new DataCallBack(){
                //    public void responseData(final JSONObject msg){
                //        //handle data here
                //        runOnUiThread(new Runnable() {
                //            @Override
                //            public void run() {
                //                textView.setText(msg.toString());
                //            }
                //        });
                //    }
                //});
                pomeloClient.sendMessage("test.entryHandler.entry", null, new Callback() {
                    @Override
                    public void onSuccess(final Object o) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText(o.toString());
                            }
                        });
                    }

                    @Override
                    public void onError() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText("onError");
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
                pomeloClient.connect();
            }
        });

        findViewById(R.id.bt3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pomeloClient.disconnect();

            }
        });
    }
}
