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
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
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
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
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


public class USBOscilloscopeHost extends Activity implements OnClickListener, OnLongClickListener {
    // Debugging
    private boolean D = false;		// Release
    //private boolean D = true;            // Debug Mode

    private static final String TAG = "USBOscilloscopeHost";
    private static final String ACTION_USB_PERMISSION = "com.google.android.HID.action.USB_PERMISSION";

    // decimal
    private static final int USB_DEVICE_VID = 8137;        // NXP Vendor ID 0x1FC9
    private static final int USB_DEVICE_PID = 33128;    // DroidOscillo's PID 0x8168

    //
    // USB Oscilloscope Configuration Message
    //
    public static final int MESSAGE_DATA_REQUEST = 1;
    public static final int MESSAGE_DATA_RECEIVED = 2;
    public static final int MESSAGE_H_TRIGGER = 3;    // change horizontal trigger value
    public static final int MESSAGE_V_TRIGGER = 4;
    public static final int MESSAGE_BIAS = 5;
    public static final int MESSAGE_TIMESCALE = 6;
    public static final int MESSAGE_VOLTAGE_SCALE = 7;
    public static final int MESSAGE_TRIGGER_MODE = 8;
    public static final int MESSAGE_RUNMODE = 9;
    public static final int MESSAGE_DCCUT = 10;
    public static final int MESSAGE_TRIGGER_EDGE = 11;
    public static final int MESSAGE_DEVICE_INIT = 255;
    public static final int MESSAGE_EEPROM_PAGE_WRITE = 12;
    public static final int MESSAGE_EEPROM_PAGE_READ = 13;


    public static final int VOLTSCALE_5V = 0;
    public static final int VOLTSCALE_2V = 1;
    public static final int VOLTSCALE_1V = 2;
    public static final int VOLTSCALE_500MV = 3;
    public static final int VOLTSCALE_200MV = 4;
    public static final int VOLTSCALE_100MV = 5;
    public static final int VOLTSCALE_50MV = 6;
    public static final int VOLTSCALE_20MV = 7;
    public static final int VOLTSCALE_10MV = 8;
    public static final int VOLTSCALE_5MV = 9;

    public static final int TIMESCALE_1S = 23;
    public static final int TIMESCALE_500MS = 22;
    public static final int TIMESCALE_250MS = 21;
    public static final int TIMESCALE_100MS = 20;
    public static final int TIMESCALE_50MS = 19;
    public static final int TIMESCALE_25MS = 18;
    public static final int TIMESCALE_10MS = 17;
    public static final int TIMESCALE_5MS = 16;
    public static final int TIMESCALE_2P5MS = 15;
    public static final int TIMESCALE_1MS = 14;
    public static final int TIMESCALE_500US = 13;
    public static final int TIMESCALE_250US = 12;
    public static final int TIMESCALE_100US = 11;
    public static final int TIMESCALE_50US = 10;
    public static final int TIMESCALE_25US = 9;
    public static final int TIMESCALE_10US = 8;
    public static final int TIMESCALE_5US = 7;
    public static final int TIMESCALE_2P5US = 6;
    public static final int TIMESCALE_1US = 5;
    public static final int TIMESCALE_500NS = 4;
    public static final int TIMESCALE_250NS = 3;
    public static final int TIMESCALE_100NS = 2;
    public static final int TIMESCALE_50NS = 1;
    public static final int TIMESCALE_25NS = 0;

    public static final double HIGHDIV_CALIBRATION_VOLTAGE = 10.0;
    public static final double LOWDIV_CALIBRATION_VOLTAGE = 0.717;


    public static final int NUM_VOLTSCALE = 10;
    public static final int TGMODE_AUTO = 0;
    public static final int TGMODE_NORMAL = 1;
    public static final int TGMODE_FREE = 2;
    public static final int TGMODE_SINGLE = 3;
    public static final int TGMODE_SINGLE_FREE = 4;        // for Autoset mode

    public static final int TGMODE_DEVICE_STOP = 0;
    public static final int SAMPLE_MAX = 4095;

    public static final int READ_BUFFER_SIZE = 1600;
    public static final int DEFAULT_SAMPLE_LENGTH = 800;
    public static final int DEFAULT_DIV_LENGTH = DEFAULT_SAMPLE_LENGTH / 10;

    public static final byte NUM_TIMESCALE = 24;

    public static final int M4_FIRM_WRITEADDR = 0x10000000;
    public static final int M4_FIRM_ENTRYADDR = 0x1000013c;
    public static final int M0APP_FIRM_WRITEADDR = 0x10010000;
    public static final int M0APP_FIRM_ENTRYADDR = 0x100100c0;    //Check LPCXpresso MAPfile

    //
    //	Vertical axis
    //
    public static final double ADC_VREF_VOLTAGE = 0.506;
    public static final double GRAPH_FULLSCALE_VOLTAGE = 0.72;        // The voltage of a range displayed by a screen
    public static final double TYPICAL_ADC_INPUT_MAX_VOLTAGE_PP = 0.8;
    public static final double ADC_PER_DIV = GRAPH_FULLSCALE_VOLTAGE / 8.0;        // Voltage per 1div
    public static final int TYPICAL_FULLSCALE_MAX_VAL = (int) (SAMPLE_MAX * (GRAPH_FULLSCALE_VOLTAGE / TYPICAL_ADC_INPUT_MAX_VOLTAGE_PP));

    public static final double TYPICAL_ONE_LSB_VOLTAGE = TYPICAL_ADC_INPUT_MAX_VOLTAGE_PP / SAMPLE_MAX;    // 0.195mV
    public static final double ADC_MAX_VOLTAGE_DROP_AT_80MSPS = 0.99;        // calibration value for 80Msps input impedance down
    public static final double OPAMP_FEEDBACK_FIX_RATIO = 0.975;            // ADC input range calibration value except 5V,200mV/div

    private int highdiv_input_calib_voltage, lowdiv_input_calib_voltage;    // 1LSB = 1mV
    private double adc_max_voltage = TYPICAL_ADC_INPUT_MAX_VOLTAGE_PP;
    private double one_lsb_voltage = TYPICAL_ADC_INPUT_MAX_VOLTAGE_PP / SAMPLE_MAX;
    private int normal_graph_fullscale = TYPICAL_FULLSCALE_MAX_VAL;
    private int normal_graph_min = (SAMPLE_MAX - TYPICAL_FULLSCALE_MAX_VAL) / 2;
    // Voltscale 8 full scale value
    public int vs8_graph_fullscale = (int) (SAMPLE_MAX * (GRAPH_FULLSCALE_VOLTAGE / TYPICAL_ADC_INPUT_MAX_VOLTAGE_PP) * 0.5);
    // Voltscale 9 full scale value
    public int vs9_graph_fullscale = (int) (SAMPLE_MAX * (GRAPH_FULLSCALE_VOLTAGE / TYPICAL_ADC_INPUT_MAX_VOLTAGE_PP) * 0.25);
    private double g_vpos_max, g_vpos_min;
    //private double opamp_feedback_fix_ratio = 1.0;

    //
    // BIAS
    //
    public static final double BIAS_FULLSCALE_VOLTAGE = 1.235 * (4.1 / 5.1) * 2.0;
    public static final int BIAS_MAX_REG_VALUE = 4095;
    public static final double BIAS_UPPER_VOLTAGE = 1.5;
    public static final double BIAS_LOWER_VOLTAGE = -0.484;
    public static final double BIAS_CENTER_VOLTAGE = (BIAS_FULLSCALE_VOLTAGE / 2.0) + BIAS_LOWER_VOLTAGE;
    public static final double TYPICAL_BIAS_LSB_VOLTAGE = BIAS_FULLSCALE_VOLTAGE / BIAS_MAX_REG_VALUE;    // maybe 0.5mV
    // DAC register value difference of ADC Vref voltage center
    public static final int BIAS_NORMAL_OFFSET_VALUE = (int) ((ADC_VREF_VOLTAGE - BIAS_CENTER_VOLTAGE) / TYPICAL_BIAS_LSB_VOLTAGE);
    public static final int TYPICAL_BIAS_FS_VALUE = (int) (BIAS_MAX_REG_VALUE * (GRAPH_FULLSCALE_VOLTAGE / BIAS_FULLSCALE_VOLTAGE));
    public static final int TYPICAL_BIAS_BOTTOM = ((BIAS_MAX_REG_VALUE - TYPICAL_BIAS_FS_VALUE) / 2) - BIAS_NORMAL_OFFSET_VALUE;            // 1310
    public static final int TYPICAL_BIAS_CENTER = TYPICAL_BIAS_BOTTOM + TYPICAL_BIAS_FS_VALUE / 2;

    public static final double HT_EXPAND_RATIO = 4.0;
    public static final double VT_EXPAND_RATIO = 2.0;
    public static final double VT_POS_MAX = 1.1;
    public static final double BIAS_EXPAND_RATIO = 2.5;

    private int highdiv_opa_diff, lowdiv_opa_diff;        // DC amp error at change opeamp amplitude  1x -> 10x
    private int[] opamp_offset_calib_value_list = new int[NUM_VOLTSCALE];
    private int bias_center, bias_lower, bias_upper, normal_bias_lower, normal_bias_upper;
    private double g_bias_pos_max, g_bias_pos_min;


    // shared preferences keys

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
    private ImageView img_trigger, img_connect, img_range;
    private GraphView mGraph = null;
    private TextView FrameRateText, triggerModeText, TimescaleText, HPotisionText, VPotisionText;
    private TextView RunStopText, BiasText, VoltscaleText, VppText, FreqText, VrmsText, MeanText;
    private Button trig_mode_button, calibration_btn, edge_btn, btn_test1, btn_test2;
    private Button td_zoom_btn, td_unzoom_btn, btn_volt_zoom, btn_volt_unzoom, btn_setzero;
    private Button btn_autoset, btn_setting,btn_fft;
    private ToggleButton tbtn_dccut, tbtn_stop, tbtn_xp, tbtn_x10;
    private ScaleGestureDetector gestureDetector;
    private GestureListener simpleListener;
    private ScrollView scview;
    private static ListView voltageDivListL;
    private static ListView voltageDivListR;

    static final String[] TRIGGER_MODE_LIST = new String[5];
    static final String[] TIME_SCALE_LIST = new String[NUM_TIMESCALE];
    static final String[] VOLT_SCALE_LIST = new String[NUM_VOLTSCALE];
    static final String[] VOLT_SCALE_LIST_X10 = new String[NUM_VOLTSCALE];

    static final double[] TYPICAL_AMP_ERROR = new double[NUM_VOLTSCALE];    // Typical input circuit dc error ratio
    static final double[] TIME_CONVERT_LIST = new double[NUM_TIMESCALE];
    static final double[] FULLSCALE_VOLTAGE_LIST = new double[NUM_VOLTSCALE];

    private UsbManager mUsbManager;
    private USBBroadcastReceiver mUsbReceiver;
    UsbReceiveThread ReceiveThread;

    private UsbInterface intf;
    private UsbEndpoint endPointRead;
    private UsbEndpoint epw_Msg;    //for message transmit endpoint
    private UsbEndpoint epw_Firm;    //for firmware write endpoint
    private UsbDeviceConnection deviceConnection;
    private int EpOutPacketSize;
    private int epw_FirmPacketSize;
    private Timer SendTimer;
    private boolean isConnected = false;
    private boolean autoModeNormal = false;
    private AutoModeTimerTask autoModeTask;
    private UsbReceiveTask usbReceiveTask;

    Handler mHandler;
    PendingIntent mPermissionIntent;
    private boolean enableFlashText = true;        // For disable flash text at startup

    Vibrator vibrator;

    // Oscilloscope settings
    private int triggerMode, verTriggerValue;
    private boolean run_status = true, dc_cut, triggerSlopeUp = true, x10Mode, expand;
    private boolean calibration = false;
    private int sampleLength;//,sampleSizeByte;
    private int timescale, voltscale, graph_fullscale, graph_min;

    private double proveRatio = 1.0;
    //g_bias_pos,g_vpos : 0 is the bottom of the graph 0.5, the center 1 is the highest position
    //g_hpos : 0 is the center. -1 is the left edge of the graph. 1 is the right end of the graph
    private double g_bias_pos, g_vpos, g_hpos;
    private GraphWave wave;
    //private boolean long_click_detected = false;


    // Constructor
    public USBOscilloscopeHost() {
        TRIGGER_MODE_LIST[0] = "AUTO";
        TRIGGER_MODE_LIST[1] = "NORMAL";
        TRIGGER_MODE_LIST[2] = "FREE";
        TRIGGER_MODE_LIST[3] = "SINGLE";

        TIME_SCALE_LIST[0] = "25ns";
        TIME_SCALE_LIST[1] = "50ns";
        TIME_SCALE_LIST[2] = "100ns";
        TIME_SCALE_LIST[3] = "250ns";
        TIME_SCALE_LIST[4] = "500ns";    // 80Msps
        TIME_SCALE_LIST[5] = "1μs";        // 80Msps
        TIME_SCALE_LIST[6] = "2.5μs";    // 32Msps
        TIME_SCALE_LIST[7] = "5μs";
        TIME_SCALE_LIST[8] = "10μs";
        TIME_SCALE_LIST[9] = "25μs";
        TIME_SCALE_LIST[10] = "50μs";
        TIME_SCALE_LIST[11] = "100μs";
        TIME_SCALE_LIST[12] = "250μs";
        TIME_SCALE_LIST[13] = "500μs";
        TIME_SCALE_LIST[14] = "1ms";
        TIME_SCALE_LIST[15] = "2.5ms";
        TIME_SCALE_LIST[16] = "5ms";
        TIME_SCALE_LIST[17] = "10ms";
        TIME_SCALE_LIST[18] = "25ms";
        TIME_SCALE_LIST[19] = "50ms";
        TIME_SCALE_LIST[20] = "100ms";
        TIME_SCALE_LIST[21] = "250ms";
        TIME_SCALE_LIST[22] = "500ms";
        TIME_SCALE_LIST[23] = "1s";

        TIME_CONVERT_LIST[0] = 0.000001;
        TIME_CONVERT_LIST[1] = TIME_CONVERT_LIST[0];
        TIME_CONVERT_LIST[2] = TIME_CONVERT_LIST[0];
        TIME_CONVERT_LIST[3] = TIME_CONVERT_LIST[0];    // timescale 0~5 are same samplerate(80Msps)
        TIME_CONVERT_LIST[4] = TIME_CONVERT_LIST[0];
        TIME_CONVERT_LIST[5] = TIME_CONVERT_LIST[0];
        TIME_CONVERT_LIST[6] = 0.0000025;
        TIME_CONVERT_LIST[7] = 0.000005;
        TIME_CONVERT_LIST[8] = 0.00001;
        TIME_CONVERT_LIST[9] = 0.000025;
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

        VOLT_SCALE_LIST[0] = "5V";    // xx.x
        VOLT_SCALE_LIST[1] = "2V";    // xx.x
        VOLT_SCALE_LIST[2] = "1V";    // x.xx
        VOLT_SCALE_LIST[3] = "500mV";// x.xx
        VOLT_SCALE_LIST[4] = "200mV";// x.xx
        VOLT_SCALE_LIST[5] = "100mV";// xxxm
        VOLT_SCALE_LIST[6] = "50mV"; // xxxm
        VOLT_SCALE_LIST[7] = "20mV"; // xxxm
        VOLT_SCALE_LIST[8] = "10mV"; // xx.x m
        VOLT_SCALE_LIST[9] = "5mV";  // xx.x m

        // Error ratio at amplitude circuit
        TYPICAL_AMP_ERROR[0] = 1.0233;    //5V
        TYPICAL_AMP_ERROR[1] = 1.0246;    //2V
        TYPICAL_AMP_ERROR[2] = 0.9906;    //1V
        TYPICAL_AMP_ERROR[3] = 1.0;        //500mV
        TYPICAL_AMP_ERROR[4] = 1.0233;    //200mV
        TYPICAL_AMP_ERROR[5] = 1.0007;    //100mV
        TYPICAL_AMP_ERROR[6] = 1.005;    //50mV
        TYPICAL_AMP_ERROR[7] = 1.0;        //20mV
        TYPICAL_AMP_ERROR[8] = 1.0;        //10mV
        TYPICAL_AMP_ERROR[9] = 1.0;        //5mV


        VOLT_SCALE_LIST_X10[0] = "50V";    // xx.x
        VOLT_SCALE_LIST_X10[1] = "20V";    // xx.x
        VOLT_SCALE_LIST_X10[2] = "10V";    // x.xx
        VOLT_SCALE_LIST_X10[3] = "5V";// x.xx
        VOLT_SCALE_LIST_X10[4] = "2V";// x.xx
        VOLT_SCALE_LIST_X10[5] = "1V";// xxxm
        VOLT_SCALE_LIST_X10[6] = "500mV"; // xxxm
        VOLT_SCALE_LIST_X10[7] = "200mV"; // xxxm
        VOLT_SCALE_LIST_X10[8] = "100mV"; // xx.x m
        VOLT_SCALE_LIST_X10[9] = "50mV";  // xx.x m

        FULLSCALE_VOLTAGE_LIST[0] = 5 * 8;
        FULLSCALE_VOLTAGE_LIST[1] = 2 * 8;
        FULLSCALE_VOLTAGE_LIST[2] = 1 * 8;
        FULLSCALE_VOLTAGE_LIST[3] = 0.5 * 8;
        FULLSCALE_VOLTAGE_LIST[4] = 0.2 * 8;
        FULLSCALE_VOLTAGE_LIST[5] = 0.1 * 8;
        FULLSCALE_VOLTAGE_LIST[6] = 0.05 * 8;
        FULLSCALE_VOLTAGE_LIST[7] = 0.02 * 8;
        FULLSCALE_VOLTAGE_LIST[8] = 0.01 * 8;
        FULLSCALE_VOLTAGE_LIST[9] = 0.005 * 8;
    }


    @SuppressWarnings("unused")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (D) Log.i(TAG, "-- ON CREATE --");
        super.onCreate(savedInstanceState);

        // Load value saved at last exit

        sharedData = getSharedPreferences("DataSave", Context.MODE_PRIVATE);
        editor = sharedData.edit();


        voltscale = sharedData.getInt(KEY_VSCALE, 0);
        timescale = sharedData.getInt(KEY_HSCALE, 10);
        triggerMode = sharedData.getInt(KEY_TRIGGER_MODE, TGMODE_AUTO);
        triggerSlopeUp = sharedData.getBoolean(KEY_TRIGGER_SLOPE, true);
        dc_cut = sharedData.getBoolean(KEY_DCCUT, false);
        x10Mode = sharedData.getBoolean(KEY_X10, false);
        expand = sharedData.getBoolean(KEY_EXPAND, false);
        g_vpos = (double) sharedData.getFloat(KEY_VPOS, 0.5f + (float) (1.0 / 8));
        g_bias_pos = (double) sharedData.getFloat(KEY_BIAS_POS, 0.5f);
        g_hpos = (double) sharedData.getFloat(KEY_HPOS, 0.0f);

        setContentView(R.layout.main);
        mGraph = (GraphView) findViewById(R.id.Graph);

        scview = (ScrollView) findViewById(R.id.scrollView1);

        FrameRateText = (TextView) findViewById(R.id.text_framerate);
        triggerModeText = (TextView) findViewById(R.id.text_trigger_mode);
        TimescaleText = (TextView) findViewById(R.id.text_timescale);
        HPotisionText = (TextView) findViewById(R.id.text_h_trigger);
        VPotisionText = (TextView) findViewById(R.id.textVtrigger);
        RunStopText = (TextView) findViewById(R.id.text_run_stop);
        BiasText = (TextView) findViewById(R.id.text_bias);
        VoltscaleText = (TextView) findViewById(R.id.text_voltscale);
        VppText = (TextView) findViewById(R.id.text_vpp);
        FreqText = (TextView) findViewById(R.id.text_freq);
        VrmsText = (TextView) findViewById(R.id.text_vrms);
        MeanText = (TextView) findViewById(R.id.text_mean);

        img_trigger = (ImageView) findViewById(R.id.img_trigger);
        img_connect = (ImageView) findViewById(R.id.img_connect);
        img_range = (ImageView) findViewById(R.id.img_range);

        btn_fft = (Button) findViewById(R.id.btn_fft);
        btn_fft.setOnClickListener(this);
        td_zoom_btn = (Button) findViewById(R.id.btn_time_zoom);
        td_zoom_btn.setOnClickListener(this);
        td_zoom_btn.setOnLongClickListener(this);
        td_unzoom_btn = (Button) findViewById(R.id.btn_time_unzoom);
        td_unzoom_btn.setOnClickListener(this);
        td_unzoom_btn.setOnLongClickListener(this);
        btn_volt_zoom = (Button) findViewById(R.id.btn_volt_zoom);
        btn_volt_zoom.setOnClickListener(this);
        btn_volt_zoom.setOnLongClickListener(this);
        btn_volt_unzoom = (Button) findViewById(R.id.btn_volt_unzoom);
        btn_volt_unzoom.setOnClickListener(this);
        btn_volt_unzoom.setOnLongClickListener(this);
        edge_btn = (Button) findViewById(R.id.btn_edge);
        edge_btn.setOnClickListener(this);
        btn_setzero = (Button) findViewById(R.id.btn_setzero);
        btn_setzero.setOnClickListener(this);
        btn_autoset = (Button) findViewById(R.id.btn_autoset);
        btn_autoset.setOnClickListener(this);
        btn_fft = (Button) findViewById(R.id.btn_fft);
        btn_fft.setOnClickListener(this);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        // for debug mode
        btn_test1 = (Button) findViewById(R.id.btn_bias_calib);
        btn_test2 = (Button) findViewById(R.id.btn_calib_value_reset);
        btn_setting = (Button) findViewById(R.id.btn_setting);
        calibration_btn = (Button) findViewById(R.id.btn_calib);


        if (D == false) {    // Hide debug button if release build
            btn_test1.setVisibility(View.GONE);
            btn_test2.setVisibility(View.GONE);
            calibration_btn.setVisibility(View.GONE);
            btn_setting.setVisibility(View.GONE);
            btn_fft.setVisibility(View.GONE);
            scview.computeScroll();        // Update UI
        } else {
            btn_test1.setOnClickListener(this);
            btn_test2.setOnClickListener(this);
            btn_setting.setOnClickListener(this);
            calibration_btn.setOnClickListener(this);
        }

        tbtn_dccut = (ToggleButton) findViewById(R.id.tbtn_dccut);
        tbtn_dccut.setOnClickListener(this);
        tbtn_stop = (ToggleButton) findViewById(R.id.tbtn_stop);
        tbtn_stop.setOnClickListener(this);
        tbtn_xp = (ToggleButton) findViewById(R.id.tbtn_expand);
        tbtn_xp.setOnClickListener(this);
        tbtn_x10 = (ToggleButton) findViewById(R.id.tbtn_x10);
        tbtn_x10.setOnClickListener(this);

        trig_mode_button = (Button) findViewById(R.id.trig_mode_button);
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
        if (device != null) {
            mUsbReceiver.onReceive(this, intent);            // When launch from USB device connection intent
        } else {

            // Check if the device is already connected when launching from the drawer
            HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
            Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
            while (deviceIterator.hasNext()) {
                device = deviceIterator.next();
                if (device.getProductId() == USB_DEVICE_PID && device.getVendorId() == USB_DEVICE_VID) {    // VID PID 確認
                    setDevice(device);
                    break;
                }
            }
        }

        if (isConnected == false) {        // If not called setDevice();
            setParameters();
        }

        //Button status set
        tbtn_x10.setChecked(x10Mode);
        if (x10Mode == true) {
            proveRatio = 10.0;
        }
        tbtn_dccut.setChecked(dc_cut);
        tbtn_xp.setChecked(expand);

        // get DP
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float dpHeight = displayMetrics.heightPixels / displayMetrics.density;
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        Log.d("DEBUG", "Display density pixel Width->" + dpWidth + ",Height=>" + dpHeight);


        // Timer to run every second
        new Timer().scheduleAtFixedRate(new TimerTask() {
            public void run() {
                mHandler.post(new Runnable() {
                    public void run() {
                        FrameRateText.setText("FPS:" + Integer.toString(mGraph.frameRateGetAndReset()));
                        if (wave == null) return;

                        VppText.setText("Vpp:" + String.format("%7s", siConverter(wave.vpp * FULLSCALE_VOLTAGE_LIST[voltscale] * proveRatio) + "V"));
                        FreqText.setText("Frq:" + String.format("%5s", siConverter(wave.freq)) + "Hz");
                        VrmsText.setText("Vrms:" + String.format("%5s", siConverter(wave.vrms * FULLSCALE_VOLTAGE_LIST[voltscale] * proveRatio)) + "V");
                        MeanText.setText("Mean:" + String.format("%5s", siConverter(wave.mean * FULLSCALE_VOLTAGE_LIST[voltscale] * proveRatio)) + "V");

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


    private class USBBroadcastReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (D) Log.i(TAG, "USBBroadcastReceiver ON RECEIVE");

            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {        // When the device is connected when the application is running
                if (D) Log.i(TAG, "ACTION_USB_DEVICE_ATTACHED");

                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                if (device == null) {
                    Log.e(TAG, "device pointer is null");
                    return;
                }

                if (device.getProductId() != USB_DEVICE_PID || device.getVendorId() != USB_DEVICE_VID) {    // Confirm VID and PID
                    Log.e(TAG, "incorrect usb device");
                    return;
                }

                setDevice(device);
            }

            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {        // If the device is disconnected while it is running
                Log.i(TAG, "device disconnected");
                if (ReceiveThread != null) {
                    ReceiveThread.EndConnection();
                }

                if(SendTimer != null) {
                    SendTimer.cancel();
                }

                if(autoModeTask!=null) {
                    autoModeTask.cancel();
                }

                if(usbReceiveTask != null) {
                    usbReceiveTask.cancel();
                }

                isConnected = false;
                img_connect.setImageResource(R.drawable.disconnect);
                calibration = false;
                biasCalibState = 0;
                autoSetState = 0;
            }


            // Receive USB connection permeation intent
            if (ACTION_USB_PERMISSION.equals(action)) {
                if (D) Log.i(TAG, "ACTION_USB_PERMISSION");
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            //call method to set up device communication
                            setDevice(device);
                        }
                    } else {
                        if (D) Log.d(TAG, "permission denied for device " + device);
                    }
                }
            }


        }
    }


    // Integer to 1byte
    // digit select: 0~4
    private byte int_to_byte(int i, int digit) {
        return (byte) ((i >> (digit * 8)) & 0xFF);
    }

    // 2byte to half word(16bit)
    private int byte_to_halfword(byte upper, byte lower) {
        return (((int) upper) << 8) | ((int) lower & 0xff);
    }

    public void setDevice(UsbDevice device) {

        if (D) Log.d(TAG, "SET DEVICE");

        enableFlashText = false;        // disable flash text

        if (mUsbManager.hasPermission(device) == false) {    // Check USB connect permeation
            Log.e(TAG, "DEVICE has no parmission!");
			//mUsbManager.requestPermission(device, mPermissionIntent);     // show permission request dialog
            //return;
        }
        ;
        //
        //	device initialize
        //

        deviceConnection = mUsbManager.openDevice(device);
        if (deviceConnection == null) {
            if (D) Log.e(TAG, "Open device failed !");
            return;
        }

        intf = device.getInterface(0);

        if (deviceConnection != null) {
            deviceConnection.claimInterface(intf, true);
        }

        try {
            if (UsbConstants.USB_DIR_IN == intf.getEndpoint(0).getDirection()) {
                endPointRead = intf.getEndpoint(0);
                //EpInPacketSize = endPointRead.getMaxPacketSize();
            }
        } catch (Exception e) {
            if (D) Log.e(TAG, "Device have no endPointRead", e);
        }

        try {
            if (UsbConstants.USB_DIR_OUT == intf.getEndpoint(1).getDirection()) {
                epw_Msg = intf.getEndpoint(1);
                EpOutPacketSize = epw_Msg.getMaxPacketSize();
            }
        } catch (Exception e) {
            if (D) Log.e(TAG, "Device have no epw_Msg", e);
        }

        epw_Firm = intf.getEndpoint(2);
        if (UsbConstants.USB_DIR_OUT != epw_Firm.getDirection()) {
            if (D) Log.e(TAG, "ep2 Direction Incorrect");
        }
        epw_FirmPacketSize = epw_Firm.getMaxPacketSize();

        //
        //		Start oscilloscope setting
        //

        // Change connection icon green
        img_connect.setImageResource(R.drawable.connected);

        // Firmware transmit
        Resources res = getResources();
        if (D) Log.d(TAG, "Send firmware");
        if (D) Log.d(TAG, "M4 firmware transfer start.");
        sendFirmware(M4_FIRM_WRITEADDR, res.openRawResource(R.raw.m4), M4_FIRM_ENTRYADDR);

        if (D) Log.d(TAG, "M0APP firmware transfer start.");
        sendFirmware(M0APP_FIRM_WRITEADDR, res.openRawResource(R.raw.m0app), M0APP_FIRM_ENTRYADDR);

        byte[] ReadBuffer = new byte[endPointRead.getMaxPacketSize()];

        // I2C EEPROM READ
        sendMessage(MESSAGE_EEPROM_PAGE_READ, 16);        //EEPROM sequential read request
        deviceConnection.bulkTransfer(endPointRead, ReadBuffer, 16, 250);    //Receive EEPROM data

        // page 0
        int i = 0;
        highdiv_input_calib_voltage = byte_to_halfword(ReadBuffer[i++], ReadBuffer[i++]);
        lowdiv_input_calib_voltage = byte_to_halfword(ReadBuffer[i++], ReadBuffer[i++]);
        highdiv_opa_diff = byte_to_halfword(ReadBuffer[i++], ReadBuffer[i++]);
        lowdiv_opa_diff = byte_to_halfword(ReadBuffer[i++], ReadBuffer[i++]);


        // page 1
        normal_bias_upper = byte_to_halfword(ReadBuffer[8], ReadBuffer[9]);
        bias_center = byte_to_halfword(ReadBuffer[10], ReadBuffer[11]);
        normal_bias_lower = byte_to_halfword(ReadBuffer[12], ReadBuffer[13]);

        if (D) {
            Toast.makeText(this,
                    "highdiv_input_calib_voltage = " + Integer.toString(highdiv_input_calib_voltage) + "\n" +
                            "lowdiv_input_calib_voltage = " + Integer.toString(lowdiv_input_calib_voltage) + "\n" +
                            "highdiv_opa_diff = " + Integer.toString(highdiv_opa_diff) + "\n" +
                            "lowdiv_opa_diff = " + Integer.toString(lowdiv_opa_diff) + "\n" +
                            "bias_center = " + Integer.toString(bias_center) + "\n" +
                            "bias_upper = " + Integer.toString(normal_bias_upper) + "\n" +
                            "bias_lower = " + Integer.toString(normal_bias_lower) + "\n"
                    , Toast.LENGTH_LONG).show();
        }

        if (bias_center == -1) {
            highdiv_input_calib_voltage = 0;
            lowdiv_input_calib_voltage = 0;
            highdiv_opa_diff = 0;
            lowdiv_opa_diff = 0;
            normal_bias_upper = TYPICAL_BIAS_FS_VALUE / 2;
            normal_bias_lower = TYPICAL_BIAS_FS_VALUE / 2;
            bias_center = TYPICAL_BIAS_CENTER;
        }

        setupVerticalAxis();
        setupBias();

        //
        // Oscilloscope configuration value set
        //
        setParameters();

        if (dc_cut) {
            sendMessage(MESSAGE_DCCUT, 1);
        } else {
            sendMessage(MESSAGE_DCCUT, 0);
        }

        ReceiveThread = new UsbReceiveThread();
        ReceiveThread.start();

//        usbReceiveTask = new UsbReceiveTask();
//        new Timer().scheduleAtFixedRate(usbReceiveTask, 0,6L);


        isConnected = true;

        autoModeTask = new AutoModeTimerTask();
        new Timer().scheduleAtFixedRate(autoModeTask, 0, 10L);    // Process per 10ms

        enableFlashText = true;

    }

    // Oscilloscope configuration
    private void setParameters() {
        setTimescale();        // also call setHTrigger()
        setVoltscale(true);    // also call setBias()
        setTriggerMode();
        setTriggerSlope();
        setVTrigger(true);
    }


    private void sendFirmware(int writeaddr, InputStream bin, int entryaddr) {

        byte buff[] = new byte[epw_FirmPacketSize];
        int file_size = 0;

        try {
            file_size = bin.available();                // get firmware binary file size

            //set write address 1~4byte

            for (int i = 0; i < 4; i++) {
                buff[i] = (byte) (writeaddr >> (i * 8) & 0xff);
            }

            //set firmware binary size 5~8byte
            for (int i = 0; i < 4; i++) {
                buff[4 + i] = (byte) (file_size >> (i * 8) & 0xff);
            }

            //set entry point address 9~12byte
            for (int i = 0; i < 4; i++) {
                buff[8 + i] = (byte) (entryaddr >> (i * 8) & 0xff);
            }

            int IsFinished = bin.read(buff, 12, epw_FirmPacketSize - 12);

            // Packet transmit up to end of file
            while (IsFinished > 0) {
                int ret;
                ret = deviceConnection.bulkTransfer(epw_Firm, buff, epw_FirmPacketSize, 100);
                if (ret < 0) {
                    Log.e(TAG, "Firmware transfer failed!");
                }
                IsFinished = bin.read(buff, 0, epw_FirmPacketSize);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //
    // Read calibration value and setup vertical axis
    //
    private void setupVerticalAxis() {
        if (voltscale < VOLTSCALE_200MV) {
            adc_max_voltage = TYPICAL_ADC_INPUT_MAX_VOLTAGE_PP + ((double) highdiv_input_calib_voltage / 1000);
        } else {
            adc_max_voltage = TYPICAL_ADC_INPUT_MAX_VOLTAGE_PP + ((double) lowdiv_input_calib_voltage / 1000);
        }
        adc_max_voltage = adc_max_voltage / TYPICAL_AMP_ERROR[voltscale];

        if (timescale <= 5) {        // for input impedance down at 80Msps
            adc_max_voltage = adc_max_voltage * ADC_MAX_VOLTAGE_DROP_AT_80MSPS;
        }

        one_lsb_voltage = adc_max_voltage / SAMPLE_MAX;
        normal_graph_fullscale = (int) (SAMPLE_MAX * (GRAPH_FULLSCALE_VOLTAGE / adc_max_voltage));
        normal_graph_min = (SAMPLE_MAX - normal_graph_fullscale) / 2;
        vs8_graph_fullscale = (int) (SAMPLE_MAX * (GRAPH_FULLSCALE_VOLTAGE / adc_max_voltage) * 0.5);
        vs9_graph_fullscale = (int) (SAMPLE_MAX * (GRAPH_FULLSCALE_VOLTAGE / adc_max_voltage) * 0.25);

        g_vpos_max = 1 + (((adc_max_voltage / GRAPH_FULLSCALE_VOLTAGE) - 1) / 2);
        g_vpos_min = -(((adc_max_voltage / GRAPH_FULLSCALE_VOLTAGE) - 1) / 2);

        if (voltscale <= 7) {
            graph_fullscale = normal_graph_fullscale;
            graph_min = normal_graph_min;
        } else if (voltscale == 8) {
            graph_fullscale = vs8_graph_fullscale;
            graph_min = (SAMPLE_MAX - vs8_graph_fullscale) / 2;
        } else {
            graph_fullscale = vs9_graph_fullscale;
            graph_min = (SAMPLE_MAX - vs9_graph_fullscale) / 2;
        }
    }

    //
    //Read calibration value and setup bias(position)
    //
    private void setupBias() {


        g_bias_pos_max = 0.5 + ((BIAS_MAX_REG_VALUE - bias_center) / (double) normal_bias_upper);
        g_bias_pos_min = 0.5 - ((double) (BIAS_MAX_REG_VALUE - bias_center) / (double) normal_bias_lower);

        double bias_one_lsb_vs_adc = (double) normal_graph_fullscale / (double) (normal_bias_upper + normal_bias_lower);
        opamp_offset_calib_value_list[0] = 0;
        opamp_offset_calib_value_list[1] = (int) Math.round(-((double) highdiv_opa_diff * (1.5 / 9.0) / bias_one_lsb_vs_adc));
        opamp_offset_calib_value_list[2] = (int) Math.round(-((double) highdiv_opa_diff * (4.0 / 9.0) / bias_one_lsb_vs_adc));
        opamp_offset_calib_value_list[3] = (int) Math.round(-((double) highdiv_opa_diff / bias_one_lsb_vs_adc));
        opamp_offset_calib_value_list[4] = 0;
        opamp_offset_calib_value_list[5] = (int) Math.round(-((double) lowdiv_opa_diff * (1.0 / 9.0) / bias_one_lsb_vs_adc));
        opamp_offset_calib_value_list[6] = (int) Math.round(-((double) lowdiv_opa_diff * (3.0 / 9.0) / bias_one_lsb_vs_adc));
        opamp_offset_calib_value_list[7] = (int) Math.round(-((double) lowdiv_opa_diff / bias_one_lsb_vs_adc));
        opamp_offset_calib_value_list[8] = opamp_offset_calib_value_list[7];
        opamp_offset_calib_value_list[9] = opamp_offset_calib_value_list[7];

        if (voltscale <= 7) {
            bias_lower = (int) (normal_bias_lower * TYPICAL_AMP_ERROR[voltscale]);
            bias_upper = (int) (normal_bias_upper * TYPICAL_AMP_ERROR[voltscale]);
        } else if (voltscale == 8) {
            bias_lower = normal_bias_lower / 2;
            bias_upper = normal_bias_upper / 2;
        } else {
            bias_lower = normal_bias_lower / 4;
            bias_upper = normal_bias_upper / 4;
        }
    }


    //
    // Timer for auto mode not triggered
    //
    private volatile int autoTime = 0;

    public class AutoModeTimerTask extends TimerTask {
        public void run() {
            if (autoModeNormal == false) {
                return;
            } else if (triggerMode != TGMODE_AUTO) {
                autoTime = 0;
                autoModeNormal = false;
                return;
            }


            // If a trigger is not detected after a lapse of a certain time, the FREE mode is set
            if (autoTime > 30) {    // 10ms * 30 = 300ms
                sendMessage(MESSAGE_RUNMODE, TGMODE_FREE);
                endAutoModeNormal();
            }

            autoTime++;

            //if(D)Log.d("AutoModeTimerTask", "autoTime is " + Integer.toString(autoTime));
        }

        public void timeReset() {
            autoTime = 0;
            //if(D)Log.d("AutoModeTimerTask", "autoTime reset");
        }

        public void endAutoModeNormal() {
            autoTime = 0;
            autoModeNormal = false;
        }
    }

    private class UsbReceiveTask extends TimerTask {

        private byte[] SendBuffer,ReceiveBuffer;
        double[] g_wave;
        int[] lawSamples;

        UsbReceiveTask(){
            // Acknowledge packet
            SendBuffer = new byte[EpOutPacketSize];
            SendBuffer[0] = MESSAGE_DATA_RECEIVED;

            ReceiveBuffer = new byte[READ_BUFFER_SIZE];

            g_wave = new double[DEFAULT_SAMPLE_LENGTH];        // normalizing samples (0~1)
            lawSamples = new int[DEFAULT_SAMPLE_LENGTH];    // law value samples
        }

        @Override
        public void run() {

            // error check
            if(deviceConnection == null || endPointRead == null || isConnected==false){
                // deviceConnection shutdown
                EndConnection();
                return;
            }

            //if (D) Log.e(TAG, "USB Data receive waiting");

            int ret;
            ret = deviceConnection.bulkTransfer(endPointRead,ReceiveBuffer,READ_BUFFER_SIZE,1);


            if (ret > 0) {
                // Send acknowledge
                deviceConnection.bulkTransfer(epw_Msg, SendBuffer, SendBuffer.length, 100);

            } else {
                //if (D) Log.e(TAG, "Usb data receive error occured");
                //EndConnection();    // thread stop
                return;
            }


            int j=0;
            for (int i = 0; i < sampleLength; i++) {
                lawSamples[i] = byte_to_halfword(ReceiveBuffer[j+1],ReceiveBuffer[j]);
                j+=2;
            }

            // Convert to data with graph height set to 0~1
            for (int i = 0; i < sampleLength; i++) {
                g_wave[i] = (double) (lawSamples[i] - graph_min) / graph_fullscale;    // 範囲を狭める
            }

            wave = new GraphWave(g_wave);        // wave analysis

            mHandler.post(new Runnable() {
                public void run() {
                    if (calibration == true) {
                        calibrate(lawSamples);
                    }

                    if (biasCalibState > 0) {
                        biasCalib(lawSamples);
                    }

                    if (autoSetState > 0) {
                        autoSet();
                    }

                    drawGraph();
                }
            });

                //debug
//    			Log.d(TAG,"DC = "+Integer.toString(getDC(lawSamples)));
//    			Log.d(TAG,"graph_fullscale = "+Integer.toString(graph_fullscale));

        }

        private void drawGraph() {

            mGraph.setWave(wave.samples, sampleLength);    // draw graph

            if (triggerMode == TGMODE_AUTO && run_status == true) {    // if Auto mode

                // Trigger detection

                boolean lower = !triggerSlopeUp;
                boolean triggerDetect = false;
                for (int i = 0; i < sampleLength; i++) {
                    if (wave.samples[i] >= g_vpos) {    // Sample value exceeds trigger
                        if (triggerSlopeUp && lower) {
                            triggerDetect = true;
                            break;
                        }

                        lower = false;
                    } else {                            // Sample value is less than trigger
                        if (!triggerSlopeUp && !lower) {
                            triggerDetect = true;
                            break;
                        }

                        lower = true;
                    }
                }

                if (triggerDetect) {
                    //
                    // トリガーを検出した
                    //

                    if (autoModeNormal == false) {    // AUTO FREE
                        autoModeNormal = true;
                        sendMessage(MESSAGE_RUNMODE, TGMODE_NORMAL);    // set NORMAL mode
                    } else {                        // AUTO NORMAL
                        autoModeTask.timeReset();    // Timer counter reset
                    }
                }
            } else if (triggerMode == TGMODE_SINGLE) {        // SINGLE mode

                // On the device side, since it will change freely internally, there is no need to send runninng mode change
                runModeSetStop();

                tbtn_stop.setChecked(true);
                if (D) Log.d(TAG, "Stop botton set cheched");
            }
        }

        public void EndConnection() {
            deviceConnection = null;
            endPointRead = null;
            epw_Msg = null;
            mGraph.endThread();
            isConnected = false;
        }
    }

    private class UsbReceiveThread extends Thread {

        private ByteBuffer ReceiveBuffer;
        private byte[] SendBuffer;
        double[] g_wave;
        int[] lawSamples;
        UsbRequest inRequest;

        @Override
        public void run() {
            // Acknowledge packet
            SendBuffer = new byte[EpOutPacketSize];
            SendBuffer[0] = MESSAGE_DATA_RECEIVED;

            ReceiveBuffer = ByteBuffer.allocate(READ_BUFFER_SIZE);
            ReceiveBuffer.order(ByteOrder.LITTLE_ENDIAN);    // set endian

            inRequest = new UsbRequest();

            if (inRequest.initialize(deviceConnection, endPointRead) == true) {
                if (D) Log.i(TAG, "inRequest initialize suceeded.");
            } else {
                if (D) Log.e(TAG, "inRequest initialize failed.");
                EndConnection();
            }

            g_wave = new double[DEFAULT_SAMPLE_LENGTH];        // normalizing samples (0~1)
            lawSamples = new int[DEFAULT_SAMPLE_LENGTH];    // law value samples


            //
            //		main loop
            //
            while (deviceConnection != null && endPointRead != null && isConnected) {

                if (inRequest.queue(ReceiveBuffer, READ_BUFFER_SIZE) == true) {

                    if(D) Log.i(TAG, "Request waiting...");
                    // wait data receive
                    if (deviceConnection.requestWait() == inRequest) {    //Request received
                        if(D) Log.i(TAG, "Request received.");
                    } else {
                        if (D) Log.e(TAG, "In Request waiting error occured");
                        EndConnection();    // thread stop
                        break;
                    }

                    // Send acknowledge
                    deviceConnection.bulkTransfer(epw_Msg, SendBuffer, SendBuffer.length, 100);

                } else {
                    if (D) Log.e(TAG, "Request queueing error occured");
                    EndConnection();    // thread stop
                    break;
                }

                ReceiveBuffer.clear();


                for (int i = 0; i < sampleLength; i++) {
                    lawSamples[i] = (int) ReceiveBuffer.getShort();
                }

                // Convert to data with graph height set to 0~1
                for (int i = 0; i < sampleLength; i++) {
                    g_wave[i] = (double) (lawSamples[i] - graph_min) / graph_fullscale;    // 範囲を狭める
                }

                wave = new GraphWave(g_wave);        // wave analysis

                mHandler.post(new Runnable() {
                    public void run() {
                        if (calibration == true) {
                            calibrate(lawSamples);
                        }

                        if (biasCalibState > 0) {
                            biasCalib(lawSamples);
                        }

                        if (autoSetState > 0) {
                            autoSet();
                        }

                        drawGraph();
                    }
                });

                //debug
//    			Log.d(TAG,"DC = "+Integer.toString(getDC(lawSamples)));
//    			Log.d(TAG,"graph_fullscale = "+Integer.toString(graph_fullscale));

            }

            inRequest.cancel();
            inRequest.close();
            inRequest = null;

            // deviceConnection shutdown
            deviceConnection = null;
            endPointRead = null;
            epw_Msg = null;
            mGraph.endThread();
        }

        private void drawGraph() {

            mGraph.setWave(wave.samples, sampleLength);    // draw graph

            if (triggerMode == TGMODE_AUTO && run_status == true) {    // if Auto mode

                // Trigger detection

                boolean lower = !triggerSlopeUp;
                boolean triggerDetect = false;
                for (int i = 0; i < sampleLength; i++) {
                    if (wave.samples[i] >= g_vpos) {    // Sample value exceeds trigger
                        if (triggerSlopeUp && lower) {
                            triggerDetect = true;
                            break;
                        }

                        lower = false;
                    } else {                            // Sample value is less than trigger
                        if (!triggerSlopeUp && !lower) {
                            triggerDetect = true;
                            break;
                        }

                        lower = true;
                    }
                }

                if (triggerDetect) {
                    //
                    // トリガーを検出した
                    //

                    if (autoModeNormal == false) {    // AUTO FREE
                        autoModeNormal = true;
                        sendMessage(MESSAGE_RUNMODE, TGMODE_NORMAL);    // set NORMAL mode
                    } else {                        // AUTO NORMAL
                        autoModeTask.timeReset();    // Timer counter reset
                    }
                }
            } else if (triggerMode == TGMODE_SINGLE) {        // SINGLE mode

                // On the device side, since it will change freely internally, there is no need to send runninng mode change
                runModeSetStop();

                tbtn_stop.setChecked(true);
                if (D) Log.d(TAG, "Stop botton set cheched");
            }
        }

        public void EndConnection() {
            isConnected = false;

        }
    }

    private class GraphWave {
        double samples[];            // Sample with 1 as the graph height
        double max = 0, min = 0, vpp = 0;
        double freq = 0;
        double vrms = 0, mean = 0;
        int num_waves = 0;
        public int range_status = 0;            // 0= range in ,1=up over,2=down over,3=bi side over
        final static int RANGE_IN = 0, RANGE_UP_OVER = 1, RANGE_DOWN_OVER = 2, RANGE_BISIDE_OVER = 3;

        GraphWave(double gwave[]) {
            samples = gwave;
            if (samples == null) return;

            //
            //	Range over detect
            //

            for (int i = 0; i < sampleLength; i++) {
                if (samples[i] > 1) {    // up over

                    if (range_status == RANGE_DOWN_OVER) {
                        range_status = RANGE_BISIDE_OVER;
                        break;
                    } else {
                        range_status = RANGE_UP_OVER;
                    }


                } else if (samples[i] < 0) {    // down over

                    if (range_status == RANGE_UP_OVER) {
                        range_status = RANGE_BISIDE_OVER;
                        break;
                    } else {
                        range_status = RANGE_DOWN_OVER;
                    }
                }
            }

            mHandler.post(new Runnable() {
                public void run() {
                    if (range_status == RANGE_IN) {
                        img_range.setImageResource(R.drawable.over_range_in);
                    } else if (range_status == RANGE_UP_OVER) {
                        img_range.setImageResource(R.drawable.over_range_up);
                    } else if (range_status == RANGE_DOWN_OVER) {
                        img_range.setImageResource(R.drawable.over_range_down);
                    } else {
                        img_range.setImageResource(R.drawable.over_range_bi);
                    }
                }
            });


            //
            //	Mean voltage
            //

            double aqum = 0;
            for (int i = 0; i < sampleLength; i++) {
                aqum += samples[i] - g_bias_pos;
            }

            mean = aqum / (double) sampleLength;
            mean = mean - (mean % 0.001);                                    // Reduce accuracy
            if (Math.abs(mean) <= 0.003) {
                mean = 0;
            }

            //
            //	Vrms
            //

            aqum = 0;
            for (int i = 0; i < sampleLength; i++) {
                aqum += Math.pow((samples[i] - g_bias_pos), 2.0);        // Adding power
            }

            vrms = aqum / (double) sampleLength;
            vrms = Math.sqrt(vrms);


            //
            //	peak-to-peak voltage
            //

            max = samples[0];
            min = samples[0];

            for (int i = 1; i < sampleLength; i++) {
                if (samples[i] > max) max = samples[i];
                if (samples[i] < min) min = samples[i];
            }
            vpp = max - min;

            //
            //	Frequency
            //

            freq = 0;

            if (vpp < (1 / 16)) {    // If the amplitude is less than 1/2 div, do not measure
                return;
            }

            // 処理の流れ
            // スレシュルドhighをダウンクロスかスレシュルドLowをアップクロスした点を探す
            // 反対側のスレシュルドをクロスするか探索
            // 見つけたら最初のスレシュルドhighをダウンクロスかスレシュルドLowをアップクロスした点を位相の開始地点にする
            //

            // process flow
            // Look for points climbing up the cross-over or threshold Threshhigh high
            // Cross or cross search the opposite threshold
            // When we find it, we make the first threshold high high as the starting point of the phase crossing up or down crossing the threshold Low

            double high_threth = max - vpp / 4;        // High threshold
            double low_threth = min + vpp / 4;        // Low threshold

            int aqum_time = 0, last_overed_i = 0;

            int state_before = 0;
            final int STATE_HIGH = 1;
            final int STATE_LOW = 2;

            for (int i = 0; i < sampleLength; i++) {
                if (high_threth < samples[i]) {                                // high
                    if (state_before == STATE_LOW) {
                        if (last_overed_i != 0) {
                            aqum_time += i - last_overed_i;
                            num_waves++;
                        }

                        last_overed_i = i;
                    }

                    state_before = STATE_HIGH;
                } else if (low_threth > samples[i]) {                        // low
//        			if(state_before == STATE_HIGH){
//        				aqum_time += i - last_overed_i;
//        				num_crossed++;
//        			}

                    //last_overed_i = i;
                    state_before = STATE_LOW;
                }
            }

            if (num_waves > 0) {
                freq = (1.0 / ((((double) aqum_time * (TIME_CONVERT_LIST[timescale] / 80.0)) / (double) num_waves)));// * 2;
            }

        }
    }


    // Send message for device
    private void sendMessage(int m, int d) {
        if (epw_Msg == null) return;

        byte[] buffer = new byte[EpOutPacketSize];
        buffer[0] = (byte) m;
        buffer[1] = (byte) (0xff & d);
        buffer[2] = (byte) ((0xff00 & d) >> 8);
        buffer[3] = (byte) ((0xff0000 & d) >> 16);
        buffer[4] = (byte) ((0xff000000 & d) >> 24);
        int ret = deviceConnection.bulkTransfer(epw_Msg, buffer, buffer.length, 50);

        if (ret < 0) {
            if (D) Log.e(TAG, "Send Message FAILURE");
        }
        //if(D)Log.d(TAG, "Send Message = " + Integer.toString(m) + " Data = " + Integer.toString(d));
    }

    //
    //	Metric prefix converter
    //
    public String siConverter(double value) {
        String si = "";
        String minus = null;


        if (value < 0) {
            minus = "-";
            value = (-value);
        }


        if (value < 0.000000001) {    // value is too little
            return "0";                // return

        } else if (value < 0.000001) {    // nano
            si = "n";
            value = value * 1000000000;
        } else if (value < 0.001) {        // micro
            si = "μ";
            value = value * 1000000;
        } else if (value < 1) {        // mili
            si = "m";
            value = value * 1000;
        } else if (value < 1000) {
            // do nothing
        } else if (value < 1000000) {    // Kiro
            si = "K";
            value = value * 0.001;
        } else if (value < 1000000000) {    // Mega
            si = "M";
            value = value * 0.000001;
        }

        String f;    // format
        if (value < 10) {
            f = "%1.2f";
        } else if (value < 100) {
            f = "%2.1f";
        } else {            // value < 1000
            f = "%3.0f";
        }

        if (minus == null)
            return String.format(String.format(f, value) + si);
        else
            return String.format(minus + String.format(f, value) + si);
    }


    public void RunStopToggle() {
        run_status = !run_status;

        if (run_status) {
            RunStopText.setText("RUN");
            RunStopText.setTextColor(Color.GREEN);
        } else {
            RunStopText.setText("STOP");
            RunStopText.setTextColor(Color.RED);
        }
    }

    public void runModeSetStop() {
        run_status = false;
        RunStopText.setText("STOP");
        RunStopText.setTextColor(Color.RED);
    }

    public void runModeSetRun() {
        run_status = true;
        RunStopText.setText("RUN");
        RunStopText.setTextColor(Color.GREEN);
    }

    private void setTriggerMode() {

        if (autoModeNormal && autoModeTask != null && triggerMode != TGMODE_AUTO) {    // End AutoModeTask
            autoModeTask.endAutoModeNormal();
        }

        if (run_status == true) {        // Not send if STOP mode
            if (triggerMode == TGMODE_AUTO || triggerMode == TGMODE_FREE) {
                sendMessage(MESSAGE_RUNMODE, TGMODE_FREE);
            } else if (triggerMode == TGMODE_NORMAL) {
                sendMessage(MESSAGE_RUNMODE, TGMODE_NORMAL);
            } else if (triggerMode == TGMODE_SINGLE) {
                sendMessage(MESSAGE_RUNMODE, TGMODE_SINGLE);
            } else if (triggerMode == TGMODE_SINGLE_FREE) {
                sendMessage(MESSAGE_RUNMODE, TGMODE_SINGLE_FREE);
                return;
            }
        }

        triggerModeText.setText(TRIGGER_MODE_LIST[triggerMode]);
        //Blink background
        blinkTextBG(triggerModeText, Color.GRAY);

    }


    private void setTimescale() {

        switch (timescale) {
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
            default:    // timescale >= 5
                sampleLength = 800;
        }

        // At 80Msps, ADC input impedance is change. Necessary to re calculation vertical axis
        setupVerticalAxis();
        setupBias();

        sendMessage(MESSAGE_TIMESCALE, timescale);
        setHTrigger();                        // For re calculate oscilloscope trigger timer
        setBias(true);

        TimescaleText.setText("Td:" + String.format("%5s", TIME_SCALE_LIST[timescale]));

        if (enableFlashText) {
            mGraph.setFlashText("Time/div = " + TIME_SCALE_LIST[timescale]);
            blinkTextBG(TimescaleText, Color.GRAY);
        }
    }


    // g_hpos: The right edge of the screen is 1 Center is 0, Left edge is -1
    public void setHTrigger() {

        // Send horizontal trigger value change message
        int h_value = (int) (g_hpos * (sampleLength / 2));
        sendMessage(MESSAGE_H_TRIGGER, h_value);

        //if(D)Log.d(TAG, "Set pos = " + Double.toString(pos));


        double ht_real = g_hpos * TIME_CONVERT_LIST[timescale] * 5.0;

        String htrigger_time_text = String.format("%7s", siConverter(ht_real) + "s");
        HPotisionText.setText("HT:" + htrigger_time_text);
        mGraph.setHTriggerPos(g_hpos, htrigger_time_text);
        //HPotisionText.setText("HT:"+Integer.toString(HTriggerValue));	// display low value
    }


    public void setVTrigger(boolean isSendData) {

        verTriggerValue = (int) (g_vpos * graph_fullscale + graph_min);
        //if (D && isSendData) Log.d(TAG, "verTriggerValue = " + Integer.toString(verTriggerValue));
        if (isSendData) {

            if (verTriggerValue > SAMPLE_MAX) {
                verTriggerValue = SAMPLE_MAX;
            } else if (verTriggerValue < 0) {
                verTriggerValue = 0;
            }

            //Send vertical trigger value change message
            sendMessage(MESSAGE_V_TRIGGER, verTriggerValue);
        }


        double realVoltage = (g_vpos - g_bias_pos) * FULLSCALE_VOLTAGE_LIST[voltscale] * proveRatio;
        //VPotisionText.setText("VT:"+Integer.toString(verTriggerValue));		// display low value

        String vtvoltage_text = String.format("%7s", siConverter(realVoltage) + "V");
        VPotisionText.setText("VT:" + vtvoltage_text);

        mGraph.setVTriggerPos(g_vpos, vtvoltage_text);
    }

    public void setBias(boolean isSendData) {
        int bias = 0;
        if (isSendData) {

            if (bias < 0.5) {    // under 0.5
                bias = bias_center - (int) ((1.0 - (g_bias_pos * 2.0)) * bias_lower);
            } else {    // higher than 0.5
                bias = bias_center + (int) (((g_bias_pos - 0.5) * 2.0) * bias_upper);
            }

            bias = bias + opamp_offset_calib_value_list[voltscale];

//	    	if(timescale <= 5){	// at 80Msps
//	    		bias += BIAS_DROP_CALIBVAL_AT_80MSPS ;
//    		}

            if (bias > BIAS_MAX_REG_VALUE) {
                bias = BIAS_MAX_REG_VALUE;
            } else if (bias < 0) {
                bias = 0;
            }

            // bias voltage change message
            sendMessage(MESSAGE_BIAS, bias);
        }


        setVTrigger(false);
        //BiasText.setText("BI:"+Integer.toString(bias));	// display low value
        double real = (g_bias_pos - 0.5) * FULLSCALE_VOLTAGE_LIST[voltscale] * proveRatio;

        String bias_voltage = String.format("%7s", siConverter(real) + "V");
        BiasText.setText("PS:" + bias_voltage);

        mGraph.setBiasPos(g_bias_pos, bias_voltage);
    }

    private void setTriggerSlope() {
        if (triggerSlopeUp) {
            sendMessage(MESSAGE_TRIGGER_EDGE, 0);
            img_trigger.setImageResource(R.drawable.slope_up);
        } else {
            sendMessage(MESSAGE_TRIGGER_EDGE, 1);
            img_trigger.setImageResource(R.drawable.slope_down);
        }
    }

    public void changeVoltscale(int vs){
        voltscale = vs;
    }

    public void changeTimescale(int ts){
        timescale = ts;
    }

    public void setVoltscale(boolean isSendData) {

        if (isSendData) {
            sendMessage(MESSAGE_VOLTAGE_SCALE, voltscale);
        }
        String scale;

        if (x10Mode) {
            scale = VOLT_SCALE_LIST_X10[voltscale];
        } else {
            scale = VOLT_SCALE_LIST[voltscale];
        }
        String s = String.format("%4s", scale);

        VoltscaleText.setText("Vd:" + s);
        if (enableFlashText) {
            mGraph.setFlashText("Volt/div = " + scale);
        }

//		//hide button
//		if(voltscale==0){
//			btn_volt_zoom.setVisibility(View.INVISIBLE);
//			btn_volt_unzoom.setVisibility(View.VISIBLE);
//		} else if(voltscale==(NUM_VOLTSCALE-1)){
//			btn_volt_zoom.setVisibility(View.VISIBLE);
//			btn_volt_unzoom.setVisibility(View.INVISIBLE);
//		} else {
//			btn_volt_zoom.setVisibility(View.VISIBLE);
//			btn_volt_unzoom.setVisibility(View.VISIBLE);
//		}

        setupVerticalAxis();
        setupBias();

        setVTrigger(false);
        setBias(true);
    }


    private int getDC(int[] samples) {
        int aqum = 0;

        for (int i = 0; i < samples.length; i++) {
            aqum += samples[i];
        }

        int dc = (int) ((double) aqum / samples.length);

        if (D) Log.d(TAG, "DC = " + Integer.toString(dc));
        return dc;
    }

    int gndValue = 0, gndValue200mV = 0;
    ;
    int calibrateReceivedCount = 0;
    double bias_vs_adc_ratio;

    public void calibrate(int[] samples) {


        if (D) Log.d(TAG, "calibrateReceivedCount = " + Integer.toString(calibrateReceivedCount));

        int exe = 0;

        if (calibrateReceivedCount == (exe++)) {    // ready for calibration
            sendMessage(MESSAGE_RUNMODE, TGMODE_DEVICE_STOP);    // STOP


            // Reset calibration value
            highdiv_input_calib_voltage = 0;
            lowdiv_input_calib_voltage = 0;
            highdiv_opa_diff = 0;
            lowdiv_opa_diff = 0;

            setupVerticalAxis();
            setupBias();

            voltscale = VOLTSCALE_5V;  // 5V div
            setVoltscale(true);

            if (dc_cut == true) {
                dc_cut = false;
                onClick(tbtn_dccut);                // DC Thru
            }


            timescale = TIMESCALE_2P5MS;        // 2.5ms
            setTimescale();

            if (run_status == false) {
                RunStopToggle();
            }

            // Set bias value at graph 3/8 point
            //sendMessage(MESSAGE_BIAS,TYPICAL_BIAS_BOTTOM+((int)(TYPICAL_BIAS_FS_VALUE*3/8.0)));
            g_bias_pos = 3.0 / 8.0;
            setBias(true);

            triggerSlopeUp = true;    // Up trigger
            setTriggerSlope();

            sendMessage(MESSAGE_RUNMODE, TGMODE_SINGLE_FREE);    //Free single shot mode
        } else if (calibrateReceivedCount == (exe++) || calibrateReceivedCount == (exe++)) {
            sendMessage(MESSAGE_RUNMODE, TGMODE_SINGLE_FREE);    //Free single shot mode

        } else if (calibrateReceivedCount == (exe++)) {        // get GND value
            gndValue = getDC(samples);        // GND value at 5V/div

            voltscale = VOLTSCALE_200MV;  // 200mV div
            setVoltscale(true);
            sendMessage(MESSAGE_RUNMODE, TGMODE_SINGLE_FREE);    //Free single shot mode

        } else if (calibrateReceivedCount == (exe++)) {        // get GND value
            Log.d(TAG, "calibration: get gnd value");

            gndValue200mV = getDC(samples);

            triggerMode = TGMODE_NORMAL;
            setTriggerMode();

            triggerSlopeUp = true;
            setTriggerSlope();

            g_vpos = 4.0 / 8.0;    // VTrigger = center
            setVTrigger(true);

            g_hpos = -1.5;        // out of graph left side
            setHTrigger();

            Toast.makeText(this, "Get GND value. Awaiting connect calibrator", Toast.LENGTH_SHORT).show();

            Log.d(TAG, "calibration: waiting connect calibrator");

        } else if (calibrateReceivedCount == (exe++)) {
            //
            //	Get low div calibration value
            //

            // ADCMax input error measure (Max10+-)
            double real_vpp200mv_val = (double) (getDC(samples) - gndValue200mV);
            double imagine_vpp200mv_val = TYPICAL_FULLSCALE_MAX_VAL * TYPICAL_AMP_ERROR[VOLTSCALE_200MV] * (LOWDIV_CALIBRATION_VOLTAGE / (0.2 * 8));    // アンプエラーを考慮
            int tmp = (int) (((imagine_vpp200mv_val / real_vpp200mv_val) - 1.0) * TYPICAL_ADC_INPUT_MAX_VOLTAGE_PP * 1000);    // mV

            // Validation
            if (Math.abs(tmp) > 250) {
                Log.e(TAG, "adc input calibration voltage is overflow : " + Integer.toString(tmp));
                calibration = false;
                Toast.makeText(this, "Calibration error.Stop calibration", Toast.LENGTH_SHORT).show();
                return;
            }
            ;

            lowdiv_input_calib_voltage = tmp;

            String p = "+";
            if (lowdiv_input_calib_voltage < 0) {
                p = "";
            }
            Toast.makeText(this, "Get low div input volatge error: " + p + Integer.toString(lowdiv_input_calib_voltage) + "mV", Toast.LENGTH_SHORT).show();

            if (D)
                Log.d(TAG, "adc input calibration voltage = " + Integer.toString(lowdiv_input_calib_voltage) + "mV" + "\n waiting disconnect calibrator");

            voltscale = VOLTSCALE_5V;
            setVoltscale(true);


        } else if (calibrateReceivedCount == (exe++)) {        // Calibrator connection detected
            //
            //	Get high div calibration value
            //

            // ADCMax input error measure (Max10+-)
            double real_vpp10v_val = (double) (getDC(samples) - gndValue);
            double imagine_vpp10v_val = TYPICAL_FULLSCALE_MAX_VAL * TYPICAL_AMP_ERROR[0] * (HIGHDIV_CALIBRATION_VOLTAGE / (5.0 * 8));    // 5V/divのアンプエラーを考慮
            int tmp = (int) (((imagine_vpp10v_val / real_vpp10v_val) - 1.0) * TYPICAL_ADC_INPUT_MAX_VOLTAGE_PP * 1000);    // mV

            // Check value
            if (Math.abs(tmp) > 250) {
                Log.e(TAG, "adc input calibration voltage is overflow : " + Integer.toString(tmp));
                calibration = false;
                Toast.makeText(this, "Calibration error.Stop calibration", Toast.LENGTH_SHORT).show();
                return;
            }
            ;

            highdiv_input_calib_voltage = tmp;

            String p = "+";
            if (highdiv_input_calib_voltage < 0) {
                p = "";
            }
            Toast.makeText(this, "Get high div input volatge error: " + p + Integer.toString(highdiv_input_calib_voltage) + "mV", Toast.LENGTH_SHORT).show();

            if (D)
                Log.d(TAG, "High div input calibration voltage = " + Integer.toString(highdiv_input_calib_voltage) + "mV" + "\n waiting disconnect calibrator");

            triggerSlopeUp = false;    // down trigger
            setTriggerSlope();


        } else if (calibrateReceivedCount == (exe++)) {    // Detect cariblator disconnect
            //
            //	Get OPAMP input offset voltage
            //
            // OPA4354's input offset voltage is Typical +-2mV. Max +-8mV
            voltscale = 3;        // 500mV/div
            setVoltscale(true);
            triggerMode = TGMODE_FREE;
            setTriggerMode();
        } else if (calibrateReceivedCount == (exe++)) {
            //Do nothing
        } else if (calibrateReceivedCount == (exe++)) {
            int tmp = getDC(samples) - gndValue;

            if (Math.abs(tmp) > 500) {
                if (D)
                    Log.e(TAG, "Opamp high offset voltage is overflow : " + Integer.toString(tmp));
                calibration = false;
                Toast.makeText(this, "Calibration error.Stop calibration", Toast.LENGTH_SHORT).show();
                return;
            }
            ;

            highdiv_opa_diff = tmp;

            Toast.makeText(this, "Opamp HIGH input offset value: " + Integer.toString(highdiv_opa_diff), Toast.LENGTH_SHORT).show();

            voltscale = VOLTSCALE_20MV;
            setVoltscale(true);
        } else if (calibrateReceivedCount == (exe++)) {
            //Do nothing
        } else if (calibrateReceivedCount == (exe++)) {
            int tmp = getDC(samples) - gndValue200mV;

            if (Math.abs(tmp) > 500) {
                if (D)
                    Log.e(TAG, "Opamp low offset voltage is overflow : " + Integer.toString(tmp));
                calibration = false;
                Toast.makeText(this, "Calibration error.Stop calibration", Toast.LENGTH_SHORT).show();
                return;
            }
            ;

            lowdiv_opa_diff = tmp;

            Toast.makeText(this, "Opamp LOW input offset value: " + Integer.toString(lowdiv_opa_diff), Toast.LENGTH_SHORT).show();
            setupBias();
            calibration = false;
            Toast.makeText(this, "Calibration completed normally", Toast.LENGTH_SHORT).show();

            // EEPROM Write
            byte[] buffer = new byte[EpOutPacketSize];
            int i = 0;
            buffer[i++] = MESSAGE_EEPROM_PAGE_WRITE;
            buffer[i++] = 0;        // EEPROM Write page number
            buffer[i++] = int_to_byte(highdiv_input_calib_voltage, 1);
            buffer[i++] = int_to_byte(highdiv_input_calib_voltage, 0);
            buffer[i++] = int_to_byte(lowdiv_input_calib_voltage, 1);
            buffer[i++] = int_to_byte(lowdiv_input_calib_voltage, 0);
            buffer[i++] = int_to_byte(highdiv_opa_diff, 1);
            buffer[i++] = int_to_byte(highdiv_opa_diff, 0);
            buffer[i++] = int_to_byte(lowdiv_opa_diff, 1);
            buffer[i++] = int_to_byte(lowdiv_opa_diff, 0);

            deviceConnection.bulkTransfer(epw_Msg, buffer, buffer.length, 250);

            biasCalibState = -1;
        }

        calibrateReceivedCount++;

        // The bias output settles down from the DAC output is about 200 us with a 10 nF capacitor
    }

    //
    // Bias Calibration
    //
    int biasCalibState = 0;
    int bias_val;
    double adc_vs_bias;

    private void biasCalib(int[] samples) {
        if (D) Log.d(TAG, "biasCalibState = " + Integer.toString(biasCalibState));

        if (biasCalibState == -1) {        //Ready for bias calibration

            // Do not consider the error rate of the amplifier
            adc_max_voltage = TYPICAL_ADC_INPUT_MAX_VOLTAGE_PP + ((double) highdiv_input_calib_voltage / 1000);
            one_lsb_voltage = adc_max_voltage / SAMPLE_MAX;
            normal_graph_fullscale = (int) (SAMPLE_MAX * (GRAPH_FULLSCALE_VOLTAGE / adc_max_voltage));
            normal_graph_min = (SAMPLE_MAX - normal_graph_fullscale) / 2;

            adc_vs_bias = TYPICAL_BIAS_LSB_VOLTAGE / one_lsb_voltage;        // DAC 1LSB vs bias output voltage

            voltscale = VOLTSCALE_5V;        // 5V
            sendMessage(MESSAGE_VOLTAGE_SCALE, voltscale);

            timescale = TIMESCALE_2P5MS;        // 2.5ms
            sendMessage(MESSAGE_TIMESCALE, timescale);

            bias_val = (TYPICAL_BIAS_FS_VALUE / 2) + TYPICAL_BIAS_BOTTOM;
            sendMessage(MESSAGE_BIAS, bias_val);        // Position at graph center
            sendMessage(MESSAGE_RUNMODE, TGMODE_SINGLE_FREE);    //Free single shot mode
            biasCalibState = 1;

        }
        if (biasCalibState <= 2) {
            biasCalibState++;    // Discard the first two received samples
            sendMessage(MESSAGE_RUNMODE, TGMODE_SINGLE_FREE);    //Free single shot mode

        } else if (biasCalibState == 3) {    // Exploring the bias value matching the center of the graph
            int diff = getDC(samples) - ((normal_graph_fullscale / 2) + graph_min);    // The difference between the graph center and the actual voltage
            if (Math.abs(diff) > 2) {
                bias_val = bias_val - (int) ((double) diff / adc_vs_bias);
                sendMessage(MESSAGE_BIAS, bias_val);
                sendMessage(MESSAGE_RUNMODE, TGMODE_SINGLE_FREE);    //Free single shot mode

            } else {    // When the deviation becomes less than 3 LSB
                bias_center = bias_val;

                Toast.makeText(this, "Bias Center = " + Integer.toString(bias_center), Toast.LENGTH_SHORT).show();
                // Fit at the top of the graph
                bias_val = TYPICAL_BIAS_BOTTOM + TYPICAL_BIAS_FS_VALUE;
                sendMessage(MESSAGE_BIAS, bias_val);
                sendMessage(MESSAGE_RUNMODE, TGMODE_SINGLE_FREE);    //Free single shot mode
                biasCalibState++;
            }

        } else if (biasCalibState == 4) {

            // Find the bias value matching the top of the graph
            int diff = getDC(samples) - (normal_graph_fullscale + graph_min);    // The difference between the top of the graph and the actual voltage
            if (Math.abs(diff) > 2) {
                bias_val = bias_val - (int) ((double) diff / adc_vs_bias);
                sendMessage(MESSAGE_BIAS, bias_val);
                sendMessage(MESSAGE_RUNMODE, TGMODE_SINGLE_FREE);    //Free single shot mode

            } else {    // When the deviation becomes less than 3 LSB
                normal_bias_upper = bias_val - bias_center;
                Toast.makeText(this, "Bias Upper = " + Integer.toString(normal_bias_upper), Toast.LENGTH_SHORT).show();

                bias_val = TYPICAL_BIAS_BOTTOM;
                sendMessage(MESSAGE_BIAS, bias_val);        // Fit at the bottom of the graph
                sendMessage(MESSAGE_RUNMODE, TGMODE_SINGLE_FREE);    //Free single shot mode
                biasCalibState++;
            }

        } else if (biasCalibState == 5) {    // Find the bias value matching the bottom of the graph
            int diff = getDC(samples) - graph_min;    // The difference between the bottom of the graph and the actual voltage
            if (Math.abs(diff) > 2) {
                bias_val = bias_val - (int) ((double) diff / adc_vs_bias);
                sendMessage(MESSAGE_BIAS, bias_val);
                sendMessage(MESSAGE_RUNMODE, TGMODE_SINGLE_FREE);    //Free single shot mode

            } else {    // When the deviation becomes less than 3 LSB
                normal_bias_lower = bias_center - bias_val;
                Toast.makeText(this, "Bias Lower = " + Integer.toString(normal_bias_lower), Toast.LENGTH_SHORT).show();

                // EEPROM write 16bit big endianness
                byte[] buffer = new byte[EpOutPacketSize];
                buffer[0] = MESSAGE_EEPROM_PAGE_WRITE;
                buffer[1] = 8;        // EEPROM Write page number
                buffer[2] = int_to_byte(normal_bias_upper, 1);
                buffer[3] = int_to_byte(normal_bias_upper, 0);
                buffer[4] = int_to_byte(bias_center, 1);
                buffer[5] = int_to_byte(bias_center, 0);
                buffer[6] = int_to_byte(normal_bias_lower, 1);
                buffer[7] = int_to_byte(normal_bias_lower, 0);
                deviceConnection.bulkTransfer(epw_Msg, buffer, buffer.length, 250);

                biasCalibState = 0;
                setupVerticalAxis();
                setupBias();
                sendMessage(MESSAGE_RUNMODE, TGMODE_FREE);
                g_bias_pos = 0.5;
                setBias(true);
            }
        }


    }


    //
    // Auto Range
    //
    // 1.サンプル値最大値最小値チェック
    // 2.サンプル値上下両方が飽和 → 終了
    //   サンプル値上側が飽和 → グラフの下端に波形の最低値を合わせるようにバイアスを合わせる2へ
    //   サンプル値下側が飽和 → グラフの上端に波形の最大値を合わせるようにバイアスを合わせる2へ
    //   VPPがグラフに収まるかチェック 収まらないなら終了 収まるなら3へ
    // 3．グラフの真ん中に波形が来るようにバイアスを合わせる
    // 4.VPPが3/div以上になる最大の電圧レンジを選択する
    // 5.波形３つ以上が収まる最大の水平レンジを選択する


    // 1. Sample value maximum value and minimum value check
    // 2. Sample value both up and down are saturated to terminate
    // Sample upper side is saturated → bias is adjusted so that the lowest value of waveform matches lower end of graph 2
    // saturate the sample value → adjust the bias so that the maximum value of the waveform matches the top of the graph 2
    // check if VPP fits into the graph If it does not fit, exit 3
    // 3. Adjust the bias so that the waveform comes in the middle of the graph
    // 4. Select the maximum voltage range where VPP is 3 / div or more
    // 5. Choose the maximum horizontal range that will fit three or more waveforms

    int autoSetState = 0;

    public void autoSet() {

        Log.d(TAG, "Autoset state = " + Integer.toString(autoSetState));

        if (autoSetState <= 2) {
            autoSetState++;    // Discard the first two received samples
            sendMessage(MESSAGE_RUNMODE, TGMODE_SINGLE_FREE);    //Free single shot mod初めて工作で使ったトランジスタe

        } else if (autoSetState == 3 || autoSetState == 5) {    // Set the waveform in the center of the graph
            if (wave.vpp > 1) {    // If the waveform does not fit in the graph end
                autoSetState = 0;
                return;
            } else if (wave.min < 0 && wave.max < 1) { // Bottom saturate
                // A bias value such that the maximum value of the waveform is at the top of the graph
                g_bias_pos = g_bias_pos + 1.0 - wave.max;
            } else if (wave.min > 0 && wave.max > 1) { // Top sasturate
                // A bias value such that the minimum value of the waveform is at the bottom of the graph
                g_bias_pos = g_bias_pos - wave.min;
            } else {        // not saturate
                // Set the waveform at the center of the graph
                g_bias_pos = g_bias_pos - ((wave.min + (wave.vpp / 2)) - 0.5);
                //g_bias_pos = g_bias_pos - (g_bias_pos % 0.125);	// Fit the divition
                autoSetState++;
            }

            if (g_bias_pos > g_bias_pos_max) {        // Over the bias output maximum value
                g_bias_pos = g_bias_pos_max;
                autoSetState = 0;
            } else if (g_bias_pos < g_bias_pos_min) {        // Below the bias output minimum value
                g_bias_pos = g_bias_pos_min;
                autoSetState = 0;
            }

            setBias(true);
            sendMessage(MESSAGE_RUNMODE, TGMODE_SINGLE_FREE);    //Free single shot mode
        } else if (autoSetState == 4) {        // Range gradually decreases from 5V/div

            // Adjust bias if either is saturated
            if (wave.min < 0 || wave.max > 1) {
                autoSetState = 3;
                autoSet();
                return;
            }

            // Select the maximum voltage range that will be greater than 2.5V/div
            if (wave.vpp < (2.5 / 8.0)) {
                voltscale++;
                if (voltscale >= (NUM_VOLTSCALE - 1)) {
                    autoSetState = 0;
                }
                setVoltscale(true);
            } else {
                autoSetState++;
                //autoSetState = 0;
            }
            sendMessage(MESSAGE_RUNMODE, TGMODE_SINGLE_FREE);    //Free single shot mode

        } else if (autoSetState == 6) {    // Horizontal range select
            if (wave.num_waves < 2) {

                timescale++;
                setTimescale();

                if (timescale > 20) {
                    autoSetState = 0;
                }
            } else if (wave.num_waves < 5) {
                autoSetState = 0;
            } else {

                timescale--;
                setTimescale();

                if (timescale == 0) {
                    autoSetState = 0;
                }
            }
            sendMessage(MESSAGE_RUNMODE, TGMODE_SINGLE_FREE);    //Free single shot mode
        }

        //
        // End processing
        //
        if (autoSetState == 0) {

            g_hpos = 0;
            setHTrigger();
            g_vpos = 0.5;
            setVTrigger(true);
            triggerMode = TGMODE_AUTO;
            setTriggerMode();
        }

    }


    //Blink text background
    TextView blinkText;
    boolean occupied = false;

    private void blinkTextBG(TextView t, int c) {
        if (occupied == true) {
            return;
        }

        blinkText = t;
        blinkText.setBackgroundColor(c);
        occupied = true;
        new Timer().schedule(new TimerTask() {
            public void run() {
                mHandler.post(new Runnable() {
                    public void run() {
                        blinkText.setBackgroundColor(Color.BLACK);
                        occupied = false;
                    }
                });
            }
        }, 50L);
    }


    //
    // Handling multiple touch gestures
    //
    boolean pinchDetect = false;
    private double tmp_hpos, tmp_vpos, tmp_bias_pos;

    private class GestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        float beginSpan;
        boolean axisIsX;

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            Log.d(TAG, "onScaleBegin : " + detector.getCurrentSpan());
            beginSpan = detector.getCurrentSpan();
            pinchDetect = true;
            mGraph.unDrawLine();

            // Restore the value
            if (g_hpos != tmp_hpos) {
                g_hpos = tmp_hpos;
                setHTrigger();
            }

            if (g_vpos != tmp_vpos) {
                g_vpos = tmp_vpos;
                setVTrigger(true);
            }

            if (g_bias_pos != tmp_bias_pos) {
                g_bias_pos = tmp_bias_pos;
                setBias(true);
            }

            // Determine axis
            if (detector.getCurrentSpanX() > detector.getCurrentSpanY()) {
                axisIsX = true;
            } else {
                axisIsX = false;
            }
            return super.onScaleBegin(detector);
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            Log.d(TAG, "onScaleEnd : " + detector.getCurrentSpan());

            float endSpan = detector.getCurrentSpan();

            if (beginSpan > endSpan) {    // Pinch in
                if (axisIsX) {    // Horizontal
                    td_unzoom_btn.performClick();    // Time-
                } else {        // Vertical
                    if (D) Log.d(TAG, "onScaleEnd :Detect pinch in axis vertical");
                    btn_volt_unzoom.performClick();  // Volt-
                }
            } else {                    // Pinch out
                if (axisIsX) {    // Horizontal
                    td_zoom_btn.performClick();      // Time+
                } else {        // Vertical
                    if (D) Log.d(TAG, "onScaleEnd :Detect Pinch out");
                    btn_volt_zoom.performClick();    // Volt+
                }
            }

            super.onScaleEnd(detector);
        }
    }

    //
    // Touch the graph area
    //
    private int[] GraphPosX;
    private int[] GraphPosY;
    private int XRightHexPos;    // The coordinates of the boundary line of the rightmost grid
    private int XLeftHexPos;    // The coordinates of the boundary line of the leftmost grid
    private int downPos = 0;
    private final int DOWN_LEFT = 1;
    private final int DOWN_CENTER = 2;
    private final int DOWN_RIGHT = 3;

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        gestureDetector.onTouchEvent(e);

        // First time only
        if (GraphPosX == null) {
            GraphPosX = new int[2];
            GraphPosY = new int[2];

            // Enter 0 at the top left corner of the graph, 1 at the bottom right corner of the graph
            mGraph.getLocationInWindow(GraphPosX);
            GraphPosY[0] = GraphPosX[1];
            GraphPosX[1] = GraphPosX[0] + mGraph.getWidth();
            GraphPosY[1] = GraphPosY[0] + mGraph.getHeight();

            XRightHexPos = (int) (GraphPosX[0] + (GraphPosX[1] - GraphPosX[0]) * (8.5 / 10.0));    //1.5 div from the right
            XLeftHexPos = (int) (GraphPosX[0] + (GraphPosX[1] - GraphPosX[0]) * (1.5 / 10.0));    //1.5 div from the Left
        }

        final float x = e.getX();
        final float y = e.getY();


        // Check touched the inside of the graph
        if ((x > GraphPosX[0]) && (x < GraphPosX[1]) && (y > GraphPosY[0]) && (y < GraphPosY[1])) {

            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.d("TouchEvent", "getAction()" + "ACTION_DOWN");
                    if (x < XLeftHexPos) {
                        downPos = DOWN_LEFT;
                    } else if (XRightHexPos < x) {
                        downPos = DOWN_RIGHT;
                    } else {
                        downPos = DOWN_CENTER;
                    }

                    // variable for pinch operation for restore
                    tmp_bias_pos = g_bias_pos;
                    tmp_hpos = g_hpos;
                    tmp_vpos = g_vpos;

                    break;

                case MotionEvent.ACTION_UP:
                    Log.d("TouchEvent", "getAction()" + "ACTION_UP");
                    pinchDetect = false;
                    mGraph.unDrawLine();
                    mGraph.invalidate();
                    return true;            // return

                case MotionEvent.ACTION_MOVE:
                    Log.d("TouchEvent", "getAction()" + "ACTION_MOVE");
                    break;

                case MotionEvent.ACTION_CANCEL:
                    Log.d("TouchEvent", "getAction()" + "ACTION_CANCEL");
                    break;
            }

            if (pinchDetect) return true;
            if (downPos == DOWN_CENTER) {    // If first touched the center of the graph
                //
                // Change horizontal trigger
                //
                g_hpos = (((x - GraphPosX[0]) / (GraphPosX[1] - GraphPosX[0])) - 0.5) * 2.0;

                if (expand) {
                    g_hpos = g_hpos * HT_EXPAND_RATIO;
                }

                setHTrigger();
                mGraph.drawHorizontalLine();

            } else if (downPos == DOWN_RIGHT) {
                //
                // Change vertical trigger
                //
                g_vpos = (-(y - GraphPosY[1])) / (GraphPosY[1] - GraphPosY[0]);

                if (expand) {
                    g_vpos += (g_vpos - 0.5) * (VT_EXPAND_RATIO - 1.0);

                    if (g_vpos > g_vpos_max) {        // Limitation
                        g_vpos = g_vpos_max;
                    } else if (g_vpos < g_vpos_min) {
                        g_vpos = g_vpos_min;
                    }
                }

                setVTrigger(true);
                mGraph.drawVerticalLine();

            } else {
                //
                // Change DC bias
                //
                double vp_bias_abs = g_vpos - g_bias_pos;    // difference of between vpos and bias_pos
                g_bias_pos = (-(y - GraphPosY[1])) / (GraphPosY[1] - GraphPosY[0]);

                if (expand) {
                    g_bias_pos += (g_bias_pos - 0.5) * (BIAS_EXPAND_RATIO - 1.0);

                    if (g_bias_pos > g_bias_pos_max) {        // limitation
                        g_bias_pos = g_bias_pos_max;
                    } else if (g_bias_pos < g_bias_pos_min) {
                        g_bias_pos = g_bias_pos_min;
                    }
                }

                g_vpos = vp_bias_abs + g_bias_pos;    // Make vtrigger follow the movement of position

                setVTrigger(true);
                setBias(true);

                mGraph.drawBiasLine();

            }

            mGraph.invalidate();    // Update graph
        }
        return true;
    }


    public static class VoltageDivisionSelectDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            // make custom dialog
            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View content = inflater.inflate(R.layout.voltage_division_dialog, null);

            builder.setView(content);

            voltageDivListL = (ListView) content.findViewById(R.id.dialog_listView_L);
            voltageDivListR = (ListView) content.findViewById(R.id.dialog_listView_R);

            final String[] list_l = {"5V", "2V", "1V", "500mV","200mV"};
            final String[] list_r = {"100mV","50mV","20mV","10mV","5mV"};

            ArrayAdapter<String> adapter_l = new ArrayAdapter<String>(getActivity(), R.layout.voltage_div_list_item, list_l);
            ArrayAdapter<String> adapter_r = new ArrayAdapter<String>(getActivity(), R.layout.voltage_div_list_item, list_r);
            voltageDivListL.setAdapter(adapter_l);
            voltageDivListR.setAdapter(adapter_r);

            // get main activity instance
            final USBOscilloscopeHost main_act = (USBOscilloscopeHost)getActivity();

            //TextView selectItem = (TextView) content.findViewById((int)voltageDivListL.getItemIdAtPosition(2));


            voltageDivListL.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    main_act.changeVoltscale(position);
                    main_act.setVoltscale(true);
                    Log.i(TAG,Integer.toString(voltageDivListL.getChildCount()));
                    dismiss();
                }
            });

            voltageDivListR.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                  @Override
                  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                      main_act.changeVoltscale(position+5);
                      main_act.setVoltscale(true);
                      dismiss();
                  }
            });

            // Create the AlertDialog object and return it
            return builder.create();
        }

    }

    public static class TimeDivisionSelectDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            // make custom dialog
            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View content = inflater.inflate(R.layout.time_division_dialog, null);

            builder.setView(content);
            final int num_list = 4;

            ListView[] time_dialog_list = new ListView[4];
            time_dialog_list[0] = (ListView) content.findViewById(R.id.time_dialog_listView_0);
            time_dialog_list[1] = (ListView) content.findViewById(R.id.time_dialog_listView_1);
            time_dialog_list[2] = (ListView) content.findViewById(R.id.time_dialog_listView_2);
            time_dialog_list[3] = (ListView) content.findViewById(R.id.time_dialog_listView_3);

            final String[][] time_list = {
                    {"1s","500ms","250ms","100ms","50ms","25ms"},
                    {"10ms","5ms","2.5ms","1ms","500us","250us"},
                    {"100us","50us","25us","10us","5us","2.5us"},
                    {"1us","500ns","250ns","100ns","50ns","25ns"}
            };

            ArrayAdapter<String> []adapters = new ArrayAdapter[4];
            for(int i=0;i<num_list;i++){
                adapters[i] = new ArrayAdapter<String>(getActivity(), R.layout.voltage_div_list_item, time_list[i]);
                time_dialog_list[i].setAdapter(adapters[i]);
            }


            // get main activity instance
            final USBOscilloscopeHost main_act = (USBOscilloscopeHost)getActivity();



            //TextView selectItem = (TextView) content.findViewById((int)voltageDivListL.getItemIdAtPosition(2));


            time_dialog_list[0].setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    main_act.changeTimescale(TIMESCALE_1S-position);
                    main_act.setTimescale();
                    dismiss();
                }
            });

            time_dialog_list[1].setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    main_act.changeTimescale(TIMESCALE_10MS-position);
                    main_act.setTimescale();
                    dismiss();
                }
            });

            time_dialog_list[2].setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    main_act.changeTimescale(TIMESCALE_100US-position);
                    main_act.setTimescale();
                    dismiss();
                }
            });

            time_dialog_list[3].setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    main_act.changeTimescale(TIMESCALE_1US-position);
                    main_act.setTimescale();
                    dismiss();
                }
            });


            // Create the AlertDialog object and return it
            return builder.create();
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            Dialog dialog = getDialog();

            WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();

            DisplayMetrics metrics = getResources().getDisplayMetrics();
            int dialogWidth = (int) (metrics.widthPixels * 0.9);
            int dialogHeight = (int) (metrics.heightPixels * 0.6);

            lp.width = dialogWidth;
            lp.height = dialogHeight;
            dialog.getWindow().setAttributes(lp);
        }

    }

    @Override
    public void onClick(View v) {

        if (autoSetState > 0) return;        // Invalid operation during Autosetting

//        if (long_click_detected) {
//            long_click_detected = false;
//            return;
//        }

        int id = v.getId();

        Log.d(TAG, "onClick");

        switch (id) {
            case R.id.tbtn_stop:
                RunStopToggle();

                if (run_status == true) {        // STOP -> RUN
                    setTriggerMode();
                } else {                    // RUN -> STOP
                    if (autoModeNormal && autoModeTask != null) {
                        autoModeTask.endAutoModeNormal();
                    }

                    sendMessage(MESSAGE_RUNMODE, TGMODE_DEVICE_STOP);
                }
                break;

            // DC CUT BUTTON
            case R.id.tbtn_dccut:

                if (tbtn_dccut.isChecked()) {
                    sendMessage(MESSAGE_DCCUT, 1);
                    dc_cut = true;
                } else {
                    sendMessage(MESSAGE_DCCUT, 0);
                    dc_cut = false;
                }
                break;

            // Change trigger slope button
            case R.id.btn_edge:
                triggerSlopeUp = !triggerSlopeUp;
                if(triggerSlopeUp){
                    Toast.makeText(this, "Up trigger", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.en_up_trigger, Toast.LENGTH_SHORT).show();
                }
                setTriggerSlope();
                break;

            // Start calibration button
            case R.id.btn_calib:

                if (isConnected == false) return;

                calibration = !calibration;
                if (calibration == true) {
                    calibrateReceivedCount = 0;
                    calibrate(null);
                }
                break;

            // Voltage scale - button
            case R.id.btn_volt_unzoom:
                if (0 < voltscale) {
                    voltscale--;
                    setVoltscale(true);
                    blinkTextBG(VoltscaleText, Color.GRAY);

                } else {
                    blinkTextBG(VoltscaleText, Color.RED);
                    vibrator.vibrate(50);        // Vibrate
                }
                break;

            // Voltage scale+ button
            case R.id.btn_volt_zoom:

                if ((NUM_VOLTSCALE - 1) > voltscale) {
                    voltscale++;
                    setVoltscale(true);
                    blinkTextBG(VoltscaleText, Color.GRAY);
                } else {
                    blinkTextBG(VoltscaleText, Color.RED);
                    vibrator.vibrate(50);
                }
                break;


            // Time scale+ button
            case R.id.btn_time_zoom:
                if (0 < timescale) {
                    timescale--;
                    setTimescale();
                } else {
                    blinkTextBG(TimescaleText, Color.RED);
                    vibrator.vibrate(50);
                }

                break;

            // Time scale- button
            case R.id.btn_time_unzoom:
                if ((NUM_TIMESCALE - 1) > timescale) {
                    timescale++;
                    setTimescale();
                } else {
                    blinkTextBG(TimescaleText, Color.RED);
                    vibrator.vibrate(50);
                }
                break;

            // Change trigger mode button
            case R.id.trig_mode_button:

                final String[] items = {"AUTO", "NORMAL", "FREERUN", "SINGLE SHOT"};
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

            // Moving ratio expand button
            case R.id.tbtn_expand:

                if (expand == false) {
                    expand = true;
                    //Toast.makeText(this, "Marker movement amount expansion has been enabled.", Toast.LENGTH_SHORT).show();
                } else {    // == true
                    //Toast.makeText(this, "Marker movement amount expansion has been disabled.", Toast.LENGTH_SHORT).show();
                    expand = false;
                }
                break;


            case R.id.tbtn_x10:
                x10Mode = !x10Mode;
                if (x10Mode) {
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
            //	AutoSet
            //
            case R.id.btn_autoset:

                if (isConnected == false) {
                    return;
                }

                // ready for autoset
                triggerMode = TGMODE_SINGLE_FREE;
                if (autoModeNormal && autoModeTask != null) {
                    autoModeTask.endAutoModeNormal();
                }

                sendMessage(MESSAGE_RUNMODE, TGMODE_DEVICE_STOP);    // stop
                voltscale = 1;            // V/div = 2V
                setVoltscale(true);
                g_bias_pos = 0.5;
                setBias(true);

                timescale = 17;    // 10ms

                run_status = true;
                setTimescale();
                autoSetState = 1;

                setTriggerMode();    //Free single shot mode
                break;


            case R.id.btn_bias_calib:

                if (isConnected == false) {
                    return;
                }

                biasCalibState = -1;
                biasCalib(null);
                break;


            case R.id.btn_calib_value_reset:

                highdiv_input_calib_voltage = 0;
                lowdiv_input_calib_voltage = 0;
                highdiv_opa_diff = 0;
                lowdiv_opa_diff = 0;
                normal_bias_upper = TYPICAL_BIAS_FS_VALUE / 2;
                normal_bias_lower = TYPICAL_BIAS_FS_VALUE / 2;
                bias_center = TYPICAL_BIAS_CENTER;
                setupVerticalAxis();
                break;

            case R.id.btn_fft:
                byte[] SendBuffer;
                SendBuffer = new byte[EpOutPacketSize];
                SendBuffer[0] = MESSAGE_DATA_RECEIVED;
                deviceConnection.bulkTransfer(epw_Msg, SendBuffer, SendBuffer.length, 100);
                break;


            default:


        }
    }

    @Override
    public boolean onLongClick(View v) {
        if(D) Log.d(TAG,"onLongClick");

        //long_click_detected = true;
        int id = v.getId();

        switch (id) {

            case R.id.btn_time_unzoom:
            case R.id.btn_time_zoom:

                new TimeDivisionSelectDialog().show(getFragmentManager(), "test");
                break;

            case R.id.btn_volt_unzoom:
            case R.id.btn_volt_zoom:
                new VoltageDivisionSelectDialog().show(getFragmentManager(), "test");

                break;

            default:
                break;


        }

        if(D) Log.d(TAG,"onLongClick end");
        return false;
    }


    @Override
    public void onStart() {
        super.onStart();
        if (D) Log.i(TAG, "++ ON START ++");
    }

    private boolean isGraphInit = false;

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (D) Log.i(TAG, "+ ON WINDOW FOCUS CHANGED +");

        if (isGraphInit) return;        // Do not run twice

        //
        //	Adjust the size of Graph
        //

        isGraphInit = true;
        Point point = new Point(0, 0);
        Display display = this.getWindowManager().getDefaultDisplay();

        // Acquire the user area range of the screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            getWindowManager().getDefaultDisplay().getRealSize(point);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {

            try {
                Method getRawWidth = Display.class.getMethod("getRawWidth");
                Method getRawHeight = Display.class.getMethod("getRawHeight");
                int width = (Integer) getRawWidth.invoke(display);
                int height = (Integer) getRawHeight.invoke(display);
                Log.d(TAG, "width = " + Integer.toString(width));

                point.set(width, height);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        int buttonWidth = trig_mode_button.getWidth();
        int graphHeight = mGraph.getHeight();
        int smallDisplayWidth = (int) ((double) point.x - (double) buttonWidth * 2.0);
        int bigDisplayWidth = (int) (graphHeight * (5.0 / 4.0));

        int w, h;
        if (bigDisplayWidth > smallDisplayWidth) {    // small display
            w = smallDisplayWidth;
            h = (int) (smallDisplayWidth * (4.0 / 5.0));
        } else {                                    // big display
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
        if (D) Log.i(TAG, "+ ON RESUME +");
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if (D) Log.i(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if (D) Log.i(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (D) Log.i(TAG, "--- ON DESTROY ---");

        editor = sharedData.edit();
        editor.putInt(KEY_TRIGGER_MODE, triggerMode);
        editor.putBoolean(KEY_TRIGGER_SLOPE, triggerSlopeUp);
        editor.putInt(KEY_VSCALE, voltscale);
        editor.putInt(KEY_HSCALE, timescale);
        editor.putFloat(KEY_VPOS, (float) g_vpos);
        editor.putFloat(KEY_HPOS, (float) g_hpos);
        editor.putFloat(KEY_BIAS_POS, (float) g_bias_pos);
        editor.putBoolean(KEY_DCCUT, dc_cut);
        editor.putBoolean(KEY_X10, x10Mode);
        editor.putBoolean(KEY_EXPAND, expand);

        editor.apply();

        unregisterReceiver(mUsbReceiver);
    }


}
