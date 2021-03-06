package com.example.jhon.wearabletabletennis;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class MainActivity extends ActionBarActivity
        implements GoogleApiClient.ConnectionCallbacks, MessageApi.MessageListener,
        DataApi.DataListener, ResultCallback<DataApi.DeleteDataItemsResult>,
        Switch.OnCheckedChangeListener{

    //固定値
    public static final int TYPE_ACC = 1;
    public static final int TYPE_GYRO = 2;
    public static final String[] TYPE = {"Acc", "Gyro"};

    private static final String TAG = MainActivity.class.getName();
    private GoogleApiClient mGoogleApiClient;
    private TextView AccText;
    private TextView GyroText;
    private TextView OutText;
    private SeekBar AccSeek;
    private SeekBar GyroSeek;
    private Button ResetButton;
    private Switch ContinuitySwitch;
    float Acc = 18;
    float Gyro = 8;
    private boolean IsJudge = false;

    private SoundPool Whistle,Bonobono;
    private int WhistleId,BonobonoId;

    private Vibrator Vib;

    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Vibratorクラスのインスタンス取得
        Vib = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

        AccText = (TextView)findViewById(R.id.AccText);
        GyroText = (TextView)findViewById(R.id.GyroText);
        OutText = (TextView)findViewById(R.id.OutText);
        AccSeek = (SeekBar)findViewById(R.id.AccSeekBar);
        GyroSeek = (SeekBar)findViewById(R.id.GyroSeekBar);
        ContinuitySwitch = (Switch)findViewById(R.id.ContinuitySwitch);
        ContinuitySwitch.setOnCheckedChangeListener(this);

        handler= new Handler();
        View.OnClickListener ResetClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Reset();
            }
        };
        ResetButton = (Button)findViewById(R.id.ResetButton);
        ResetButton.setOnClickListener(ResetClickListener);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        Log.d(TAG, "onConnectionFailed:" + connectionResult.toString());
                    }
                })
                .addApi(Wearable.API)
                .build();

        AccSeek.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    public void onProgressChanged(SeekBar seekBar,
                                                  int progress, boolean fromUser) {
                        //つまみの値が変化したときに呼ばれる
                    }

                    public void onStartTrackingTouch(SeekBar seekBar) {
                        // ツマミに触れたときに呼ばれる
                    }

                    public void onStopTrackingTouch(SeekBar seekBar) {
                        // ツマミを離したときに呼ばれる
                        Acc = (float) (9 + (AccSeek.getProgress() * 0.5));
                        SetData(String.valueOf(Acc), "Acc");
                        AccText.setText(String.format("加速度閾値 : %.2f", Acc));
                    }
                }
        );

        GyroSeek.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    public void onProgressChanged(SeekBar seekBar,
                                                  int progress, boolean fromUser) {
                        //つまみの値が変化したときに呼ばれる
                    }

                    public void onStartTrackingTouch(SeekBar seekBar) {
                        // ツマミに触れたときに呼ばれる
                    }

                    public void onStopTrackingTouch(SeekBar seekBar) {
                        // ツマミを離したときに呼ばれる
                        Gyro = (float) (9 + (GyroSeek.getProgress() * 0.5));
                        SetData(String.valueOf(Gyro), "Gyro");
                        GyroText.setText(String.format("角速度閾値 : %.2f", Gyro));
                    }
                }
        );

    }

    private void SetData(String Data, String Key) {
        // DataMapインスタンスを生成する
        PutDataMapRequest dataMapRequest = PutDataMapRequest.create("/datapath");
        DataMap dataMap = dataMapRequest.getDataMap();

        // データをセットする
        dataMap.putString(Key, Data);
        // データを更新する
        PutDataRequest request = dataMapRequest.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient, request);
        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(DataApi.DataItemResult dataItemResult) {
                Log.d("TAG", "onResult: " + dataItemResult.getStatus());
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        //アラーム読み込み
        Whistle = new SoundPool(1, AudioManager.STREAM_SYSTEM,0);
        Bonobono = new SoundPool(1, AudioManager.STREAM_SYSTEM,0);
        WhistleId = Whistle.load(getApplicationContext(),R.raw.whistle,0);
        BonobonoId = Bonobono.load(getApplicationContext(),R.raw.bonobono,0);
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if ( (mGoogleApiClient != null) && mGoogleApiClient.isConnected()) {
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        Whistle.release();
        Bonobono.release();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    @Override
        protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

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
                Acc = dataMap.getFloat("Acc",Acc);
                Gyro = dataMap.getFloat("Gyro",Gyro);
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

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        float OverSize = 0;
        String MessagePayload;
        if (messageEvent.getPath().equals("/Whistle")) {
            MessagePayload = new String(messageEvent.getData());
            final String[] CData = MessagePayload.split(" ");
            OverSize = Float.parseFloat(CData[0]);
            final int Type = Integer.parseInt(CData[1]);
            if (ContinuitySwitch.isChecked() || (!ContinuitySwitch.isChecked() && !IsJudge)) {
                //ハンドラを使ってUIスレッドへポスト
                if (Type == TYPE_ACC) {
                    OverSize = OverSize - Acc;
                } else if (Type == TYPE_GYRO) {
                    OverSize = OverSize - Gyro;
                }
                final float finalOverSize = OverSize;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        OutText.setText(String.format("つよすぎ！！！\n" +
                                TYPE[Type - 1] + "超過値：%.3f", finalOverSize));
                    }
                });
                Whistle.play(WhistleId, 1.0F, 1.0F, 0, 0, 1.0F);
                Vibration();
                IsJudge = true;
                SendCommand("Over" + TYPE[Type - 1]);
            }
        }
    }

    private void Vibration() {
        Vib.vibrate(400);
    }

    private void Reset() {
        OutText.setText("せーふ");
        IsJudge = false;
        SetData(String.valueOf(IsJudge), "IsJudge");
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        SetData(String.valueOf(!isChecked), "Continuity");
    }

    public void SendCommand(String Data) {
        try (final Socket socket = new Socket("localhost", 8888)) {
            final OutputStream outputStream = socket.getOutputStream();
            final BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(outputStream));
            writer.append(Data);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
