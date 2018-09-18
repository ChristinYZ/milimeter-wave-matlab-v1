package com.example.bupt632.usb_test;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.hoho.android.Radar.RadarSystemExample.AOA_moving_target;
import com.hoho.android.usbserial.driver.CommonUsbSerialPort;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author bupt632
 */
public class AoAMovingTargetActivity extends Activity {

    public final String TAG = AoAMovingTargetActivity.class.getName();

    private static CommonUsbSerialPort sPort = null;

    private TextView mTitleTextView;
    private LineChart chart;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private AOA_moving_target mAOA_moving_target;
    private AOA_moving_target.Listener mListener = new AOA_moving_target.Listener(){
        @Override
        public void onNewData(final double[] Xdata, final double[] Ydata, final double[] c) {
            AoAMovingTargetActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AoAMovingTargetActivity.this.updateReceivedData(Xdata, Ydata, c);  // 一旦监听接收到数据，反应到 UI 上
                    AoAMovingTargetActivity.this.onDeviceStateChange();   // 每次更新完数据后，自动重启线程
                }
            });
        }

        @Override
        public void onDeviceStateChange() {
            AoAMovingTargetActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AoAMovingTargetActivity.this.onDeviceStateChange();   // 如果没有数据，重启线程
                }
            });
        }


        @Override
        public void onRunError(Exception e) {

        }
    };

    public void updateReceivedData(double[] Xdata, double[] recent, double[] c){
        chart.clear();

        int offset = recent.length/2;
        List<Entry> recentFirstRow = new ArrayList<>();
        for(int i = 0;i< offset;i++){
            recentFirstRow.add(new Entry((float)Xdata[i], (float)recent[i]));
        }


        List<Entry> recentSecondRow = new ArrayList<>();
        for(int i =  offset;i< recent.length;i++){
            recentSecondRow.add(new Entry((float)Xdata[i-offset], (float)recent[i]));
        }

        offset = c.length/2;
        List<Entry> cFirstRow = new ArrayList<>();
        for(int i = 0;i< offset;i++){
            cFirstRow.add(new Entry((float)Xdata[i], (float)c[i]));
        }


        List<Entry> cSecondRow = new ArrayList<>();
        for(int i = offset; i<c.length; i++){
            cSecondRow.add(new Entry((float)Xdata[i-offset], (float)c[i]));
        }

        LineDataSet dsRFR = new LineDataSet(recentFirstRow, "recent1");
        LineDataSet dsRSR = new LineDataSet(recentSecondRow, "recent2");
        LineDataSet dsSFR = new LineDataSet(cFirstRow, "c1");
        LineDataSet dsSSR = new LineDataSet(cSecondRow, "c2");

        dsRFR.setColor(Color.RED);
        dsRSR.setColor(Color.YELLOW);
        dsSFR.setColor(Color.BLUE);
        dsSSR.setColor(Color.GREEN);


        dsRFR.setDrawCircles(false);//在点上画圆 默认true
        dsRSR.setDrawCircles(false);
        dsSFR.setDrawCircles(false);
        dsSSR.setDrawCircles(false);

        LineData lineData = new LineData(dsRSR);
        lineData.addDataSet(dsRSR);
        lineData.addDataSet(dsRFR);
//        lineData.addDataSet(dsSFR);
//        lineData.addDataSet(dsSSR);

        chart.setData(lineData);
        chart.invalidate(); // refresh
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aoa_moving_target);

        mTitleTextView = (TextView)findViewById(R.id.mTitleTextView);
        chart = (LineChart)findViewById(R.id.chart);
    }

    @Override
    protected void onPause(){
        super.onPause();
        stopAoAMovingTarget();
        if(sPort != null){
            try{
                sPort.close();
            }catch (IOException e){
                //忽略
            }
            sPort = null;
        }
    }

    protected void onResume(){
        super.onResume();
        if(sPort == null){
            mTitleTextView.setText("No serial device");
        }else{
            final UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

            mUsbManager = usbManager;
            tryGetUsbPermission(); // 尝试获得权限

            UsbDeviceConnection connection = usbManager.openDevice(sPort.getDriver().getDevice());
            if (connection == null) {
                mTitleTextView.setText("USB 打开失败");
                return;
            }

            try {
                sPort.open(connection);
                sPort.setParameters(9600, 8, 1, 0);
                sPort.setWriteBufferSize(65536 * 8);
                sPort.setReadBufferSize(65536 * 8);
            }catch (IOException e){
                Log.e(TAG, "读取 USB 失败");
                mTitleTextView.setText("读取 USB 失败" + e.getMessage());
            }
        }
        onDeviceStateChange();
    }

    private void stopAoAMovingTarget(){
        if(mAOA_moving_target != null){
            Log.i(TAG, "正在停止 AoA_moving_target");
            mAOA_moving_target.stop();
            mAOA_moving_target = null;
        }
    }

    private void startAoAMovingTarget(){
        if(sPort!=null){
            Log.i(TAG, "正在启动 AoA_moving_target");
            mAOA_moving_target = new AOA_moving_target(sPort, mListener);
            mExecutor.submit(mAOA_moving_target);  // ExecutorService 单线程池，该任务只执行一次。若需多次执行，在任务执行完成时重启
        }
    }

    private void onDeviceStateChange() {
        stopAoAMovingTarget();
        startAoAMovingTarget();
    }

    static void show(Context context, CommonUsbSerialPort port) {
        sPort = port;
        final Intent intent = new Intent(context, AoAMovingTargetActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
        context.startActivity(intent);
    }

    /**
     * 获得 usb 权限
     */
    private void openUsbDevice(){
        //before open usb device
        //should try to get usb permission
        tryGetUsbPermission();
    }
    UsbManager mUsbManager;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    private void tryGetUsbPermission(){
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbPermissionActionReceiver, filter);

        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);

        //here do emulation to ask all connected usb device for permission
        for (final UsbDevice usbDevice : mUsbManager.getDeviceList().values()) {
            //add some conditional check if necessary
            //if(isWeCaredUsbDevice(usbDevice)){
            if(mUsbManager.hasPermission(usbDevice)){
                //if has already got permission, just goto connect it
                //that means: user has choose yes for your previously popup window asking for grant perssion for this usb device
                //and also choose option: not ask again
                afterGetUsbPermission(usbDevice);
            }else{
                //this line will let android popup window, ask user whether to allow this app to have permission to operate this usb device
                mUsbManager.requestPermission(usbDevice, mPermissionIntent);
            }
            //}
        }
    }


    private void afterGetUsbPermission(UsbDevice usbDevice){
        //call method to set up device communication
        //Toast.makeText(this, String.valueOf("Got permission for usb device: " + usbDevice), Toast.LENGTH_LONG).show();
        //Toast.makeText(this, String.valueOf("Found USB device: VID=" + usbDevice.getVendorId() + " PID=" + usbDevice.getProductId()), Toast.LENGTH_LONG).show();

        doYourOpenUsbDevice(usbDevice);
    }

    private void doYourOpenUsbDevice(UsbDevice usbDevice){
        //now follow line will NOT show: User has not given permission to device UsbDevice
        UsbDeviceConnection connection = mUsbManager.openDevice(usbDevice);
        //add your operation code here
    }

    private final BroadcastReceiver mUsbPermissionActionReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice usbDevice = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        //user choose YES for your previously popup window asking for grant perssion for this usb device
                        if(null != usbDevice){
                            afterGetUsbPermission(usbDevice);
                        }
                    }
                    else {
                        //user choose NO for your previously popup window asking for grant perssion for this usb device
                        Toast.makeText(context, String.valueOf("Permission denied for device" + usbDevice), Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    };

}
