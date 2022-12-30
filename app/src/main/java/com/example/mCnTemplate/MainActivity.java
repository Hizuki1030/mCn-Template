package com.example.mCnTemplate;

import static java.util.stream.Stream.concat;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Array;
import java.util.Arrays;
import org.json.*;
import java.util.Iterator;



import android.graphics.Color;
import android.graphics.DashPathEffect;
import java.util.ArrayList;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;



// どこのクラスでも良いのですが、UIスレッドへのコールバックができる仕組みが必要になります
public class MainActivity extends AppCompatActivity {
    private SerialInputOutputManager mSerialIoManager;
    private SerialInputOutputManager.Listener mListener;
    private Handler mHandler;
    private UsbSerialPort mPort;


    private String SerialDataBuffer ="";
    // 任意のメソッド。恐らくonCreate等のライフサイクルで実施することになる。


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        TextView StatusMsgView = findViewById(R.id.STATUS_VALUE);
        TextView SerialMsgView = findViewById(R.id.SERIAL_MSG);

        TextView[] N0KeyViews = new TextView[5];
        N0KeyViews[0] = findViewById(R.id.N0_KEY0);
        N0KeyViews[1] = findViewById(R.id.N0_KEY1);
        N0KeyViews[2] = findViewById(R.id.N0_KEY2);
        N0KeyViews[3] = findViewById(R.id.N0_KEY3);
        N0KeyViews[4] = findViewById(R.id.N0_KEY4);

        TextView[] N1KeyViews = new TextView[5];
        N1KeyViews[0] = findViewById(R.id.N1_KEY0);
        N1KeyViews[1] = findViewById(R.id.N1_KEY1);
        N1KeyViews[2] = findViewById(R.id.N1_KEY2);
        N1KeyViews[3] = findViewById(R.id.N1_KEY3);
        N1KeyViews[4] = findViewById(R.id.N1_KEY4);

        TextView[] N0ValueViews = new TextView[5];
        N0ValueViews[0] = findViewById(R.id.N0_VALUE0);
        N0ValueViews[1] = findViewById(R.id.N0_VALUE1);
        N0ValueViews[2] = findViewById(R.id.N0_VALUE2);
        N0ValueViews[3] = findViewById(R.id.N0_VALUE3);
        N0ValueViews[4] = findViewById(R.id.N0_VALUE4);

        TextView[] N1ValueViews = new TextView[5];
        N1ValueViews[0] = findViewById(R.id.N1_VALUE0);
        N1ValueViews[1] = findViewById(R.id.N1_VALUE1);
        N1ValueViews[2] = findViewById(R.id.N1_VALUE2);
        N1ValueViews[3] = findViewById(R.id.N1_VALUE3);
        N1ValueViews[4] = findViewById(R.id.N1_VALUE4);


        // 「初期処理」相当の処理や、onCreateで必要な処理は省略
        // シリアルポート作成処理

        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            return;
        }

        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());


        mPort = driver.getPorts().get(0); // Most devices have just one port (port 0)
        try{
            mPort.open(connection);
            mPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            mPort.setDTR(true);
            StatusMsgView.setText("connected");
        }catch (Exception e){
            StatusMsgView.setText("cloud not connected");
        }

        // UIスレッドのハンドラを生成
        mHandler = new Handler(Looper.getMainLooper());
        // コールバック用のリスナを生成
        mListener = new SerialInputOutputManager.Listener() {
            // データ受信時に呼ばれるコールバックメソッド
            @Override
            public void onNewData(byte[] data) {
                // UIスレッドに処理をコールバック
                String DataString = new String(data);

                SerialDataBuffer += DataString;


                if(SerialDataBuffer.indexOf("\n") > 0){
                    runOnUiThread(() -> { SerialMsgView.setText(SerialDataBuffer);});
                    try{
                        JSONObject SerialJson = new JSONObject(SerialDataBuffer);
                        try{
                            JSONObject N1 = SerialJson.getJSONObject("N1");

                            Iterator<String> N1Keys = N1.keys();
                            int IndexCounter =0;
                            while(N1Keys.hasNext()){
                                TextView KeyView;
                                TextView ValueView;
                                String key = N1Keys.next();
                                String value = N1.getString(key);
                                KeyView = N0KeyViews[IndexCounter];
                                ValueView = N0ValueViews[IndexCounter];
                                runOnUiThread(() -> {KeyView.setText(key);});
                                runOnUiThread(() -> {ValueView.setText(value);});
                                ++IndexCounter;
                            }

                        }catch(JSONException e) {
                            e.printStackTrace();
                        }

                        try{
                            JSONObject N2 = SerialJson.getJSONObject("N2");

                            Iterator<String> N2Keys = N2.keys();
                            int IndexCounter =0;
                            while(N2Keys.hasNext()) {
                                TextView KeyView;
                                TextView ValueView;
                                String key = N2Keys.next();
                                String value = N2.getString(key);
                                KeyView = N1KeyViews[IndexCounter];
                                ValueView = N1ValueViews[IndexCounter];
                                runOnUiThread(() -> {
                                    KeyView.setText(key);
                                });
                                runOnUiThread(() -> {
                                    ValueView.setText(value);
                                });
                                ++IndexCounter;
                            }
                        }catch(JSONException e) {
                            e.printStackTrace();
                        }
                    }catch(JSONException e) {
                        e.printStackTrace();
                    }
                    SerialDataBuffer = "";

                }else{

                }


            }

            // 何かしらのエラーを検知したときに呼ばれるコールバックメソッド
            // 例えばケーブルが抜けた、とか
            @Override
            public void onRunError(Exception e) {
                // UIスレッドに処理をコールバック
                mHandler.post(() -> {
                    // やりたい処理
                    runOnUiThread(() -> { StatusMsgView.setText("disconnected");});
                });
            }
        };
        this.startSerial();
    }



    // シリアル通信開始用のメソッド
    private void startSerial() {
        if (mPort != null) {
            // シリアル通信マネージャと、シリアルポート、イベント受信時のコールバックを紐づける
            mSerialIoManager = new SerialInputOutputManager(mPort, mListener);
            // マルチスレッド出来れば何でもよい
            new Thread(mSerialIoManager).start();
        } else {
            // 適当にエラーハンドリング
        }
    }

    // シリアル通信停止用のメソッド
    private void stopSerial() {
        if (mSerialIoManager != null) {
            // シリアル通信マネージャを停止
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
        if (mPort != null) {
            try {
                // シリアルポートをクローズする
                mPort.close();
            } catch (IOException e) {
                // 適当にエラーハンドリング
            }
        }
    }

}


