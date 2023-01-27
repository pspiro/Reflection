package test;

import static test.TestErrors.sendData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import junit.framework.TestCase;
import reflection.Prices;
import reflection.RefCode;
import tw.util.S;

public class TestOne extends TestCase {
	static SimpleDateFormat hhmm = new SimpleDateFormat( "kk:mm");

	static {
		TimeZone zone = TimeZone.getTimeZone("America/New_York");
		hhmm.setTimeZone( zone);			
	}
	
	/** Check to see if we are in extended trading hours or not so we know which 
	 * market data to use for the ETF's. For now it's hard-coded from 4am to 8pm; 
	 * better would be to check against the trading hours of an actual ETF. */
	public void testCheckTime() {
		String now = hhmm.format( new Date() );
		boolean inside = now.compareTo("04:00") >= 0 && now.compareTo("20:00") < 0;
		assertTrue( !inside);
	}
}