package com.example.CookApp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.ActionBar;
import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;

import com.example.mCnTemplate.R;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.List;

import org.json.*;


import android.widget.ImageView;
// どこのクラスでも良いのですが、UIスレッドへのコールバックができる仕組みが必要になります
public class MainActivity extends AppCompatActivity {
    private SerialInputOutputManager mSerialIoManager;
    private SerialInputOutputManager.Listener mListener;
    private Handler mHandler;
    private UsbSerialPort mPort;

    int menu_counter = 1;
    private String SerialDataBuffer ="";
    // 任意のメソッド。恐らくonCreate等のライフサイクルで実施することになる。


    @Override
    protected void onCreate(Bundle savedInstanceState) {



        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        TextView temp_textview = findViewById(R.id.temp_textview);
        TextView StatusMsgView = findViewById(R.id.STATUS_VALUE);
        TextView alart_textview = findViewById(R.id.alart_textview);
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
                    Log.d("tag", SerialDataBuffer);
                    try{
                        JSONObject SerialJson = new JSONObject(SerialDataBuffer);
                        try{
                            JSONObject N1 = SerialJson.getJSONObject("N1");
                            try {
                                String temperature = N1.getString("Temp");
                            } catch(JSONException e) {
                                e.printStackTrace();
                            }



                        }catch(JSONException e) {
                            e.printStackTrace();
                        }

                        try{
                            JSONObject N2 = SerialJson.getJSONObject("N2");
                            String temperature_string = N2.getString("Temp");
                            Log.d("tag", temperature_string);
                            runOnUiThread(() -> {temp_textview.setText(temperature_string);});

                            float temperature_float = Float.valueOf(temperature_string);

                            if(menu_counter == 3){
                                ImageView menu_image = findViewById(R.id.menu_image);

                                if(temperature_float < 50-1){
                                    runOnUiThread(() -> {alart_textview.setText("お湯を追加");});
                                }else if(temperature_float > 50+1){
                                    runOnUiThread(() -> {alart_textview.setText("お湯を冷ます");});
                                }else{
                                    runOnUiThread(() -> {alart_textview.setText("いい感じ");});
                                }
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




    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        TextView textView = findViewById(R.id.temp_textview);
        ImageView menu_image = findViewById(R.id.menu_image);

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                menu_counter++;
                int Id_a = getResources().getIdentifier("drawable/cslide" + menu_counter, null, getPackageName());
                Log.d("tag", String.valueOf(menu_counter));

                runOnUiThread(() -> {menu_image.setImageResource(Id_a);});
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                menu_counter--;

                int Id_b = getResources().getIdentifier("drawable/cslide" + menu_counter, null, getPackageName());
                Log.d("tag", String.valueOf(menu_counter));
                runOnUiThread(() -> {menu_image.setImageResource(Id_b);});

                return true;
            default:
                Log.d("tag", String.valueOf(keyCode));
                return super.onKeyUp(keyCode, event);
        }


    }



}


