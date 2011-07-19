package com.justinsoliz.weatherreporter;

import com.justinsoliz.weatherreporter.data.DBHelper;
import com.justinsoliz.weatherreporter.data.DBHelper.Location;
import com.justinsoliz.weatherreporter.data.WeatherForecast;
import com.justinsoliz.weatherreporter.data.WeatherRecord;
import com.justinsoliz.weatherreporter.data.YWeatherFetcher;
import com.justinsoliz.weatherreporter.service.WeatherAlertService;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class ReportViewDetail extends Activity {

	private static final String CLASSTAG = ReportViewDetail.class
			.getSimpleName();
	private static final int MENU_VIEW_SAVED_LOCATIONS = Menu.FIRST;
	private static final int MENU_REMOVE_SAVED_LOCATION = Menu.FIRST + 1;
	private static final int MENU_SAVE_LOCATION = Menu.FIRST + 2;
	private static final int MENU_SPECIFY_LOCATION = Menu.FIRST + 3;
	private static final int MENU_VIEW_CURRENT_LOCATION = Menu.FIRST + 4;

	private TextView location;
	private TextView date;
	private TextView condition;
	private TextView forecast;
	private ImageView conditionImage;
	private CheckBox currentCheck;

	private ProgressDialog progressDialog;
	private WeatherRecord report;
	private String reportZip;
	private String deviceZip;
	private boolean useDeviceLocation;

	private Location savedLocation;
	private Location deviceAlertEnabledLocation;

	private DBHelper dbHelper;

	private final Handler handler = new Handler() {

		@Override
		public void handleMessage(final Message msg) {
			progressDialog.dismiss();

			if ((report == null) || (report.getCondition() == null)) {
				Toast.makeText(ReportViewDetail.this,
						R.string.message_report_unavailable, Toast.LENGTH_SHORT)
						.show();
			} else {
				Log.v(Constants.LOGTAG, " " + ReportViewDetail.CLASSTAG
						+ "  Handler report - " + report);
				location.setText(report.getCity() + ", " + report.getRegion()
						+ " " + report.getCountry());
				date.setText(report.getDate());

				StringBuffer cond = new StringBuffer();
				cond.append(report.getCondition().getDisplay() + "\n");
				cond.append("Temperature: " + report.getTemp() + " F "
						+ " (wind chill " + report.getWindChill() + " F)\n");
				cond.append("Barometer: " + report.getPressure() + " and "
						+ report.getPressureState() + "\n");
				cond.append("Humidity: " + report.getHumidity() + "% - Wind: "
						+ report.getWindDirection() + " "
						+ report.getWindSpeed() + "mph\n");
				cond.append("Sunrise: " + report.getSunrise() + " - Sunset:  "
						+ report.getSunset());
				condition.setText(cond.toString());

				StringBuilder fore = new StringBuilder();
				for (int i = 0; (report.getForecasts() != null)
						&& (i < report.getForecasts().length); i++) {
					WeatherForecast fc = report.getForecasts()[i];
					fore.append(fc.getDay() + ":\n");
					fore.append(fc.getCondition().getDisplay() + " High:"
							+ fc.getHigh() + " F - Low:" + fc.getLow() + " F");
					if (i == 0) {
						fore.append("\n\n");
					}
				}

				forecast.setText(fore.toString());
				String resPath = "com.justinsoliz.weatherreport:drawable/"
						+ "cond" + report.getCondition().getId();
				int resId = getResources().getIdentifier(resPath, null, null);
				conditionImage.setImageDrawable(getResources().getDrawable(
						resId));
			}
		}
	};

	@Override
	public void onCreate(final Bundle icicle) {
		super.onCreate(icicle);
		Log.v(Constants.LOGTAG, " " + ReportViewDetail.CLASSTAG + " onCreate");

		this.setContentView(R.layout.report_view_detail);

		this.location = (TextView) findViewById(R.id.view_location);
		this.date = (TextView) findViewById(R.id.view_date);
		this.condition = (TextView) findViewById(R.id.view_condition);
		this.forecast = (TextView) findViewById(R.id.view_forecast);
		this.conditionImage = (ImageView) findViewById(R.id.condition_image);
		this.currentCheck = (CheckBox) findViewById(R.id.view_configure_alerts);

		// currentCheck listener, enable/disable alerts
		this.currentCheck
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					public void onCheckedChanged(final CompoundButton button,
							final boolean isChecked) {
						Log.v(Constants.LOGTAG, " " + ReportViewDetail.CLASSTAG
								+ " onCheckedChanged - isChecked - "
								+ isChecked);
						updateAlertStatus(isChecked);
					}
				});

		// start the service - though it may already have been started on boot
		// multiple starts don't hurt it, and if app is installed and device NOT
		// booted needs this
		// TODO: WeatherAlertService
		startService(new Intent(this, WeatherAlertService.class));
	}

	@Override
	public void onStart() {
		super.onStart();
		dbHelper = new DBHelper(this);
		deviceZip = WeatherAlertService.deviceLocationZIP;

		if ((getIntent().getData() != null)
				&& (getIntent().getData().getEncodedQuery() != null)
				&& (getIntent().getData().getEncodedQuery().length() > 8)) {
			String queryString = getIntent().getData().getEncodedQuery();
			reportZip = queryString.substring(4, 9);
			useDeviceLocation = false;
		} else {
			reportZip = deviceZip;
			useDeviceLocation = true;
		}
		savedLocation = dbHelper.get(reportZip);
		deviceAlertEnabledLocation = dbHelper
				.get(DBHelper.DEVICE_ALERT_ENABLED_ZIP);
		if (useDeviceLocation) {
			currentCheck.setText(R.string.view_checkbox_current);
			if (deviceAlertEnabledLocation != null) {
				currentCheck.setChecked(true);
			} else {
				currentCheck.setChecked(false);
			}
		} else {
			currentCheck.setText(R.string.view_checkbox_specific);
			if (savedLocation != null) {
				if (savedLocation.alertEnabled == 1) {
					currentCheck.setChecked(true);
				} else {
					currentCheck.setChecked(false);
				}
			}
		}
		loadReport(reportZip);
	}

	private void loadReport(final String zip) {
		Log.v(Constants.LOGTAG, " " + ReportViewDetail.CLASSTAG + " loadReport");
		Log.v(Constants.LOGTAG, " " + ReportViewDetail.CLASSTAG + "    zip - "
				+ zip);

		this.progressDialog = ProgressDialog.show(this,
				getResources().getText(R.string.view_working), getResources()
						.getText(R.string.view_get_report), true, false);
		
		final YWeatherFetcher weatherFetcher = new YWeatherFetcher(zip);
		
		// get report in a separate thread for ProgressDialog/Handler
		// when complete send "empty" msg to handler indicating thread is done
		new Thread() {
			
			@Override
			public void run() {
				report = weatherFetcher.getWeather();
				Log.v(Constants.LOGTAG, " " + ReportViewDetail.CLASSTAG + "  report - " + report);
				handler.sendEmptyMessage(0);
			}
		}.start();
	}

	private void updateAlertStatus(final boolean isChecked) {
		Log.v(Constants.LOGTAG, " " + ReportViewDetail.CLASSTAG
				+ " updateAlertStatus - " + isChecked);
		// Non Device
		if (!this.useDeviceLocation) {
			if (isChecked) {
				// no loc at all, create it as saved and set alertenabled 1
				if (this.savedLocation == null) {
					Location loc = new Location();
					loc.alertEnabled = 1;
					loc.lastAlert = 0;
					loc.zip = this.reportZip;
					loc.city = this.report.getCity();
					loc.region = this.report.getRegion();
					this.dbHelper.insert(loc);
					// if loc already is saved, just update alertenabled
				} else {
					this.savedLocation.alertEnabled = 1;
					this.dbHelper.update(this.savedLocation);
				}
			}
		}
	}
}