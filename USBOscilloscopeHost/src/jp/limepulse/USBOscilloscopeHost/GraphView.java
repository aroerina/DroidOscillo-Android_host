package jp.limepulse.USBOscilloscopeHost;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.util.Log;
import android.view.View;
import jp.limepulse.USBOscilloscopeHost.R;

public class GraphView extends View {
	private static final String TAG = "Graph";
	private static final boolean D = false;
	private static final double WAVE_STROKE_WIDTH = 0.002;	// * graph_width
	private static final double TEXT_SIZE = 0.12; // * graph_width


	private int FrameRate = 0;

	// Graph width and height
	private double width;
	private double height;

	// Paint objects (definition of color and line width)
	private final Paint p_wave = new Paint();
	private final Paint p_frame = new Paint();
	private final Paint p_matrix = new Paint();
	private final Paint p_HTriggerLine = new Paint();
	private final Paint p_BiasLine = new Paint();
	private final Paint p_VTriggerLine = new Paint();
	private final Paint p_BiasText = new Paint();	// For bias voltage text
	private final Paint p_VTText = new Paint();		//	For vertical trigger voltage text
	private final Paint p_HTText = new Paint();		//	For horizontal trigger voltage text
	private final Paint p_BiasArea = new Paint();
	private final Paint p_VTArea = new Paint();
	private final Paint p_HTArea = new Paint();
	private final Paint p_IndicatorHTLine = new Paint();
	private final Paint p_IndicatorBiasLine = new Paint();
	private final Paint p_IndicatorVTLine = new Paint();
	private final Paint p_IndicatorDisplayArea = new Paint();

	private int sampleLength;
	private double pos_bitmap_height,pos_bitmap_width,hpos_bitmap_height,hpos_bitmap_width;
	private Bitmap matrix = null;
	private Bitmap bm_pos_b = BitmapFactory.decodeResource(getResources(),R.drawable.pos_b);
	private Bitmap bm_pos_v = BitmapFactory.decodeResource(getResources(),R.drawable.pos_v);
	private Bitmap bm_pos_h = BitmapFactory.decodeResource(getResources(),R.drawable.pos_h);

	private double[] data = null;	//waveform data

	// trigger position in pixel
	private double h_trigger_pos_px = 0;
	private double bias_pos_px = 0;
	private double v_trigger_pos_px = 0;

	private double h_trigger_pos = 0;
	private double bias_pos = 0;
	private double v_trigger_pos = 0;

	private boolean isNewWaveSet;
	private boolean running;

	private boolean isDrawVerticalLine = false;
	private boolean isDrawHorizontalLine = false;
	private boolean isDrawBiasLine = false;
	private boolean enableFlashBiasArea = false;
	private boolean enableFlashHTArea = false;
	private boolean enableFlashVTArea = false;

	private String BiasVoltageText;
	private String VTVoltageText;
	private String HTText;

	private double gradiation = 1.0;
	private double gradiation_flash = 1.0;
	private double gradiation_touch_area = 1.0;
	private double bias_indicator_move = 0;
	private double vt_indicator_move = 0;
	private Handler mHandler;

	private String flashText = "";


    public GraphView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mHandler = new Handler();

        // Paint initialize
        p_wave.setARGB(255, 255, 0, 0);
        p_frame.setARGB(255, 128, 128, 128);
        p_frame.setStyle(Paint.Style.STROKE);		// Do not fill the frame
        p_matrix.setARGB(255, 60, 60, 60);
        p_HTriggerLine.setARGB(100, 0, 255, 0);
        p_BiasLine.setARGB(100, 0, 255, 255);
        p_VTriggerLine.setARGB(120, 255, 0, 0);

        p_BiasText.setColor(p_BiasLine.getColor());
        p_BiasText.setAntiAlias(true);
        p_VTText.setColor(p_VTriggerLine.getColor());
        p_VTText.setAntiAlias(true);
        p_HTText.setColor(p_HTriggerLine.getColor());
        p_HTText.setAntiAlias(true);

        p_BiasArea.setARGB(20, 0, 255, 255);
        p_VTArea.setARGB(30, 255, 0, 0);
        p_HTArea.setARGB(20, 0, 255, 0);

        p_IndicatorHTLine.setARGB(100, 0, 255, 0);
        p_IndicatorVTLine.setARGB(120, 255, 0, 0);
        p_IndicatorBiasLine.setARGB(100, 0, 255, 255);
        p_IndicatorDisplayArea.setARGB(40,255,255,255);

    }

	@Override
	public void onWindowFocusChanged (boolean hasWindowFocus){
		Log.i("++ onWindowForcusChanged ++", TAG);
		invalidate();
	}

	//
	//		initialize grahp
	//
	public void init(int w,int h){	// w = set width ,h = set height
		if(matrix != null)return;	// do first time only

		width = (double)w;
		height = (double)h;

		// Define line thickness
		float line_stroke_width = (float)(width/200);

		// Define wave stroke width
		float wave_stroke_width = (float)(width * WAVE_STROKE_WIDTH);
		p_wave.setStrokeWidth(wave_stroke_width);
		if(width>800){
			p_wave.setAntiAlias(false);
		} else {
			p_wave.setAntiAlias(true);
		}

		//
		// Paint initialize
		//

        p_HTriggerLine.setStrokeWidth(line_stroke_width);
        p_BiasLine.setStrokeWidth(line_stroke_width);
        p_VTriggerLine.setStrokeWidth(line_stroke_width);

        p_IndicatorHTLine.setStrokeWidth(line_stroke_width);
        p_IndicatorVTLine.setStrokeWidth(line_stroke_width);
        p_IndicatorBiasLine.setStrokeWidth(line_stroke_width);

        p_matrix.setStrokeWidth((float)(width/600));

        float text_size = (float)(width*TEXT_SIZE);
        p_BiasText.setTextSize(text_size);
        p_HTText.setTextSize(text_size);
        p_VTText.setTextSize(text_size);

		//
		// Create matrix bitmap
		//

		setLayoutParams(new LinearLayout.LayoutParams(w,h));
		double widthCenter = width/2.0;
		double heightCenter = height/2.0;
		double heightDiv8 = height/8.0;
		double widthDiv10 = width/10.0;

		matrix = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(matrix);

		c.drawColor(Color.BLACK);	//Fill the canvas with black

	    Paint p_point = new Paint();
	    p_point.setColor(Color.argb(255, 255, 255, 255));
	    p_point.setStrokeWidth((float)((int)(width / 1000)));

		float p;
		//Draw horizontal line of grid
		for(int i=1;i<8;i++){
			p = (float)(i*heightDiv8);
			//c.drawLine(0, p, (float)width, p, p_matrix);
			for(int j=1;j<50;j++){
				c.drawPoint((float)((width/50)*j), p, p_point);
			}
		}

		//Draw vertical line of grid
		for(int i=1;i<10;i++){
			p = (float)(i*widthDiv10);
			//c.drawLine(p, 0, p, (float) height, p_matrix);
			for(int j=1;j<40;j++){
				c.drawPoint(p, (float)((height/40)*j), p_point);
			}
		}

		c.drawRect(0, 0, (float)(width-1), (float)(h-1), p_frame);	// ˜g•`‰æ
//		c.drawLine((float) widthCenter,0,(float) widthCenter,(float)height, p_frame);		// ’†Sü•`‰æ
//		c.drawLine(0,(float) heightCenter,(float) width,(float) heightCenter, p_frame);

		//
		//		Draw scale on frame
		//

		// Vertical center
		float centerLineWidth = (float)(width/100);
		float verticalLineDrawStartX = (float)(widthCenter - centerLineWidth/2);
		float verticalLineDrawEndX = (float)(verticalLineDrawStartX + centerLineWidth);
		for(int i=1;i<40;i++){
			float y = (float)((height/40)*i);
			c.drawLine(verticalLineDrawStartX,y,verticalLineDrawEndX,y,p_frame);
		}
		// Vertical left
		verticalLineDrawStartX = 0;
		verticalLineDrawEndX = centerLineWidth;
		for(int i=1;i<40;i++){
			float y = (float)((height/40)*i);
			if(i%5 == 0){
				c.drawLine(verticalLineDrawStartX,y,(verticalLineDrawEndX+(centerLineWidth/2)),y,p_frame);
			} else {
				c.drawLine(verticalLineDrawStartX,y,verticalLineDrawEndX,y,p_frame);
			}
		}
		// Vertical right
		verticalLineDrawStartX = (float)(width-centerLineWidth);
		verticalLineDrawEndX = (float)width;
		for(int i=1;i<40;i++){
			float y = (float)((height/40)*i);
			c.drawLine(verticalLineDrawStartX,y,verticalLineDrawEndX,y,p_frame);
			if(i%5 == 0){
				c.drawLine(verticalLineDrawStartX-(centerLineWidth/2),y,verticalLineDrawEndX,y,p_frame);
			} else {
				c.drawLine(verticalLineDrawStartX,y,verticalLineDrawEndX,y,p_frame);
			}
		}


		// Horizontal Center
		float horizontalLineDrawStartY = (float)(heightCenter - centerLineWidth/2);
		float horizontalLineDrawEndY = (float)(horizontalLineDrawStartY + centerLineWidth);
		for(int i=1;i<50;i++){
			float x = (float)((width/50)*i);
			c.drawLine(x,horizontalLineDrawStartY,x,horizontalLineDrawEndY,p_frame);
		}
		// Horizontal left
		horizontalLineDrawStartY = 0;
		horizontalLineDrawEndY = centerLineWidth;
		for(int i=1;i<50;i++){
			float x = (float)((width/50)*i);
			if(i%5 > 0){
				c.drawLine(x,horizontalLineDrawStartY,x,horizontalLineDrawEndY,p_frame);
			} else {
				c.drawLine(x,horizontalLineDrawStartY,x,horizontalLineDrawEndY+(centerLineWidth/2),p_frame);
			}
		}

		// Horizontal bottom
		horizontalLineDrawStartY = (float)(height - centerLineWidth);
		horizontalLineDrawEndY = (float)height;
		for(int i=1;i<50;i++){
			float x = (float)((width/50)*i);
			if(i%5 > 0){
				c.drawLine(x,horizontalLineDrawStartY,x,horizontalLineDrawEndY,p_frame);
			} else {
				c.drawLine(x,horizontalLineDrawStartY-(centerLineWidth/2),x,horizontalLineDrawEndY,p_frame);
			}
		}


		// Trigger position marker bitmap
		pos_bitmap_height = heightDiv8/3;
		pos_bitmap_width = pos_bitmap_height * (46.0/64.0);
		hpos_bitmap_height = heightDiv8/3;
		hpos_bitmap_width = hpos_bitmap_height * (64.0/96.0);
		bm_pos_b = Bitmap.createScaledBitmap(bm_pos_b,(int)(pos_bitmap_width),(int)(pos_bitmap_height),false);
		bm_pos_v = Bitmap.createScaledBitmap(bm_pos_v,(int)(pos_bitmap_width),(int)(pos_bitmap_height),false);
		bm_pos_h = Bitmap.createScaledBitmap(bm_pos_h,(int)(hpos_bitmap_width),(int)(hpos_bitmap_height),false);
	}

	public void run(){
		running = true;
		isNewWaveSet = false;

		while(running){
			if(isNewWaveSet){
				invalidate();
			} else {
			    try{
			    	Thread.sleep(1L);
			    }catch(InterruptedException e){}
			}
		}

	}

	public void endThread() {
		running = false;
	}


    public int frameRateGetAndReset(){
    	int tmp = FrameRate;
    	FrameRate = 0;
    	return tmp;
    }


    public void setHTriggerPos(double t,String ht_text){
    	 //t = The right end is 1,Center is 0,The left end is -1
    	h_trigger_pos = t;

    	if (t > 1.0){
    		t = 1.0;
    	} else if(t < -1.0){
    		t = -1.0;
    	}

   		final double width_half = width/2.0;
		h_trigger_pos_px = width_half + (width_half * t);

		HTText = ht_text;
		invalidate();
    }

    // The bottom is 0, the top is 1
    public void setVTriggerPos(double t,String vt_voltage_text){
    	v_trigger_pos = t;

    	if (t<0) {
    		t = 0;
    	} else if(t>1.0){
    		t = 1.0;
    	}

    	v_trigger_pos_px = height - t*height;
    	VTVoltageText = vt_voltage_text;
    	invalidate();
    }

    // The bottom is 0, the top is 1
    public void setBiasPos(double t,String bias_voltage_text){
    	bias_pos = t;

    	if (t<0) {
    		t = 0;
    	} else if(t>1.0){
    		t = 1.0;
    	}

        bias_pos_px = height - t*height;
        BiasVoltageText = bias_voltage_text;
        invalidate();
    }

    public void setWave(double []samples,int sampleLen){
    	if((samples != null) && (samples.length > 0)){	// Error check
    		data = samples.clone();
    		isNewWaveSet = true;
    		sampleLength = sampleLen;

    		invalidate();
    	}
    }

    public void drawVerticalLine(){
    	vt_indicator_move = 1.0;
    	isDrawVerticalLine = true;
    }

    public void drawHorizontalLine(){
    	isDrawHorizontalLine = true;
    }

    public void drawBiasLine(){
    	bias_indicator_move = 1.0;
    	vt_indicator_move = 0;
    	isDrawBiasLine = true;
    }

    public void setFlashText(String s){
    	gradiation_flash = 1.0;
    	flashText = s;

    	new Timer().scheduleAtFixedRate(new TimerTask(){
    		public void run(){

    			if(gradiation_flash <= 0.05){
    		    	cancel();
    		    	gradiation_flash = 0;
    			} else {
    				gradiation_flash = gradiation_flash - 0.05;
    			}

    			mHandler.post(new Runnable(){
    				public void run(){
    					invalidate();
    				}
    			});
    		}
    	} , 0,60L);
    }

    public void unDrawLine(){
    	// for fadeout text
    	new Timer().scheduleAtFixedRate(new TimerTask(){
    		public void run(){

    			if(gradiation <= 0.1){
    		    	isDrawVerticalLine = false;
    		    	isDrawHorizontalLine = false;
    		    	isDrawBiasLine = false;
    		    	cancel();
    		    	gradiation = 1.0;
    			} else {
    				gradiation = gradiation - 0.1;
    			}

    			mHandler.post(new Runnable(){
    				public void run(){
    					invalidate();
    				}
    			});
    		}
    	} , 0,50L);

    	// indicator move
    	if(isDrawBiasLine && (bias_pos > 1.0 || bias_pos < 0 )){
	    	new Timer().scheduleAtFixedRate(new TimerTask(){
	    		public void run(){

	    			if(bias_indicator_move <= 0.1){
	    				bias_indicator_move = 0;
	    		    	cancel();
	    			} else {
	    				bias_indicator_move = bias_indicator_move - 0.1;
	    			}

	    			mHandler.post(new Runnable(){
	    				public void run(){
	    					invalidate();
	    				}
	    			});
	    		}
	    	} , 0,7L);
    	}

    	// indicator move
    	if(isDrawVerticalLine && (v_trigger_pos > 1.0 || v_trigger_pos < 0 )){
	    	new Timer().scheduleAtFixedRate(new TimerTask(){
	    		public void run(){

	    			if(vt_indicator_move <= 0.1){
	    				vt_indicator_move = 0;
	    		    	cancel();
	    			} else {
	    				vt_indicator_move = vt_indicator_move - 0.1;
	    			}

	    			mHandler.post(new Runnable(){
	    				public void run(){
	    					invalidate();
	    				}
	    			});
	    		}
	    	} , 0,7L);
    	}
    }

	@Override
	protected void onMeasure(int w, int h ){
	        super.onMeasure(w,h);
	}

	// The bottom is 0 ATop is 1
	private void drawWaveform(Canvas c){

		// error check
		if(width <= 0 || data.length <= 0)return;

		final double[] sample = data;		// In order not to be changed during drawing
		isNewWaveSet = false;

		double deltaX;
		double deltaI;
		int numDrawLines;	//  Number of drawing times

		if(width > sampleLength){		// Width of graph > Number of samples (high dot density)
			// Number of drawing times is the number of samples -1
			numDrawLines = sampleLength-1;
			deltaX =  width / (double) numDrawLines;
			deltaI = 1.0;

		} else {						// Width of graph < Number of samples (low dot density)
			// The number of drawing times is the number of pixels of the graph width - 1
			numDrawLines = ((int)width)-1;
			deltaX = 1.0;
			deltaI = (double) sampleLength / (double) numDrawLines;
		}

		float[] wave = new float[numDrawLines*4];
		wave[0] = 0f; 							// start x0 = 0
		wave[1] = (float)(height * (1.0 - sample[0]));	// start y0

		double fi = deltaI;		// float‚Ìindex

		// Set the coordinates
		final int loop_stop = wave.length-4;
		for(int j = 2;; j += 4) {
			wave[j] = (float) (wave[j-2] + deltaX);						// stop x
			wave[j+1] = (float) (height * (1.0 - sample[(int)fi]));	// stop y

			if(j > loop_stop)break;

			wave[j+2] = wave[j];								// start x
			wave[j+3] = wave[j+1];								// start y
			fi += deltaI;
		}


		c.drawLines(wave,p_wave);

		FrameRate++;
	}

	private void drawTriggerLine(Canvas c){
		if(isDrawVerticalLine){

			// fadeout
			int tmp = (int)(gradiation * (255*0.6));
			p_VTText.setAlpha(tmp);
			p_VTriggerLine.setAlpha(tmp);
			p_VTArea.setAlpha((int)(gradiation * (255*0.20)));

			c.drawRect((float)(width - width*0.15), 0, (float) width, (float)height, p_VTArea);	// display touch area

			c.drawLine(0,(float) v_trigger_pos_px,(float) width,(float) v_trigger_pos_px,p_VTriggerLine);		 // Draw vertical trigger line

			// Draw vertical trigger voltage text
			float text_y_pos;
			if(v_trigger_pos_px > (height * 0.2)){
				text_y_pos = (float)(v_trigger_pos_px-(p_VTriggerLine.getStrokeWidth()));
			} else {
				text_y_pos = (float)(v_trigger_pos_px + TEXT_SIZE*height - p_BiasLine.getStrokeWidth());
			}

			c.drawText(VTVoltageText, (float)(width*0.3), text_y_pos, p_VTText);

		} else if(isDrawHorizontalLine){

			int tmp = (int)(gradiation * (255*0.5));
			p_HTText.setAlpha(tmp);
			p_HTriggerLine.setAlpha(tmp);
			p_HTArea.setAlpha((int)(gradiation * (255*0.08)));

			c.drawRect((float)(width*0.15), 0, (float)(width - width*0.15), (float)height, p_HTArea);	// display touch area
			c.drawLine((float) h_trigger_pos_px,0,(float) h_trigger_pos_px,(float) height,p_HTriggerLine);		// Draw horizontal trigger line

			// Draw horizontal trigger time text
			float text_x_pos;
			if(h_trigger_pos_px < width/2.0){
				text_x_pos = (float)(h_trigger_pos_px + width*0.02);
			} else {
				text_x_pos = (float)(h_trigger_pos_px - width/2.0 + width*0.05);
			}

			c.drawText(HTText, text_x_pos, (float)(TEXT_SIZE*height + height*0.05), p_HTText);

		} else if(isDrawBiasLine){

			int tmp = (int)(gradiation * (255*0.4));
			p_BiasText.setAlpha(tmp);
			p_BiasLine.setAlpha(tmp);
			p_BiasArea.setAlpha((int)(gradiation * (255*0.15)));
			c.drawRect(0, 0, (float)(width*0.15), (float)height, p_BiasArea);	// display touch area

			c.drawLine(0,(float) bias_pos_px,(float) width,(float) bias_pos_px,p_BiasLine);						// Draw bias(position) line

			// Draw bias voltage text
			float text_y_pos;
			if(bias_pos_px < (height * 0.8)){
				text_y_pos = (float)(bias_pos_px + TEXT_SIZE*height - p_BiasLine.getStrokeWidth());
			} else {
				text_y_pos = (float)(bias_pos_px-(p_BiasLine.getStrokeWidth()));
			}

			c.drawText(BiasVoltageText, (float)(width*0.15), text_y_pos, p_BiasText);
		}

		// Draw trigger marker bitmap
		c.drawBitmap(bm_pos_h, (float)(h_trigger_pos_px - (hpos_bitmap_width/2)), 0, null);
		c.drawBitmap(bm_pos_b, 0, (float) (bias_pos_px-(pos_bitmap_height/2)), null);
		c.drawBitmap(bm_pos_v, (float) (width-pos_bitmap_width), (float) (v_trigger_pos_px-(pos_bitmap_height/2)), null);

	}

	private void drawHTIndicator(Canvas c){		// Draw horizontal trigger indicator
		c.drawRect(0, 0, (float)width, (float)(height * 0.05), p_frame);		// draw frame

		double ex_ratio = USBOscilloscopeHost.HT_EXPAND_RATIO;
		c.drawRect((float)(width/2.0 - width/(ex_ratio * 2)),				// draw center
				0,
				(float)(width/2 + width/(2*ex_ratio)),
				(float)(height * 0.05),
				p_IndicatorDisplayArea);

		float x = (float) (width/2 + h_trigger_pos*(width/(ex_ratio * 2)));
		c.drawLine(x, 0, x, (float)(height * 0.05), p_IndicatorHTLine);
	}

	private void drawBiasIndicator(Canvas c){

		float ih = (float)(height*0.05);	// indicator height
		float sx = (float)(width*0.15*bias_indicator_move);		//  start x pos

		c.drawRect(sx, 0, sx + ih, (float)(height), p_frame);		// draw frame

		// wave display area
		double ex_ratio = USBOscilloscopeHost.BIAS_EXPAND_RATIO;
		c.drawRect((float)(sx),										// draw center
				(float)(height/2.0 - height/(ex_ratio * 2)),
				(float)(sx + ih),
				(float)(height/2 + height/(2*ex_ratio)),
				p_IndicatorDisplayArea);

		float y = (float) (height - ((bias_pos + (ex_ratio-1)/2) / ex_ratio)*height);
		c.drawLine(sx, y, sx+ih, y, p_IndicatorBiasLine);
	}

	private void drawVTIndicator(Canvas c){

		float ih = (float)(height*0.05);	// indicator height
		float sx;
		if(isDrawBiasLine){
			sx = (float)(width - ih);	// start x pos
		} else {
			sx = (float)(width - width*0.15*vt_indicator_move - ih);	// start x pos
		}

		c.drawRect(sx, 0, sx + ih, (float)(height), p_frame);			// draw frame

		// wave display area
		double ex_ratio = USBOscilloscopeHost.VT_POS_MAX;
		c.drawRect((float)(sx),											// draw center
				(float)(height/2.0 - height/(ex_ratio * 2)),
				(float)(sx + ih),
				(float)(height/2 + height/(2*ex_ratio)),
				p_IndicatorDisplayArea);

		float y = (float) (height - ((v_trigger_pos + (ex_ratio-1)/2) / ex_ratio)*height);

		float stroke_width = p_IndicatorVTLine.getStrokeWidth();
		if(y <= stroke_width){
			y = stroke_width;
		} else if(y>=height-stroke_width) {
			y = (float)(height-stroke_width);
		}
		c.drawLine(sx, y, sx+ih, y, p_IndicatorVTLine);
	}

	private void drawFlashText(Canvas c){

		Paint p = new Paint();

		// flash whole graph
		//p.setARGB((int)(gradiation_flash*15), 255, 255, 255);
		//c.drawRect(0,0,(float)width,(float)height,p);

		// draw text
		p.setAntiAlias(true);
		p.setARGB((int)(gradiation_flash*200), 255, 255, 255);
		p.setTextSize((float)(TEXT_SIZE*width*0.8));
		c.drawText(flashText, (float)(width*0.12), (float)(height*0.4),p);
	}


	public void onDraw(Canvas c){

		if(D)Log.d(TAG,"doDraw Start");
		if(c == null)return;

		//drawMatrix(c);
		if(matrix != null)c.drawBitmap(matrix, 0, 0, null);
		if(data != null && data.length>0)drawWaveform(c);
		drawTriggerLine(c);

		if(Math.abs(h_trigger_pos) > 1.0){
			drawHTIndicator(c);
		}

		if(bias_pos > 1.0 || bias_pos < 0 ){
			drawBiasIndicator(c);
		}

		if(v_trigger_pos > 1.0 || v_trigger_pos < 0){
			drawVTIndicator(c);
		}

		if(gradiation_flash > 0){
			drawFlashText(c);
		}

		//if(D)Log.d(TAG,"doDraw End");

	}


}
