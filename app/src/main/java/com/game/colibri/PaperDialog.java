package com.game.colibri;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PaperDialog extends Dialog {
	
	private Typeface font;
	private int buttonsPadding;
	private int normalPadding;
	private View contentView = null;
	
	public PaperDialog(Context context) {
		super(context);
		initPaperDialog(context, 0, false);
	}
	
	public PaperDialog(Context context, int layoutResID) {
		super(context);
		initPaperDialog(context, layoutResID, false);
	}
	
	public PaperDialog(Context context, int layoutResID, boolean expand) {
		super(context);
		initPaperDialog(context, layoutResID, expand);
	}
	
	private void initPaperDialog(Context context, int layoutResID, boolean expand) {
		font = Typeface.createFromAsset(context.getAssets(), "fonts/Passing Notes.ttf");
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
		LinearLayout root = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.paper_dialog_layout, new LinearLayout(context));
		if(layoutResID!=0){
			ViewGroup parent = (ViewGroup) LayoutInflater.from(context).inflate(layoutResID, (ViewGroup)root.findViewById(expand ? R.id.windowPaperDialog : R.id.contentPaperDialog));
			contentView = parent.getChildAt(parent.getChildCount()-1);
		}
		setContentView(root);
		ViewGroup window = (ViewGroup) root.findViewById(R.id.windowPaperDialog);
		if(expand) {
			ViewGroup.LayoutParams params = window.getLayoutParams();
			params.width = ViewGroup.LayoutParams.MATCH_PARENT;
			window.setLayoutParams(params);
		}
		buttonsPadding = window.getPaddingBottom();
		normalPadding = window.getPaddingTop();
		window.setPadding(normalPadding, normalPadding, normalPadding, normalPadding);
	}

	public Typeface getFont() {
		return font;
	}
	
	public View getContentView() {
		return contentView;
	}
	
	@Override
	public void setTitle(CharSequence title) {
		TextView text = (TextView) findViewById(R.id.titrePaperDialog);
		text.setTypeface(font);
		text.setText(title);
		text.setVisibility(View.VISIBLE);
	}
	
	@Override
	public void setTitle(int titleId) {
		TextView text = (TextView) findViewById(R.id.titrePaperDialog);
		text.setTypeface(font);
		text.setText(titleId);
		text.setVisibility(View.VISIBLE);
	}
	
	public void setMessage(int msgId) {
		TextView msg = (TextView) findViewById(R.id.msgPaperDialog);
		msg.setTypeface(font);
		msg.setText(msgId);
		msg.setVisibility(View.VISIBLE);
	}

	public void setMessage(String msgText) {
		TextView msg = (TextView) findViewById(R.id.msgPaperDialog);
		msg.setTypeface(font);
		msg.setText(msgText);
		msg.setVisibility(View.VISIBLE);
	}
	
	public void setPositiveButton(View.OnClickListener clickAction, String label) {
		setButton(R.id.positivePaperDialog, clickAction, label);
	}
	
	public void setNeutralButton(View.OnClickListener clickAction, String label) {
		setButton(R.id.neutralPaperDialog, clickAction, label);
	}
	
	public void setNegativeButton(View.OnClickListener clickAction, String label) {
		setButton(R.id.negativePaperDialog, clickAction, label);
	}
	
	private void setButton(int buttonId, View.OnClickListener clickAction, String label) {
		Button but = (Button) findViewById(buttonId);
		but.setTypeface(font);
		but.setOnClickListener(clickAction);
		if(label!=null) {
			but.setText(label);
		}
		but.setVisibility(View.VISIBLE);
		findViewById(R.id.buttonsPaperDialog).setVisibility(View.VISIBLE);
		findViewById(R.id.windowPaperDialog).setPadding(normalPadding, normalPadding, normalPadding, buttonsPadding);
	}
}
