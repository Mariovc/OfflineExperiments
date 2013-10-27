/**
 * @author Mario Velasco Casquero
 * @version 2.00
 */

package com.gloria.offlineexperiments;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gloria.offlineexperiments.proxy.GloriaApiProxy;
import com.gloria.offlineexperiments.ui.fonts.TypefaceManager;

public class SunMarkerActivity extends Activity{

	private static final int DATE_DIALOG_ID = 0;
	private static final int GET_CONTEXT_TIMEOUT_MS = 15000;

	private String authorizationToken;

	//private boolean loadImage = true;
	private int imageID = -1;

	private ZoomableImageView imgTouchable;
	private View buttonsLayout;
	private int buttonsVisibility = RelativeLayout.INVISIBLE;

	private TextView mDateDisplay;

	private boolean datePickerCancelled = false;
	private int displayedYear;
	private int displayedMonth;
	private int displayedDay; 
	private int auxYear;
	private int auxMonth;
	private int auxDay;
	
	private GloriaApiProxy apiProxy = new GloriaApiProxy();

	private Object taskLock = new Object();
	private GetContext getContextTask = null;
	private GetImage getImageTask = null;
	private SendResults sendResultsTask = null;


	/* *****************************************
	 ************** Life cycle ***************
	 ***************************************** */

	@Override 
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("DEBUG", "onCreate");

		Bundle extras = getIntent().getExtras();
		authorizationToken = extras.getString("authorizationToken");
		
		setContentView(R.layout.sun_marker);
		prepareWidgets();

		getContextTask = new GetContext();
		getContextTask.execute();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		synchronized (taskLock) {
			if (getContextTask != null) {
				getContextTask.cancel(true);
				getContextTask = null;
			}
			if (getImageTask != null) {
				getImageTask.cancel(true);
				getImageTask = null;
			}
			if (sendResultsTask != null) {
				sendResultsTask.cancel(true);
				sendResultsTask = null;
			}
		}
	}
	
	private void prepareWidgets() {
		final Typeface typeface = TypefaceManager.INSTANCE.getTypeface(
				getApplicationContext(), TypefaceManager.VERDANA);
		TypefaceManager.INSTANCE.applyTypefaceToAllViews(this, typeface);
		
		imgTouchable = (ZoomableImageView) findViewById(R.id.zoomable_image);
		imgTouchable.setWolfNumberText((TextView) findViewById(R.id.wolfNumberText));
		imgTouchable.setMaxZoom(4f); //change the max level of zoom, default is 3f

		buttonsLayout = findViewById(R.id.buttonsLayout);
		buttonsLayout.setVisibility(buttonsVisibility); 
		
		// calendar settings
		mDateDisplay = (TextView) findViewById(R.id.dateDisplay);
		final Calendar calendar = Calendar.getInstance();
		//calendar.add(Calendar.DAY_OF_YEAR, -2);
		displayedYear = auxYear = calendar.get(Calendar.YEAR);
		displayedMonth = auxMonth = calendar.get(Calendar.MONTH);
		displayedDay = auxDay = calendar.get(Calendar.DAY_OF_MONTH);
		updateDisplay();
	}	

	@Override
	protected void onResume(){
		super.onResume();
		Log.d("DEBUG", "onResume");
	}

	@Override
	protected void onPause() {
		Log.d("DEBUG", "onPause");
		super.onPause();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {  
		super.onConfigurationChanged(newConfig);
		Log.d("DEBUG", "onConfigChanged");
		
		// Set views to new orientation 
		// Remove views from old parents (Unlink)
		RelativeLayout imgLayout = (RelativeLayout) findViewById(R.id.image_layout);
		ArrayList<ZoomablePinView> pins = imgTouchable.getPins();
		for (int i=0; i < pins.size(); i++){
			imgLayout.removeView(pins.get(i));
		}
		imgLayout.removeView(imgTouchable);

        setContentView(R.layout.sun_marker); // set layout with new orientation
        
        // set views to new layout
		imgLayout = (RelativeLayout) findViewById(R.id.image_layout);
		imgLayout.addView(imgTouchable);
		for (int i=0; i < pins.size(); i++){
			imgTouchable.addPin(pins.get(i));
		}
		imgTouchable.setPins(pins);
		imgTouchable.selectPin(imgTouchable.getSelectedPin());
		imgTouchable.setWolfNumberText((TextView) findViewById(R.id.wolfNumberText));
		buttonsLayout = findViewById(R.id.buttonsLayout);
		buttonsLayout.setVisibility(buttonsVisibility); 
		mDateDisplay = (TextView) findViewById(R.id.dateDisplay);
		updateDisplay();
		
		// reset parameters
		imgTouchable.saveScale = 1f;
	}

	/* *****************************************
	 ********** Interface functions **********
	 ***************************************** */

	public void nextImage (View view) {
		auxYear = displayedYear;                   
		auxMonth = displayedMonth;                   
		auxDay = displayedDay;  
		if (imageID > -1) {
			displaySendResultsDialog();
		} else {
			getImageTask = new GetImage();
			getImageTask.execute();
		}
	}

	private void displaySendResultsDialog() {
		AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(SunMarkerActivity.this);
		myAlertDialog.setTitle(R.string.sendResultsTitle);
		myAlertDialog.setMessage(R.string.sendResultsMsg);
		myAlertDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface arg0, int arg1) {
				new SendResults().execute();
			}});
		myAlertDialog.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface arg0, int arg1) {
				getImageTask = new GetImage();
				getImageTask.execute();
			}});
		myAlertDialog.show();
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
		buttonsLayout.setVisibility(buttonsVisibility);
	}

	public void hideButtons () {
		buttonsVisibility = RelativeLayout.INVISIBLE;
		buttonsLayout.setVisibility(buttonsVisibility);
	}



	/* *****************************************
	 ************ Async Tasks ****************
	 ***************************************** */

	private class GetContext extends AsyncTask<Void, Void, Integer> {
		private ProgressDialog progressDialog = null;

		@Override
		protected void onPreExecute() {
			progressDialog = new ProgressDialog(SunMarkerActivity.this);
			progressDialog.setCancelable(false);
			progressDialog.setCanceledOnTouchOutside(false);
			progressDialog.setMessage(getString(R.string.requestingAccess));
			progressDialog.show();
		}
		
		@Override
		protected Integer doInBackground(Void... voids) {
			Integer reservationID = null;
			try { 
				reservationID = getContextID();
				if (reservationID < 0) {
					applyWolf();
					long timeoutMs = System.currentTimeMillis() + GET_CONTEXT_TIMEOUT_MS;
					while (reservationID < 0 && System.currentTimeMillis() < timeoutMs && !isCancelled()) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							Log.d("SUN MARKER", "Get Context interrupted", e);
							break;
						}
						reservationID = getContextID();
					}
				}
			} catch (MalformedURLException e) {
				Log.d("DEBUG", "URL is invalid");
			} catch (SocketTimeoutException e) {
				Log.d("DEBUG", "data retrieval or connection timed out");
			} catch (JSONException e) {
				Log.d("DEBUG", "JSON exception");
			} catch (IOException e) {
				Log.d("DEBUG", "IO exception");
			}  finally {
				apiProxy.shutdown();
			}
			return reservationID;
		}


		// This function is called when doInBackground is done
		@Override
		protected void onPostExecute(Integer reservationID) {
			if (progressDialog != null && progressDialog.isShowing()) {
				try {
					progressDialog.dismiss();
				} catch (Exception ex) {
				}
				progressDialog = null;
			}	

			if (!isCancelled()) {
				if (reservationID != null && reservationID > 0) {
					apiProxy.setContextId(reservationID);
					datePickerCancelled = false;
					showDialog(DATE_DIALOG_ID);
				} else {
					// this shouldn't happen - it means the server hasn't given us a context in 12 sec.
					// let's show that there is a problem with the connection for now
					Toast.makeText(SunMarkerActivity.this, getString(R.string.noInternetMsg), Toast.LENGTH_LONG).show();
					finish();
				}
			}
			synchronized (taskLock) {
				getContextTask = null;
			}
		}

	}

	private class GetImage extends AsyncTask<Void, Void, Bitmap> {
		private ProgressDialog progressDialog = null;

		// This function is called at the beginning, before doInBackground
		@Override
		protected void onPreExecute() {
			progressDialog = new ProgressDialog(SunMarkerActivity.this);
			progressDialog.setCancelable(false);
			progressDialog.setCanceledOnTouchOutside(false);
			progressDialog.setMessage(getString(R.string.gettingImageMsg));
			progressDialog.show();
		}

		@Override
		protected Bitmap doInBackground(Void... voids) {
			Bitmap bitmap = null;

			try {
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault());
				Calendar calendar = new GregorianCalendar(auxYear, auxMonth, auxDay);
				setDate("\"" + dateFormat.format(calendar.getTime()) + "T0:00:00\"");
				
				loadURL();
				if (!isCancelled()) {
					String url = getURL(); 			// get URL and image ID
					if (imageID > -1 && !isCancelled()) {
						bitmap = loadBitmap(url);  //load image from URL
					}
				}

			} catch (MalformedURLException e) {
				Log.d("DEBUG", "URL is invalid");
			} catch (SocketTimeoutException e) {
				Log.d("DEBUG", "data retrieval or connection timed out");
			} catch (JSONException e) {
				Log.d("DEBUG", "JSON exception");
			} catch (IOException e) {
				Log.d("DEBUG", "IO exception");
			}  finally {
				apiProxy.shutdown();
			}       

			return bitmap;
		}


		// This function is called when doInBackground is done
		@Override
		protected void onPostExecute(Bitmap bitmap){
			if (progressDialog != null && progressDialog.isShowing()) {
				try {
					progressDialog.dismiss();
				} catch (Exception ex) {
				}
				progressDialog = null;
			}	

			if (!isCancelled()) {
				if (bitmap != null) {
					BitmapDrawable oldDrawable = (BitmapDrawable)imgTouchable.getDrawable();
					imgTouchable.setImageBitmap(bitmap);
					if (oldDrawable != null && oldDrawable.getBitmap() != null)
						oldDrawable.getBitmap().recycle();
					imgTouchable.resetAttributes();
					displayedYear = auxYear;
					displayedMonth = auxMonth;
					displayedDay = auxDay;
					updateDisplay();
				} else {
					Toast.makeText(SunMarkerActivity.this, getString(R.string.noImagesForThisDateMsg), Toast.LENGTH_LONG).show();
					datePickerCancelled = false;
					showDialog(DATE_DIALOG_ID);
				}
			}
			synchronized (taskLock) {
				getImageTask = null;
			}
		}

		private Bitmap loadBitmap(String url) {
			Bitmap bitmap = null;
			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inScaled = false; // ask the bitmap factory not to scale the loaded bitmaps
			try {
				URL urlObject = new URL(url);
				HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();
				connection.setDoInput(true);
				connection.connect();
				InputStream input = connection.getInputStream();
				bitmap = BitmapFactory.decodeStream(input,null,opts);
			} 
			catch (IOException e) {}
			return bitmap;
		}
	}

	private class SendResults extends AsyncTask<Void, Void, Void> {
		private ProgressDialog progressDialog = null;

		// This function is called at the beginning, before doInBackground
		@Override
		protected void onPreExecute() {
			progressDialog = new ProgressDialog(SunMarkerActivity.this);
			progressDialog.setCancelable(false);
			progressDialog.setCanceledOnTouchOutside(false);
			progressDialog.setMessage(getString(R.string.sendingResultsMsg));
			progressDialog.show();
		}


		@Override
		protected Void doInBackground(Void... voids) {
			try { 

				sendResults();
				saveResults();

			} catch (MalformedURLException e) {
				Log.d("DEBUG", "URL is invalid");
			} catch (SocketTimeoutException e) {
				Log.d("DEBUG", "data retrieval or connection timed out");
			} catch (JSONException e) {
				Log.d("DEBUG", "JSON exception");
			} catch (IOException e) {
				Log.d("DEBUG", "IO exception");
			}  finally {
				apiProxy.shutdown();
			}

			return null;
		}


		// This function is called when doInBackground is done
		@Override
		protected void onPostExecute(Void nothing){
			if (progressDialog != null && progressDialog.isShowing()) {
				try {
					progressDialog.dismiss();
				} catch (Exception ex) {
				}
				progressDialog = null;
			}
			if (!isCancelled()) {
				getImageTask = new GetImage();
				getImageTask.execute();
			}
			synchronized (taskLock) {
				sendResultsTask = null;
			}
		}

	}




	/* *****************************************
	 ************ HTTP Requests **************
	 ***************************************** */

	private int getContextID() throws ClientProtocolException, IOException, JSONException {
		int reservationID = -1;
		HttpClient httpClient = apiProxy.getHttpClient();
		HttpGet getRequest = apiProxy.getHttpGetRequest(GloriaApiProxy.OP_ACTIVE_EXPERIMENTS, authorizationToken);

		HttpResponse resp = httpClient.execute(getRequest);
		int statusCode = resp.getStatusLine().getStatusCode();
		Log.d("DEBUG", "get context ID, status code: " + statusCode);

		// handle issues
		if (statusCode != HttpURLConnection.HTTP_OK) {
			// handle any errors, like 404, 500,..
			Log.d("DEBUG", "Unknown error");
		}


		String respStr = EntityUtils.toString(resp.getEntity());
		JSONArray experiments = new JSONArray(respStr);
		boolean wolfFound = false;
		int i = 0;
		while (!wolfFound && i < experiments.length()) {
			JSONObject experiment = experiments.optJSONObject(i);
			if (experiment.optString("experiment").compareTo("WOLF") == 0){
				reservationID = experiment.optInt("reservationId");
				wolfFound = true;
			}
			i++;
		}
		return reservationID;
	}

	private void applyWolf() throws ClientProtocolException, IOException {
		HttpClient httpClient = apiProxy.getHttpClient();
		HttpGet getRequest = apiProxy.getHttpGetRequest(GloriaApiProxy.OP_APPLY_FOR_WOLF, authorizationToken); 
		HttpResponse resp = httpClient.execute(getRequest);
		int statusCode = resp.getStatusLine().getStatusCode();
		Log.d("DEBUG", "Apply WOLF, status code: " + statusCode);

		// handle issues
		if (statusCode != HttpURLConnection.HTTP_OK) {
			// handle any errors, like 404, 500,..
			Log.d("DEBUG", "Unknown error");
		}
	}

	private void setDate(String date) throws ClientProtocolException, IOException {
		StringEntity entity = new StringEntity(date);
		HttpClient httpClient = apiProxy.getHttpClient();
		HttpPost postRequest = apiProxy.getHttpPostRequest(GloriaApiProxy.OP_SET_DATE, authorizationToken, entity);
		Log.d("DEBUG", "opnening connection");
		HttpResponse resp = httpClient.execute(postRequest);
		int statusCode = resp.getStatusLine().getStatusCode();
		Log.d("DEBUG", "Set date, status code: " + statusCode);

		// handle issues
		if (statusCode != HttpURLConnection.HTTP_OK) {
			// handle any errors, like 404, 500,..
			Log.d("DEBUG", "Unknown error");
		}
	}

	private void loadURL() throws ClientProtocolException, IOException{
		HttpClient httpClient = apiProxy.getHttpClient();
		HttpGet getRequest = apiProxy.getHttpGetRequest(GloriaApiProxy.OP_LOAD_URL, authorizationToken); 
		HttpResponse resp = httpClient.execute(getRequest);
		int statusCode = resp.getStatusLine().getStatusCode();
		Log.d("DEBUG", "Load, status code: " + statusCode);

		// handle issues
		if (statusCode != HttpURLConnection.HTTP_OK) {
			// handle any errors, like 404, 500,..
			Log.d("DEBUG", "Unknown error");
		}
	}

	private String getURL() throws ParseException, IOException, JSONException{
		HttpClient httpClient = apiProxy.getHttpClient();
		HttpGet getRequest = apiProxy.getHttpGetRequest(GloriaApiProxy.OP_GET_URL, authorizationToken); 

		Log.d("DEBUG", "Before getting URL");
		HttpResponse resp = httpClient.execute(getRequest);
		Log.d("DEBUG", "after request");
		int statusCode = resp.getStatusLine().getStatusCode();
		Log.d("DEBUG", "Get URL, status code: " + statusCode);

		// handle issues
		if (statusCode != HttpURLConnection.HTTP_OK) {
			// handle any errors, like 404, 500,..
			Log.d("DEBUG", "Unknown error");
		}


		String respString = EntityUtils.toString(resp.getEntity());
		JSONObject imageJSON = new JSONObject(respString);
		String urlStr = imageJSON.optString("jpg");
		imageID = imageJSON.optInt("id", -1);
		Log.d("DEBUG", "url: " + urlStr + " \nID: " + imageID);
		return urlStr;
	}

	private void sendResults() throws ClientProtocolException, IOException, JSONException {
		JSONObject jsonResult = new JSONObject();
		jsonResult.put("image", imageID);
		JSONArray jsonSpots = new JSONArray();
		// for
		ArrayList<ZoomablePinView> pins = imgTouchable.getPins();
		for (int i = 0; i < pins.size(); i++) {
			JSONObject jsonPin = new JSONObject();
			jsonPin.put("n", pins.get(i).getNumber());
			jsonPin.put("x", (int) pins.get(i).getRealPosX());
			jsonPin.put("y", (int) pins.get(i).getRealPosY());
			jsonSpots.put(jsonPin);
		}

		jsonResult.put("spots", jsonSpots);
		Log.d("DEBUG", "Send results: " + jsonResult.toString());
		StringEntity entity = new StringEntity(jsonResult.toString());

		HttpClient httpClient = apiProxy.getHttpClient();
		HttpPost postRequest = apiProxy.getHttpPostRequest(GloriaApiProxy.OP_SET_MARKERS, authorizationToken, entity);
		HttpResponse resp = httpClient.execute(postRequest);
		int statusCode = resp.getStatusLine().getStatusCode();
		Log.d("DEBUG", "Send results, status code: " + statusCode);

		// handle issues
		if (statusCode != HttpURLConnection.HTTP_OK) {
			// handle any errors, like 404, 500,..
			Log.d("DEBUG", "Unknown error");
		}
	}

	private void saveResults() throws ClientProtocolException, IOException{
		HttpClient httpClient = apiProxy.getHttpClient();
		HttpGet getRequest = apiProxy.getHttpGetRequest(GloriaApiProxy.OP_SAVE, authorizationToken); 
		HttpResponse resp = httpClient.execute(getRequest);
		int statusCode = resp.getStatusLine().getStatusCode();
		Log.d("DEBUG", "Save, status code: " + statusCode);

		// handle issues
		if (statusCode != HttpURLConnection.HTTP_OK) {
			// handle any errors, like 404, 500,..
			Log.d("DEBUG", "Unknown error");
		}
	}

	
	/* *****************************************
	 ************ Calendar *******************
	 ***************************************** */

	private void updateDisplay() {       
		DateFormat formatter = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
		Calendar calendar = new GregorianCalendar(displayedYear, displayedMonth, displayedDay);
		Date date = calendar.getTime();

		mDateDisplay.setText(formatter.format(date));
	}

	// the callback received when the user "sets" the date in the dialog
	private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {

		public void onDateSet(DatePicker view, int year,
				int monthOfYear, int dayOfMonth) {
			if (!datePickerCancelled) {
				datePickerCancelled = true; // known bug - onDateSet called twice sometimes. This will avoid it
				auxYear = year;                   
				auxMonth = monthOfYear;                   
				auxDay = dayOfMonth;     
				if (imageID > -1) {
					displaySendResultsDialog();
				} else {
					getImageTask = new GetImage();
					getImageTask.execute();  
				}
			}
		}

	};

	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id)
		{
		case DATE_DIALOG_ID:
			final DatePickerDialog datePicker = new DatePickerDialog(this, mDateSetListener, auxYear, auxMonth,auxDay);
			datePicker.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancelBtn), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					datePickerCancelled = true;
					if (which == DialogInterface.BUTTON_NEGATIVE && imageID == -1) {
						finish();
					}
				}
			});
			datePicker.setTitle(R.string.selectDateMsg);
			datePicker.setCancelable(false);
			datePicker.setCanceledOnTouchOutside(false);
			return datePicker;
		}
		return null;
	}
	
	/* *****************************************
	 ************ Menu options ***************
	 ***************************************** */
	
	@Override 
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.sun_marker, menu);
		return true; /** true -> the menu is already visible */
	}

	@Override 
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.setDate:
			datePickerCancelled = false;
			showDialog(DATE_DIALOG_ID);
			break;
		}
		return true; /** true -> the action will not be propagate*/
	}
}



