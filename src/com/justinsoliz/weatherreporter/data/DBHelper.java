package com.justinsoliz.weatherreporter.data;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.justinsoliz.weatherreporter.Constants;

public class DBHelper {

	public static final String DEVICE_ALERT_ENABLED_ZIP = "DAEZ99";
	public static final String DB_NAME = "w_alert";
	public static final String DB_TABLE = "w_alert_loc";
	public static final int DB_VERSION = 3;

	private static final String CLASSNAME = DBHelper.class.getSimpleName();
	private static final String[] COLS = new String[] { "_id", "zip", "city",
			"region", "lastalert", "alertenabled" };
	private SQLiteDatabase db;
	private final DBOpenHelper dbOpenHelper;

	/**
	 * Begin inner classes
	 */

	public static class Location {

		public long id;
		public long lastAlert;
		public int alertEnabled;
		public String zip;
		public String city;
		public String region;

		public Location() {
		}

		public Location(final long id, final long lastAlert,
				final int alertEnabled, final String zip, final String city,
				final String region) {
			this.id = id;
			this.lastAlert = lastAlert;
			this.alertEnabled = alertEnabled;
			this.zip = zip;
			this.city = city;
			this.region = region;
		}

		@Override
		public String toString() {
			return this.zip + " " + this.city + ", " + this.region;
		}
	}

	private static class DBOpenHelper extends SQLiteOpenHelper {

		private static final String DB_CREATE = "CREATE TABLE "
				+ DBHelper.DB_TABLE
				+ " (_id INTEGER PRIMARY KEY, zip TEXT UNIQUE NOT NULL, city TEXT, region TEXT, lastalert INTEGER, alertenabled INTEGER);";

		public DBOpenHelper(final Context context) {
			super(context, DBHelper.DB_NAME, null, DBHelper.DB_VERSION);
		}

		@Override
		public void onCreate(final SQLiteDatabase db) {
			try {
				db.execSQL(DBOpenHelper.DB_CREATE);
			} catch (SQLException ex) {
				Log.e(Constants.LOGTAG, DBHelper.CLASSNAME, ex);
			}
		}

		@Override
		public void onOpen(final SQLiteDatabase db) {
			super.onOpen(db);
		}

		@Override
		public void onUpgrade(final SQLiteDatabase db, final int oldVersion,
				final int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + DBHelper.DB_TABLE);
			onCreate(db);
		}
	}

	/**
	 * end inner classes
	 */

	public DBHelper(final Context context) {
		this.dbOpenHelper = new DBOpenHelper(context);
		establishDb();
	}

	private void establishDb() {
		if (this.db == null) {
			this.db = this.dbOpenHelper.getWritableDatabase();
		}
	}

	public void cleanup() {
		if (this.db != null) {
			this.db.close();
			this.db = null;
		}
	}

	public void insert(final Location location) {
		ContentValues values = new ContentValues();
		values.put("zip", location.zip);
		values.put("city", location.city);
		values.put("region", location.region);
		values.put("lastalert", location.lastAlert);
		values.put("alertenabled", location.alertEnabled);
		this.db.insert(DBHelper.DB_TABLE, null, values);
	}
	
	public void update(final Location location) {
        ContentValues values = new ContentValues();
        values.put("zip", location.zip);
        values.put("city", location.city);
        values.put("region", location.region);
        values.put("lastalert", location.lastAlert);
        values.put("alertenabled", location.alertEnabled);
        this.db.update(DBHelper.DB_TABLE, values, "_id=" + location.id, null);
	}

	public void delete(final long id) {
		this.db.delete(DBHelper.DB_TABLE, "_id=" + id, null);
	}

	public void delete(final String zip) {
		this.db.delete(DBHelper.DB_TABLE, "zip='" + zip + "'", null);
	}

	public Location get(final String zip) {
		Cursor c = null;
		Location location = null;

		try {
			c = this.db.query(true, DBHelper.DB_TABLE, DBHelper.COLS, "ZIP= '"
					+ zip + "'", null, null, null, null, null);

			if (c.getCount() > 0) {
				c.moveToFirst();
				location = new Location();
				location.id = c.getLong(0);
				location.zip = c.getString(1);
				location.city = c.getString(2);
				location.region = c.getString(3);
				location.lastAlert = c.getLong(4);
				location.alertEnabled = c.getInt(5);
			}
		} catch (SQLException ex) {
			Log.v(Constants.LOGTAG, DBHelper.CLASSNAME, ex);
		} finally {
			if (c != null && !c.isClosed()) {
				c.close();
			}
		}
		return location;
	}

	public List<Location> getAll() {
		ArrayList<Location> ret = new ArrayList<Location>();
		Cursor c = null;

		try {
			c = this.db.query(DBHelper.DB_TABLE, DBHelper.COLS, null, null,
					null, null, null);
			int numRows = c.getCount();
			c.moveToFirst();

			for (int i = 0; i < numRows; ++i) {
				Location location = new Location();
				location.id = c.getLong(0);
				location.zip = c.getString(1);
				location.city = c.getString(2);
				location.region = c.getString(3);
				location.lastAlert = c.getLong(4);
				location.alertEnabled = c.getInt(5);

				// don't return special device alert enabled marker location in
				// all list
				if (!location.zip.equals(DBHelper.DEVICE_ALERT_ENABLED_ZIP)) {
					ret.add(location);
				}
				c.moveToNext();
			}
		} catch (SQLException ex) {
			Log.v(Constants.LOGTAG, DBHelper.CLASSNAME, ex);
		} finally {
			if (c != null && !c.isClosed()) {
				c.close();
			}
		}
		return ret;
	}

	public List<Location> getAllAlertEnabled() {
		Cursor c = null;
		ArrayList<Location> ret = new ArrayList<Location>();
		
		try {
			c = this.db.query(DBHelper.DB_TABLE, DBHelper.COLS, "alertenabled = 1", null, null, null, null);
			int numRows = c.getCount();
			
			for (int i = 0; i < numRows; ++i) {
				Location location = new Location();
				location.id = c.getLong(0);
				location.zip = c.getString(1);
				location.city = c.getString(2);
				location.region = c.getString(3);
				location.lastAlert = c.getLong(4);
				location.alertEnabled = c.getInt(5);

				// don't return special device alert enabled marker location in
				// all list
				if (!location.zip.equals(DBHelper.DEVICE_ALERT_ENABLED_ZIP)) {
					ret.add(location);
				}
				c.moveToNext();
			}
		} catch (SQLException ex) {
				Log.v(Constants.LOGTAG, DBHelper.CLASSNAME, ex);
		} finally {
			if (c!= null && !c.isClosed()) {
				c.close();
			}
		}
		return ret;
	}
}