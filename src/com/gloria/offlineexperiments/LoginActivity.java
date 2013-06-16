package com.gloria.offlineexperiments;

import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpsTransportSE;
import org.kxml2.kdom.Element;
import org.kxml2.kdom.Node;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class LoginActivity extends Activity {

	private static final String NAMESPACE = "http://user.repository.services.gs.gloria.eu/";
	private static String HOST="saturno.datsi.fi.upm.es";
	private static int PORT=8443; 
	private static String WSDL_LOCATION="/GLORIA/services/UserRepositoryPort?wsdl";
	private static int TIMEOUT=5000;
	private static String METHOD_NAME = "authenticateUser";
	private static final String SOAP_ACTION =  "";

	private static TrustManager[] trustManagers;
	private String username = "";
	private String password = "";
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final boolean customTitleSupported = requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_login);
		if ( customTitleSupported ) {
			getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.titlebar);
		}

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
							
			SoapObject bodyRequest = buildBodyRequest();
			Element[] header = buildHeader();
			SoapSerializationEnvelope envelope = buildRequest(header, bodyRequest);

			Log.d("DEBUG",envelope.bodyOut.toString());
			allowAllSSL();

			System.setProperty("http.keepAlive", "false");
			HttpsTransportSE httpsConnection = new HttpsTransportSE(HOST,PORT, WSDL_LOCATION, TIMEOUT);
			httpsConnection.debug = true;

			try {
				httpsConnection.call(SOAP_ACTION, envelope);
				SoapObject responseSOAP = (SoapObject) envelope.bodyIn;
				String resultValue = responseSOAP.getProperty(0).toString();
				if (resultValue.equals("true")) {
					Log.d("DEBUG","AUTHENTICATEUSER RES- user authenticated");
					response = true;
				}
				else {
					Log.d("DEBUG","AUTHENTICATEUSER RES- invalid username or password");
					Log.d("DEBUG",httpsConnection.requestDump);
					errorResponse = getString(R.string.wrongLoginMsg);
					response = false;
				}	
			} catch (UnknownHostException e){
				errorResponse = getString(R.string.noInternetMsg);
			} catch (Exception exception) {
				Log.d("DEBUG","AUTHENTICATEUSER EXC - "+exception.toString());
				Log.d("DEBUG",httpsConnection.requestDump);
				errorResponse = getString(R.string.noAccessMsg) + exception.toString();
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
		
		private Element[] buildHeader(){
			Element[] header = new Element[1];
			header[0] = new Element().createElement("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd","Security");
			header[0].setAttribute(null, "mustUnderstand","1"); 

			Element usernametoken = new Element().createElement("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd", "UsernameToken");
			header[0].addChild(Node.ELEMENT,usernametoken);

			Element username = new Element().createElement(null, "n0:Username");
			username.addChild(Node.TEXT,"");
			usernametoken.addChild(Node.ELEMENT,username);

			Element pass = new Element().createElement(null,"n0:Password");
			pass.setAttribute(null, "Type", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText");
			pass.addChild(Node.TEXT, "");
			usernametoken.addChild(Node.ELEMENT, pass);
			
			return header;
		}
		
		private SoapObject buildBodyRequest(){
			SoapObject bodyRequest = new SoapObject(NAMESPACE, METHOD_NAME);
			bodyRequest.addProperty("name", username);
			try {
				String sha1Password = sha1(password);
				bodyRequest.addProperty("password", sha1Password);
				//bodyRequest.addProperty("password", password);
			} catch (Exception e) {
				errorResponse = e.toString();
			}
			
			return bodyRequest;
		}
		
		private SoapSerializationEnvelope buildRequest(Element[] header, SoapObject bodyRequest) {
			SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11); 
			envelope.implicitTypes = true; // omit attribute types such as i:type="d:string"
			envelope.dotNet = false;	// set false to .Net encoding
			envelope.setAddAdornments(false); // omit adornments such as id="o0" c:root="1"
			envelope.setOutputSoapObject(bodyRequest); // Add body to the request
			envelope.headerOut = header; // Add header to the request
			
			return envelope;
		}
	}
	


	public static void allowAllSSL() {
		javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		});

		javax.net.ssl.SSLContext context = null;

		if (trustManagers == null) {
			trustManagers = new javax.net.ssl.TrustManager[] { new _FakeX509TrustManager() };
		}

		try {
			context = javax.net.ssl.SSLContext.getInstance("TLS");
			context.init(null, trustManagers, new SecureRandom());
		} catch (NoSuchAlgorithmException e) {
			Log.e("allowAllSSL", e.toString());
		} catch (KeyManagementException e) {
			Log.e("allowAllSSL", e.toString());
		}
		javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
	}
	
	
	public static String sha1(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		md.update(text.getBytes("iso-8859-1"), 0, text.length());
		byte[] sha1hash = md.digest();
		return Base64.encodeBase64String(sha1hash);
	}
}
