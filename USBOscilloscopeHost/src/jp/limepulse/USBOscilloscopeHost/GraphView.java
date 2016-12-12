package jp.limepulse.USBOscilloscopeHost;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.util.Log;
import android.view.View;
import jp.limepulse.USBOscilloscopeHost.R;

public class GraphView extends View {
	private static final String TAG = "Graph";
	private static final boolean D = false;


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

	private int sampleLength;
	private double pos_bitmap_height,pos_bitmap_width,hpos_bitmap_height,hpos_bitmap_width;
	private Bitmap matrix = null;
	private Bitmap bm_pos_b = BitmapFactory.decodeResource(getResources(),R.drawable.pos_b);
	private Bitmap bm_pos_v = BitmapFactory.decodeResource(getResources(),R.drawable.pos_v);
	private Bitmap bm_pos_h = BitmapFactory.decodeResource(getResources(),R.drawable.pos_h);

	private double[] data = null;	//waveform data

	private double h_trigger_pos = 0;
	private double bias_pos = 0;
	private double v_trigger_pos = USBOscilloscopeHost.SAMPLE_MAX/2;

	private boolean isNewWaveSet;
	private boolean running;

	private boolean isDrawVerticalLine = false;
	private boolean isDrawHorizontalLine = false;
	private boolean isDrawBiasLine = false;



    public GraphView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Paint initialize
        p_wave.setColor(Color.argb(255, 255, 0, 0));
        p_frame.setColor(Color.argb(255, 128, 128, 128));
        p_frame.setStyle(Paint.Style.STROKE);		// Do not fill the frame
        p_matrix.setColor(Color.argb(255, 60, 60, 60));
        p_HTriggerLine.setColor(Color.argb(100, 0, 255, 0));
        p_BiasLine.setColor(Color.argb(100, 0, 255, 255));
        p_VTriggerLine.setColor(Color.argb(100, 255, 0, 0));

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
		float wave_stroke_width = (float)((int)width/500);
		p_wave.setStrokeWidth(wave_stroke_width);
		if(width>800){
			p_wave.setAntiAlias(false);
		} else {
			p_wave.setAntiAlias(true);
		}


        p_HTriggerLine.setStrokeWidth(line_stroke_width);
        p_BiasLine.setStrokeWidth(line_stroke_width);
        p_VTriggerLine.setStrokeWidth(line_stroke_width);

        p_matrix.setStrokeWidth((float)(width/600));

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

		c.drawRect(0, 0, (float)(width-1), (float)(h-1), p_frame);	// ògï`âÊ
//		c.drawLine((float) widthCenter,0,(float) widthCenter,(float)height, p_frame);		// íÜêSê¸ï`âÊ
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

	private void drawTriggerLine(Canvas c){
		if(isDrawVerticalLine){
			c.drawLine(0,(float) v_trigger_pos,(float) width,(float) v_trigger_pos,p_VTriggerLine);		 // Draw vertical trigger line
		} else if(isDrawHorizontalLine){
			c.drawLine((float) h_trigger_pos,0,(float) h_trigger_pos,(float) height,p_HTriggerLine);		// Draw horizontal trigger line
		} else if(isDrawBiasLine){
			c.drawLine(0,(float) bias_pos,(float) width,(float) bias_pos,p_BiasLine);						// Draw bias(position) line
		}

		// Draw trigger marker bitmap
		c.drawBitmap(bm_pos_h, (float)(h_trigger_pos - (hpos_bitmap_width/2)), 0, null);
		c.drawBitmap(bm_pos_b, 0, (float) (bias_pos-(pos_bitmap_height/2)), null);
		c.drawBitmap(bm_pos_v, (float) (width-pos_bitmap_width), (float) (v_trigger_pos-(pos_bitmap_height/2)), null);

	}

	// The bottom is 0 ÅATop is 1
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

		double fi = deltaI;		// floatÇÃindex

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
	}

	public void endThread() {
		running = false;
	}


    public int frameRateGetAndReset(){
    	int tmp = FrameRate;
    	FrameRate = 0;
    	return tmp;
    }

    //The right end is 1,Center is 0,The left end is -1
    public void setHTriggerPos(double t){
    	if (t > 1.0){
    		t = 1.0;
    	} else if(t < -1.0){
    		t = -1.0;
    	}

   		final double width_half = width/2.0;
		h_trigger_pos = width_half + (width_half * t);

		invalidate();
    }

    // The bottom is 0, the top is 1
    public void setVTriggerPos(double t){
    	if (t<0) {
    		t = 0;
    	} else if(t>1.0){
    		t = 1.0;
    	}

    	v_trigger_pos = height - t*height;
    	invalidate();
    }

    // The bottom is 0, the top is 1
    public void setBiasPos(double t){
    	if (t<0) {
    		t = 0;
    	} else if(t>1.0){
    		t = 1.0;
    	}

        bias_pos = height - t*height;
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
    	isDrawVerticalLine = true;
    }

    public void drawHorizontalLine(){
    	isDrawHorizontalLine = true;
    }

    public void drawBiasLine(){
    	isDrawBiasLine = true;
    }

    public void unDrawLine(){
    	isDrawVerticalLine = false;
    	isDrawHorizontalLine = false;
    	isDrawBiasLine = false;
    }

	@Override
	protected void onMeasure(int w, int h ){
	        super.onMeasure(w,h);
	}

	public void onDraw(Canvas c){

		if(D)Log.d(TAG,"doDraw Start");
		if(c == null)return;

		//drawMatrix(c);
		if(matrix != null)c.drawBitmap(matrix, 0, 0, null);
		if(data != null && data.length>0)drawWaveform(c);
		drawTriggerLine(c);


		FrameRate++;
		//if(D)Log.d(TAG,"doDraw End");

	}


}
