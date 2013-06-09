package com.gloria.offlineexperiments;

import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SunMarkerActivity extends Activity{

	private static final String NAMESPACE = "http://image.repository.services.gs.gloria.eu/";
	private static String HOST="saturno.datsi.fi.upm.es";
	private static int PORT=8443; 
	private static String WSDL_LOCATION="/GLORIA/services/ImageRepositoryPort?wsdl";
	private static int TIMEOUT=5000;
	private static String METHOD_NAME_IDS = "getAllImageIdentifiersByDate";
	private static String METHOD_NAME_URL = "getImageInformation";
	private static final String SOAP_ACTION =  "";

	private static TrustManager[] trustManagers;

	private static final int MAX_NUM_IMAGES = 10;
	private int currentImage = 0;
	private String[] imageIDs = new String[MAX_NUM_IMAGES];
	private ProgressDialog progressDialog = null;
	private boolean loadImage = true;

	private ZoomableImageView imgTouchable;
	private RelativeLayout buttons;
	private int buttonsVisibility = RelativeLayout.INVISIBLE;

	@Override 
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sun_marker);
		imgTouchable = (ZoomableImageView) findViewById(R.id.zoomable_image);
		imgTouchable.setMaxZoom(4f); //change the max level of zoom, default is 3f
		imgTouchable.setWolfNumberText((TextView) findViewById(R.id.wolfNumberText));

		buttons = (RelativeLayout) findViewById(R.id.Buttons);
		buttons.setVisibility(buttonsVisibility); 
	}

	public void nextImage (View view) {
		currentImage++;
		if (currentImage < MAX_NUM_IMAGES && imageIDs[currentImage] != null)
			new GetImage().execute();
		else
			Toast.makeText(this, "There is no more images today", Toast.LENGTH_SHORT).show();
	}

	public void removePin (View view) {
		imgTouchable.removePin();
	}

	public void increase (View view) {
		imgTouchable.increasePin();
	}

	public void decrease (View view) {
		imgTouchable.decreasePin();
	}

	public void displayButtons () {
		buttonsVisibility = RelativeLayout.VISIBLE;
		buttons.setVisibility(buttonsVisibility);
	}

	public void hideButtons () {
		buttonsVisibility = RelativeLayout.INVISIBLE;
		buttons.setVisibility(buttonsVisibility);
	}

	@Override
	protected void onResume(){
		super.onResume();
		if (loadImage)
			new GetIDs().execute();
	}
	
	
	@Override
	protected void onSaveInstanceState(Bundle savedState){
		super.onSaveInstanceState(savedState);
		ArrayList<ZoomablePinView> pins = imgTouchable.getPins();
		savedState.putInt("numPins", pins.size());
		for (int i = 0; i < pins.size(); i++) {
			String pinName = "pin" + i;
			savedState.putInt(pinName + "number", pins.get(i).getNumber());
			PointF pos = pins.get(i).getRealPosition();
			savedState.putFloat(pinName + "posX", pos.x);
			savedState.putFloat(pinName + "posY", pos.y);
		}
		savedState.putInt("selectedPin", imgTouchable.getSelectedPin());
		savedState.putInt("buttonsVisibility", buttonsVisibility);
		Bitmap bitmap = ((BitmapDrawable)imgTouchable.getDrawable()).getBitmap();
		savedState.putParcelable("bitmap", bitmap);
		for (int j = 0; j < MAX_NUM_IMAGES; j++) {
			savedState.putString("id"+j, imageIDs[j]);
		}
		savedState.putInt("currentImage", currentImage);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedState){
		super.onRestoreInstanceState(savedState);
		ArrayList<ZoomablePinView> pins = new ArrayList<ZoomablePinView>();
		int numPins = savedState.getInt("numPins");
		for (int i = 0; i < numPins; i++) {
			ZoomablePinView pin = new ZoomablePinView(this);
			pin.setNumber(savedState.getInt("pin" + i + "number"));
			pin.setImageBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.marker));
			float posX = savedState.getFloat("pin" + i + "posX");
			float posY = savedState.getFloat("pin" + i + "posY");
			pin.setRealPosition(posX, posY);
			pins.add(pin);
		}
		imgTouchable.setPins(pins);
		imgTouchable.setSelectedPin(savedState.getInt("selectedPin"));
		Bitmap bitmap = savedState.getParcelable("bitmap");
		imgTouchable.setImageBitmap(bitmap);
		loadImage = false;
		imgTouchable.setReload(true);
		buttonsVisibility = savedState.getInt("buttonsVisibility");
		buttons.setVisibility(buttonsVisibility);
		for (int j = 0; j < MAX_NUM_IMAGES; j++) {
			imageIDs[j] = savedState.getString("id"+j);
		}
		currentImage = savedState.getInt("currentImage", currentImage);
	}


	private class GetIDs extends AsyncTask<Void, Void, String> {

		// This function is called at the beginning, before doInBackground
		@Override
		protected void onPreExecute() {
			progressDialog = new ProgressDialog(SunMarkerActivity.this);
			progressDialog.setMessage("Getting images ...");
			progressDialog.show();
		}


		@Override
		protected String doInBackground(Void... voids) {
			String identifier = "";

			Element[] header = buildHeader();
			SoapObject bodyRequest = buildIDsBodyRequest();
			SoapSerializationEnvelope envelope = buildRequest(header, bodyRequest);
			Log.d("DEBUG",envelope.bodyOut.toString());

			allowAllSSL();
			System.setProperty("http.keepAlive", "false");
			HttpsTransportSE httpsConnection = new HttpsTransportSE(HOST,PORT, WSDL_LOCATION, TIMEOUT);
			httpsConnection.debug = true; // to see requests in XML format while debugging

			try {
				httpsConnection.call(SOAP_ACTION, envelope);
				SoapObject responseSOAP_IDs = (SoapObject) envelope.bodyIn;
				Log.d("DEBUG", String.valueOf(responseSOAP_IDs.getPropertyCount()));
				int i = 0;
				while (i < MAX_NUM_IMAGES && i < responseSOAP_IDs.getPropertyCount()) {
					imageIDs[i] = responseSOAP_IDs.getPropertyAsString(i);
					if (i == 0)
						identifier = imageIDs[0];
					i++;
				}
				Log.d("DEBUG","Getting Ids  - SUCCESS");
			} catch (Exception exception) {
				Log.d("DEBUG","Getting Ids EXC - " + exception.toString());
			}

			publishProgress();
			return identifier;
		}


		// This function is called when doInBackground is done
		@Override
		protected void onPostExecute(String id){
			if(progressDialog.isShowing()) {
				progressDialog.dismiss();
				progressDialog = null;
			}	
			new GetImage().execute(); 
		}

	}

	private class GetImage extends AsyncTask<Void, Void, Bitmap> {

		// This function is called at the beginning, before doInBackground
		@Override
		protected void onPreExecute() {
			if(progressDialog == null)
				progressDialog = new ProgressDialog(SunMarkerActivity.this);
			else if (progressDialog.isShowing())
				progressDialog.dismiss();
			progressDialog.setMessage("Loading image ...");
			progressDialog.show();
		}


		@Override
		protected Bitmap doInBackground(Void... voids) {
			Bitmap bitmap = null;

			Element[] header = buildHeader();
			SoapObject bodyRequest = buildURLBodyRequest(imageIDs[currentImage]);
			SoapSerializationEnvelope envelope = buildRequest(header, bodyRequest);

			allowAllSSL();
			System.setProperty("http.keepAlive", "false");
			HttpsTransportSE httpsConnection = new HttpsTransportSE(HOST,PORT, WSDL_LOCATION, TIMEOUT);
			try {
				httpsConnection.call(SOAP_ACTION, envelope);
				SoapObject responseSOAP_URL = (SoapObject) envelope.bodyIn;
				SoapObject returnValue = (SoapObject) responseSOAP_URL.getProperty(0);
				String url = returnValue.getPropertyAsString("url");
				bitmap = loadBitmap(url);  //load image from URL
				Log.d("DEBUG","Getting URL  - SUCCESS");
			} catch (Exception exception) {
				Log.d("DEBUG","Getting URL EXC - " + exception.toString());
				new GetImage().execute(); 
			}
			
			publishProgress();
			return bitmap;
		}


		// This function is called when doInBackground is done
		@Override
		protected void onPostExecute(Bitmap bitmap){
			if(progressDialog.isShowing()) {
				progressDialog.dismiss();
				progressDialog = null;
			}	
			BitmapDrawable oldDrawable = (BitmapDrawable)imgTouchable.getDrawable();
			imgTouchable.setImageBitmap(bitmap);
			if (oldDrawable != null && oldDrawable.getBitmap() != null)
				oldDrawable.getBitmap().recycle();
			imgTouchable.resetAttributes();
			Toast.makeText(SunMarkerActivity.this, "Image #" + (currentImage+1), Toast.LENGTH_LONG).show();
		}


		private Bitmap loadBitmap(String url) {
			Bitmap bitmap = null;
			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inScaled = false; // ask the bitmap factory not to scale the loaded bitmaps
			try {
				URL urlObject = new URL(url);
				bitmap = BitmapFactory.decodeStream(urlObject.openConnection().getInputStream(), null, opts);
			} 
			catch (IOException e) {}
			return bitmap;
		}
	}




	private Element[] buildHeader(){
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

		return header;
	}

	private SoapObject buildIDsBodyRequest(){
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd",Locale.US);
		Calendar calendar = Calendar.getInstance();
		Date today = calendar.getTime();
		calendar.add(Calendar.DAY_OF_YEAR, -1);
		Date yesterday = calendar.getTime();
		String todaySting = dateFormat.format(today);
		String yesterdayString = dateFormat.format(yesterday);
		SoapObject bodyRequest = new SoapObject(NAMESPACE, METHOD_NAME_IDS);
		bodyRequest.addProperty("dateFrom", yesterdayString);
		bodyRequest.addProperty("dateTo", todaySting);
		return bodyRequest;
	}

	private SoapObject buildURLBodyRequest(String id){
		SoapObject bodyRequest = new SoapObject(NAMESPACE, METHOD_NAME_URL);
		bodyRequest.addProperty("id", id);
		return bodyRequest;
	}

	private SoapSerializationEnvelope buildRequest(Element[] header, SoapObject bodyRequest){
		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11); 
		envelope.implicitTypes = true; // omit attribute types such as i:type="d:string"
		envelope.dotNet = false;	// set false to .Net encoding
		envelope.setAddAdornments(false); // omit adornments such as id="o0" c:root="1"
		envelope.setOutputSoapObject(bodyRequest); // Add body to the request
		envelope.headerOut = header; // Add header to the request
		return envelope;
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
}
