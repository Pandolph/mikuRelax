package com.cn.enter.relax;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cn.enter.relax.view.BasicLineGraphView;
import com.cn.enter.relax.view.DoubleLineGraphView;
import com.cn.enter.relax.view.HistogramView;
import com.cn.enter.relax.view.ViewSize;
import com.neurosky.thinkgear.TGDevice;
import com.neurosky.thinkgear.TGEegPower;
import com.neurosky.thinkgear.TGRawMulti;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import data.LastTime;
import data.MindTest;
import data.MindTestPack;
import data.TestDate;

public class MonitorActivity extends Activity {
    private static final String TAG = "MonitorActivity";
    private BluetoothAdapter bluetoothAdapter;//蓝牙适配器
    private TGDevice tgDevice;//头戴提供的SDK类

    private long id;//本次测试的ID
    private String name;//本次测试者的姓名
    private int age;//本次测试者的年龄
    private boolean isTesting;//标记是否开始测试，开始测试了，便不能返回用户管理的界面
    private ImageView backBtn;//左上角的返回标志
    private ImageView startStopTestBtn;//开始/停止测试按钮
    private ImageView addTagBtn;//添加标签的按钮
    private ImageView connectBtn;//连接设备的按钮
    private ImageView signalImg;//信号强度的图片
    private TextView signalTv;//信号强度的文字
    private TextView timerTv;//定时器的文字
    private EditText commentEt;//备注的文字

    //柱状图
    private HistogramView attentionHistogram;
    private HistogramView relaxHistogram;

    //折线图
    private BasicLineGraphView deltaLineGraph;
    private BasicLineGraphView thetaLineGraph;
    private BasicLineGraphView lowAlphaLineGraph;
    private BasicLineGraphView highAlphaLineGraph;
    private BasicLineGraphView lowBetaLineGraph;
    private BasicLineGraphView highBetaLineGraph;
    private BasicLineGraphView lowGamaLineGraph;
    private BasicLineGraphView midGamaLineGraph;

    //原始数据折线图
    private BasicLineGraphView rawDateLineGraph;

    //专注度放松度折线图
    private DoubleLineGraphView attentionRelaxLineGraph;

    private ArrayList<Integer> rawDataBuffer;//缓冲

    private enum SIGNAL {NOT_CONNECTED, BAD, POOR, MID, GOOD}//5种信号状态。分别对应未连接，200-150,150-100,100-50,50-0的噪声值
    private SIGNAL signalState;//当前的信号状态

    private MindTest mindTestData;//存储MindTest
    private long testStartTime;//开始测试时根据System.currentTimeMillis()得到的时间戳
    //计时器显示的小时分钟秒
    private int hour;
    private int minute;
    private int second;

    //计时器使用Handler+Timer+TimerTask实现
    Handler timerHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                tick();
                updateTimerUI();
            }
            super.handleMessage(msg);
        }
    };

    Timer timer = new Timer();
    TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            Message msg = timerHandler.obtainMessage();
            msg.what = 1;
            timerHandler.sendMessage(msg);
        }
    };

    /*
    计时器自动加一
     */
    private void tick() {
        second++;
        if (second >= 60) {
            second = 0;
            minute++;
        }
        if (minute >= 60) {
            minute = 0;
            hour++;
        }
    }

    /*
    更新显示计时器的UI
     */
    private void updateTimerUI() {
        String result = "" + hour + ":";
        if (minute < 10) {
            result = result + "0" + minute + ":";
        } else {
            result = result + minute + ":";
        }
        if (second < 10) {
            result = result + "0" + second;
        } else {
            result = result + second;
        }
        timerTv.setText(result);
    }

    //该Handler用于处理来自设备的信号
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //Log.d("EEG", "received message " + msg.what);
            long receiveTime = System.currentTimeMillis();
            int timeStamp =(int)( receiveTime - testStartTime);
            switch (msg.what) {
                case TGDevice.MSG_STATE_CHANGE:
                    switch (msg.arg1) {
                        case TGDevice.STATE_IDLE:
                            break;
                        case TGDevice.STATE_CONNECTING:
                            Log.d(TAG, "Connecting...");
                            signalTv.setText(R.string.connect_state_connecting);
                            connectBtn.setVisibility(View.GONE);
                            startStopTestBtn.setVisibility(View.VISIBLE);
                            addTagBtn.setVisibility(View.VISIBLE);
                            break;
                        case TGDevice.STATE_CONNECTED:
                            Log.d(TAG, "Connected");
                            signalTv.setText(R.string.connect_state_connected);
                            tgDevice.start();
                            signalState = SIGNAL.BAD;
                            startStopTestBtn.setImageResource(R.drawable.start_test_btn_normal);
                            addTagBtn.setImageResource(R.drawable.add_tag_normal);
                            break;
                        case TGDevice.STATE_NOT_FOUND:
                            Log.d(TAG, "Device not found");
                            initial(false);
                            signalTv.setText(R.string.connect_state_not_found);
                            break;
                        case TGDevice.STATE_NOT_PAIRED:
                            Log.d(TAG, "Device not paired");
                            initial(false);
                            signalTv.setText(R.string.connect_state_not_paired);
                            break;
                        case TGDevice.STATE_DISCONNECTED:
                            Log.d(TAG, "Device disconnected");
                            initial(true);
                    }
                    updateSignalUI();
                    break;
                case TGDevice.MSG_POOR_SIGNAL:
                    handlerPoorSignal(msg.arg1);
                    //Log.d(TAG, "PoorSignal: " + msg.arg1 + "\n");
                    if (isTesting) {
                        mindTestData.addSignalData(timeStamp, msg.arg1);
                    }
                    break;
                case TGDevice.MSG_RAW_DATA:
                    //Log.d(TAG, "RawData: " + msg.arg1 + "\n");
                    if (isTesting) {
                        mindTestData.addRawData(msg.arg1);
                        addRawDataToBuffer(msg.arg1);
                    }
                    break;
                case TGDevice.MSG_HEART_RATE:
                    //Log.d(TAG, "Heart rate: " + msg.arg1 + "\n");
                    break;
                case TGDevice.MSG_ATTENTION:
                    //Log.d(TAG, "Attention: " + msg.arg1 + "\n");
                    if (isTesting) {
                        mindTestData.addAttentionData(timeStamp, msg.arg1);
                        attentionHistogram.setData( msg.arg1);
                        attentionHistogram.invalidate();
                        attentionRelaxLineGraph.addAttentionData(msg.arg1);
                        attentionRelaxLineGraph.invalidate();
                    }
                    break;
                case TGDevice.MSG_MEDITATION:
                    //Log.d(TAG, "Meditation:" + msg.arg1 + "\n");
                    if (isTesting) {
                        mindTestData.addRelaxData(timeStamp, msg.arg1);
                        relaxHistogram.setData(msg.arg1);
                        relaxHistogram.invalidate();
                        attentionRelaxLineGraph.addRelaxData(msg.arg1);
                        attentionRelaxLineGraph.invalidate();
                    }
                    break;
                case TGDevice.MSG_BLINK:
                    //Log.d(TAG, "Blink: " + msg.arg1 + "\n");
                    break;
                case TGDevice.MSG_RAW_COUNT:
                    //Log.d(TAG, "Raw Count: " + msg.arg1 + "\n");
                    break;
                case TGDevice.MSG_LOW_BATTERY:
                    Toast.makeText(getApplicationContext(), "Low battery!", Toast.LENGTH_SHORT).show();
                    //Log.d(TAG, "Low battery!");
                    break;
                case TGDevice.MSG_EEG_POWER:
                    TGEegPower eegPower = (TGEegPower) msg.obj;
                     /*
                     delta(0.5-2.75Hz)
                     theta(3.5-6.75Hz)
                     low-alpha(7.5-9.25Hz)
                     high-alpha(10-11.75Hz)
                     low-beta(13-16.75Hz)
                     high-beta(18-29.75Hz)
                     low-gamma(31-39.75Hz)
                     mid-gamma(41-49.75Hz)
                      */
                    Log.d(TAG, "EEGPower: delta: " + eegPower.delta + " theta: " + eegPower.theta + " low-alpha: " + eegPower.lowAlpha
                            + " high-alpha: " + eegPower.highAlpha + " low-beta: " + eegPower.lowBeta + " high-beta: " + eegPower.highBeta
                            + " low-gamma: " + eegPower.lowGamma + " mid-gamma: " + eegPower.midGamma);
                    if (isTesting) {
                        mindTestData.addEEGPowerData(timeStamp, eegPower.delta, eegPower.theta, eegPower.lowAlpha, eegPower.highAlpha, eegPower.lowBeta, eegPower.highBeta, eegPower.lowGamma, eegPower.midGamma);
                        deltaLineGraph.addData((int)Math.log(eegPower.delta+1));
                        deltaLineGraph.invalidate();
                        thetaLineGraph.addData((int)Math.log(eegPower.theta+1));
                        thetaLineGraph.invalidate();
                        lowAlphaLineGraph.addData((int)Math.log(eegPower.lowAlpha+1));
                        lowAlphaLineGraph.invalidate();
                        highAlphaLineGraph.addData((int)Math.log(eegPower.highAlpha+1));
                        highAlphaLineGraph.invalidate();
                        lowBetaLineGraph.addData((int)Math.log(eegPower.lowBeta+1));
                        lowBetaLineGraph.invalidate();
                        highBetaLineGraph.addData((int)Math.log(eegPower.highBeta+1));
                        highBetaLineGraph.invalidate();
                        lowGamaLineGraph.addData((int)Math.log(eegPower.lowGamma+1));
                        lowGamaLineGraph.invalidate();
                        midGamaLineGraph.addData((int)Math.log(eegPower.midGamma+1));
                        midGamaLineGraph.invalidate();
                    }
                    break;
                case TGDevice.MSG_RAW_MULTI:
                    TGRawMulti rawM = (TGRawMulti) msg.obj;
                    //Log.d(TAG, "Raw1: " + rawM.ch1 + "\nRaw2: " + rawM.ch2);
                default:
                    break;
            }
        }

        /*
        处理信号强度（噪声强度）
         */
        private void handlerPoorSignal(int signal) {

            //如果当前的状态是未连接，则不需要更换信号图片UI
            if (signalState == SIGNAL.NOT_CONNECTED)
                return;

            //如果输入的信号超过了范围，也不更换信号图片UI
            if (signal < 0 || signal > 200) {
                Log.e(TAG, "Signal error, out of range: " + signal);
                return;
            }

            //根据输入的信号，将信号分为4个等级
            SIGNAL signalStateNow = SIGNAL.BAD;//信号默认为BAD
            if (0 <= signal && signal < 50)
                signalStateNow = SIGNAL.GOOD;
            if (50 <= signal && signal < 100)
                signalStateNow = SIGNAL.MID;
            if (100 <= signal && signal < 150)
                signalStateNow = SIGNAL.POOR;
            if (150 <= signal && signal < 200)
                signalStateNow = SIGNAL.BAD;

            //如果信号的等级相较于signalState有所变动，更新UI显示
            if (signalState != signalStateNow) {
                signalState = signalStateNow;
                updateSignalUI();
            }
        }
    };

    /*
    根据当前的信号状态来更换UI
     */
    private void updateSignalUI() {
        if (signalState == SIGNAL.NOT_CONNECTED)
            signalImg.setImageResource(R.drawable.signal_0);
        if (signalState == SIGNAL.BAD)
            signalImg.setImageResource(R.drawable.signal_1);
        if (signalState == SIGNAL.POOR)
            signalImg.setImageResource(R.drawable.signal_2);
        if (signalState == SIGNAL.MID)
            signalImg.setImageResource(R.drawable.signal_3);
        if (signalState == SIGNAL.GOOD)
            signalImg.setImageResource(R.drawable.signal_4);
    }

    /*
    初始化操作,输入参数为是否清除显示连接状态的文字（清除后为显示未连接）
     */
    private void initial(boolean clearConnectStateTextViewFlag) {
        signalState = SIGNAL.NOT_CONNECTED;
        hour = 0;
        minute = 0;
        second = 0;
        isTesting = false;
        updateSignalUI();
        updateTimerUI();
        if (clearConnectStateTextViewFlag)
            signalTv.setText(R.string.connect_state_not_connected);
        startStopTestBtn.setImageResource(R.drawable.start_test_btn_disable);
        addTagBtn.setImageResource(R.drawable.add_tag_disable);
        connectBtn.setImageResource(R.drawable.connect_normal);
        startStopTestBtn.setVisibility(View.GONE);
        addTagBtn.setVisibility(View.GONE);
        connectBtn.setVisibility(View.VISIBLE);
    }

    /*
    开始测试
     */
    private void startTest() {
        timer.schedule(timerTask, 1000, 1000);
        this.testStartTime = System.currentTimeMillis();
        isTesting = true;
        Calendar c = Calendar.getInstance();
        //Calendar的月份从0开始计算
        TestDate testDate=new TestDate(c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DATE),
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND));
        this.mindTestData = new MindTest(this.id, this.name,this.age, testDate);


    }
    /*弹出对话框对话框*/
    private void showNormalDia()
    {
        //AlertDialog.Builder normalDialog=new AlertDialog.Builder(getApplicationContext());
        AlertDialog.Builder normalDia=new AlertDialog.Builder(MonitorActivity.this);
        normalDia.setIcon(R.drawable.ic_launcher);
        normalDia.setTitle("");
        normalDia.setMessage("停止并保存数据？");

        normalDia.setPositiveButton("保存", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                stopTest();
                saveData();
                setResult(1);
                tgDevice.close();//关闭设备
                finish();
            }
        });
        normalDia.setNegativeButton("不保存", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                stopTest();
                setResult(0);
                tgDevice.close();//关闭设备
                finish();
            }
        });
        normalDia.setNeutralButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        normalDia.create().show();
    }

    private void stopTest(){
        timer.cancel();
        isTesting=false;
        //记录下停止的时间
        long testStopTime=System.currentTimeMillis();
        int lastTimeMillisecond=(int)(testStopTime-testStartTime);
        int hour=lastTimeMillisecond/(1000*3600);
        lastTimeMillisecond=lastTimeMillisecond-1000*3600*hour;
        int minute=lastTimeMillisecond/(1000*60);
        lastTimeMillisecond=lastTimeMillisecond-1000*60*minute;
        int second=lastTimeMillisecond/(1000);
        int millisecond=lastTimeMillisecond%(1000);
        this.mindTestData.setLastTime(new LastTime(hour, minute, second, millisecond));
    }

    /*
    保存数据
     */
    private void saveData(){
        mindTestData.setComment(commentEt.getText().toString());
        MindTestPack mtPack=new MindTestPack(mindTestData);
        try {
            //获取SDCard目录
            File sdCardDir;
            sdCardDir = Environment.getExternalStorageDirectory();
            File dirFirstFile=new File(sdCardDir,"/mindwave/");//新建一级主目录

            if(!dirFirstFile.exists()){//判断文件夹目录是否存在
                dirFirstFile.mkdir();//如果不存在则创建
            }

            File f = new File(dirFirstFile,this.id+".data");
            FileOutputStream fos = new FileOutputStream(f);
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            oos.writeObject(mtPack);
            oos.flush();
            oos.close();

            fos.close();
            Log.e(TAG,"finish");

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitor);
        Bundle extras = getIntent().getExtras();
        if(extras!=null) {
            this.id = extras.getLong("id", 0);
            this.name = extras.getString("name","");
            this.age = extras.getInt("age",0);
        }

        rawDataBuffer=new ArrayList<>();

        backBtn = (ImageView) findViewById(R.id.back_btn);
        startStopTestBtn = (ImageView) findViewById(R.id.start_stop_test_btn);
        addTagBtn = (ImageView) findViewById(R.id.add_tag_btn);
        connectBtn = (ImageView) findViewById(R.id.connect_btn);
        signalImg = (ImageView) findViewById(R.id.signal_img);
        signalTv = (TextView) findViewById(R.id.signal_tv);
        timerTv = (TextView) findViewById(R.id.timer);
        commentEt = (EditText) findViewById(R.id.comment);

        relaxHistogram = (HistogramView) findViewById(R.id.relaxHistogram);
        attentionHistogram = (HistogramView) findViewById(R.id.attentionHistogram);
        deltaLineGraph = (BasicLineGraphView) findViewById(R.id.EEG_delta_chart);
        thetaLineGraph = (BasicLineGraphView) findViewById(R.id.EEG_theta_chart);
        lowAlphaLineGraph = (BasicLineGraphView) findViewById(R.id.EEG_low_alpha_chart);
        highAlphaLineGraph = (BasicLineGraphView) findViewById(R.id.EEG_high_alpha_chart);
        lowBetaLineGraph = (BasicLineGraphView) findViewById(R.id.EEG_low_beta_chart);
        highBetaLineGraph = (BasicLineGraphView) findViewById(R.id.EEG_high_beta_chart);
        lowGamaLineGraph = (BasicLineGraphView) findViewById(R.id.EEG_low_gama_chart);
        midGamaLineGraph = (BasicLineGraphView) findViewById(R.id.EEG_mid_gama_chart);
        rawDateLineGraph = (BasicLineGraphView) findViewById(R.id.raw_line_graph);
        attentionRelaxLineGraph = (DoubleLineGraphView) findViewById(R.id.attention_relax_line_graph);


        //TODO 自动获取图表的尺寸
        relaxHistogram.initial(getResources().getColor(R.color.relax_histogram),new ViewSize(0,0,57,209));
        attentionHistogram.initial(getResources().getColor(R.color.attention_histogram),new ViewSize(0,0,57,209));
        deltaLineGraph.initial(getResources().getColor(R.color.eeg_power_line_graph),new ViewSize(0,0,227,51),50,5,4,0,(int)Math.log(16777215)+1);
        thetaLineGraph.initial(getResources().getColor(R.color.eeg_power_line_graph),new ViewSize(0,0,227,51),50,5,4,0,(int)Math.log(16777215)+1);
        lowAlphaLineGraph.initial(getResources().getColor(R.color.eeg_power_line_graph),new ViewSize(0,0,227,51),50,5,4,0,(int)Math.log(16777215)+1);
        highAlphaLineGraph.initial(getResources().getColor(R.color.eeg_power_line_graph),new ViewSize(0,0,227,51),50,5,4,0,(int)Math.log(16777215)+1);
        lowBetaLineGraph.initial(getResources().getColor(R.color.eeg_power_line_graph),new ViewSize(0,0,227,51),50,5,4,0,(int)Math.log(16777215)+1);
        highBetaLineGraph.initial(getResources().getColor(R.color.eeg_power_line_graph),new ViewSize(0,0,227,51),50,5,4,0,(int)Math.log(16777215)+1);
        lowGamaLineGraph.initial(getResources().getColor(R.color.eeg_power_line_graph),new ViewSize(0,0,227,51),50,5,4,0,(int)Math.log(16777215)+1);
        midGamaLineGraph.initial(getResources().getColor(R.color.eeg_power_line_graph),new ViewSize(0,0,227,51),50,5,4,0,(int)Math.log(16777215)+1);
        rawDateLineGraph.initial(getResources().getColor(R.color.raw_data_line_graph),new ViewSize(0,0,739,100),1024,5,4,-2048,2047);
        attentionRelaxLineGraph.initial(getResources().getColor(R.color.attention_line_graph),getResources().getColor(R.color.relax_line_graph),
                new ViewSize(0,0,739,230),50,5,4,0,100
                );
        initial(true);

        //添加OnClickListener以处理点击
        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tgDevice.connect(true);
            }
        });

        //添加OnTouchListner以处理按钮按下的效果
        backBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    //更改为按下时的背景图片
                    backBtn.setImageResource(R.drawable.back_button_press);
                    if(!isTesting){
                        setResult(0);
                        finish();
                    }
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    //改为抬起时的图片
                    backBtn.setImageResource(R.drawable.back_button_normal);
                }
                return false;
            }
        });

        startStopTestBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (signalState != SIGNAL.NOT_CONNECTED) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        Log.d(TAG, "ACTION:DOWN " + isTesting);
                        //改为按下时的图片
                        if (isTesting) {
                            startStopTestBtn.setImageResource(R.drawable.start_test_btn_press);
							//停止测试
                            showNormalDia();
                        } else {
                            startStopTestBtn.setImageResource(R.drawable.stop_test_btn_press);
                            //开始测试
                            startTest();
                        }

                        return true;
                    }
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        Log.d(TAG, "ACTION:UP");
                        //改为抬起时的图片
                        if (isTesting) {
                            startStopTestBtn.setImageResource(R.drawable.stop_test_btn_normal);
                        } else {
                            startStopTestBtn.setImageResource(R.drawable.start_test_btn_normal);
                        }
                    }
                }
                return false;
            }
        });

        addTagBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (signalState != SIGNAL.NOT_CONNECTED) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        //更改为按下时的背景图片
                        addTagBtn.setImageResource(R.drawable.add_tag_press);
                        return true;
                    }
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        //改为抬起时的图片
                        addTagBtn.setImageResource(R.drawable.add_tag_normal);
                    }
                }
                return false;
            }
        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Alert user that Bluetooth is not available
            Toast.makeText(this, "Bluetooth not available", Toast.LENGTH_LONG).show();
            finish();
        } else {
        	/* create the TGDevice */
            tgDevice = new TGDevice(bluetoothAdapter, handler);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(tgDevice!=null)
            tgDevice.close();
    }

    //重载返回方法，禁止在测试时返回界面
    @Override
    public void onBackPressed() {
        if(!isTesting) {
            super.onBackPressed();
            onDestroy();
        }
    }

    private void addRawDataToBuffer(int data){
        rawDataBuffer.add(data);
        if(rawDataBuffer.size()>=256){
            for(Integer next :rawDataBuffer){
                rawDateLineGraph.addData(next);
            }
            rawDateLineGraph.invalidate();
            rawDataBuffer.clear();
        }
    }
}