package com.game.colibri;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class ColiBrain extends Drawable {

	private Bitmap frame, content, progressContent;
	private Paint textPaint, contentPaint;
	private String text;
	private float verticalOffset;
	
	public ColiBrain(Context context, String text, float progress) {
		int density = context.getResources().getDisplayMetrics().densityDpi;
		frame = ((BitmapDrawable) context.getResources().getDrawable(R.drawable.cerveau)).getBitmap();
		frame = Bitmap.createScaledBitmap(frame, frame.getWidth()*density/frame.getDensity(), frame.getHeight()*density/frame.getDensity(), true);
		content = ((BitmapDrawable) context.getResources().getDrawable(R.drawable.cerveau_contenu)).getBitmap();
		content = Bitmap.createScaledBitmap(content, content.getWidth()*density/content.getDensity(), content.getHeight()*density/content.getDensity(), true);
		contentPaint = new Paint();
		contentPaint.setAlpha(160);
		textPaint = new Paint();
		textPaint.setColor(0xffd00707);
		textPaint.setTextSize(frame.getWidth()*0.34f);
		textPaint.setTextAlign(Paint.Align.CENTER);
		textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
		textPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		setText(text);
		setProgress(progress);
		setBounds(0, 0, frame.getWidth(), frame.getHeight());
	}
	
	/**
	 * Définit le texte (nombre de colibrains) à afficher dans le cerveau.
	 * @param text nombre de colibrains ou autre caractère
	 */
	public ColiBrain setText(String text) {
		this.text = text;
		invalidateSelf();
		return this;
	}
	
	/**
	 * Définit le niveau de remplissage du ColiBrain.
	 * @param progress entre 0 et 1
	 */
	public ColiBrain setProgress(float progress) {
		verticalOffset = content.getHeight()*(0.16f+0.68f*(1-progress));
		progressContent = Bitmap.createBitmap(content, 0, (int)verticalOffset, content.getWidth(), (int)(content.getHeight()-verticalOffset));
		invalidateSelf();
		return this;
	}
	
	@Override
	public void draw(Canvas can) {
		can.drawBitmap(progressContent, 0, verticalOffset, contentPaint);
		can.drawBitmap(frame, 0, 0, null);
		can.drawText(text, frame.getWidth()*0.5f, frame.getHeight()*0.58f, textPaint);
	}

	@Override
	public int getIntrinsicWidth() {
		return frame.getWidth();
	}
	
	@Override
	public int getIntrinsicHeight() {
		return frame.getHeight();
	}
	
	@Override
	public int getOpacity() {
		return PixelFormat.OPAQUE;
	}

	@Override
	public void setAlpha(int alpha) {}

	@Override
	public void setColorFilter(ColorFilter cf) {}

}
