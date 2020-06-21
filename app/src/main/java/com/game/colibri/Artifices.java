package com.game.colibri;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.Random;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;

public class Artifices extends RelativeLayout {
	
	private static final int NEW_EXPLO=1;
	private static final int MAX_DELAY_MS = 1500;
	private static final int PERIODE = 1000/25;
	private static final int[] images = new int[] {R.drawable.doge, R.drawable.doge_wow, R.drawable.doge_skills, R.drawable.doge_smart, R.drawable.doge_lunettes, R.drawable.such_talent, R.drawable.megusta};
	private static final int[] anims = new int[] {R.anim.artifice_anim, R.anim.artifice_anim, R.anim.artifice_anim_spin};
	
	private int ww,wh;
	private LinkedList<Explosion> expl;
	private Random ran;
	private RefreshHandler handler;
	private Paint p;
	
	public boolean doge=false;
	
	public Artifices(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public Artifices(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
	private void init() {
		expl = new LinkedList<Explosion>();
		ran = new Random();
		handler = new RefreshHandler(this);
		p = new Paint();
		setLayerType(View.LAYER_TYPE_SOFTWARE, null);
	}
	
	public void start() {
		handler.sleep(PERIODE);
		handler.sendMessageDelayed(handler.obtainMessage(NEW_EXPLO), 0);
	}
	
	public void pause() {
		handler.removeMessages(0);
		handler.removeMessages(NEW_EXPLO);
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		ww = w;
		wh = h;
		Particule.ww = w;
		Particule.wh = h;
		Particule.V_MAX = ww/60f;
		Particule.ACC = ww/1200f;
		start();
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(event.getAction()==MotionEvent.ACTION_DOWN)
			newExplo(event.getX(), event.getY());
		performClick();
		return false;
	}
	
	@Override
	public boolean performClick() {
		return super.performClick();
	}
	
	@Override
    protected void onDraw(Canvas can) {
		for(Explosion e : expl) {
			p.setMaskFilter(e.blur);
			for(Particule pa : e.p) {
				p.setColor(Color.rgb(e.red+ran.nextInt(51), e.green+ran.nextInt(51), e.blue+ran.nextInt(51)));
				p.setStrokeWidth(e.stroke+1.5f*ran.nextFloat());
				can.drawLine(pa.x, pa.y, pa.x+pa.vx, pa.y+pa.vy, p);
			}
		}
	}
	
	@Override
	protected void onDetachedFromWindow() {
		pause();
		super.onDetachedFromWindow();
	}
	
	private void move() {
		if(!expl.isEmpty()) {
			if(expl.getFirst().moveBool()) {
				View dog = expl.removeFirst().dog;
				if(dog!=null)
					removeView(dog);
			}
		}
		for(int i=1; i<expl.size(); i++) {
			expl.get(i).move();
		}
		invalidate();
		handler.sleep(PERIODE);
	}
	
	private void newExplo() {
		newExplo(ran.nextInt(ww), ran.nextInt(wh));
		handler.sendMessageDelayed(handler.obtainMessage(NEW_EXPLO), ran.nextInt(MAX_DELAY_MS));
	}
	
	private void newExplo(float x, float y) {
		Explosion e = new Explosion(x,y,ran); 
		expl.add(e);
		if(doge && ran.nextFloat()<0.7) {
			int img = images[ran.nextInt(images.length)];
			View dog = new View(getContext());
			dog.setBackgroundResource(img);
			dog.setVisibility(View.INVISIBLE);
			int d = ww*e.p.length/(4*Explosion.N_PART_MAX);
			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(d, d);
			params.leftMargin = (int) x-d/2;
			params.topMargin = (int) y-d/2;
			params.rightMargin = -80;
			params.bottomMargin = -80;
			addView(dog, params);
			dog.startAnimation(AnimationUtils.loadAnimation(getContext(), anims[ran.nextInt(anims.length)]));
			e.dog = dog;
		}
	}
	
	private static class RefreshHandler extends Handler {
		
		private final WeakReference<Artifices> act;
		
		public RefreshHandler(Artifices a) {
			act = new WeakReference<Artifices>(a);
		}
		
		@Override
		public void handleMessage(Message msg) {
			if(act.get()==null)
				return;
			if(msg.what==NEW_EXPLO) // Fin de l'animation de l'explosion. (on utilise le handler pour contourner l'abscence de listener pour AnimationDrawable)
				act.get().newExplo();
			else
				act.get().move();
		}
		public void sleep(long delayMillis) {
			this.removeMessages(0);
			sendMessageDelayed(obtainMessage(0), delayMillis);
		}
	}
	
	static private class Explosion {
		
		public static final int N_PART_MAX=80;
		
		public int red,green,blue;
		public float stroke;
		public BlurMaskFilter blur;
		public Particule[] p;
		public View dog=null;
		
		public Explosion(float x, float y, Random r) {
			red = r.nextInt(206);
			green = r.nextInt(206);
			blue = r.nextInt(206);
			stroke = 1+r.nextFloat()*5;
			blur = new BlurMaskFilter(1+r.nextInt(8), BlurMaskFilter.Blur.NORMAL);
			p = new Particule[15+r.nextInt(N_PART_MAX-15)];
			for(int i=0; i<p.length; i++) {
				p[i] = new Particule(x, y, r);
			}
		}
		
		public void move() {
			for(Particule pa : p) {
				pa.move();
			}
		}
		
		public boolean moveBool() {
			boolean dehors = true;
			for(Particule pa : p) {
				dehors&=pa.moveBool();
			}
			return dehors;
		}
		
	}
	
	static private class Particule {
		
		public static float V_MAX;
		public static float ACC;
		public static int ww,wh;
		
		public float x,y,vx,vy;
		
		public Particule(float x, float y, Random r) {
			this.x = x;
			this.y = y;
			float v = (2*r.nextFloat()-1)*V_MAX;
			float a = 2*r.nextFloat()*(float)Math.PI;
			this.vx = v*(float)Math.cos(a);
			this.vy = v*(float)Math.sin(a);
		}
		
		public void move() {
			vy += ACC;
			x += vx;
			y += vy;
		}
		
		public boolean moveBool() {
			move();
			return x<0 || x>ww || y>wh;
		}
	}
}
