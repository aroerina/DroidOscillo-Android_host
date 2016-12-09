/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.limepulse.USBOscilloscopeHost;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import jp.limepulse.USBOscilloscopeHost.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.widget.Toast;



public class USBOscilloscopeHost extends Activity implements OnClickListener {
    // Debugging
	private static final boolean D = false;		// Release
	//private static final boolean D = true;			// Debug Mode

    private static final String TAG = "USBOscilloscopeHost";
	private static final String ACTION_USB_PERMISSION = "com.google.android.HID.action.USB_PERMISSION";

	// decimal
	private static final int USB_DEVICE_VID = 8137;		// NXP Vendor ID 0x1FC9
	private static final int USB_DEVICE_PID = 33128;	// PID 0x8168

	//
    // USB Oscilloscope Configuration Message
	//
	public static final int MESSAGE_DATA_REQUEST = 	1;
	public static final int MESSAGE_DATA_RECEIVED= 	2;
    public static final int MESSAGE_H_TRIGGER =	 	3;	// change horizontal trigger value
    public static final int MESSAGE_V_TRIGGER =	 	4;
    public static final int MESSAGE_BIAS =			5;
    public static final int MESSAGE_TIMESCALE =		6;
    public static final int MESSAGE_VOLTAGE_SCALE =	7;
    public static final int MESSAGE_TRIGGER_MODE =	8;
    public static final int MESSAGE_RUNMODE =		9;
    public static final int MESSAGE_DCCUT =			10;
    public static final int MESSAGE_TRIGGER_EDGE =	11;
    public static final int MESSAGE_DEVICE_INIT	 =	255;
    public static final int MESSAGE_EEPROM_PAGE_WRITE =	12;
    public static final int MESSAGE_EEPROM_PAGE_READ  =	13;


    public static final int VOLTSCALE_5V	 = 	0;
    public static final int VOLTSCALE_2V	 = 	1;
    public static final int VOLTSCALE_1V	 = 	2;
    public static final int VOLTSCALE_500MV	 = 	3;
    public static final int VOLTSCALE_200MV  = 	4;
    public static final int VOLTSCALE_100MV	 = 	5;
    public static final int VOLTSCALE_50MV	 = 	6;
    public static final int VOLTSCALE_20MV	 = 	7;
    public static final int VOLTSCALE_10MV	 = 	8;
    public static final int VOLTSCALE_5MV	 = 	9;

    public static final int TIMESCALE_1S	= 23;
    public static final int TIMESCALE_500MS	= 22;
    public static final int TIMESCALE_250MS	= 21;
    public static final int TIMESCALE_100MS	= 20;
    public static final int TIMESCALE_50MS	= 19;
    public static final int TIMESCALE_25MS	= 18;
    public static final int TIMESCALE_10MS	= 17;
    public static final int TIMESCALE_5MS	= 16;
    public static final int TIMESCALE_2P5MS	= 15;
    public static final int TIMESCALE_1MS	= 14;
    public static final int TIMESCALE_500US	= 13;
    public static final int TIMESCALE_250US	= 12;
    public static final int TIMESCALE_100US	= 11;
    public static final int TIMESCALE_50US	= 10;
    public static final int TIMESCALE_25US	= 9;
    public static final int TIMESCALE_10US	= 8;
    public static final int TIMESCALE_5US	= 7;
    public static final int TIMESCALE_2P5US	= 6;
    public static final int TIMESCALE_1US	= 5;
    public static final int TIMESCALE_500NS	= 4;
    public static final int TIMESCALE_250NS	= 3;
    public static final int TIMESCALE_100NS	= 2;
    public static final int TIMESCALE_50NS	= 1;
    public static final int TIMESCALE_25NS	= 0;

    public static final double HIGHDIV_CALIBRATION_VOLTAGE = 10.0;
    public static final double LOWDIV_CALIBRATION_VOLTAGE = 0.717;


    public static final int NUM_VOLTSCALE =   10;
    public static final int TGMODE_AUTO = 	0;
    public static final int TGMODE_NORMAL = 1;
    public static final int TGMODE_FREE = 	2;
    public static final int TGMODE_SINGLE = 3;
    public static final int TGMODE_SINGLE_FREE = 4;		// Autosetモード用

    public static final int TGMODE_DEVICE_STOP = 	0;
    public static final int SAMPLE_MAX = 4095;

    public static final int BUFFER_SIZE = 1600;
    public static final int DEFAULT_SAMPLE_LENGTH = 800;
    public static final int DEFAULT_DIV_LENGTH = DEFAULT_SAMPLE_LENGTH / 10;

    public static final byte NUM_TIMESCALE = 24;

    public static final int M4_FIRM_WRITEADDR = 	0x10000000;
    public static final int M4_FIRM_ENTRYADDR = 	0x1000013c;
    public static final int M0APP_FIRM_WRITEADDR = 	0x10010000;
    public static final int M0APP_FIRM_ENTRYADDR = 	0x100100c0;	//mapを確認すること

    //
    //	縦軸用
    //
    //1.045 0.240

    public static final double ADC_VREF_VOLTAGE = 0.506;
    public static final double GRAPH_FULLSCALE_VOLTAGE = 0.72;		// 画面に表示される範囲の電圧
    public static final double TYPICAL_ADC_INPUT_MAX_VOLTAGE_PP = 0.8;
    //public static final double ADC_MAX_CALIB_VOLTAGE = 50 * 0.001;	// mV
    //public static final double ADC_INPUT_MAX_VOLTAGE = 0.8 + ADC_MAX_CALIB_VOLTAGE;		// ADC入力最大電圧
    public static final double ADC_PER_DIV = GRAPH_FULLSCALE_VOLTAGE / 8.0;		// 1div あたりの電圧
    public static final int TYPICAL_FULLSCALE_MAX_VAL = (int) (SAMPLE_MAX * (GRAPH_FULLSCALE_VOLTAGE / TYPICAL_ADC_INPUT_MAX_VOLTAGE_PP));

    public static final double TYPICAL_ONE_LSB_VOLTAGE = TYPICAL_ADC_INPUT_MAX_VOLTAGE_PP / SAMPLE_MAX;	// 0.195mV
    //public static final int FS_BOTTOM = (SAMPLE_MAX - normal_graph_fullscale) / 2;
    public static final double ADC_MAX_VOLTAGE_DROP_AT_80MSPS = 0.99;		// 80Msps時にADC入力抵抗が低まるので補正
    public static final double OPAMP_FEEDBACK_FIX_RATIO = 0.975;		// 5V 200mV　以外のレンジの時のADC入力幅の補正

    private int highdiv_input_calib_voltage,lowdiv_input_calib_voltage;	// 1LSB = 1mV
    private double adc_max_voltage = TYPICAL_ADC_INPUT_MAX_VOLTAGE_PP;
    private double one_lsb_voltage = TYPICAL_ADC_INPUT_MAX_VOLTAGE_PP / SAMPLE_MAX;
    private int normal_graph_fullscale = TYPICAL_FULLSCALE_MAX_VAL;
    private int normal_graph_min = (SAMPLE_MAX - TYPICAL_FULLSCALE_MAX_VAL) / 2;
    // Voltscale 8 用フルスケール
    public int vs8_graph_fullscale = (int) (SAMPLE_MAX * (GRAPH_FULLSCALE_VOLTAGE / TYPICAL_ADC_INPUT_MAX_VOLTAGE_PP) * 0.5);
    // Voltscale 9 用フルスケール
    public int vs9_graph_fullscale = (int) (SAMPLE_MAX * (GRAPH_FULLSCALE_VOLTAGE / TYPICAL_ADC_INPUT_MAX_VOLTAGE_PP) * 0.25);
    private double g_vpos_max,g_vpos_min;
    private double opamp_feedback_fix_ratio = 1.0;

    //
    // BIAS
    //
    public static final double BIAS_FULLSCALE_VOLTAGE = 1.235 * (4.1/5.1) * 2.0;
    public static final int BIAS_MAX_REG_VALUE = 4095;
    public static final double bias_upper_VOLTAGE = 1.5;
    public static final double bias_lower_VOLTAGE = -0.484;
    public static final double BIAS_CENTER_VOLTAGE = (BIAS_FULLSCALE_VOLTAGE / 2.0) + bias_lower_VOLTAGE;
    public static final double TYPICAL_BIAS_LSB_VOLTAGE = BIAS_FULLSCALE_VOLTAGE / BIAS_MAX_REG_VALUE;	// 0.5mVぐらい
	// ADR VREFとバイアス出力中点に生じるズレ分のDACレジスタ値
    public static final int BIAS_NORMAL_OFFSET_VALUE = (int)((ADC_VREF_VOLTAGE - BIAS_CENTER_VOLTAGE) / TYPICAL_BIAS_LSB_VOLTAGE);
    public static final int TYPICAL_BIAS_FS_VALUE = (int)(BIAS_MAX_REG_VALUE * (GRAPH_FULLSCALE_VOLTAGE / BIAS_FULLSCALE_VOLTAGE));
    public static final int TYPICAL_BIAS_BOTTOM = ((BIAS_MAX_REG_VALUE - TYPICAL_BIAS_FS_VALUE ) / 2) - BIAS_NORMAL_OFFSET_VALUE;			// 1310
    public static final int TYPICAL_BIAS_CENTER = TYPICAL_BIAS_BOTTOM + TYPICAL_BIAS_FS_VALUE/2;

    public static final double HT_EXPAND_RATIO = 4.0;
    public static final double VT_EXPAND_RATIO = 2.0;
    public static final double BIAS_EXPAND_RATIO = 3.0;

    private int highdiv_opa_diff,lowdiv_opa_diff;		// 増幅率1倍→10倍にしたときのADCに現れるDC誤差
    private int[] opamp_offset_calib_value_list = new int[NUM_VOLTSCALE];
    private int bias_center,bias_lower,bias_upper,normal_bias_center,normal_bias_lower,normal_bias_upper;
    private int vs8_bias_upper_value,vs9_bias_upper_value,vs8_bias_lower,vs9_bias_lower;;
    private double g_bias_pos_max,g_bias_pos_min;
    private double opa_fix_bias_lower,opa_fix_bias_upper;		// フィードバック回路の誤差を修正したバイアスTOP　BOTTOM値
    public static final int BIAS_DROP_CALIBVAL_AT_80MSPS = 0;		// 80Msps時にADC入力抵抗が低まるので補正

    // shared preferences key

    private static final String KEY_TRIGGER_MODE = "tmode";
    private static final String KEY_TRIGGER_SLOPE = "tslope";
    private static final String KEY_VSCALE = "vscale";
    private static final String KEY_HSCALE = "hscale";
	private static final String KEY_DCCUT = "dc_cut";
	private static final String KEY_X10 = "x10";
	private static final String KEY_EXPAND = "expand";
	private static final String KEY_VPOS = "vpos";
	private static final String KEY_HPOS = "hpos";
	private static final String KEY_BIAS_POS = "bias_pos";

    SharedPreferences sharedData;
    SharedPreferences.Editor editor;

    // UI Objects
    private ImageView img_trigger,img_connect,img_range;
    private GraphView mGraph = null;
    private TextView FrameRateText,triggerModeText,TimescaleText,HPotisionText,VPotisionText;
    private TextView RunStopText,BiasText,VoltscaleText,VppText,FreqText,VrmsText,MeanText;
    private Button trig_mode_button,calibration_btn,edge_btn,btn_test1,btn_test2;
    private Button td_zoom_btn,td_unzoom_btn,vs_plus_btn,vs_minus_btn,btn_setzero;
    private Button btn_autoset,btn_setting;
    private ToggleButton tbtn_dccut,tbtn_stop,tbtn_xp,tbtn_x10;
    private ScaleGestureDetector gestureDetector;
    private GestureListener simpleListener;
    private ScrollView scview;

    static final String [] TRIGGER_MODE_LIST = new String[5];
    static final String [] TIME_SCALE_LIST = new String[NUM_TIMESCALE];
    static final String [] VOLT_SCALE_LIST = new String[NUM_VOLTSCALE];
    static final String [] VOLT_SCALE_LIST_X10 = new String[NUM_VOLTSCALE];

    static final double [] TYPICAL_AMP_ERROR = new double[NUM_VOLTSCALE];	// 入力増幅回路の標準エラー率
    static final double [] TIME_CONVERT_LIST = new double[NUM_TIMESCALE];
    static final double [] FULLSCALE_VOLTAGE_LIST  = new double[NUM_VOLTSCALE];

	//private UsbDevice device;
	private UsbManager mUsbManager;
	private USBBroadcastReceiver mUsbReceiver;
	UsbReceiveThread ReceiveThread;

	private UsbInterface intf;
	private UsbEndpoint endPointRead;
	private UsbEndpoint epw_Msg;	//Message 送信用
	private UsbEndpoint epw_Firm;	//Firmware　書き込み用
	private UsbDeviceConnection deviceConnection;
	//private int EpInPacketSize;
	private int EpOutPacketSize;
	private int epw_FirmPacketSize;
	private Timer SendTimer;
	private boolean isConnected = false;
	private boolean autoModeNormal = false;
	private AutoModeTimerTask autoModeTask;
	private UsbRequest inRequest;
	Handler mHandler;
	PendingIntent mPermissionIntent;

	Vibrator vibrator;

	// Oscilloscope setting
	private int triggerMode,verTriggerValue;
	private boolean run_status = true,dc_cut,triggerSlopeUp = true,x10Mode,expand;
	private boolean calibration = false;
	private int sampleLength;//,sampleSizeByte;
	private int timescale,voltscale,graph_fullscale,graph_min;

	private double proveRatio = 1.0;
	//g_bias_pos,g_vpos : 0がグラフ一番下0.5が中心1が一番上の位置
	//g_hpos : 0が中心-1がグラフ左端1がグラフ右端
	private double g_bias_pos,g_vpos,g_hpos;
    private GraphWave wave;


	// Constructor
	public USBOscilloscopeHost(){
    	TRIGGER_MODE_LIST[0] = "AUTO";
    	TRIGGER_MODE_LIST[1] = "NORMAL";
    	TRIGGER_MODE_LIST[2] = "FREE";
    	TRIGGER_MODE_LIST[3] = "SINGLE";

    	TIME_SCALE_LIST[0] = "25n";
    	TIME_SCALE_LIST[1] = "50n";
        TIME_SCALE_LIST[2] = "100n";
        TIME_SCALE_LIST[3] = "250n";
        TIME_SCALE_LIST[4] = "500n";	// 80Msps
        TIME_SCALE_LIST[5] = "1μ";		// 80Msps
        TIME_SCALE_LIST[6] = "2.5μ";	// 32Msps
        TIME_SCALE_LIST[7] = "5μ";
        TIME_SCALE_LIST[8] = "10μ";
        TIME_SCALE_LIST[9] = "25μ";
        TIME_SCALE_LIST[10] = "50μ";
        TIME_SCALE_LIST[11] = "100μ";
        TIME_SCALE_LIST[12] = "250μ";
        TIME_SCALE_LIST[13] = "500μ";
        TIME_SCALE_LIST[14] = "1m";
        TIME_SCALE_LIST[15] = "2.5m";
        TIME_SCALE_LIST[16] = "5m";
        TIME_SCALE_LIST[17] = "10m";
        TIME_SCALE_LIST[18] = "25m";
        TIME_SCALE_LIST[19] = "50m";
        TIME_SCALE_LIST[20] = "100m";
        TIME_SCALE_LIST[21] = "250m";
        TIME_SCALE_LIST[22] = "500m";
        TIME_SCALE_LIST[23] = "1";

    	TIME_CONVERT_LIST[0] =  0.000001;
    	TIME_CONVERT_LIST[1] =  TIME_CONVERT_LIST[0];
        TIME_CONVERT_LIST[2] =  TIME_CONVERT_LIST[0];
        TIME_CONVERT_LIST[3] =  TIME_CONVERT_LIST[0];	// 0~5まではサンプルレートは同じ
        TIME_CONVERT_LIST[4] =  TIME_CONVERT_LIST[0];
        TIME_CONVERT_LIST[5] =  TIME_CONVERT_LIST[0];
        TIME_CONVERT_LIST[6] =  0.0000025;
        TIME_CONVERT_LIST[7] =  0.000005;
        TIME_CONVERT_LIST[8] =  0.00001;
        TIME_CONVERT_LIST[9] =  0.000025;
        TIME_CONVERT_LIST[10] = 0.00005;
        TIME_CONVERT_LIST[11] = 0.0001;
        TIME_CONVERT_LIST[12] = 0.00025;
        TIME_CONVERT_LIST[13] = 0.0005;
        TIME_CONVERT_LIST[14] = 0.001;
        TIME_CONVERT_LIST[15] = 0.0025;
        TIME_CONVERT_LIST[16] = 0.005;
        TIME_CONVERT_LIST[17] = 0.01;
        TIME_CONVERT_LIST[18] = 0.025;
        TIME_CONVERT_LIST[19] = 0.05;
        TIME_CONVERT_LIST[20] = 0.1;
        TIME_CONVERT_LIST[21] = 0.25;
        TIME_CONVERT_LIST[22] = 0.5;
        TIME_CONVERT_LIST[23] = 1.0;

        VOLT_SCALE_LIST[0] = "5";	// xx.x
        VOLT_SCALE_LIST[1] = "2";	// xx.x
        VOLT_SCALE_LIST[2] = "1";	// x.xx
        VOLT_SCALE_LIST[3] = "500m";// x.xx
        VOLT_SCALE_LIST[4] = "200m";// x.xx
        VOLT_SCALE_LIST[5] = "100m";// xxxm
        VOLT_SCALE_LIST[6] = "50m"; // xxxm
        VOLT_SCALE_LIST[7] = "20m"; // xxxm
        VOLT_SCALE_LIST[8] = "10m"; // xx.x m
        VOLT_SCALE_LIST[9] = "5m";  // xx.x m

        // 増幅段の誤差
        TYPICAL_AMP_ERROR[0] = 1.0233;	//5V
        TYPICAL_AMP_ERROR[1] = 1.0246;	//2V
        TYPICAL_AMP_ERROR[2] = 0.9906;	//1V
        TYPICAL_AMP_ERROR[3] = 1.0;		//500mV
        TYPICAL_AMP_ERROR[4] = 1.0233;	//200mV
        TYPICAL_AMP_ERROR[5] = 1.0007;	//100mV
        TYPICAL_AMP_ERROR[6] = 1.005;	//50mV
        TYPICAL_AMP_ERROR[7] = 1.0;		//20mV
        TYPICAL_AMP_ERROR[8] = 1.0;		//10mV
        TYPICAL_AMP_ERROR[9] = 1.0;		//5mV


        VOLT_SCALE_LIST_X10[0] = "50";	// xx.x
        VOLT_SCALE_LIST_X10[1] = "20";	// xx.x
        VOLT_SCALE_LIST_X10[2] = "10";	// x.xx
        VOLT_SCALE_LIST_X10[3] = "5";// x.xx
        VOLT_SCALE_LIST_X10[4] = "2";// x.xx
        VOLT_SCALE_LIST_X10[5] = "1";// xxxm
        VOLT_SCALE_LIST_X10[6] = "500m"; // xxxm
        VOLT_SCALE_LIST_X10[7] = "200m"; // xxxm
        VOLT_SCALE_LIST_X10[8] = "100m"; // xx.x m
        VOLT_SCALE_LIST_X10[9] = "50m";  // xx.x m

        FULLSCALE_VOLTAGE_LIST[0] = 5 *8;
        FULLSCALE_VOLTAGE_LIST[1] = 2 *8;
        FULLSCALE_VOLTAGE_LIST[2] = 1 *8;
        FULLSCALE_VOLTAGE_LIST[3] = 0.5 *8;
        FULLSCALE_VOLTAGE_LIST[4] = 0.2 *8;
        FULLSCALE_VOLTAGE_LIST[5] = 0.1 *8;
        FULLSCALE_VOLTAGE_LIST[6] = 0.05 *8;
        FULLSCALE_VOLTAGE_LIST[7] = 0.02 *8;
        FULLSCALE_VOLTAGE_LIST[8] = 0.01 *8;
        FULLSCALE_VOLTAGE_LIST[9] = 0.005 *8;
	}


    @SuppressWarnings("unused")
	@Override
    public void onCreate(Bundle savedInstanceState) {
    	if(D) Log.i(TAG, "-- ON CREATE --");
        super.onCreate(savedInstanceState);

        // 前回終了時にセーブした値をロード

    	sharedData = getSharedPreferences("DataSave", Context.MODE_PRIVATE);
    	editor = sharedData.edit();


        voltscale = sharedData.getInt(KEY_VSCALE, 0);
        timescale = sharedData.getInt(KEY_HSCALE, 10);
        triggerMode = sharedData.getInt(KEY_TRIGGER_MODE, TGMODE_AUTO);
        triggerSlopeUp = sharedData.getBoolean(KEY_TRIGGER_SLOPE, true);
        dc_cut = sharedData.getBoolean(KEY_DCCUT,false);
        x10Mode = sharedData.getBoolean(KEY_X10,false);
        expand = sharedData.getBoolean(KEY_EXPAND,false);
        g_vpos =  (double) sharedData.getFloat(KEY_VPOS,0.5f+(float)(1.0/8));
        g_bias_pos =  (double) sharedData.getFloat(KEY_BIAS_POS,0.5f);
        g_hpos =  (double) sharedData.getFloat(KEY_HPOS,0.0f);

        setContentView(R.layout.main);
        mGraph = (GraphView) findViewById(R.id.Graph);

        scview = (ScrollView)findViewById(R.id.scrollView1);

        FrameRateText = (TextView)findViewById(R.id.text_framerate);
        triggerModeText = (TextView)findViewById(R.id.text_trigger_mode);
        TimescaleText = (TextView)findViewById(R.id.text_timescale);
        HPotisionText = (TextView)findViewById(R.id.text_h_trigger);
        VPotisionText = (TextView)findViewById(R.id.textVtrigger);
        RunStopText = (TextView)findViewById(R.id.text_run_stop);
        BiasText = (TextView)findViewById(R.id.text_bias);
        VoltscaleText = (TextView)findViewById(R.id.text_voltscale);
        VppText = (TextView)findViewById(R.id.text_vpp);
        FreqText = (TextView)findViewById(R.id.text_freq);
        VrmsText = (TextView)findViewById(R.id.text_vrms);
        MeanText = (TextView)findViewById(R.id.text_mean);

        img_trigger = (ImageView)findViewById(R.id.img_trigger);
        img_connect = (ImageView)findViewById(R.id.img_connect);
        img_range = (ImageView)findViewById(R.id.img_range);

        td_zoom_btn =  (Button)findViewById(R.id.btn_zoomplus);
        td_zoom_btn.setOnClickListener(this);
        td_unzoom_btn =  (Button)findViewById(R.id.btn_zoomminus);
        td_unzoom_btn.setOnClickListener(this);
        vs_plus_btn = (Button)findViewById(R.id.btn_vscale_plus);
        vs_plus_btn.setOnClickListener(this);
        vs_minus_btn = (Button)findViewById(R.id.btn_vscale_minus);
        vs_minus_btn.setOnClickListener(this);
        edge_btn = (Button)findViewById(R.id.btn_edge);
        edge_btn.setOnClickListener(this);
        btn_setzero  = (Button)findViewById(R.id.btn_setzero);
        btn_setzero.setOnClickListener(this);
        btn_autoset = (Button)findViewById(R.id.btn_autoset);
        btn_autoset.setOnClickListener(this);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        // for debug mode
        btn_test1 = (Button)findViewById(R.id.btn_bias_calib);
        btn_test2 = (Button)findViewById(R.id.btn_calib_value_reset);
        btn_setting = (Button)findViewById(R.id.btn_setting);
        calibration_btn = (Button)findViewById(R.id.btn_calib);


        if(D==false){	// リリース時にボタンを隠す
        	btn_test1.setVisibility(View.GONE);
        	btn_test2.setVisibility(View.GONE);
        	calibration_btn.setVisibility(View.GONE);
        	btn_setting.setVisibility(View.GONE);

        	scview.computeScroll();		// 表示を更新
        } else {
            btn_test1.setOnClickListener(this);
            btn_test2.setOnClickListener(this);
        	btn_setting.setOnClickListener(this);
        	calibration_btn.setOnClickListener(this);
        }

        tbtn_dccut = (ToggleButton)findViewById(R.id.tbtn_dccut);
        tbtn_dccut.setOnClickListener(this);
        tbtn_stop = (ToggleButton)findViewById(R.id.tbtn_stop);
        tbtn_stop.setOnClickListener(this);
        tbtn_xp = (ToggleButton)findViewById(R.id.tbtn_expand);
        tbtn_xp.setOnClickListener(this);
        tbtn_x10 = (ToggleButton)findViewById(R.id.tbtn_x10);
        tbtn_x10.setOnClickListener(this);

        trig_mode_button = (Button)findViewById(R.id.trig_mode_button);
        trig_mode_button.setOnClickListener(this);

		simpleListener = new GestureListener();
        gestureDetector = new ScaleGestureDetector(this, simpleListener);




        //
        //		USB Intent
        //
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_UPDATE_CURRENT);

		mUsbReceiver = new USBBroadcastReceiver();
		registerReceiver(mUsbReceiver, filter);
		mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		SendTimer = new Timer();

		mHandler = new Handler();

		// Call SetDevice()
		Intent intent = getIntent();
		UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
		if(device != null){
			mUsbReceiver.onReceive(this, intent);			// アプリが起動していない状態でデバイスが接続されてインテントから起動する時
		} else {

			// アイコンをクリックして起動した時にすでにデバイスが接続されているか調べる
			HashMap<String,UsbDevice> deviceList = mUsbManager.getDeviceList();
			Iterator<UsbDevice> deviceIterator  = deviceList.values().iterator();
			while(deviceIterator.hasNext()){
				device = deviceIterator.next();
				if(device.getProductId() == USB_DEVICE_PID && device.getVendorId() == USB_DEVICE_VID){	// VID PID 確認
					setDevice(device);
					break;
				}
			}
		}

		if(isConnected == false){		// setDeviceが呼ばれなかったら
			setParameters();			// テキスト、ポジションセット
		}

		//ボタン状態セット
		tbtn_x10.setChecked(x10Mode);
		if(x10Mode == true){
			proveRatio = 10.0;
		}
		tbtn_dccut.setChecked(dc_cut);
		tbtn_xp.setChecked(expand);

		// getDP
		DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
		float dpHeight = displayMetrics.heightPixels / displayMetrics.density;
		float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
		Log.d("DEBUG","Width->" + dpWidth + ",Height=>" + dpHeight);


		// 1秒ごとに実行するタイマー
		new Timer().scheduleAtFixedRate(new TimerTask(){
			public void run(){
				mHandler.post(new Runnable(){
					public void run(){
						FrameRateText.setText("FPS:"+Integer.toString(mGraph.frameRateGetAndReset()));
						if(wave==null)return;

						VppText.setText("Vpp:"+ String.format("%7s",siConverter(wave.vpp*FULLSCALE_VOLTAGE_LIST[voltscale]*proveRatio)+"V"));
						FreqText.setText("Frq:"+String.format("%5s",siConverter(wave.freq))+"Hz");
						VrmsText.setText("Vrms:"+String.format("%5s",siConverter(wave.vrms*FULLSCALE_VOLTAGE_LIST[voltscale]*proveRatio))+"V");
						MeanText.setText("Mean:"+String.format("%5s",siConverter(wave.mean*FULLSCALE_VOLTAGE_LIST[voltscale]*proveRatio))+"V");

//						if(wave.range_status == GraphWave.RANGE_IN){
//							img_range.setImageResource(R.drawable.over_range_in);
//						} else if(wave.range_status == GraphWave.RANGE_UP_OVER){
//							img_range.setImageResource(R.drawable.over_range_up);
//						} else if(wave.range_status == GraphWave.RANGE_DOWN_OVER){
//							img_range.setImageResource(R.drawable.over_range_down);
//						} else {
//							img_range.setImageResource(R.drawable.over_range_bi);
//						}
					}
				});
			}
		}, 0, 1000L);

    }

    private class USBBroadcastReceiver extends BroadcastReceiver{

			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();

				if(D) Log.i(TAG, "USBBroadcastReceiver ON RECEIVE");

				if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {		// アプリが起動している時にデバイスが接続されたら
					if(D) Log.i(TAG, "ACTION_USB_DEVICE_ATTACHED");

					UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

					if(device == null) {
						Log.e(TAG, "device pointer is null");
						return;
					}

					if(device.getProductId() != USB_DEVICE_PID || device.getVendorId() != USB_DEVICE_VID){	// VID PID 確認
						Log.e(TAG, "incorrect usb device");
						return;
					}

					setDevice(device);
				}

				if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {		// 起動している時にデバイスが切断されたら
					Log.i(TAG, "device disconnected");
					if(ReceiveThread != null){
						isConnected = false;
						ReceiveThread.EndConnection();
						SendTimer.cancel();
						img_connect.setImageResource(R.drawable.disconnect);
					}
				}


				//USB 接続許可インテントを受け取る
		        if (ACTION_USB_PERMISSION.equals(action)) {
		        	if(D) Log.i(TAG, "ACTION_USB_PERMISSION");
		            synchronized (this) {
		                UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

		                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
		                    if(device != null){
		                      //call method to set up device communication
		                    setDevice(device);
		                   }
		                }
		                else {
		                    if(D)Log.d(TAG, "permission denied for device " + device);
		                }
		            }
		        }


			}
		}


	// 指定の桁をbyteで返す
	// digit: 上位バイト=1  下位バイト=0
	private byte int_to_byte(int i,int digit) {
		return (byte)((i >> (digit * 8) ) & 0xFF);
	}

	// 2byteを結合して16bitの符号付き整数をintで返す
	private int byte_to_halfword(byte upper,byte lower){
		return (((int)upper) << 8) | ((int)lower & 0xff);
	}

	public void setDevice(UsbDevice device) {

			if(D)Log.d(TAG, "SET DEVICE");

			if(mUsbManager.hasPermission(device)==false){	// パーミッション確認
				Log.e(TAG, "DEVICE has no parmission!");
//				mUsbManager.requestPermission(device, mPermissionIntent);
//				return;
			};
			//
			//	デバイス初期化
			//

			deviceConnection = mUsbManager.openDevice(device);
			if(deviceConnection == null){
				if(D)Log.e(TAG, "Open device failed !");
				return;
			}

			intf = device.getInterface(0);

			if (deviceConnection != null){
				deviceConnection.claimInterface(intf, true);
			}

			try {
				if (UsbConstants.USB_DIR_IN == intf.getEndpoint(0).getDirection()) {
					endPointRead = intf.getEndpoint(0);
					//EpInPacketSize = endPointRead.getMaxPacketSize();
				}
			} catch (Exception e) {
				if(D)Log.e(TAG, "Device have no endPointRead", e);
			}

			try {
				if (UsbConstants.USB_DIR_OUT == intf.getEndpoint(1).getDirection()) {
					epw_Msg = intf.getEndpoint(1);
					EpOutPacketSize = epw_Msg.getMaxPacketSize();
				}
			} catch (Exception e) {
				if(D)Log.e(TAG, "Device have no epw_Msg", e);
			}

			epw_Firm = intf.getEndpoint(2);
			if (UsbConstants.USB_DIR_OUT != epw_Firm.getDirection()){
				if(D)Log.e(TAG, "ep2 Direction Incorrect");
			}
			epw_FirmPacketSize = epw_Firm.getMaxPacketSize();

			//
			//		オシロスコープセッティング開始
			//
			// 接続アイコンを緑色に
			img_connect.setImageResource(R.drawable.connected);

			// ファームウェア送信
			Resources res = getResources();
			if(D)Log.d(TAG,"Send firmware");
			if(D)Log.d(TAG, "M4 firmware transfer start.");
			sendFirmware(M4_FIRM_WRITEADDR,res.openRawResource(R.raw.m4),M4_FIRM_ENTRYADDR);

			if(D)Log.d(TAG, "M0APP firmware transfer start.");
			sendFirmware(M0APP_FIRM_WRITEADDR,res.openRawResource(R.raw.m0app),M0APP_FIRM_ENTRYADDR);

			byte[] ReadBuffer = new byte[endPointRead.getMaxPacketSize()];
//			while(true){	// コンフィグ終了待機){
//				deviceConnection.bulkTransfer(endPointRead, ReadBuffer,1, 1000);
//				if(ReadBuffer[0]==(byte)43)break;
//			}

			// I2C EEPROM READ
			sendMessage(MESSAGE_EEPROM_PAGE_READ,16);		//EEPROM データシーケンシャルリードリクエスト
			deviceConnection.bulkTransfer(endPointRead, ReadBuffer,16, 250);	//EEPROM データ受信

			// page 0
			int i=0;
			highdiv_input_calib_voltage	 	= byte_to_halfword(ReadBuffer[i++],ReadBuffer[i++]);
			lowdiv_input_calib_voltage 		= byte_to_halfword(ReadBuffer[i++],ReadBuffer[i++]);
		    highdiv_opa_diff				= byte_to_halfword(ReadBuffer[i++],ReadBuffer[i++]);
		    lowdiv_opa_diff	 				= byte_to_halfword(ReadBuffer[i++],ReadBuffer[i++]);


		    // page 1
		    normal_bias_upper		 = byte_to_halfword(ReadBuffer[8],ReadBuffer[9]);
		    normal_bias_center		 		 = byte_to_halfword(ReadBuffer[10],ReadBuffer[11]);
		    normal_bias_lower		 = byte_to_halfword(ReadBuffer[12],ReadBuffer[13]);

		    if(D){
		    	Toast.makeText(this,
		    			"highdiv_input_calib_voltage = "+Integer.toString(highdiv_input_calib_voltage)+"\n"+
		    			"lowdiv_input_calib_voltage = "+Integer.toString(lowdiv_input_calib_voltage)+"\n"+
		    			"highdiv_opa_diff = "+Integer.toString(highdiv_opa_diff)+"\n"+
		    			"lowdiv_opa_diff = "+Integer.toString(lowdiv_opa_diff)+"\n"+
		    			"bias_center = "+Integer.toString(bias_center)+"\n"+
		    			"bias_upper = "+Integer.toString(normal_bias_upper)+"\n"+
		    			"bias_lower = "+Integer.toString(normal_bias_lower)+"\n"
		    			, Toast.LENGTH_LONG).show();
		    }

		    if(bias_center == -1){
		    	highdiv_input_calib_voltage = 0;
		    	lowdiv_input_calib_voltage = 0;
		    	highdiv_opa_diff = 0;
		    	lowdiv_opa_diff = 0;
		    	normal_bias_upper = TYPICAL_BIAS_FS_VALUE/2;
		    	normal_bias_lower = TYPICAL_BIAS_FS_VALUE/2;
		    	bias_center = TYPICAL_BIAS_CENTER;
		    }

	        setupVerticalAxis();
	        setupBias();

			//
			// オシロスコープ初期値コンフィグ
			//
	        setParameters();

			if(dc_cut){
				sendMessage(MESSAGE_DCCUT,1);
			} else {
				sendMessage(MESSAGE_DCCUT,0);
			}

			ReceiveThread = new UsbReceiveThread();
			ReceiveThread.start();

			isConnected = true;

			autoModeTask = new AutoModeTimerTask();
			new Timer().scheduleAtFixedRate(autoModeTask,0,10L);	// 10msごとに実行

		}

	// オシロスコープ設定値セット
	private void setParameters(){
		setTimescale();		// setHTrigger() も呼ばれる
		setVoltscale(true);	// setBias() も呼ばれる
		setTriggerMode();
		setTriggerSlope();
		setVTrigger(true);
	}


	private void sendFirmware(int writeaddr,InputStream bin,int entryaddr){

		byte buff[] = new byte[epw_FirmPacketSize];
		int file_size = 0;

		try {
			file_size = bin.available();				// ファイルサイズ取得

			//先頭1~4バイトにリトルエンディアンで書き込みアドレスを記述

			for(int i=0;i<4;i++){
				buff[i] = (byte)(writeaddr>>(i*8) & 0xff);
			}

			//先頭4~8バイトにリトルエンディアンでファイルサイズを記述
			for(int i=0;i<4;i++){
				buff[4+i] = (byte)(file_size>>(i*8) & 0xff);
			}

			//先頭9~12バイトにリトルエンディアンで実行アドレスを記述
			for(int i=0;i<4;i++){
				buff[8+i] = (byte)(entryaddr>>(i*8) & 0xff);
			}

			int IsFinished = bin.read(buff, 12, epw_FirmPacketSize-12);

			// ファイルの最後までパケット送信
			while(IsFinished > 0){
				int ret;
				ret = deviceConnection.bulkTransfer(epw_Firm, buff, epw_FirmPacketSize, 100);
				if(ret<0){
					Log.e(TAG, "Firmware transfer failed!");
				}
				IsFinished = bin.read(buff,0,epw_FirmPacketSize);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	// キャリブレーション値を読み込んで電圧軸セットアップ
    private void setupVerticalAxis(){
    	if(voltscale < VOLTSCALE_200MV){
    		adc_max_voltage = TYPICAL_ADC_INPUT_MAX_VOLTAGE_PP + ((double)highdiv_input_calib_voltage / 1000);
    	} else {
    		adc_max_voltage = TYPICAL_ADC_INPUT_MAX_VOLTAGE_PP + ((double)lowdiv_input_calib_voltage / 1000);
    	}
    	adc_max_voltage = adc_max_voltage / TYPICAL_AMP_ERROR[voltscale];

    	if(timescale <= 5){		// 80Msps の時は入力レンジが狭まるので補正
    		adc_max_voltage = adc_max_voltage * ADC_MAX_VOLTAGE_DROP_AT_80MSPS;
    	}

    	one_lsb_voltage = adc_max_voltage / SAMPLE_MAX;
    	normal_graph_fullscale = (int) (SAMPLE_MAX * (GRAPH_FULLSCALE_VOLTAGE / adc_max_voltage));
    	normal_graph_min = (SAMPLE_MAX - normal_graph_fullscale) / 2;
    	vs8_graph_fullscale = (int) (SAMPLE_MAX * (GRAPH_FULLSCALE_VOLTAGE / adc_max_voltage) * 0.5);
    	vs9_graph_fullscale = (int) (SAMPLE_MAX * (GRAPH_FULLSCALE_VOLTAGE / adc_max_voltage) * 0.25);

    	g_vpos_max = 1 + (((adc_max_voltage / GRAPH_FULLSCALE_VOLTAGE)-1) / 2);
    	g_vpos_min = -(((adc_max_voltage / GRAPH_FULLSCALE_VOLTAGE)-1) / 2);

		if(voltscale <= 7){
			graph_fullscale = normal_graph_fullscale;
			graph_min = normal_graph_min;
		} else if(voltscale == 8) {
			graph_fullscale = vs8_graph_fullscale;
			graph_min =  (SAMPLE_MAX - vs8_graph_fullscale) / 2;
		} else {
			graph_fullscale = vs9_graph_fullscale;
			graph_min = (SAMPLE_MAX - vs9_graph_fullscale) / 2;
		}
    }

    //キャリブレーション値を読み込んでバイアスを調整
    private void setupBias(){


    	g_bias_pos_max = 0.5 + ((BIAS_MAX_REG_VALUE - bias_center)/(double)normal_bias_upper);
    	g_bias_pos_min = 0.5 - ((double) (BIAS_MAX_REG_VALUE - bias_center)/(double)normal_bias_lower);

    	double bias_one_lsb_vs_adc = (double)normal_graph_fullscale / (double)(normal_bias_upper+normal_bias_lower);
        opamp_offset_calib_value_list [0] = 0;
        opamp_offset_calib_value_list [1] = (int) Math.round(-((double)highdiv_opa_diff * (1.5/9.0) / bias_one_lsb_vs_adc));
        opamp_offset_calib_value_list [2] = (int) Math.round(-((double)highdiv_opa_diff * (4.0/9.0) / bias_one_lsb_vs_adc));
        opamp_offset_calib_value_list [3] = (int) Math.round(-((double)highdiv_opa_diff / bias_one_lsb_vs_adc));
        opamp_offset_calib_value_list [4] = 0;
        opamp_offset_calib_value_list [5] = (int) Math.round(-((double)lowdiv_opa_diff * (1.0/9.0) / bias_one_lsb_vs_adc));
        opamp_offset_calib_value_list [6] = (int) Math.round(-((double)lowdiv_opa_diff * (3.0/9.0) / bias_one_lsb_vs_adc));
        opamp_offset_calib_value_list [7] = (int) Math.round(-((double)lowdiv_opa_diff / bias_one_lsb_vs_adc));
        opamp_offset_calib_value_list [8] = opamp_offset_calib_value_list [7];
        opamp_offset_calib_value_list [9] = opamp_offset_calib_value_list [7];

		if(voltscale <= 7){
			bias_lower = (int)(normal_bias_lower * TYPICAL_AMP_ERROR[voltscale]);
			bias_upper = (int)(normal_bias_upper * TYPICAL_AMP_ERROR[voltscale]);
		} else if(voltscale == 8) {
			bias_lower = normal_bias_lower / 2;
			bias_upper = normal_bias_upper / 2;
		} else {
			bias_lower = normal_bias_lower / 4 ;
			bias_upper = normal_bias_upper / 4;
		}

    	if(timescale <= 5){		// 80Msps の時は入力レンジが狭まるので補正
    		bias_center = normal_bias_center + BIAS_DROP_CALIBVAL_AT_80MSPS;
//    		bias_lower = (int)(bias_lower * ADC_MAX_VOLTAGE_DROP_AT_80MSPS);
//    		bias_upper = (int)(bias_upper * ADC_MAX_VOLTAGE_DROP_AT_80MSPS);
    	} else {
    		bias_center = normal_bias_center;
    	}
    }



    // オートモード時のトリガーがかからなかった時のタイマー
    private volatile int autoTime = 0;
    public class AutoModeTimerTask extends TimerTask {
    	public void run(){
    		if(autoModeNormal == false){
    			return;
    		} else if(triggerMode != TGMODE_AUTO){
    			autoTime = 0;
    			autoModeNormal = false;
    			return;
    		}


    		// 一定時間経過してトリガーを検出しなかったらFREEモードにする
    		if(autoTime > 30){	// 10ms * 30 = 300ms
    			sendMessage(MESSAGE_RUNMODE,TGMODE_FREE);
    			endAutoModeNormal();
    		}

    		autoTime++;

    		//if(D)Log.d("AutoModeTimerTask", "autoTime is " + Integer.toString(autoTime));
    	}

    	public void timeReset(){
    		autoTime = 0;
    		//if(D)Log.d("AutoModeTimerTask", "autoTime reset");
    	}

    	public void endAutoModeNormal(){
			autoTime = 0;
			autoModeNormal = false;
    	}
    }



    private class UsbReceiveThread extends Thread {

		private ByteBuffer ReceiveBuffer;
		private byte[] SendBuffer;
		double[] g_wave;
		int []lawSamples;


		@Override
		public void run(){
			// Acknowledgeパケット
			SendBuffer = new byte[EpOutPacketSize];
        	SendBuffer[0] = MESSAGE_DATA_RECEIVED;

        	ReceiveBuffer = ByteBuffer.allocate(1600);
        	ReceiveBuffer.order(ByteOrder.LITTLE_ENDIAN);	// set endian

        	inRequest = new UsbRequest();

        	if(inRequest.initialize(deviceConnection, endPointRead) == true){
        		if(D) Log.i(TAG, "inRequest initialize suceeded.");
        	} else {
        		if(D) Log.e(TAG, "inRequest initialize failed.");
        		EndConnection();
        	}

        	g_wave = new double[DEFAULT_SAMPLE_LENGTH];		// 正規化したサンプル
        	lawSamples = new int[DEFAULT_SAMPLE_LENGTH];		// 受信したサンプルをそのまま格納する


        	//
        	//		main loop
        	//
        	while (deviceConnection != null && endPointRead != null && isConnected) {
        		//int t_sample_length = sampleLength;	// sampleLength をホールド

        		if (inRequest.queue(ReceiveBuffer, BUFFER_SIZE) == true) {

        			// 受信待ち
        			if (deviceConnection.requestWait() == inRequest){	//Request received
        				//if(D) Log.i(TAG, "Request received.");
        			}

        			// Acknowledge送信
        			deviceConnection.bulkTransfer(epw_Msg, SendBuffer, SendBuffer.length, 10);

        		}  else {
        			if(D) Log.e(TAG, "Request wait error occured");
        			EndConnection();	// thread stop
        			break;
        		}

        		ReceiveBuffer.clear();


        		for(int i=0;i<sampleLength;i++){
        			lawSamples[i] = (int) ReceiveBuffer.getShort();
        		}

        		// グラフ高さを１としたデータに変換
        		for(int i=0;i< sampleLength;i++){
        			g_wave[i] = (double)(lawSamples[i] - graph_min) / graph_fullscale;	// 範囲を狭める

//        			if(g_wave[i] < 0){	// クリップ処理
//        				g_wave[i] = 0;
//        			} else if(g_wave[i] > 1.0){
//        				g_wave[i] = 1.0;
//        			}

        		}

        		wave =  new GraphWave(g_wave);		// 解析

    			mHandler.post(new Runnable(){
    				public void run(){
    		    		if(calibration == true){
    		    			calibrate(lawSamples);
    		    		}

    		    		if(biasCalibState > 0){
    		    			biasCalib(lawSamples);
    		    		}

    		    		if(autoSetState > 0){
    		    			autoSet();
    		    		}

    		    		drawGraph();
    				}
    			});

    			//debug
//    			Log.d(TAG,"DC = "+Integer.toString(getDC(lawSamples)));
//    			Log.d(TAG,"graph_fullscale = "+Integer.toString(graph_fullscale));

        	}

			// deviceConnection shutdown
			deviceConnection = null;
			endPointRead = null;
			epw_Msg = null;
			mGraph.endThread();
		}

		private void drawGraph(){

			mGraph.setWave(wave.samples,sampleLength); 	//画面描画

    		if(triggerMode == TGMODE_AUTO && run_status==true){	// if Auto mode

    			// トリガー検出

    			boolean lower = !triggerSlopeUp;
    			boolean triggerDetect = false;
        		for(int i=0;i<sampleLength;i++){
    				if(wave.samples[i] >= g_vpos){	// サンプル値はトリガーを上回る
    					if(triggerSlopeUp && lower){
    						triggerDetect = true;
    						break;
    					}

    					lower = false;
    				} else{							// サンプル値はトリガーを下回る
    					if(!triggerSlopeUp && !lower){
    						triggerDetect = true;
    						break;
    					}

    					lower = true;
    				}
        		}

        		if(triggerDetect){
					//
					// トリガーを検出した
					//

					if(autoModeNormal == false){	// AUTO FREE
						autoModeNormal = true;
						sendMessage(MESSAGE_RUNMODE,TGMODE_NORMAL);	// NORMALモードにする
					} else {						// AUTO NORMAL
						autoModeTask.timeReset();	// タイマーカウントリセット
					}
        		}
    		} else if(triggerMode == TGMODE_SINGLE){		// SINGLE モード
    			runModeSetStop();	// デバイス側では内部的に勝手に変わるので送信する必要は無い
    			tbtn_stop.setChecked(true);
    			if(D)Log.d(TAG, "Stop botton set cheched");
    		}
    	}

		public void EndConnection(){
			isConnected = false;

		}
	}

    private class GraphWave {
    	double samples[];			// graph全体を１としたサンプル
    	double max=0,min=0,vpp=0;	// graph全体を1とした際の値にする
    	double freq=0;
    	double vrms=0,mean=0;
    	int num_waves=0;
    	int range_status=0;			// 0= range in ,1=up over,2=down over,3=bi side over
    	final static int RANGE_IN = 0,RANGE_UP_OVER = 1,RANGE_DOWN_OVER = 2,RANGE_BISIDE_OVER = 3;

    	GraphWave(double gwave[]){
    		samples = gwave;
        	if(samples == null)return;

        	//
        	//	レンジオーバーチェック
        	//

        	for(int i=0;i<sampleLength;i++){
        		if(samples[i] > 1){	// up over

        			if(range_status==RANGE_DOWN_OVER){
        				range_status = RANGE_BISIDE_OVER;
        				break;
        			} else {
        				range_status = RANGE_UP_OVER;
        			}


        		} else if(samples[i] < 0){	// down over

        			if(range_status==RANGE_UP_OVER){
        				range_status = RANGE_BISIDE_OVER;
        				break;
        			} else {
        				range_status = RANGE_DOWN_OVER;
        			}
        		}
        	}

			mHandler.post(new Runnable(){
				public void run(){
					if(wave.range_status == RANGE_IN){
						img_range.setImageResource(R.drawable.over_range_in);
					} else if(wave.range_status == RANGE_UP_OVER){
						img_range.setImageResource(R.drawable.over_range_up);
					} else if(wave.range_status == RANGE_DOWN_OVER){
						img_range.setImageResource(R.drawable.over_range_down);
					} else {
						img_range.setImageResource(R.drawable.over_range_bi);
					}
				}
			});


        	//
        	//	平均電圧 mean
        	//

			double aqum = 0;
        	for(int i=0;i<sampleLength;i++){
        		aqum +=  samples[i] - g_bias_pos;
        	}

        	mean = aqum / (double) sampleLength;
        	mean = mean - (mean%0.001);	// 精度を落とす
        	if(Math.abs(mean) <= 0.003){
        		mean = 0;
        	}

        	//
        	//	実効値　Vrms
        	//

        	aqum = 0;
        	for(int i=0;i<sampleLength;i++){
        		aqum +=  Math.pow((samples[i] - g_bias_pos),2.0);		// パワーを足していく
        	}

        	vrms = aqum / (double) sampleLength;
        	vrms = Math.sqrt(vrms);


        	//
        	//	peak-to-peak 計測
        	//

        	max = samples[0];
        	min = samples[0];	//最高値、最低値

        	for(int i=1;i<sampleLength;i++){
        		if(samples[i] > max) max = samples[i];
        		if(samples[i] < min) min = samples[i];
        	}
        	vpp = max-min;

        	//
        	//	周波数計測
        	//

        	freq = 0;

        	if(vpp < (1/16)){	// 振幅が1/2div 以下なら計測しな
        		return;
        	}

        	// 処理の流れ
        	// スレシュルドhighをダウンクロスかスレシュルドLowをアップクロスした点を探す
        	// 反対側のスレシュルドをクロスするか探索
        	// 見つけたら最初のスレシュルドhighをダウンクロスかスレシュルドLowをアップクロスした点を位相の開始地点にする
        	//

        	double high_threth = max - vpp/4;		// ハイ側しきい値
        	double low_threth = min + vpp/4;		// ロー側しきい値

        	int aqum_time = 0,last_overed_i=0;

        	int state_before = 0;
        	final int STATE_HIGH = 1;
        	final int STATE_LOW = 2;

        	for(int i=0;i<sampleLength;i++){
        		if(high_threth < samples[i] ){								// high
        			if(state_before == STATE_LOW){
        				if(last_overed_i != 0){
        					aqum_time += i - last_overed_i;
        					num_waves++;
        				}

        				last_overed_i = i;
        			}

        			state_before = STATE_HIGH;
        		} else if(low_threth > samples[i] ){						// low
//        			if(state_before == STATE_HIGH){
//        				aqum_time += i - last_overed_i;
//        				num_crossed++;
//        			}

        			//last_overed_i = i;
        			state_before = STATE_LOW;
        		}
        	}

        	if(num_waves > 0){
        		freq = (1.0 / ((((double)aqum_time * ( TIME_CONVERT_LIST[timescale] / 80.0) ) / (double)num_waves)));// * 2;
        	}

    	}
    }


	// すぐメッセージ送信する
    private void sendMessage(int m,int d) {
    	if(epw_Msg == null)return;

    	byte[] buffer = new byte[EpOutPacketSize];
	    buffer[0] = (byte) m;
	    buffer[1] = (byte) (0xff&d);
	    buffer[2] = (byte) ((0xff00&d)>>8);
	    buffer[3] = (byte) ((0xff0000&d)>>16);
	    buffer[4] = (byte) ((0xff000000&d)>>24);
	    int ret = deviceConnection.bulkTransfer(epw_Msg, buffer, buffer.length, 50);

	    if(ret<0){
	    	 if(D)Log.e(TAG, "Send Message FAILURE");
	    }
	    //if(D)Log.d(TAG, "Send Message = " + Integer.toString(m) + " Data = " + Integer.toString(d));
    }


//	// すぐメッセージ送信する
//    private void sendMessage(int m) {
//    	if(epw_Msg == null)return;
//
//    	byte[] buffer = new byte[EpOutPacketSize];
//	    buffer[0] = (byte) m;
//	    int ret = deviceConnection.bulkTransfer(epw_Msg, buffer, 1, 50);
//	    if(ret<0){
//	    	 if(D)Log.e(TAG, "Send Message FAILURE");
//	    }
//
//	    if(D)Log.d(TAG, "Send Message = " + Integer.toString(m));
//    }

	public String siConverter(double value){
		String si = "";
		String minus = null;

		// 負値なら
		if(value < 0){
			minus = "-";
			value = (-value);
		}


		if (value < 0.000000001){	// 値はすごく小さい
			return "0";				// return

		} else if( value < 0.000001 ){	// nano
			si = "n";
			value = value * 1000000000;
		} else if( value < 0.001) {		// micro
			si = "μ";
			value = value * 1000000;
		} else if ( value < 1 ){		// mili
			si = "m";
			value = value * 1000;
		} else if (value < 1000){
			// 何もしない
		} else if (value < 1000000) {	// Kiro
			si = "K";
			value = value * 0.001;
		} else if (value < 1000000000){	// Mega
			si = "M";
			value = value * 0.000001;
		}

		String f;	// format
		if( value < 10 ){
			f = "%1.2f";
		} else if (value < 100) {
			f = "%2.1f";
		} else {			// value < 1000
			f = "%3.0f";
		}

		if(minus==null)
			return String.format(String.format(f,value)+si);
		else
			return String.format(minus+String.format(f,value)+si);
    }


    public void RunStopToggle(){
    	run_status = !run_status;

    	if(run_status){
    		RunStopText.setText("RUN");
    		RunStopText.setTextColor(android.graphics.Color.GREEN);
    	}else{
    		RunStopText.setText("STOP");
    		RunStopText.setTextColor(android.graphics.Color.RED);
    	}
    }

    public void runModeSetStop(){
    	run_status = false;
    	RunStopText.setText("STOP");
		RunStopText.setTextColor(android.graphics.Color.RED);
    }

    public void runModeSetRun(){
    	run_status = true;
    	RunStopText.setText("RUN");
		RunStopText.setTextColor(android.graphics.Color.GREEN);
    }

    private void setTriggerMode(){

		if(autoModeNormal && autoModeTask != null && triggerMode != TGMODE_AUTO){	// AutoModeTaskを終了させる
			autoModeTask.endAutoModeNormal();
		}

    	if(run_status == true){		// STOPの時は送信しない
    		if(triggerMode == TGMODE_AUTO  || triggerMode == TGMODE_FREE) {
    			sendMessage(MESSAGE_RUNMODE,TGMODE_FREE);
    		} else if(triggerMode == TGMODE_NORMAL){
    			sendMessage(MESSAGE_RUNMODE,TGMODE_NORMAL);
    		} else if (triggerMode == TGMODE_SINGLE) {
    			sendMessage(MESSAGE_RUNMODE,TGMODE_SINGLE);
    		} else if (triggerMode == TGMODE_SINGLE_FREE) {
    			sendMessage(MESSAGE_RUNMODE,TGMODE_SINGLE_FREE);
    			return;
    		}
    	}

    	triggerModeText.setText(TRIGGER_MODE_LIST[triggerMode]);
    	//背景を点滅
    	blinkTextBG(triggerModeText,Color.GRAY);
    }


    private void setTimescale(){	// 80Msps時は4.5%波形が大きくなる　→　最大入力レンジが4.5%狭まる

    	switch(timescale){
    		case 0:
    			sampleLength = 20;
    			break;
    		case 1:
    			sampleLength = 40;
    			break;
			case 2:
				sampleLength = 80;
				break;
			case 3:
				sampleLength = 200;
				break;
			case 4:
				sampleLength = 400;
				break;
			default:	// 5以上
				sampleLength = 800;
    	}

    	// 80Mspsの時はADCの範囲が狭まるので縦軸補正値を計算し直す
    	setupVerticalAxis();
    	setupBias();

    	sendMessage(MESSAGE_TIMESCALE,timescale);
    	setHTrigger();						// トリガーTimerを再計算させるため
    	setBias(true);

    	TimescaleText.setText("Td:"+String.format("%4s", TIME_SCALE_LIST[timescale])+"s");

    	//背景を点滅
    	blinkTextBG(TimescaleText,Color.GRAY);
    }


    // g_hpos: 画面右端が1中央が0左端が-1
	public void setHTrigger(){

		// 水平トリガー変更メッセージ送信
		int h_value = (int) (g_hpos * (sampleLength/2));
		sendMessage(MESSAGE_H_TRIGGER, h_value);

		//if(D)Log.d(TAG, "Set pos = " + Double.toString(pos));
		mGraph.setHTriggerPos(g_hpos);

		double ht_real = g_hpos * TIME_CONVERT_LIST[timescale] * 4.0;
		HPotisionText.setText("HT:"+ String.format("%7s", siConverter(ht_real) + "s"));

		//HPotisionText.setText("HT:"+Integer.toString(HTriggerValue));
	}


	public void setVTrigger(boolean isSendData){

    	verTriggerValue = (int)( g_vpos * graph_fullscale + graph_min);
    	if(D && isSendData)Log.d(TAG, "verTriggerValue = "+Integer.toString(verTriggerValue));
    	if(isSendData){

    		if(verTriggerValue > SAMPLE_MAX){
    			verTriggerValue = SAMPLE_MAX;
    		} else if(verTriggerValue < 0){
    			verTriggerValue = 0;
    		}

			// 垂直トリガー変更メッセージ送信
    		sendMessage(MESSAGE_V_TRIGGER,verTriggerValue);
    	}

    	mGraph.setVTriggerPos(g_vpos);

    	double realVoltage = (g_vpos-g_bias_pos) * FULLSCALE_VOLTAGE_LIST[voltscale] * proveRatio;
		//VPotisionText.setText("VT:"+Integer.toString(verTriggerValue));		// 生の値を表示
    	VPotisionText.setText("VT:" + String.format("%7s",siConverter(realVoltage) +"V"));
    }

    public void setBias(boolean isSendData){
		int bias = 0;
		if(isSendData){

			if(bias < 0.5) {	// 0.5 未満
				bias = bias_center - (int)((1.0 - (g_bias_pos * 2.0)) * bias_lower);
			} else {	// 0.5以上
				bias = bias_center + (int)(((g_bias_pos-0.5) * 2.0) * bias_upper);
			}

			bias = bias + opamp_offset_calib_value_list[voltscale];

//	    	if(timescale <= 5){	// 80Msps の時は入力レンジが狭まるので補正
//	    		bias += BIAS_DROP_CALIBVAL_AT_80MSPS ;
//    		}

			if(bias > BIAS_MAX_REG_VALUE){
				bias = BIAS_MAX_REG_VALUE;
			} else if (bias < 0){
				bias = 0;
			}

			// バイアス電圧変更メッセージ送信
			sendMessage(MESSAGE_BIAS,bias);
		}

		mGraph.setBiasPos(g_bias_pos);
		setVTrigger(false);
		//BiasText.setText("BI:"+Integer.toString(bias));	// 生の値を表示
		double real = (g_bias_pos-0.5) * FULLSCALE_VOLTAGE_LIST[voltscale] * proveRatio;
		BiasText.setText("PS:"+String.format("%7s",siConverter(real)+"V"));
	}

    private void setTriggerSlope(){
		if(triggerSlopeUp){
			sendMessage(MESSAGE_TRIGGER_EDGE,0);
			img_trigger.setImageResource(R.drawable.slope_up);
		} else {
			sendMessage(MESSAGE_TRIGGER_EDGE,1);
			img_trigger.setImageResource(R.drawable.slope_down);
		}
    }


	private void setVoltscale(boolean isSendData){

		if(isSendData){
			sendMessage(MESSAGE_VOLTAGE_SCALE,voltscale);
		}
		String scale;

		if(x10Mode){
			scale = VOLT_SCALE_LIST_X10[voltscale];
		} else {
			scale = VOLT_SCALE_LIST[voltscale];
		}
		VoltscaleText.setText("Vd:"+String.format("%4s",scale)+"V");

//		//ボタンを消す
//		if(voltscale==0){
//			vs_plus_btn.setVisibility(View.INVISIBLE);
//			vs_minus_btn.setVisibility(View.VISIBLE);
//		} else if(voltscale==(NUM_VOLTSCALE-1)){
//			vs_plus_btn.setVisibility(View.VISIBLE);
//			vs_minus_btn.setVisibility(View.INVISIBLE);
//		} else {
//			vs_plus_btn.setVisibility(View.VISIBLE);
//			vs_minus_btn.setVisibility(View.VISIBLE);
//		}

		setupVerticalAxis();
		setupBias();

		setVTrigger(false);
		setBias(true);
	}


	private int getDC(int[] samples) {
		int aqum = 0;

		for(int i=0;i<samples.length;i++){
			aqum += samples[i];
		}

		int dc = (int)((double)aqum / samples.length);

		if(D)Log.d(TAG, "DC = "+Integer.toString(dc));
		return dc;
	}

	int gndValue=0,gndValue200mV = 0;;
	int calibrateReceivedCount = 0;
	double bias_vs_adc_ratio;
	public void calibrate(int [] samples){


		if(D)Log.d(TAG, "calibrateReceivedCount = "+Integer.toString(calibrateReceivedCount));

		int exe=0;

		if(calibrateReceivedCount == (exe++)){	// キャリブレーション準備
			sendMessage(MESSAGE_RUNMODE,TGMODE_DEVICE_STOP);	// STOP


			// キャリブレーション値リセット
			highdiv_input_calib_voltage = 0;
			lowdiv_input_calib_voltage = 0;
			highdiv_opa_diff = 0;
			lowdiv_opa_diff = 0;

			setupVerticalAxis();
			setupBias();

	    	voltscale = VOLTSCALE_5V;  // 5V div
	    	setVoltscale(true);

	    	if(dc_cut == true){
    	    	dc_cut = false;
    	    	onClick(tbtn_dccut);				// DC Thru
	    	}


	    	timescale = TIMESCALE_2P5MS;		// 2.5ms
	    	setTimescale();

	    	if(run_status == false){
	    		RunStopToggle();
	    	}

			// バイアスをグラフの3/8にセット
			//sendMessage(MESSAGE_BIAS,TYPICAL_BIAS_BOTTOM+((int)(TYPICAL_BIAS_FS_VALUE*3/8.0)));
			g_bias_pos = 3.0/8.0;
			setBias(true);

			triggerSlopeUp = true;	// Up trigger
			setTriggerSlope();

			sendMessage(MESSAGE_RUNMODE,TGMODE_SINGLE_FREE);	//フリーシングル
		} else if(calibrateReceivedCount == (exe++) || calibrateReceivedCount == (exe++)){
			sendMessage(MESSAGE_RUNMODE,TGMODE_SINGLE_FREE);	//フリーシングル

		} else if(calibrateReceivedCount == (exe++)){		// GND 値取得
			gndValue = getDC(samples);		// 5V divの時のGND値

	    	voltscale = VOLTSCALE_200MV;  // 200mV div
	    	setVoltscale(true);
			sendMessage(MESSAGE_RUNMODE,TGMODE_SINGLE_FREE);	//フリーシングル

		} else if( calibrateReceivedCount == (exe++)){		// GND 値取得
			Log.d( TAG,"calibration: get gnd value" );

			gndValue200mV = getDC(samples);

			triggerMode = TGMODE_NORMAL;
			setTriggerMode();

			triggerSlopeUp = true;
			setTriggerSlope();

			g_vpos = 4.0/8.0;	// VTrigger = center
			setVTrigger(true);

			g_hpos = -1.5;		// 画面外左に設定
			setHTrigger();

			Toast.makeText(this, "GNDレベル取得。校正器接続待機", Toast.LENGTH_SHORT).show();

			Log.d( TAG,"calibration: waiting connect calibrator" );

		} else if(calibrateReceivedCount == (exe++)){
			//
			//	Low div キャリブレーション値取得
			//

			// ADC最大入力電圧幅誤差計測（最大１０％±）
			double real_vpp200mv_val = (double)(getDC(samples) - gndValue200mV);
			double imagine_vpp200mv_val = TYPICAL_FULLSCALE_MAX_VAL * TYPICAL_AMP_ERROR[VOLTSCALE_200MV] * (LOWDIV_CALIBRATION_VOLTAGE / (0.2*8)); 	// アンプエラーを考慮
			int tmp = (int)(((imagine_vpp200mv_val / real_vpp200mv_val) - 1.0) * TYPICAL_ADC_INPUT_MAX_VOLTAGE_PP * 1000);	// mV

			// エラーチェック
			if(Math.abs(tmp) > 250){
				Log.e( TAG,"adc input calibration voltage is overflow : "+Integer.toString(tmp));
				calibration = false;
				Toast.makeText(this, "キャリブレーションエラー　キャリブレーション終了", Toast.LENGTH_SHORT).show();
				return;
			};

			lowdiv_input_calib_voltage = tmp;

			String p = "+";
			if(lowdiv_input_calib_voltage < 0){
				p = "";
			}
			Toast.makeText(this, "Low div入力電圧幅誤差取得: "+p+Integer.toString(lowdiv_input_calib_voltage)+"mV", Toast.LENGTH_SHORT).show();

			if(D)Log.d( TAG,"adc input calibration voltage = "+Integer.toString(lowdiv_input_calib_voltage)+"mV"+"\n 校正器切断待機" );

			voltscale = VOLTSCALE_5V;
			setVoltscale(true);


		} else if(calibrateReceivedCount == (exe++)){		// 校正器接続検知
			//
			//	High div キャリブレーション値取得
			//

	    	// ADC最大入力電圧幅誤差計測（最大１０％±）
	    	double real_vpp10v_val = (double)(getDC(samples) - gndValue);
	    	double imagine_vpp10v_val = TYPICAL_FULLSCALE_MAX_VAL * TYPICAL_AMP_ERROR[0] * (HIGHDIV_CALIBRATION_VOLTAGE / (5.0*8)); 	// ５V/divのアンプエラーを考慮
			int tmp = (int)((( imagine_vpp10v_val/ real_vpp10v_val) - 1.0) * TYPICAL_ADC_INPUT_MAX_VOLTAGE_PP * 1000);	// mV

			// エラーチェック
			if(Math.abs(tmp) > 250){
				Log.e( TAG,"adc input calibration voltage is overflow : "+Integer.toString(tmp));
				calibration = false;
				Toast.makeText(this, "キャリブレーションエラー　キャリブレーション終了", Toast.LENGTH_SHORT).show();
				return;
			};

			highdiv_input_calib_voltage = tmp;

			String p = "+";
			if(highdiv_input_calib_voltage < 0){
				p = "";
			}
			Toast.makeText(this, "High div入力電圧幅誤差取得: "+p+Integer.toString(highdiv_input_calib_voltage)+"mV", Toast.LENGTH_SHORT).show();

			if(D)Log.d( TAG,"High div input calibration voltage = "+Integer.toString(highdiv_input_calib_voltage)+"mV"+"\n 校正器切断待機" );

			triggerSlopeUp = false;	// down trigger
			setTriggerSlope();


		}  else if (calibrateReceivedCount == (exe++)){	// 校正器切断検知
			//
			//	オペアンプ入力オフセット電圧取得
			//
			// OPA4354の入力オフセット電圧 Typ ±2mV Max ±8mV これが１０倍になるので　最大±80mV
			voltscale = 3;		// 500mV
			setVoltscale(true);
			triggerMode = TGMODE_FREE;
			setTriggerMode();
		} else if (calibrateReceivedCount == (exe++)){
			//何もしない
		} else if (calibrateReceivedCount == (exe++)){
			int tmp = getDC(samples) - gndValue;

			if(Math.abs(tmp) > 500){
				if(D)Log.e( TAG,"Opamp high offset voltage is overflow : "+Integer.toString(tmp));
				calibration = false;
				Toast.makeText(this, "キャリブレーションエラー　キャリブレーション終了", Toast.LENGTH_SHORT).show();
				return;
			};

			highdiv_opa_diff = tmp;

			Toast.makeText(this, "オペアンプHIGH入力オフセット値: "+Integer.toString(highdiv_opa_diff), Toast.LENGTH_SHORT).show();

			voltscale = VOLTSCALE_20MV;
			setVoltscale(true);
		} else if (calibrateReceivedCount == (exe++)){
			//何もしない
		} else if (calibrateReceivedCount == (exe++)){
			int tmp = getDC(samples) - gndValue200mV;

			if(Math.abs(tmp) > 500){
				if(D)Log.e( TAG,"Opamp low offset voltage is overflow : "+Integer.toString(tmp));
				calibration = false;
				Toast.makeText(this, "キャリブレーションエラー　キャリブレーション終了", Toast.LENGTH_SHORT).show();
				return;
			};

			lowdiv_opa_diff = tmp;

			Toast.makeText(this, "オペアンプLOW入力オフセット値: "+Integer.toString(lowdiv_opa_diff), Toast.LENGTH_SHORT).show();
			setupBias();
			calibration = false;
			Toast.makeText(this, "キャリブレーション終了", Toast.LENGTH_SHORT).show();

			// EEPROM 書き込み
			byte[] buffer = new byte[EpOutPacketSize];
			int i=0;
			buffer[i++] = MESSAGE_EEPROM_PAGE_WRITE;
			buffer[i++] = 0;		// EEPROM Write page number
			buffer[i++] = int_to_byte(highdiv_input_calib_voltage,1);
			buffer[i++] = int_to_byte(highdiv_input_calib_voltage,0);
			buffer[i++] = int_to_byte(lowdiv_input_calib_voltage,1);
			buffer[i++] = int_to_byte(lowdiv_input_calib_voltage,0);
			buffer[i++] = int_to_byte(highdiv_opa_diff, 1);
			buffer[i++] = int_to_byte(highdiv_opa_diff, 0);
			buffer[i++] = int_to_byte(lowdiv_opa_diff, 1);
			buffer[i++] = int_to_byte(lowdiv_opa_diff, 0);

			deviceConnection.bulkTransfer(epw_Msg, buffer, buffer.length, 250);

			biasCalibState = -1;
			//biasCalib(null);	// バイアスキャリブレーション

//			voltscale = 0;		// 5V
//			setVoltscale(true);
		}

		calibrateReceivedCount++;

		// DAC出力からバイアス出力が落ち着くのは10nFコンデンサで200us程度
	}

	//
	// Bias Calibration
	//
	int biasCalibState = 0;
	int bias_val;
	double adc_vs_bias;
	private void biasCalib(int [] samples){
		if(D)Log.d(TAG, "biasCalibState = "+Integer.toString(biasCalibState));

		if(biasCalibState == -1){		//バイアスキャリブレーション準備

			// 縦軸をバイアスキャリブレーション用にセットアップ　
			// アンプのエラー率を考慮しない
	    	adc_max_voltage = TYPICAL_ADC_INPUT_MAX_VOLTAGE_PP + ((double)highdiv_input_calib_voltage / 1000);
	    	one_lsb_voltage = adc_max_voltage / SAMPLE_MAX;
	    	normal_graph_fullscale = (int) (SAMPLE_MAX * (GRAPH_FULLSCALE_VOLTAGE / adc_max_voltage));
	    	normal_graph_min = (SAMPLE_MAX - normal_graph_fullscale) / 2;

			adc_vs_bias = TYPICAL_BIAS_LSB_VOLTAGE / one_lsb_voltage;		// DAC1LSBの変化対してADCの値がどれだけ変化するか

			voltscale = VOLTSCALE_5V;		// 5V
			sendMessage(MESSAGE_VOLTAGE_SCALE,voltscale);

			timescale = TIMESCALE_2P5MS;		// 2.5ms
			sendMessage(MESSAGE_TIMESCALE,timescale);

			bias_val = (TYPICAL_BIAS_FS_VALUE/2) + TYPICAL_BIAS_BOTTOM;
			sendMessage(MESSAGE_BIAS,bias_val);		// グラフの中心に合わせる
			sendMessage(MESSAGE_RUNMODE,TGMODE_SINGLE_FREE);	//フリーシングル
			biasCalibState = 1;

		} if(biasCalibState <= 2){
			biasCalibState++;	// 最初の２回の受信波形は捨てる
	    	sendMessage(MESSAGE_RUNMODE,TGMODE_SINGLE_FREE);	//フリーシングル

		} else if(biasCalibState == 3){	// グラフ中心に合うバイアス値を探る
			int diff = getDC(samples) - ((normal_graph_fullscale/2) + graph_min);	// グラフ中心と実際の電圧との差
			if(Math.abs(diff) > 2){
				bias_val = bias_val - (int) ((double)diff/adc_vs_bias);
				sendMessage(MESSAGE_BIAS,bias_val);
				sendMessage(MESSAGE_RUNMODE,TGMODE_SINGLE_FREE);	//フリーシングル

			} else {	// ズレが3LSB未満になったら
				bias_center = bias_val;

				Toast.makeText(this, "Bias Center = "+Integer.toString(bias_center), Toast.LENGTH_SHORT).show();
				// バイアスをグラフの上端にセット
				bias_val = TYPICAL_BIAS_BOTTOM + TYPICAL_BIAS_FS_VALUE;
				sendMessage(MESSAGE_BIAS,bias_val);
				sendMessage(MESSAGE_RUNMODE,TGMODE_SINGLE_FREE);	//フリーシングル
				biasCalibState++;
			}

		} else if(biasCalibState == 4){

			// グラフ上端に合うバイアス値を探る
			int diff = getDC(samples) - (normal_graph_fullscale + graph_min);	// グラフ上端と実際の電圧との差
			if(Math.abs(diff) > 2){
				bias_val = bias_val - (int) ((double)diff/adc_vs_bias);
				sendMessage(MESSAGE_BIAS,bias_val);
				sendMessage(MESSAGE_RUNMODE,TGMODE_SINGLE_FREE);	//フリーシングル

			} else {	// ズレが3LSB未満になったら
				normal_bias_upper = bias_val-bias_center;
				Toast.makeText(this, "Bias Upper = "+Integer.toString(normal_bias_upper), Toast.LENGTH_SHORT).show();

				bias_val = TYPICAL_BIAS_BOTTOM;
				sendMessage(MESSAGE_BIAS,bias_val);		// グラフの下端に合わせる
				sendMessage(MESSAGE_RUNMODE,TGMODE_SINGLE_FREE);	//フリーシングル
				biasCalibState++;
			}

		} else if(biasCalibState == 5) {	// グラフ下端に合うバイアス値を探る
			int diff = getDC(samples) - graph_min;	// グラフ下端と実際の電圧との差
			if(Math.abs(diff) > 2){
				bias_val = bias_val - (int) ((double)diff/adc_vs_bias);
				sendMessage(MESSAGE_BIAS,bias_val);
				sendMessage(MESSAGE_RUNMODE,TGMODE_SINGLE_FREE);	//フリーシングル

			} else {	// ズレが3LSB未満になったら
				normal_bias_lower = bias_center - bias_val;
				Toast.makeText(this, "Bias Lower = "+Integer.toString(normal_bias_lower), Toast.LENGTH_SHORT).show();

				// EEPROM 書き込み	16bitビッグエンディアンで保存
				byte[] buffer = new byte[EpOutPacketSize];
				buffer[0] = MESSAGE_EEPROM_PAGE_WRITE;
				buffer[1] = 8;		// EEPROM Write page number
				buffer[2] = int_to_byte(normal_bias_upper,1);
				buffer[3] = int_to_byte(normal_bias_upper,0);
				buffer[4] = int_to_byte(bias_center,1);
				buffer[5] = int_to_byte(bias_center,0);
				buffer[6] = int_to_byte(normal_bias_lower,1);
				buffer[7] = int_to_byte(normal_bias_lower,0);
				deviceConnection.bulkTransfer(epw_Msg, buffer, buffer.length, 250);

				biasCalibState = 0;
				setupVerticalAxis();
				setupBias();
				sendMessage(MESSAGE_RUNMODE,TGMODE_FREE);	//フリー
				g_bias_pos = 0.5;
				setBias(true);
			}
		}


	}




	//
	// Auto Range
	//
	// １.サンプル値最大値最小値チェック
	// サンプル値上下両方が飽和　→　終了
	// サンプル値上側が飽和　→　グラフの下端に波形の最低値を合わせるようにバイアスを合わせる　２へ
	// サンプル値下側が飽和　→　グラフの上端に波形の最大値を合わせるようにバイアスを合わせる　２へ
	// ２．VPPがグラフに収まるかチェック　収まらないなら終了　収まるなら３へ
	// ３．グラフの真ん中に波形が来るようにバイアスを合わせる
	// ４.VPPが3/div以上になる最大の電圧レンジを選択する
	// ５.波形３つ以上が収まる最大の水平レンジを選択する
	int autoSetState = 0;
	public void autoSet(){

		Log.d(TAG,"Autoset state = " + Integer.toString(autoSetState));

		if(autoSetState <= 2){
			autoSetState++;	// 最初の２回の受信波形は捨てる
	    	sendMessage(MESSAGE_RUNMODE,TGMODE_SINGLE_FREE);	//フリーシングル

		}else if(autoSetState == 3 || autoSetState == 5){	// 波形をグラフの真ん中に持ってくる
			if(wave.vpp > 1){	// 波形はグラフに収まらないなら終了
				autoSetState = 0;
				return;
			} else if(wave.min < 0 && wave.max < 1){ // 下が飽和
				// 波形の最大値がグラフの一番上に来るようなバイアス値
				g_bias_pos = g_bias_pos + 1.0 - wave.max;
			} else if(wave.min > 0 && wave.max > 1){ // 上が飽和
				// 波形の最小値がグラフの一番下に来るようなバイアス値
				g_bias_pos = g_bias_pos - wave.min;
			} else {		// 飽和なし
				// 波形をグラフの中心に
				g_bias_pos = g_bias_pos - ((wave.min + (wave.vpp / 2)) - 0.5);
				//g_bias_pos = g_bias_pos - (g_bias_pos % 0.125);	// divの区切りに合わせる
				autoSetState++;
			}

			if(g_bias_pos > g_bias_pos_max) {		// バイアス出力最大値を超える場合
				g_bias_pos = g_bias_pos_max;
				autoSetState = 0;
			} else if(g_bias_pos < g_bias_pos_min) {		// バイアス出力最小値を下回る場合
				g_bias_pos = g_bias_pos_min;
				autoSetState = 0;
			}

			setBias(true);
	    	sendMessage(MESSAGE_RUNMODE,TGMODE_SINGLE_FREE);	//フリーシングル
		}
		else if(autoSetState == 4){		// 5V/div からレンジを小さくしていく

			//　どちらかが飽和しているならバイアスを調節
			if(wave.min < 0 || wave.max > 1){
				autoSetState = 3;
				autoSet();
				return;
			}

			// 2.5div 以上になる最大の電圧レンジを選択する
			if(wave.vpp < (2.5/8.0)){
				voltscale++;
				if(voltscale >= (NUM_VOLTSCALE-1)){
					autoSetState = 0;
				}
				setVoltscale(true);
			} else {
				autoSetState++;
				//autoSetState = 0;
			}
			sendMessage(MESSAGE_RUNMODE,TGMODE_SINGLE_FREE);	//フリーシングル

		}else if(autoSetState == 6){	// 水平レンジ選択
			if(wave.num_waves < 2){

				timescale++;
				setTimescale();

				if(timescale > 20){
					autoSetState = 0;
				}
			} else if (wave.num_waves < 5) {
				autoSetState = 0;
			} else {

				timescale--;
				setTimescale();

				if(timescale == 0){
					autoSetState = 0;
				}
			}
			sendMessage(MESSAGE_RUNMODE,TGMODE_SINGLE_FREE);	//フリーシングル
		}

		//
		// 終了処理
		//
		if(autoSetState == 0){

			g_hpos = 0;
			setHTrigger();
			g_vpos = 0.5;
			setVTrigger(true);
			triggerMode = TGMODE_AUTO;
			setTriggerMode();
		}

	}


	//背景を点滅させる
    TextView blinkText;
    boolean occupied = false;
    private void blinkTextBG(TextView t,int c){
    	if(occupied==true){
    		return;
    	}

    	blinkText = t;
    	blinkText.setBackgroundColor(c);
    	occupied = true;
		new Timer().schedule(new TimerTask(){
			public void run(){
				mHandler.post(new Runnable(){
					public void run(){
						blinkText.setBackgroundColor(Color.BLACK);
						occupied = false;
					}
				});
			}
		}, 50L);
    }


	//
	// マルチタッチジェスチャーの処理
	//
	boolean pinchDetect = false;
	private double tmp_hpos,tmp_vpos,tmp_bias_pos;

	private class GestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
		float beginSpan;
		boolean axisIsX;

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            Log.d(TAG, "onScaleBegin : "+ detector.getCurrentSpan());
            beginSpan = detector.getCurrentSpan();
            pinchDetect = true;
            mGraph.unDrawLine();

            // 値を元に戻す
            if(g_hpos != tmp_hpos){
            	g_hpos = tmp_hpos;
            	setHTrigger();
            }

            if(g_vpos != tmp_vpos){
            	g_vpos = tmp_vpos;
            	setVTrigger(true);
            }

            if(g_bias_pos != tmp_bias_pos){
            	g_bias_pos = tmp_bias_pos;
            	setBias(true);
            }

            // 軸を決定する
            if(detector.getCurrentSpanX() > detector.getCurrentSpanY()){
            	axisIsX = true;
            } else {
            	axisIsX = false;
            }
            return super.onScaleBegin(detector);
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            Log.d(TAG, "onScaleEnd : "+ detector.getCurrentSpan());

            float endSpan = detector.getCurrentSpan();

            if(beginSpan > endSpan){	// ピンチイン
            	if(axisIsX){	// 水平方向
            		td_unzoom_btn.performClick();	// Time +
            	} else {		// 垂直方向
            		if(D) Log.d(TAG, "onScaleEnd :Detect pinch in axis vertical");
            		vs_plus_btn.performClick();		// Volt +
            	}
            } else {					// ピンチアウト
            	if(axisIsX){	// 水平方向
            		td_zoom_btn.performClick();		// Time -
            	} else {		// 垂直方向
	            	if(D) Log.d(TAG, "onScaleEnd :Detect Pinch out");
	            	vs_minus_btn.performClick();	// Volt -
            	}
            }

            super.onScaleEnd(detector);
        }
	}

    //
    // グラフを触ったら
    //
    private int[] GraphPosX;
    private int[] GraphPosY;
    private int XRightHexPos;	// 一番右のマス目の境界線の座標
    private int XLeftHexPos;	// 一番右のマス目の境界線の座標
    private int downPos = 0;
    private final int DOWN_LEFT = 1;
    private final int DOWN_CENTER = 2;
    private final int DOWN_RIGHT = 3;
    @Override
    public boolean onTouchEvent(MotionEvent e) {
    	gestureDetector.onTouchEvent(e);

    	// 1回だけ
    	if(GraphPosX == null){
    		GraphPosX = new int[2];
    		GraphPosY = new int[2];

	        // 0にグラフの左上の角座標、1にグラフ右下の角の座標
	        mGraph.getLocationInWindow(GraphPosX);
	        GraphPosY[0] = GraphPosX[1];
	        GraphPosX[1] = GraphPosX[0]+mGraph.getWidth();
	        GraphPosY[1] = GraphPosY[0]+mGraph.getHeight();

	        XRightHexPos = (int)(GraphPosX[0]+(GraphPosX[1]-GraphPosX[0])*(8.5/10.0));	//右側から1.5div分
	        XLeftHexPos = (int)(GraphPosX[0]+(GraphPosX[1]-GraphPosX[0])*(1.5/10.0));	//左側から1.5div分
    	}

    	final float x = e.getX();
    	final float y = e.getY();


    	// グラフの中かどうか
    	if((x>GraphPosX[0]) && (x<GraphPosX[1]) && (y>GraphPosY[0]) && (y<GraphPosY[1])){

            switch (e.getAction()) {
		        case MotionEvent.ACTION_DOWN:
		            Log.d("TouchEvent", "getAction()" + "ACTION_DOWN");
		            if(x<XLeftHexPos){
		            	downPos = DOWN_LEFT;
		            } else if(XRightHexPos < x){
		            	downPos = DOWN_RIGHT;
		            } else {
		            	downPos = DOWN_CENTER;
		            }

		            // ピンチ操作の時に元に戻す用
		            tmp_bias_pos = g_bias_pos;
		            tmp_hpos = g_hpos;
		            tmp_vpos = g_vpos;

		        break;

		        case MotionEvent.ACTION_UP:
		            Log.d("TouchEvent", "getAction()" + "ACTION_UP");
		            pinchDetect = false;
		            mGraph.unDrawLine();
		            mGraph.invalidate();
			    return true;			// return

		        case MotionEvent.ACTION_MOVE:
		            Log.d("TouchEvent", "getAction()" + "ACTION_MOVE");
		        break;

		        case MotionEvent.ACTION_CANCEL:
		            Log.d("TouchEvent", "getAction()" + "ACTION_CANCEL");
		        break;
            }

            if(pinchDetect)return true;
            if(downPos == DOWN_CENTER){	// 最初に触ったのが中央
                //
        		// 水平トリガー変更
                //
            	g_hpos = (((x-GraphPosX[0]) / (GraphPosX[1]-GraphPosX[0])) - 0.5) * 2.0;

	    		if(expand){
	    			g_hpos = g_hpos * HT_EXPAND_RATIO;
	    		}

	    		setHTrigger();
	    		mGraph.drawHorizontalLine();

	    	} else if (downPos == DOWN_RIGHT) {
	    		//
	    		//垂直トリガー変更
	    		//
	    		g_vpos = (-(y-GraphPosY[1])) / (GraphPosY[1]-GraphPosY[0]);

	    		if(expand){
	    			g_vpos += (g_vpos - 0.5) * (VT_EXPAND_RATIO-1.0);

		    		if(g_vpos > g_vpos_max){		// 制限
		    			g_vpos = g_vpos_max;
		    		} else if(g_vpos < g_vpos_min){
		    			g_vpos = g_vpos_min;
		    		}
	    		}

	    		setVTrigger(true);
	    		mGraph.drawVerticalLine();

	    	} else {
	    		//
	    		// DCバイアス変更
	    		//
	    		double vp_bias_abs = g_vpos - g_bias_pos;	// vpos と bias_posの差
	    		g_bias_pos = (-(y-GraphPosY[1])) / (GraphPosY[1]-GraphPosY[0]);

	    		if(expand){
	    			g_bias_pos += (g_bias_pos - 0.5) * (BIAS_EXPAND_RATIO-1.0);

		    		if(g_bias_pos > g_bias_pos_max){		// 制限
		    			g_bias_pos = g_bias_pos_max;
		    		} else if(g_bias_pos < g_bias_pos_min){
		    			g_bias_pos = g_bias_pos_min;
		    		}
	    		}

	    		g_vpos = vp_bias_abs + g_bias_pos;	// biasの動きにvtriggerを追従させる

	    		setVTrigger(true);
	    		setBias(true);

	    		mGraph.drawBiasLine();

	    	}

    		mGraph.invalidate();	// 画面更新
    	}
    	return true;
    }


    @Override
    public void onClick(View v) {

    	if(autoSetState > 0)return;		// Autoset中の操作は無効

    	int id = v.getId();

    	Log.d(TAG, "onClick");

    	switch(id){
    		case R.id.tbtn_stop:
    			RunStopToggle();

        		if(run_status == true){		// STOP -> RUN
        			setTriggerMode();
        		} else {					// RUN -> STOP
        			if(autoModeNormal && autoModeTask!=null){
        				autoModeTask.endAutoModeNormal();
        			}

        			sendMessage(MESSAGE_RUNMODE,TGMODE_DEVICE_STOP);
        		}
        	break;

    		case R.id.tbtn_dccut:
        		if(tbtn_dccut.isChecked()){
        			sendMessage(MESSAGE_DCCUT,1);
        			dc_cut = true;
        		}else{
        			sendMessage(MESSAGE_DCCUT,0);
        			dc_cut = false;
        		}
    		break;

    		case R.id.btn_edge:		// トリガーエッジ変更
    			triggerSlopeUp = !triggerSlopeUp;
    			setTriggerSlope();
    		break;

    		case R.id.btn_calib:		// キャリブレーション

    			// キャリブレーションセットアップ
    			calibration = !calibration;
    			if(calibration == true){
    				calibrateReceivedCount = 0;
    				calibrate(null);
    			}
    		break;

    		case R.id.btn_vscale_plus:
    			if(0<voltscale){
        			voltscale--;
        			setVoltscale(true);
        			blinkTextBG(VoltscaleText,Color.GRAY);

        		} else {
        			blinkTextBG(VoltscaleText,Color.RED);
        			vibrator.vibrate(50);	// 振動
        		}
			break;

    		case R.id.btn_vscale_minus:

        		if((NUM_VOLTSCALE-1)>voltscale){
        			voltscale++;
        			setVoltscale(true);
        			blinkTextBG(VoltscaleText,Color.GRAY);
        		} else {
        			blinkTextBG(VoltscaleText,Color.RED);
        			vibrator.vibrate(50);	// 振動
        		}
    		break;


    		case R.id.btn_zoomplus:
    			if(0<timescale){
        			timescale--;
        			setTimescale();
        		} else {
        			blinkTextBG(TimescaleText,Color.RED);
        			vibrator.vibrate(50);	// 振動
        		}

    		break;

    		case R.id.btn_zoomminus:
        		if((NUM_TIMESCALE-1)>timescale){
        			timescale++;
        			setTimescale();
        		} else {
        			blinkTextBG(TimescaleText,Color.RED);
        			vibrator.vibrate(50);	// 振動
        		}
    		break;


    		case R.id.trig_mode_button:		// トリガーモード変更

        		final String[] items = {"AUTO", "NORMAL", "FREERUN" , "SINGLE SHOT"};
        		new AlertDialog.Builder(this)
        		        .setTitle("Select trigger mode")
        		        .setItems(items, new DialogInterface.OnClickListener() {
        		            @Override
        		            public void onClick(DialogInterface dialog, int which) {
        		                // item_which pressed
        		            	triggerMode = which;
        		            	setTriggerMode();
        		            }
        		        })
        		        .show();
    		break;


    		case R.id.tbtn_expand:			// 水平トリガー　バイアス　移動量拡張ボタン

    			if(expand == false){
    				expand = true;
    			} else {	// == true
    				expand = false;
    			}
    		break;


    		case R.id.tbtn_x10:
    			x10Mode = !x10Mode;
    			if(x10Mode){
    				proveRatio = 10.0;
    			} else {
    				proveRatio = 1.0;
    			}
    			setVoltscale(false);
    			setBias(false);
    		break;


    		case R.id.btn_setzero:
    			g_bias_pos = 0.5;
    			g_vpos = 0.5;
    			g_hpos = 0;

    			setHTrigger();
    			setVTrigger(true);
    			setBias(true);
    		break;


    		//
    		//	オートセット
    		//
    		case R.id.btn_autoset:
    	    	// オートセット準備
    			triggerMode = TGMODE_SINGLE_FREE;
    			if(autoModeNormal && autoModeTask!=null){
    				autoModeTask.endAutoModeNormal();
    			}

    			sendMessage(MESSAGE_RUNMODE,TGMODE_DEVICE_STOP);	//とりあえずストップ
    	    	voltscale = 1;  			// V/div = 2V
    	    	setVoltscale(true);
    	    	g_bias_pos = 0.5;			// バイアスを中心に
    	    	setBias(true);

    	    	timescale = 17;	// 10ms

    	    	run_status = true;
    	    	setTimescale();
    			autoSetState = 1;

    			setTriggerMode();	//フリーシングル
    		break;


    		case R.id.btn_bias_calib:

    			biasCalibState = -1;
    			biasCalib(null);
    		break;


    		case R.id.btn_calib_value_reset:

		    	highdiv_input_calib_voltage = 0;
		    	lowdiv_input_calib_voltage = 0;
		    	highdiv_opa_diff = 0;
		    	lowdiv_opa_diff = 0;
		    	normal_bias_upper = TYPICAL_BIAS_FS_VALUE/2;
		    	normal_bias_lower = TYPICAL_BIAS_FS_VALUE/2;
		    	bias_center = TYPICAL_BIAS_CENTER;
    			setupVerticalAxis();
    		break;

    		default:


    	}
    }


    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.i(TAG, "++ ON START ++");
    }

    private boolean isGraphInit = false;
    @Override
	public void onWindowFocusChanged (boolean hasWindowFocus){
    	if(D) Log.i(TAG, "+ ON WINDOW FOCUS CHANGED +");

    	if(isGraphInit)return;		// 一度だけ実行

    	//
    	//	Graphの大きさ調節
    	//

    	isGraphInit = true;
    	Point point = new Point(0, 0);
    	Display display = this.getWindowManager().getDefaultDisplay();

    	// 画面のユーザー領域範囲取得
    	 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
    		getWindowManager().getDefaultDisplay().getRealSize(point);
    	 }
    	 else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {

	    	try{
	    		// ユーザー領域取得
		        Method getRawWidth = Display.class.getMethod("getRawWidth");
		        Method getRawHeight = Display.class.getMethod("getRawHeight");
		        int width = (Integer) getRawWidth.invoke(display);
		        int height = (Integer) getRawHeight.invoke(display);
		        Log.d(TAG, "width = "+ Integer.toString(width));

		        point.set(width, height);
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}
    	}


    	int buttonWidth = trig_mode_button.getWidth();				// ボタンの幅取得
    	int graphHeight = mGraph.getHeight();
    	int smallDisplayWidth = (int)((double)point.x - (double)buttonWidth*2.0);				// 画面が小さい端末用の幅
    	int bigDisplayWidth = (int) (graphHeight*(5.0/4.0));			// 画面が大きい端末用の幅

    	int w,h;
    	if(bigDisplayWidth > smallDisplayWidth){	// 小さい端末
    		w = smallDisplayWidth;
    		h = (int) (smallDisplayWidth*(4.0/5.0));
    	} else {									// 大きい端末
    		w = bigDisplayWidth;
    		h = graphHeight;
    	}
    	mGraph.init(w, h);

		setHTrigger();
		setVTrigger(false);
		setBias(false);

		mGraph.invalidate();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.i(TAG, "+ ON RESUME +");
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.i(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.i(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(D) Log.i(TAG, "--- ON DESTROY ---");

        editor = sharedData.edit();
        editor.putInt(KEY_TRIGGER_MODE, triggerMode);
        editor.putBoolean(KEY_TRIGGER_SLOPE, triggerSlopeUp);
        editor.putInt(KEY_VSCALE, voltscale);
        editor.putInt(KEY_HSCALE, timescale);
        editor.putFloat(KEY_VPOS, (float)g_vpos);
        editor.putFloat(KEY_HPOS, (float)g_hpos);
        editor.putFloat(KEY_BIAS_POS,(float)g_bias_pos);
        editor.putBoolean(KEY_DCCUT, dc_cut);
        editor.putBoolean(KEY_X10, x10Mode);
        editor.putBoolean(KEY_EXPAND, expand);

        editor.apply();

        unregisterReceiver(mUsbReceiver);
    }


}
