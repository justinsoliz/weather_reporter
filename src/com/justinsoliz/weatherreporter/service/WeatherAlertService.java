package com.justinsoliz.weatherreporter.service;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.justinsoliz.weatherreporter.Constants;
import com.justinsoliz.weatherreporter.R;
import com.justinsoliz.weatherreporter.data.DBHelper;
import com.justinsoliz.weatherreporter.data.WeatherRecord;
import com.justinsoliz.weatherreporter.data.DBHelper.Location;
import com.justinsoliz.weatherreporter.data.YWeatherFetcher;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

/**
 * Background service to check for severe weather for specific locations and
 * alert user.
 * 
 * Note that this is started at BOOT (in which case onCreate and onStart are
 * called), and is bound from within ReportDetail Activity in WeatherReporter
 * application. This Service is started in the background for alert processing
 * (standalone), bound in Activities to call methods on Binder to register alert
 * locations.
 * 
 * @author justinsoliz
 * 
 */

public class WeatherAlertService extends Service {

	private static final String CLASSTAG = WeatherAlertService.class
			.getSimpleName();
	private static final String LOC = "LOC";
	private static final String ZIP = "ZIP";
	private static final long ALERT_QUIET_PERIOD = 10000;
	private static final long ALERT_POLL_INTERVAL = 15000;

	// convenience for Activity classes in the same process to get current
	// device location
	// (so they don't have to repeat all the LocationManager and provider stuff
	// locally)
	// (this would NOT work across applications, only for things in the same
	// PROCESS)
	public static String deviceLocationZIP = "94102";

	private Timer timer;
	private DBHelper dbHelper;
	private NotificationManager notificationManager;

	private TimerTask task = new TimerTask() {

		@Override
		public void run() {
			// poll user specified locations
			List<Location> locations = dbHelper.getAllAlertEnabled();

			for (Location loc : locations) {
				WeatherRecord record = loadRecord(loc.zip);

				if (record.isSevere()) {
					if ((loc.lastAlert + WeatherAlertService.ALERT_QUIET_PERIOD) < System
							.currentTimeMillis()) {
						loc.lastAlert = System.currentTimeMillis();
						dbHelper.update(loc);
						sendNotification(loc.zip, record);
					}
				}
			}

			// poll device location
		}
	};

	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			notifyFromHandler(
					(String) msg.getData().getString(WeatherAlertService.LOC),
					(String) msg.getData().getString(WeatherAlertService.ZIP));
		}
	};

	@Override
	public void onCreate() {
		dbHelper = new DBHelper(this);
		timer = new Timer();
		timer.schedule(task, 5000, WeatherAlertService.ALERT_POLL_INTERVAL);
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);

		Log.v(Constants.LOGTAG, " " + WeatherAlertService.CLASSTAG + " onStart");

		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		final Geocoder geocoder = new Geocoder(this);

		LocationListener locationListener = new LocationListener() {

			public void onLocationChanged(android.location.Location location) {
				Log.v(Constants.LOGTAG,
						" "
								+ WeatherAlertService.CLASSTAG
								+ " locationProvider LOCATION CHANGED lat/long - "
								+ location.getLatitude() + " "
								+ location.getLongitude());

				double latitude = location.getLatitude();
				double longitude = location.getLongitude();

				try {
					List<Address> addresses = geocoder.getFromLocation(
							latitude, longitude, 5);
					if (addresses != null) {
						for (Address address : addresses) {
							Log.w(Constants.LOGTAG,
									" "
											+ WeatherAlertService.CLASSTAG
											+ " parsing address for geocode ZIP - country:"
											+ address.getCountryCode());
							if (address.getPostalCode() != null) {
								WeatherAlertService.deviceLocationZIP = addresses
										.get(0).getPostalCode();
								Log.v(Constants.LOGTAG, " "
										+ WeatherAlertService.CLASSTAG
										+ " updating deviceLocationZIP to "
										+ WeatherAlertService.deviceLocationZIP);
								break;
							}
						}
						Log.v(Constants.LOGTAG,
								" "
										+ WeatherAlertService.CLASSTAG
										+ " after parsing all geocode addresses deviceLocationZIP = "
										+ WeatherAlertService.deviceLocationZIP);
					} else {
						Log.v(Constants.LOGTAG,
								" "
										+ WeatherAlertService.CLASSTAG
										+ " NOT updating deviceLocationZIP, geocode addresses null");
					}
				} catch (IOException ex) {
					Log.e(Constants.LOGTAG, " " + WeatherAlertService.CLASSTAG,
							ex);
				}
			}

			public void onProviderDisabled(String s) {
				Log.v(Constants.LOGTAG, " " + WeatherAlertService.CLASSTAG
						+ "   locationProvider DISABLED - " + s);
			}

			public void onProviderEnabled(String s) {
				Log.v(Constants.LOGTAG, " " + WeatherAlertService.CLASSTAG
						+ "   locationProvider ENABLED - " + s);
			}

			public void onStatusChanged(String s, int i, Bundle b) {
				Log.v(Constants.LOGTAG, " " + WeatherAlertService.CLASSTAG
						+ "   locationProvider STATUS CHANGE - " + s);
			}

		};
	}
	
	@Override 
	public void onDestroy() {
		super.onDestroy();
		dbHelper.cleanup();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	protected WeatherRecord loadRecord(String zip) {
		final YWeatherFetcher weatherFetcher = new YWeatherFetcher(zip, true);
		return weatherFetcher.getWeather();
	}

	private void notifyFromHandler(String location, String zip) {
		Uri uri = Uri.parse("weather://com.justinsoliz/loc?zip=" + zip);
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		PendingIntent pendingIntent = PendingIntent.getActivity(this,
				Intent.FLAG_ACTIVITY_NEW_TASK, intent,
				PendingIntent.FLAG_ONE_SHOT);
		final Notification notification = new Notification(
				R.drawable.severe_weather_24, "Severe Weather Alert!",
				System.currentTimeMillis());
	}

	private void sendNotification(String zip, WeatherRecord record) {
		Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		bundle.putString(WeatherAlertService.ZIP, zip);
		bundle.putString(WeatherAlertService.LOC, record.getCity() + ", "
				+ record.getRegion());
		msg.setData(bundle);
		handler.sendMessage(msg);
	}
}