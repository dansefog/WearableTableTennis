package com.example.jhon.wearabletabletennis;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MainWearActivity extends Activity implements SensorEventListener,GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,MessageApi.MessageListener,DataApi.DataListener, ResultCallback<DataApi.DeleteDataItemsResult> {

    private TextView DataTextView,ThresholdText;
    private Vibrator Vib;
    private CheckBox PowerLimit;
    private CheckBox SnapLimit;
    private SensorManager mSensorManager;
    private GoogleApiClient mGoogleApiClient;
    SensorFilter AccSensorFilter,GyroSensorFilter;

    private final String TAG = MainWearActivity.class.getName();
    /** GYRO行列 */
    private float[] mGyroValues;
    /** 加速度行列 */
    private float[] mAccelerometerValues;

    //ふりすぎ閾値判定
    private ArrayList<Float> AccOverList,GyroOverList;
    private Collection AccCollection,GyroCollection;
    boolean NowVib = false;
    boolean IsAccOver = false;
    boolean IsGyroOver = false;
    float GyroSize = 0;
    float AccSize = 0;
    float GyroTh = 8;
    float AccTh = 18;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main_wear);
        // Vibratorクラスのインスタンス取得
        Vib = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                DataTextView = (TextView) stub.findViewById(R.id.DataWindow);
                ThresholdText = (TextView)stub.findViewById(R.id.Threshold);
                PowerLimit = (CheckBox) stub.findViewById(R.id.PowerLimit);
                SnapLimit = (CheckBox) stub.findViewById(R.id.SnapLimit);
            }
        });

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        Log.d(TAG, "onConnectionFailed : " + connectionResult.toString());
                    }
                })
                .build();

        //各センサ用フィルタのインスタンス生成
        AccSensorFilter = new SensorFilter();
        GyroSensorFilter = new SensorFilter();
        //閾値超えたリストのインスタンス生成
        AccOverList = new ArrayList<Float>();
        GyroOverList = new ArrayList<Float>();
        AccCollection = AccOverList;
        GyroCollection = GyroOverList;
    }

    @Override
    protected void onResume(){
        super.onResume();

        List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        // 加速度センサー登録
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_GAME);
        // 角速度センサー登録
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_GAME);

        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        mSensorManager.unregisterListener(this);
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

    }



    @Override
    public void onSensorChanged(SensorEvent event) {
        float AccOverMax,GyroOverMax;
        AccOverMax = 0;
        GyroOverMax = 0;
        if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            if(NowVib == false){
                GyroSensorFilter.addSample(event.values.clone());
            }
            if (GyroSensorFilter.isSampleEnable() == true) {
                //mGyroValues[0]が手首の角加速度
                mGyroValues = GyroSensorFilter.getParam();
                GyroSize = (float) Math.sqrt(mGyroValues[0] * mGyroValues[0] + mGyroValues[1] * mGyroValues[1] + mGyroValues[2] * mGyroValues[2]);
                if (DataTextView != null) {
                    DataTextView.setText(String.format("Acc : %f\nGyro : %f\n",AccSize,GyroSize));
                }
                if(GyroSize >= GyroTh && SnapLimit.isChecked()){
                    //GyroOverList.add(GyroSize);
                    Vibration();
                    ReqestWhistle(GyroOverMax);
                }
                else{

                }
            }
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            if (NowVib == false && GyroSensorFilter.isSampleEnable() == true) {
                AccSensorFilter.addSample(event.values.clone());
            }
            if(AccSensorFilter.isSampleEnable() == true ){
                mAccelerometerValues = AccSensorFilter.getParam();
                AccSize = (float) Math.sqrt(mAccelerometerValues[0] * mAccelerometerValues[0] +
                        mAccelerometerValues[1] * mAccelerometerValues[1] +
                        mAccelerometerValues[2] * mAccelerometerValues[2]);
                if (DataTextView != null) {
                    DataTextView.setText(String.format("Acc : %f\nGyro : %f\n",AccSize,GyroSize));
                }
                //閾値を超えた場合の処理
                if(AccSize >= AccTh && PowerLimit.isChecked()) {
                    //超えだしたとき
                    if(!IsAccOver){IsAccOver = true;}
                    AccOverList.add(AccSize);
                }
                else if(IsAccOver && AccSize <= (AccTh - 0.5)) {
                    //超えて元に戻ったとき：最大値をスマホに投げてリストを初期化
                    AccOverMax = (float) Collections.max(AccCollection);
                    Vibration();
                    ReqestWhistle(AccOverMax);
                    IsAccOver = false;
                    AccOverList = new ArrayList<Float>();
                    AccCollection = AccOverList;
                }
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        for (DataEvent event : dataEventBuffer) {
            // TYPE_DELETEDがデータ削除時、TYPE_CHANGEDがデータ登録・変更時
            if (event.getType() == DataEvent.TYPE_DELETED) {
                Log.d("TAG", "DataItem deleted: " + event.getDataItem().getUri());

            } else if (event.getType() == DataEvent.TYPE_CHANGED) {
                Log.d("TAG", "DataItem changed: " + event.getDataItem().getUri());
                // 更新されたデータを取得する
                DataMap dataMap = DataMap.fromByteArray(event.getDataItem().getData());
                //データマップに書かれた値を取得（なければ今の値を保持）
                AccTh = dataMap.getFloat("Acc",AccTh);
                GyroTh = dataMap.getFloat("Gyro",GyroTh);
                ThresholdText.setText(String.format("Threshold\n Acc : %f\n Gyro : %f\n", AccTh, GyroTh));
            }
        }
    }

    @Override
    public void onResult(DataApi.DeleteDataItemsResult deleteDataItemsResult) {
        if (!deleteDataItemsResult.getStatus().isSuccess()) {
            Log.e(TAG,
                    "Failed to delete a dataItem, status code: " + deleteDataItemsResult.getStatus()
                            .getStatusCode() + deleteDataItemsResult.getStatus()
                            .getStatusMessage());

        }
    }

    private void ReqestWhistle(float OverSize) {
        final float FinalOverSize = OverSize;
        PendingResult<NodeApi.GetConnectedNodesResult> nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient);
        nodes.setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult result) {
                for (Node node : result.getNodes()) {
                    final byte[] bs = (FinalOverSize + " " + node.getId()).getBytes();
                    PendingResult<MessageApi.SendMessageResult> messageResult =
                            Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), "/Whistle", bs);
                    messageResult.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            Status status = sendMessageResult.getStatus();
                            Log.d("TAG", "Status: " + status.toString());
                        }
                    });
                }
            }
        });
    }

    private void Vibration() {
        NowVib = true;
        Vib.vibrate(400);
        NowVib = false;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
