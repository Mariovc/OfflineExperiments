package com.gloria.offlineexperiments;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

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
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity {

	private static final String NAMESPACE = "http://user.repository.services.gs.gloria.eu/";
	private static String HOST="saturno.datsi.fi.upm.es";
	private static int PORT=8443; 
	private static String WSDL_LOCATION="/GLORIA/services/UserRepositoryPort?wsdl";
	private static int TIMEOUT=5000;
	private static String METHOD_NAME = "authenticateUser";
	private static final String SOAP_ACTION =  "";

	private HttpsTransportSE httpsConnection;
	private String username = "";
	private String password = "";
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//final boolean customTitleSupported = requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_login);

		/*if ( customTitleSupported ) {
			getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.titlebar);
		}

		ImageView infoButton = (ImageView)findViewById(R.id.infoButton);
		infoButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				exit(null);
			}
		}); 
		 */
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.login, menu);
		return true;
	}

	public void launchInfo() {
		//Intent i = new Intent(this, Info.class);
		//startActivity(i);
	}

	public void authenticate(View view) {
		//Intent i = new Intent(this, SunMarkerActivity.class);
		//startActivity(i);
		EditText usernameText = (EditText) findViewById(R.id.username);
		this.username = usernameText.getText().toString();
		EditText passwordText = (EditText) findViewById(R.id.password);
		this.password = passwordText.getText().toString();
		new Authentication().execute(); 
	}

	public void exit(View view) {
		finish();
	}

	private class Authentication extends AsyncTask<Void, Void, Boolean> {
		private ProgressDialog progressDialog;
		private String errorResponse = "Unexpected error";
		//int prevOrientation = getRequestedOrientation();

		@Override
		protected Boolean doInBackground(Void... voids) {
			boolean response = false;
			/*
			if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			} else if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			} else {
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
			}*/				
			Log.d("DEBUG","do in background");
			// building body request
			SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME);
			request.addProperty("name", username);
			request.addProperty("password", password);


			// Build header
			Element[] header = new Element[1];
			header[0] = new Element().createElement("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd","Security");
			header[0].setAttribute(null, "mustUnderstand","1"); 

			Element usernametoken = new Element().createElement("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd", "UsernameToken");
			header[0].addChild(Node.ELEMENT,usernametoken);

			Element username = new Element().createElement(null, "n0:Username");
			username.addChild(Node.TEXT,"mario");
			usernametoken.addChild(Node.ELEMENT,username);

			Element pass = new Element().createElement(null,"n0:Password");
			pass.setAttribute(null, "Type", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText");
			pass.addChild(Node.TEXT, "mario123");
			usernametoken.addChild(Node.ELEMENT, pass);
			// End building header


			SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11); 
			envelope.implicitTypes = true; // omit attribute types such as i:type="d:string"
			envelope.dotNet = false;	// set false to .Net encoding
			envelope.setAddAdornments(false); // omit adornments such as id="o0" c:root="1"
			envelope.setOutputSoapObject(request); // Add body to the request
			envelope.headerOut = header; // Add header to the request

			Log.d("DEBUG",envelope.bodyOut.toString());
			allowAllSSL();

			//HttpTransportSE androidHttpTransport = new HttpTransportSE(URL);
			System.setProperty("http.keepAlive", "false");
			httpsConnection = new HttpsTransportSE(HOST,PORT, WSDL_LOCATION, TIMEOUT);
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
					errorResponse = "Invalid username or password";
					response = false;
				}	
			} catch (Exception exception) {
				Log.d("DEBUG","AUTHENTICATEUSER EXC - "+exception.toString());
				Log.d("DEBUG",httpsConnection.requestDump);
				errorResponse = exception.toString();
			}

			publishProgress();
			return response;

		}
		// This function is used to send progress back 
		//   to the UI during doInBackground
		@Override
		protected void onProgressUpdate(Void...voids){

			// Log what the functions is doing

		}

		// This function is called when doInBackground is done
		@Override
		protected void onPostExecute(Boolean authenticated){
			//Bundle bundle = new Bundle();
			//bundle.putStringArray("Experimentos",experimentos);
			//i.putExtras(bundle);               
			//setRequestedOrientation(prevOrientation);
			if(progressDialog.isShowing()) {
				progressDialog.cancel();
			}
			
			
			if (authenticated) {
				Intent i = new Intent(LoginActivity.this, SunMarkerActivity.class);
				startActivity(i);
			}
			else {
				Toast.makeText(LoginActivity.this, errorResponse, Toast.LENGTH_LONG).show();
			}
		}
		
		
		// This function is called at the beginning, before doInBackground
		@Override
		protected void onPreExecute() {
			progressDialog = new ProgressDialog(LoginActivity.this);
			progressDialog.setMessage("Authenticating.....");
			progressDialog.show();
		}
	}


	private static TrustManager[] trustManagers;

	private static class _FakeX509TrustManager implements
	javax.net.ssl.X509TrustManager {
		private static final X509Certificate[] _AcceptedIssuers = new X509Certificate[] {};

		public void checkClientTrusted(X509Certificate[] arg0, String arg1)
				throws CertificateException {
		}

		public void checkServerTrusted(X509Certificate[] arg0, String arg1)
				throws CertificateException {
		}

		public boolean isClientTrusted(X509Certificate[] chain) {
			return (true);
		}

		public boolean isServerTrusted(X509Certificate[] chain) {
			return (true);
		}

		public X509Certificate[] getAcceptedIssuers() {
			return (_AcceptedIssuers);
		}
	}

	public static void allowAllSSL() {

		javax.net.ssl.HttpsURLConnection
		.setDefaultHostnameVerifier(new HostnameVerifier() {
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
		javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(context
				.getSocketFactory());
	}
}
