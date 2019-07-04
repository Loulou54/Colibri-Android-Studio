package com.game.colibri;

import static com.network.colibri.CommonUtilities.SERVER_URL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;

public class DropDownAdapter extends ArrayAdapter<DropDownAdapter.NameAndId> {
	
	private HttpClient client;
	private HttpPost post;
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
		HttpParams params = new BasicHttpParams();
		params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
		client = new DefaultHttpClient();
		post = new HttpPost(SERVER_URL+"/suggestions.php");
		this.user = user;
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
		Filter filtre = new Filter() {
			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) {
				if(results==null)
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
				if(constraint!=null) {
					try {
				        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
				        nameValuePairs.add(new BasicNameValuePair("entree", (String) constraint));
				        post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				        HttpResponse response = client.execute(post);
				        String jsonRe = EntityUtils.toString(response.getEntity(), "UTF-8");
				        FilterResults res = new FilterResults();
				        res.values = jsonRe;
				        return res;
				    } catch (ClientProtocolException e) {
				        e.printStackTrace();
				    } catch (IOException e) {
				        e.printStackTrace();
				    }
				}
				return null;
			}
		};
		return filtre;
	}
	
}
