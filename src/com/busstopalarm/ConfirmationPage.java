/**
 * Author: Pyong Byon (mostly)
 * Date: 02/23/2010
 * 
 * Confirmation Page where the user interacts with this page to 
 * set vibrate, ringtone, proximity, and proximityUnit.
 * and to set alarm with those settings defined in this page
 * The user can also cancel the current alarm, also save the settings as a favorite

 * TODO: seekbar problem for proximity
 * 		 alarm update consistently
 *  	 when saving as a favorite, it should also save the current destination
 *       incorporate with database to get all data needed for calculating remaining time
 */

package com.busstopalarm;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class ConfirmationPage extends Activity {

	// this TAG is for debugging
	private static final String TAG = "inConfirmationPage";

	private static final int NOTIFICATION_ID1 = 1001;
	private static final int PENDING_INTENT_REQUEST_CODE1 = 1000001;
	private static final int PENDING_INTENT_REQUEST_CODE2 = 1000002;

	private boolean vibration;
	private Uri ringtoneUri;
	private double proximity;
	private String proximityUnit;

	/* these are not used yet
	private BusStop destination;
	private BusRoute currentBusRoute;
	 */

	// these below are the data saved in the "favorite_settings_data" file in sdcard
	// to be retrieved from the file to load the recent settings
	private String dataVibrate;
	private String dataRingtone;
	private String dataProximity;
	private String dataProximityUnit;

	private String ringtoneTitleToSave;

	// time (in seconds) is used for Alarm, alarm goes off after time seconds
	private static int time;      

	private NotificationManager notificationManager;
	private AlarmManager alarmManager;

	/**
	 * ConfirmationPage constructor
	 */
	public ConfirmationPage() {
		dataVibrate = null;
		dataRingtone = null;
		dataProximity = null;
		dataProximityUnit = null;

		vibration = false;
		ringtoneUri = null;
		proximity = 0;
		proximityUnit = null;

		ringtoneTitleToSave = null;

		/* these are not used yet
		currentBusRoute = null;
		destination = null;
		 */

		time = 10;  // 10 seconds for testing
	}

	/**
	 * it gets the default proximity from the file
	 * "favorite_settings_data" that holds the data saved when the user
	 * saved settings as a favorite
	 * not implemented yet
	 * @return proximity value (double)
	 */
	private double default_proximity() {

		return 3;  // hard wired value for now
	}

	/** 
	 * it gets the default proximityUnit from the file
	 * "favorite_settings_data" that holds the data saved when the user
	 * saved settings as a favorite
	 * @return String proximity unit
	 */
	private String default_proximity_unit() {
		return dataProximityUnit;
	}

	/** 
	 * it gets the default ringtone uri from the file
	 * "favorite_settings_data" that holds the data saved when the user
	 * saved settings as a favorite
	 * @return Uri
	 */
	private Uri default_rington_uri() {
		if (dataRingtone == null)
			return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
		return null;
	}


	/** 
	 * it gets the default vibrate from the file "favorite_settings_data" which
	 * holds the data saved when the user saved the settings as a favorite
	 * @return false if dataVibrate is null (if settings data have not been created)
	 * @return false if dataVibrate is non-null and is "vibrate_false" 
	 * @return true if dataVibrate is non-null and is "vibrate"
	 */
	private boolean default_vibrate(){
		if (dataVibrate != null && dataVibrate.equals("vibrate"))
			return true;
		return false;	
	}


	/** Called when the activity is first created on the confirmation page.
	 *  
	 *  
	 *  */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		notificationManager = (NotificationManager) getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);

		try {  // load saved settings
			loadRecentSettings();
		} catch (IOException e1) {  // if the file "favorite_settings_data" is not found
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 

		vibration = default_vibrate();
		//ringtoneUri = default_rington_uri();
		proximity = default_proximity();
		proximityUnit = default_proximity_unit();

		Log.v(TAG, "dataVibrate:  " + dataVibrate);
		Log.v(TAG, "dataRingtone:  " + dataRingtone);
		Log.v(TAG, "dataProximity:  " + dataProximity);
		Log.v(TAG, "dataProximityUnit:  " + dataProximityUnit);

		setContentView(R.layout.confirmation);
		String stop = getIntent().getStringExtra("name");
		TextView stopView = (TextView) findViewById(R.id.stopname);
		stopView.setText(stop);

		okButton();
		cancelButton();
		saveButton();
		getVibrate();
		getRingtones();
		getProximity();
		getProximityUnits();

	}  // ends onCreate method


	/**
	 *  OK Button confirms the alarm setting
	 *  it creates alarm set in the alarm manager with selected ringtone and vibrate
	 *  it notifies on the screen that the alarm is set.
	 *  then, it goes back to MainPage
	 */
	private void okButton() {

		final Button OKButton = (Button)findViewById(R.id.OKButton);
		OKButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				Intent intent = new Intent(ConfirmationPage.this, Alarm.class);

				intent.putExtra("Ringtone", ringtoneUri);
				intent.putExtra("Vibration", vibration);
				PendingIntent pendingIntent_alarm = PendingIntent.getBroadcast(getBaseContext(), PENDING_INTENT_REQUEST_CODE1,
						intent, PendingIntent.FLAG_CANCEL_CURRENT);

				//	AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
				alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (time * 1000), pendingIntent_alarm);

				//	NotificationManager manager = (NotificationManager) v.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
				Notification notification = new Notification(R.drawable.icon, "Bus Stop Alarm is set!", System.currentTimeMillis());
				PendingIntent contentIntent = PendingIntent.getActivity(v.getContext(), PENDING_INTENT_REQUEST_CODE2, 
						new Intent(v.getContext(), ConfirmationPage.class), PendingIntent.FLAG_CANCEL_CURRENT);

				notification.setLatestEventInfo(v.getContext(), "Bus Stop Alarm", timeConverter(), contentIntent);
				notification.flags = Notification.FLAG_INSISTENT;
				notificationManager.notify(NOTIFICATION_ID1, notification);
				Log.v(TAG, "Alarm is set ");
				Toast.makeText(ConfirmationPage.this, "Alarm is set", Toast.LENGTH_LONG).show();

				//Alarm alarmObject =	new Alarm (ConfirmationPage.this, proximityUnit, vibration, ringtone, ringtoneUri, time);
				//Alarm alarmObject =	new Alarm (proximityUnit, vibration, ringtone, ringtoneUri, time);
				//alarmObject =	new Alarm (ConfirmationPage.this, proximityUnit, vibration, ringtone, ringtoneUri, time);
				//alarmObject.setAlarm(v);

				//Log.v(TAG, "Alarm destination: " + alarmObject.getDestination());
				Log.v(TAG, "Alarm vibration: " + vibration);
				Log.v(TAG, "Alarm ringtoneUri: " + ringtoneUri);
				Log.v(TAG, "Alarm proximity: " + proximity);
				Log.v(TAG, "Alarm proximityUnit: " + proximityUnit);

				startActivity(new Intent(v.getContext(), MainPage.class));
				finish();
			}
		});
	} // ends okButton method


	/** 
	 *  Cancel Button cancels the current alarm set
	 *  erases the notification
	 *  then, it goes back to MainPage
	 *  
	 *  To show how alarms are canceled we will create a new Intent and a new PendingIntent with the
	 *  same requestCode as the PendingIntent alarm we want to cancel. In this case, it is PENDING_INTENT_REQUEST_CODE1.
	 *  Note: The intent and PendingIntent have to be the same as the ones used to create the alarm.
	 */
	private void cancelButton() {
		final Button CancelButton = (Button)findViewById(R.id.CancelButton);
		CancelButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(ConfirmationPage.this, Alarm.class);
				PendingIntent pendingIntent_alarm = PendingIntent.getBroadcast(getBaseContext(), PENDING_INTENT_REQUEST_CODE1,
						intent, PendingIntent.FLAG_CANCEL_CURRENT);
				//	AlarmManager alarmManager1 = (AlarmManager) getSystemService(ALARM_SERVICE);
				alarmManager.cancel(pendingIntent_alarm);
				notificationManager.cancel(NOTIFICATION_ID1);
				finish();
			}	
		});
	}


	/** 
	 *  This is invoked when the user pushes "Save as favorite" button
	 *  It gets all the current settings (vibrate, ringtone, proximity, and proximity unit) from this page
	 *  and writes them on the file "favorite_settings_data", which is located on 
	 *  data/data/com.busstopalarm/files/
	 *  If the file does not exist (if "Save as favorite" button has never been pushed before),
	 *  It will create the file in the designated location
	 *  If it exists, it will overwrite the old settings when the button is pushed
	 *  It stays in the current page
	 */
	private void saveButton() {
		final Button SaveButton = (Button) findViewById(R.id.SetAsFavButton);
		SaveButton.setOnClickListener(new View.OnClickListener(){
			public void onClick(View v){

				// data settings to be written on the file
				String settings = "";
				if (vibration)
					settings += "vibrate";
				else
					settings += "vibrate_false";

				settings += "\t";

				if (ringtoneUri != null)
					settings += ringtoneTitleToSave;
				settings += "\t";
				settings += Double.toString(proximity);
				settings += "\t";
				settings += proximityUnit;

				FileOutputStream fileOut = null; 
				OutputStreamWriter writer = null; 

				try {
					fileOut = openFileOutput("favorite_settings_data",MODE_PRIVATE);  
					writer = new OutputStreamWriter(fileOut); 
					writer.write(settings); 
					writer.flush(); 
					Toast.makeText(ConfirmationPage.this, "Settings saved",Toast.LENGTH_SHORT).show(); 
				} 
				catch (Exception e) {       
					e.printStackTrace(); 
					Toast.makeText(ConfirmationPage.this, "Settings not saved",Toast.LENGTH_SHORT).show(); 
				} 
				finally { 
					try { 
						writer.close(); 
						fileOut.close(); 
					} catch (IOException e) { 
						e.printStackTrace(); 
					} 
				}
			} // ends onClick
		}); // ends "Save as favorite" button
	} // ends saveButton method


	/**
	 * this method loads from data/data/com.busstopalarm/files/favorite_settings_data
	 * to read the user's recent settings saved.
	 * the file contains the values with tabs to separate them.
	 * After reading from the file, it sets the data values
	 * dataVibrate, dataRingtone, dataProximity, dataProximityUnit appropriately.
	 */
	private void loadRecentSettings() throws IOException {
		FileInputStream fIn = openFileInput("favorite_settings_data"); 
		InputStreamReader isr = new InputStreamReader(fIn);
		BufferedReader bin = new BufferedReader(isr);

		if (bin == null)
			return;
		String line = bin.readLine();
		if (line == null) {
			bin.close();
			return;
		}

		String[] settingResult = line.split("\t");
		bin.close();
		Log.v(TAG, "settingResult length:  " + settingResult.length);

		dataVibrate = settingResult[0];
		dataRingtone = settingResult[1];
		dataProximity = settingResult[2];
		dataProximityUnit = settingResult[3];
	}


	/**
	 *  For distance between two points, we will use Euclidean distance. The Earth is not an Euclidean plane,
	 *  but this will give a good approximation. Assuming Euclidean plane, this algorithm sums a number of straight 
	 *  line distances.  This means the calculated distance will never underestimate the actual distance, which is good.
	 *  To calculate the remaining distance once alarm has started, we need to get the current location with the GPS.
	 *  Then we need to find the closest busstop to the current location (with caveat), then do sum of straight lines again.
	 *  The return values will be in some unit that will need to be converted to either miles or km. 
	 *  not implemented yet!
	 *   
	 * @return double initial distance
	 */ 
	public static double calculateInitialDistance() {
		// get starting s busstop in busroute
		// will the starting busstop be specified by the user or does the app have to figure it out?
		// get ending d busstop in busroute
		// double dist = 0.0;
		// for (int i = s; i < d; i++) {
		//     dist += calculateDist(busroute[i],busroute[i+1];
		// return dist;
		return 0.0;
	}


	/**
	 * not implemented yet!
	 * 
	 * @return double remaining distance
	 */
	public static double calculateRemainingDistance() {
		return 0.0;
	}


	/**
	 *  Updates the average speed based on previous average speed and current speed.
	 *  If implemented like this, we need a average speed field?
	 *  We could start with an initial average speed (equivalent to 30 mph?) and do a something like
	 *  avg = k*avg + (1-k)current where 0 <= k <= 1.
	 *  
	 */
	public static void updateAverageSpeed() {

	}


	/**
	 * this method converts time (remaining) into easily readable format   
	 * 
	 * 
	 * @return String remaining time message
	 */
	public static String timeConverter() {
		if (time < 0)
			return ("timeConverter(): Error! time should not be negative.");
		if (time < 60)
			return time + " seconds left until alarm goes off";
		if (time < 120)
			return "1 minute  " + time%60 + " seconds left until alarm goes off";
		if (time < 3600) 
			return time/60 + " minutes  " + time % 60 + " seconds left until alarm goes off";
		else
			return time/3600 + "hour(s)  " + (time%3600)/60 + " minutes left until alarm goes off";
	}

	/**
	 * It is invoked when vibrate is clicked. 
	 * when loading vibrate is not checked if dataVibrate is null or "vibrate_false"
	 * when loading vibrate is checked if dataVibrate is non-null and is "vibrate"
	 * vibrate is on / off when the check box is checked / unchecked respectively.
	 */
	public void getVibrate(){

		final CheckBox vib = (CheckBox) findViewById(R.id.VibrateCheckbox);

		if (dataVibrate != null && dataVibrate.equalsIgnoreCase("vibrate"))
			vib.setChecked(true);

		vib.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {	
				vibration = isChecked;
				Log.v(TAG, "under onCheckedChanged: " + vibration);
			}
		});
	}  // ends getVibrate method


	/**
	 * proximity needs to be implemented !
	 * 
	 * 
	 * 
	 */
	public void getProximity() {
		final SeekBar proximitySeekBar = (SeekBar) findViewById(R.id.ProximityBar);

		proximitySeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){


			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				// TODO Auto-generated method stub

			}

			public void onStartTrackingTouch(SeekBar arg0) {
				// TODO Auto-generated method stub

			}

			public void onStopTrackingTouch(SeekBar arg0) {
				// TODO Auto-generated method stub
				//  arg0.get
				// proximity = ...;

			}
		});


	} // ends getProximity method


	/**
	 * It is invoked when the user selects the proximity unit on the spinner
	 * It first loads all units (Miles, Kilometers, Minutes) on the spinner
	 * And then, sets it to the one that the user previously has saved as a favorite 
	 * 
	 */
	public void getProximityUnits() {
		Spinner proximityUnitsSpinner = (Spinner) findViewById(R.id.ProximityUnits);
		ArrayAdapter<CharSequence> proxSpinnerValues = ArrayAdapter.createFromResource(this, R.array.ProximityUnitList,
				android.R.layout.simple_spinner_item);
		proxSpinnerValues.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		proximityUnitsSpinner.setAdapter(proxSpinnerValues);

		if (dataProximityUnit != null && dataProximityUnit.equalsIgnoreCase("Kilometers"))
			proximityUnitsSpinner.setSelection(1);
		if (dataProximityUnit != null && dataProximityUnit.equalsIgnoreCase("Minutes"))
			proximityUnitsSpinner.setSelection(2);

		proximityUnitsSpinner.setOnItemSelectedListener(new OnItemSelectedListener() { 
			public void onItemSelected(AdapterView<?> adapterView, View arg1,
					int arg2, long arg3) {

				int indexProx = adapterView.getSelectedItemPosition();
				CharSequence selectedUnit = (CharSequence) adapterView.getSelectedItem();
				selectedUnit.toString();
				proximityUnit = (String) selectedUnit;
				Log.v(TAG, "under onItemSelected(proximity unit): " + indexProx);
				Log.v(TAG, "under onItemSelected(proximity unit): " + selectedUnit);
			}

			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub

			}
		});
	}  // ends getProximityUnits method


	/**
	 * It loads all types of sounds (ringtones, notifications, alarms) from the local storage. 
	 * ringtone cursor goes through all the sounds and get the titles and show them on the spinner.
	 * if the user has set the ringtone as a favorite, it will set it to that ringtone in the first place
	 * if not, it will set the ringtone to the first one on the list
	 */
	public void getRingtones() {

		final RingtoneManager ringtoneManager = new RingtoneManager(this);

		// get all types of sounds (ringtones, notifications, alarms)
		ringtoneManager.setType(RingtoneManager.TYPE_ALL);

		//	int ringtonePosition; = ringtoneManager.getRingtonePosition(ringtoneUri)

		Cursor ringtoneCursor = ringtoneManager.getCursor();

		int defaultRingtoneIndex = 0;
		String[] ringtoneList = new String[ringtoneCursor.getCount()];
		//	Log.v(TAG, "ringtones row count: " + ringtoneCursor.getCount());
		for (int i = 0; i < ringtoneCursor.getCount(); i++) {
			String titleOfRingtone = ringtoneCursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
			ringtoneList[i] = titleOfRingtone;
			if (dataRingtone != null && dataRingtone.equals(titleOfRingtone))
				defaultRingtoneIndex = i;
			ringtoneCursor.moveToNext(); 
		}

		//String ringtoneList = ringtone.getTitle(this);	
		// = ringtoneCursor.getColumnNames();

		Spinner ringtoneSpinner = (Spinner) findViewById(R.id.RingtoneSelector);
		ArrayAdapter<String> ringtoneAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
				ringtoneList);
		ringtoneAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		ringtoneSpinner.setAdapter(ringtoneAdapter);

		if (defaultRingtoneIndex != 0)
			ringtoneSpinner.setSelection(defaultRingtoneIndex);

		ringtoneSpinner.setOnItemSelectedListener(new OnItemSelectedListener() { 

			public void onItemSelected(AdapterView<?> adapterView, View arg1,
					int arg2, long arg3) {

				int indexRingtone = adapterView.getSelectedItemPosition();
				ringtoneUri = ringtoneManager.getRingtoneUri(indexRingtone);
				Ringtone rt = ringtoneManager.getRingtone(indexRingtone);
				ringtoneTitleToSave = rt.getTitle(getBaseContext());

				Log.v(TAG, "under onItemSelected(index ringtone): " + indexRingtone);
				Log.v(TAG, "under onItemSelected(ringtoneTitleToSave): " + ringtoneTitleToSave);

			}
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub

			}
		});

	}  // ends getRingtones method


	/** 
	 * It is invoked when option menu is pushed
	 * @param Menu
	 * @return boolean
	 * 
	 * 
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0,1,1, "Exit");
		return true;
	}


	/** 
	 * It is invoked when option item is selected
	 * @param MenuItem
	 * @return boolean
	 * 
	 * 
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 1:
			finish();
			break;
		}
		return super.onOptionsItemSelected(item);


	}

} // class ends