package com.game.colibri;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;

public class SuggestionsEditText extends AutoCompleteTextView {

    private static final int MESSAGE_TEXT_CHANGED = 100;
    private static final int AUTOCOMPLETE_DELAY = 750;
    private ProgressBar mLoadingIndicator;
    private final FilterHandler filterHandler = new FilterHandler(this);
    
	private static class FilterHandler extends Handler {
		
		private final WeakReference<SuggestionsEditText> act;
		
		public FilterHandler(SuggestionsEditText a) {
			act = new WeakReference<SuggestionsEditText>(a);
		}
		
        @Override
        public void handleMessage(Message msg) {
        	if(act.get()!=null)
        		act.get().filter((CharSequence) msg.obj, msg.arg1);
        }
    };

    public SuggestionsEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setLoadingIndicator(ProgressBar progressBar) {
        mLoadingIndicator = progressBar;
    }
    
    /**
     * Effectue le vrai filtrage (la requête) en appelant la fonction parente.
     * @param text
     * @param keyCode
     */
    private void filter(CharSequence text, int keyCode) {
    	super.performFiltering(text, keyCode);
    }
    
    /**
     * Appelée lorsque l'on entre des caractères dans l'EditText.
     * Appelle performFiltering de super (qui fait le vrai filtrage, i.e. la requête)
     * après un délai, par le Handler qui appelle filter.
     */
    @Override
    protected void performFiltering(CharSequence text, int keyCode) {
        if (mLoadingIndicator != null) {
            mLoadingIndicator.setVisibility(View.VISIBLE);
        }
        filterHandler.removeMessages(MESSAGE_TEXT_CHANGED);
        filterHandler.sendMessageDelayed(filterHandler.obtainMessage(MESSAGE_TEXT_CHANGED, text), AUTOCOMPLETE_DELAY);
    }
    
    @Override
    public void onFilterComplete(int count) {
        if (mLoadingIndicator != null) {
            mLoadingIndicator.setVisibility(View.GONE);
        }
    	if(getAdapter().getCount()==0)
    		setTextColor(getContext().getResources().getColor(R.color.red));
    	else
    		setTextColor(getContext().getResources().getColor(R.color.noir));
        super.onFilterComplete(count);
    }

}
