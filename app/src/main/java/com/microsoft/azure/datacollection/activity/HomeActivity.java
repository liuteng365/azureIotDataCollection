package com.microsoft.azure.datacollection.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.microsoft.azure.DCApplication;
import com.microsoft.azure.datacollection.R;
import com.microsoft.azure.datacollection.base.BaseData;
import com.microsoft.azure.datacollection.https.AzureClient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity implements SensorEventListener {

    public static final String MOTION_EVENTS_UPLOADED = "motion.events.uploaded";
    public static final String MOTION_EVENTS_DOWNLOADED = "motion.events.downloaded";
    public static final String MOTION_EVENTS_UPLOAD_RESULT = "result.motion.events.uploaded";
    public static final String MOTION_EVENTS_DOWNLOAD_RESULT = "result.motion.events.downloaded";

    private TextView uploadRespTextView;
    private TextView downloadMsgTextView;
    private SensorManager sensorManager;
    private ConcurrentLinkedQueue<MotionEvent> queue = new ConcurrentLinkedQueue<>();
    private long lastSensorSampleTime = 0;
    private IotHubUploadDataThread uploadDataThread;
    private boolean isUploading = false;
    private BroadcastReceiver receiver;
    private ScrollView upScrollView;
    private ScrollView downScrollView;
    private IotHubDownloadDataThread downloadMsgThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        uploadRespTextView = (TextView) findViewById(R.id.response);
        downloadMsgTextView = (TextView) findViewById(R.id.received);
        upScrollView = (ScrollView) findViewById(R.id.up_scroll_view);
        downScrollView = (ScrollView) findViewById(R.id.down_scroll_view);
        onCreateBroadCastReceiver();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (downloadMsgThread == null) {
            downloadMsgThread = new IotHubDownloadDataThread();
            downloadMsgThread.start();
        }
    }

    private void onCreateBroadCastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(MOTION_EVENTS_UPLOADED);
        filter.addAction(MOTION_EVENTS_DOWNLOADED);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                onBroadCast(intent);
            }
        };
        LocalBroadcastManager.getInstance(DCApplication.getInstance()).registerReceiver(receiver, filter);
    }

    public void onBroadCast(Intent intent) {
        if (MOTION_EVENTS_UPLOADED.equals(intent.getAction())) {
            CharSequence result = intent.getCharSequenceExtra(MOTION_EVENTS_UPLOAD_RESULT);
            uploadRespTextView.append(result);
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    upScrollView.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
        } else if (MOTION_EVENTS_DOWNLOADED.equals(intent.getAction())) {
            String result = intent.getStringExtra(MOTION_EVENTS_DOWNLOAD_RESULT);
            downloadMsgTextView.append(result);
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    downScrollView.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_send) {
            if (isUploading) {
                sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
                uploadDataThread.cancel();
                queue.clear();
                item.setTitle("Start");
                isUploading = false;
            } else {
                queue.clear();
                sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
                if (uploadDataThread == null || !uploadDataThread.isAlive()) {
                    uploadDataThread = new IotHubUploadDataThread();
                    uploadDataThread.start();
                }
                isUploading = true;
                item.setTitle("Stop");
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (System.currentTimeMillis() - lastSensorSampleTime > 100) {
            lastSensorSampleTime = System.currentTimeMillis();
            float aX = event.values[0];
            float aY = event.values[1];
            float aZ = event.values[2];
            MotionEvent data = new MotionEvent(aX, aY, aZ);
            queue.add(data);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
        LocalBroadcastManager.getInstance(DCApplication.getInstance()).unregisterReceiver(receiver);
        if (uploadDataThread != null) {
            uploadDataThread.cancel();
        }
        if (downloadMsgThread != null) {
            downloadMsgThread.cancel();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public class MotionEvent extends BaseData {
        private long timestamp;
        private float aX;
        private float aY;
        private float aZ;

        public MotionEvent() {}

        public MotionEvent(float aX, float aY, float aZ) {
            this.aX = aX;
            this.aY = aY;
            this.aZ = aZ;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public class EventsPkg extends BaseData {
         private List<MotionEvent> data;

        public EventsPkg(){}

        public EventsPkg(List<MotionEvent> data) {
            this.data = data;
        }
    }

    public class IotHubUploadDataThread extends Thread {

        private boolean isCanceled = false;

        public void cancel() {
            isCanceled = true;
        }

        @Override
        public void run() {
            super.run();
            while (!isCanceled) {
                if (queue.size() >= 10) {
                    List<MotionEvent> list = new LinkedList<>();
                    synchronized (queue) {
                        list.addAll(queue);
                        queue.clear();
                    }
                    EventsPkg eventsPkg = new EventsPkg(list);
                    Gson gson = new Gson();
                    String data = gson.toJson(eventsPkg);
                    Call<JsonObject> call = AzureClient.getInstance().getAzureService().sendEvent(DCApplication.getInstance().getConfig().getDeviceId(), data);

                    call.enqueue(new Callback<JsonObject>() {
                        @Override
                        public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                            Intent intent = new Intent(MOTION_EVENTS_UPLOADED);
                            StringBuilder sb = new StringBuilder();
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            String time = dateFormat.format(new Date());
                            if (response.isSuccessful()) {
                                sb.append(time + " : upload success.\n");
                                sb.append("\tresponse : status code:" + response.code());
                                if (response.body() != null) {
                                    sb.append(", body:" + response.body().toString());
                                }
                                sb.append("\n");
                                String s = sb.toString();
                                Spannable ss = new SpannableString(s);
                                ss.setSpan(new ForegroundColorSpan(Color.GREEN), 0, s.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                                intent.putExtra(MOTION_EVENTS_UPLOAD_RESULT, ss);
                            } else {
                                sb.append(time + " : upload failed.\n");
                                sb.append("\tresponse : status code:" + response.code() + "\n");
                                String s = sb.toString();
                                Spannable ss = new SpannableString(s);
                                ss.setSpan(new ForegroundColorSpan(Color.RED), 0, s.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                                intent.putExtra(MOTION_EVENTS_UPLOAD_RESULT, ss);
                            }
                            LocalBroadcastManager.getInstance(DCApplication.getInstance()).sendBroadcast(intent);
                        }

                        @Override
                        public void onFailure(Call<JsonObject> call, Throwable t) {
                            Intent intent = new Intent(MOTION_EVENTS_UPLOADED);
                            StringBuilder sb = new StringBuilder();
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            String time = dateFormat.format(new Date());
                            sb.append(time + " : upload failed.\n");
                            sb.append("\texception : " + t.toString() + "\n");
                            String s = sb.toString();
                            Spannable ss = new SpannableString(s);
                            ss.setSpan(new ForegroundColorSpan(Color.RED), 0, s.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                            intent.putExtra(MOTION_EVENTS_UPLOAD_RESULT, ss);
                            LocalBroadcastManager.getInstance(DCApplication.getInstance()).sendBroadcast(intent);
                        }
                    });
                }
            }
            SystemClock.sleep(20);
        }
    }

    public class IotHubDownloadDataThread extends Thread {

        private boolean isCanceled = false;

        public void cancel() {
            isCanceled = true;
        }

        @Override
        public void run() {
            super.run();
            while (!isCanceled) {
                Call<String> call = AzureClient.getInstance().getAzureService().receiveEvent(DCApplication.getInstance().getConfig().getDeviceId());
                try {
                    Intent intent = new Intent(MOTION_EVENTS_DOWNLOADED);
                    StringBuilder sb = new StringBuilder();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String time = dateFormat.format(new Date());
                    Response<String> response = call.execute();
                    if (response.isSuccessful()) {
                        sb.append(time + " : received. statuscode:" + response.code() + "\n");
                        sb.append(("\treceived message : ") + (response.body() == null ? "" : response.body()) + "\n");
                    }
                    if (response.body() != null) {
                        String s = sb.toString();
                        Spannable ss = new SpannableString(s);
                        ss.setSpan(new ForegroundColorSpan(Color.RED), 0, s.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                        intent.putExtra(MOTION_EVENTS_DOWNLOAD_RESULT, ss);
                    } else {
                        intent.putExtra(MOTION_EVENTS_DOWNLOAD_RESULT, sb.toString());
                    }
                    LocalBroadcastManager.getInstance(DCApplication.getInstance()).sendBroadcast(intent);
                } catch (Throwable e) {
                }
                SystemClock.sleep(1000);
            }
        }
    }
}