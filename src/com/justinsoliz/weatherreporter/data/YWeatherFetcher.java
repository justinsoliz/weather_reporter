package com.justinsoliz.weatherreporter.data;

import java.net.URL;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.util.Log;

import com.justinsoliz.weatherreporter.Constants;

/**
 * Invoke Yahoo! Weather API and parse into WeatherRecord.
 * 
 * @see YWeatherHandler
 * 
 * @author justinsoliz
 * 
 */

public class YWeatherFetcher {

	private static final String CLASSTAG = YWeatherFetcher.class
			.getSimpleName();
	private static final String QBASE = "http://weather.yahooapis.com/forecastrss?p=";

	private String query;
	private String zip;

	public YWeatherFetcher(String zip, boolean overrideSevere) {

		// validate location is a zip
		if (zip == null || zip.length() != 5 || !isNumeric(zip)) {
			return;
		}

		this.zip = zip;

		// build query
		this.query = YWeatherFetcher.QBASE + this.zip;
	}

	public YWeatherFetcher(String zip) {
		this(zip, false);
	}

	public WeatherRecord getWeather() {
    	WeatherRecord weatherRecord = new WeatherRecord();
    	
    	try	{
    		URL url = new URL(this.query);
    		SAXParserFactory parserFactory = SAXParserFactory.newInstance();
    		SAXParser parser = parserFactory.newSAXParser();
    		XMLReader reader = parser.getXMLReader();
    		YWeatherHandler handler = new YWeatherHandler();
    		reader.setContentHandler(handler);
    		reader.parse(new InputSource(url.openStream()));
    		// after parsed, get record
    		weatherRecord = handler.getWeatherRecord();
    		weatherRecord.setOverrideSevere(true);
    	} catch (Exception ex) {
    		Log.e(Constants.LOGTAG, " " + YWeatherFetcher.CLASSTAG, ex);
    	}
    	return weatherRecord;
    }
	
    public static WeatherRecord getMockRecord() {
        WeatherRecord r = new WeatherRecord();
        r.setCity("Crested Butte");
        r.setCondition(WeatherCondition.SUNNY);
        r.setCountry("US");
        r.setDate("03-08-2008");
        WeatherForecast[] forecasts = new WeatherForecast[2];
        WeatherForecast w1 = new WeatherForecast();
        w1.setCondition(WeatherCondition.HEAVY_SNOW_WINDY);
        w1.setDate("03-09-2008");
        w1.setDay("Sun");
        w1.setHigh(22);
        w1.setLow(3);
        WeatherForecast w2 = new WeatherForecast();
        w2.setCondition(WeatherCondition.FAIR_DAY);
        w2.setDate("03-10-2008");
        w2.setDay("Mon");
        w2.setHigh(28);
        w2.setLow(5);
        forecasts[0] = w1;
        forecasts[1] = w2;
        r.setForecasts(forecasts);
        r.setHumidity(100);
        r.setLink("link");
        r.setPressure(30.4);
        r.setPressureState(WeatherRecord.PRESSURE_RISING);
        r.setRegion("CO");
        r.setSunrise("6:27 am");
        r.setSunset("6:11 pm");
        r.setTemp(11);
        r.setVisibility(250);
        r.setWindChill("-12");
        r.setWindDirection("NNE");
        r.setWindSpeed(23);
        return r;
    }

	private boolean isNumeric(String s) {
		try {
			Integer.parseInt(s);
		} catch (NumberFormatException ex) {
			return false;
		} catch (NullPointerException ex) {
			return false;
		}
		return true;
	}
}