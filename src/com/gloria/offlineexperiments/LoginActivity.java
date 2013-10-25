/**
 * @author Mario Velasco Casquero
 * @version 2.00
 */

package com.gloria.offlineexperiments;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;

import com.gloria.offlineexperiments.ui.fonts.TypefaceManager;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class LoginActivity extends Activity {

	private String username = "";
	private String password = "";
	private String authorizationToken = "";
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		Typeface typeface = TypefaceManager.INSTANCE.getTypeface(getApplicationContext(), TypefaceManager.VERDANA);
		TypefaceManager.INSTANCE.applyTypefaceToAllViews(this, typeface);
		
		ImageView infoButton = (ImageView)findViewById(R.id.infoButton);
		infoButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				launchInfo();
			}
		}); 
		
	}
	
	
	@Override 
	protected void onResume(){
		super.onResume();
		loadUser();	
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.login, menu);
		return true;
	}

	public void launchInfo() {
		Intent i = new Intent(this, InfoActivity.class);
		startActivity(i);
	}

	public void authenticate(View view) {
		EditText usernameText = (EditText) findViewById(R.id.username);
		this.username = usernameText.getText().toString();
		EditText passwordText = (EditText) findViewById(R.id.password);
		this.password = passwordText.getText().toString();
		CheckBox checkbox = (CheckBox) findViewById(R.id.rememberMe);
		if (checkbox.isChecked())
			saveUser();
		new Authentication().execute(); 
	}

	
	private void saveUser(){
		SharedPreferences settings = getSharedPreferences("LoginGLORIA", MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("username", this.username);
		editor.putString("password", this.password);
		editor.commit();
	}
	
	private void loadUser(){
		SharedPreferences settings = getSharedPreferences("LoginGLORIA", MODE_PRIVATE);
		String username = settings.getString("username", "");
		EditText usernameText = (EditText) findViewById(R.id.username);
		usernameText.setText(username);
		String password = settings.getString("password", "");
		EditText passwordText = (EditText) findViewById(R.id.password);
		passwordText.setText(password);
	}
	
	
	private class Authentication extends AsyncTask<Void, Void, Boolean> {
		private ProgressDialog progressDialog;
		private String errorResponse = getString(R.string.defaultErrorMsg);

		
		// This function is called at the beginning, before doInBackground
		@Override
		protected void onPreExecute() {
			progressDialog = new ProgressDialog(LoginActivity.this);
			progressDialog.setMessage(getString(R.string.authenticatingMsg));
			progressDialog.show();
		}
		
		
		@Override
		protected Boolean doInBackground(Void... voids) {
			boolean response = false;
							
			disableConnectionReuseIfNecessary();
			HttpClient httpClient = null;
			try {
				// create connection
				httpClient = getNewHttpClient();
				HttpGet getRequest =  new HttpGet("https://venus.datsi.fi.upm.es:8443/GLORIAAPI/experiments/offline/list");
				getRequest.setHeader("content-type", "application/json");
				String accessToken = username + ":" + password;
				byte[] accessTokenBytes = accessToken.getBytes("iso-8859-1");
				authorizationToken = Base64.encodeBase64String(accessTokenBytes);
				getRequest.setHeader("Authorization", "Basic " + authorizationToken);
				Log.d("DEBUG", "Authorization: " + getRequest.getFirstHeader("Authorization").getValue());
				Log.d("DEBUG", "opnening connection");
				HttpResponse resp = httpClient.execute(getRequest);
				int statusCode = resp.getStatusLine().getStatusCode();
				Log.d("DEBUG", "status code: " + statusCode);

				// handle issues
				if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
					Log.d("DEBUG", "Invalid user or password");
					errorResponse = getString(R.string.wrongLoginMsg);
					return response;
				} else if (statusCode != HttpURLConnection.HTTP_OK) {
					// handle any other errors, like 404, 500,..
					Log.d("DEBUG", "Unknown error");
					errorResponse = getString(R.string.defaultErrorMsg);
					return response;
				}

				// Get response
				String respStr = EntityUtils.toString(resp.getEntity());
				Log.d("DEBUG", "response: " + respStr);
				JSONArray jsonArray = new JSONArray(respStr);
				String experiment = "";
				int i = 0;
				while (!response && i < jsonArray.length()) {
					experiment = jsonArray.optString(i);
					if (experiment.compareTo("WOLF") == 0)
						response = true;
					else
						i++;
				}
				Log.d("DEBUG", "valid username, experiment: " + experiment);

			/*}catch (UnknownHostException e){
					errorResponse = getString(R.string.noInternetMsg);
					} catch (Exception exception) {
				Log.d("DEBUG","AUTHENTICATEUSER EXC - "+exception.toString());
				Log.d("DEBUG",httpsConnection.requestDump);
				errorResponse = getString(R.string.noAccessMsg) + exception.toString();*/
			} catch (MalformedURLException e) {
				Log.d("DEBUG", "URL is invalid");
			} catch (SocketTimeoutException e) {
				Log.d("DEBUG", "data retrieval or connection timed out");
			} catch (IOException e) {
				Log.d("DEBUG", "IO exception");
				errorResponse = getString(R.string.noInternetMsg);
				// could not read response body 
				// (could not create input stream)
			} catch (JSONException e) {
				Log.d("DEBUG", "JSON exception"); // response body is no valid JSON string
			} finally {
				if (httpClient != null) {
					httpClient.getConnectionManager().shutdown();
				}
			}       

			publishProgress();
			return response;
		}
		

		// This function is called when doInBackground is done
		@Override
		protected void onPostExecute(Boolean authenticated){
			if(progressDialog.isShowing()) {
				progressDialog.cancel();
			}
			
			if (authenticated) {
				Intent intent = new Intent(LoginActivity.this, ExperimentsActivity.class);
				intent.putExtra("authorizationToken", authorizationToken);
				
				intent.putExtra("username", username);
				try {
					String sha1Password = sha1(password);
					intent.putExtra("password", sha1Password);
					//intent.putExtra("password", password);
				} catch (Exception e) {}
				
				startActivity(intent);
			}
			else {
				Toast.makeText(LoginActivity.this, errorResponse, Toast.LENGTH_LONG).show();
			}
		}
		
		private HttpClient getNewHttpClient() {
			try {
				KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
				trustStore.load(null, null);

				SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
				sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

				HttpParams params = new BasicHttpParams();
				HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
				HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

				SchemeRegistry registry = new SchemeRegistry();
				registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
				registry.register(new Scheme("https", sf, 443));

				ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

				return new DefaultHttpClient(ccm, params);
			} catch (Exception e) {
				return new DefaultHttpClient();
			}
		}



		/**
		 * required in order to prevent issues in earlier Android version.
		 */
		private void disableConnectionReuseIfNecessary() {
			// see HttpURLConnection API doc
			if (Integer.parseInt(Build.VERSION.SDK) 
					< Build.VERSION_CODES.FROYO) {
				System.setProperty("http.keepAlive", "false");
			}
		}
		
		private String sha1(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			md.update(text.getBytes("iso-8859-1"), 0, text.length());
			byte[] sha1hash = md.digest();
			return Base64.encodeBase64String(sha1hash);
		}
	}
	
}
