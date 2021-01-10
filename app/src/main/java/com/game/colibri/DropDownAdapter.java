package com.game.colibri;

import static com.network.colibri.CommonUtilities.SERVER_URL;
import static com.network.colibri.CommonUtilities.SSL_SOCKET_FACTORY;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;
import com.loopj.android.http.TextHttpResponseHandler;

import cz.msebera.android.httpclient.Header;

public class DropDownAdapter extends ArrayAdapter<DropDownAdapter.NameAndId> {
	
	private SyncHttpClient client;
	private int user;
	private List<Joueur> joueurs;
	
	public class NameAndId {
		public int id;
		public String name;
		
		public NameAndId(JSONObject j) throws JSONException {
			id = j.getInt("id");
			name = j.getString("pseudo");
		}
		
		@Override
		public String toString() {
			return name;
		}
	}
	
	public DropDownAdapter(Context context, int resource, int user, List<Joueur> joueurs) {
		super(context, resource, new ArrayList<NameAndId>());
		client = new SyncHttpClient();
		client.setSSLSocketFactory(SSL_SOCKET_FACTORY);
		this.user = joueurs.isEmpty() ? user : 0; // Si joueurs est non vide, on est dans ModifDefi et user est inclu dans les joueurs
		this.joueurs = joueurs;
	}
	
	private boolean dejaPris(int id) {
		if(id==user)
			return true;
		for(Joueur j : joueurs) {
			if(id==j.getId())
				return true;
		}
		return false;
	}
	
	@Override
	public Filter getFilter() {
		return new Filter() {
			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) {
				if(results==null || results.values==null)
					return;
				try {
					JSONArray sug = new JSONArray((String) results.values);
			        clear();
			        for(int i=0; i<sug.length(); i++) {
			        	NameAndId j = new NameAndId(sug.getJSONObject(i));
			        	if(!dejaPris(j.id)) {
			        		add(j);
			        	}
			        }
					DropDownAdapter.this.notifyDataSetChanged();
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				if(constraint==null)
					return null;
				final FilterResults res = new FilterResults();
				final RequestParams params = new RequestParams();
				params.put("entree", (String)constraint);
				client.post(SERVER_URL + "/suggestions.php", params, new TextHttpResponseHandler() {
					@Override
					public void onSuccess(int statusCode, Header[] headers, String responseString) {
						res.values = responseString;
					}
					@Override
					public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {}
				});
				return res;
			}
		};
	}
	
}
