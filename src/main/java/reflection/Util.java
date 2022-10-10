package reflection;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.json.simple.JSONObject;

import com.ib.client.Decimal;

import tw.util.S;

public class Util {
	static SimpleDateFormat fmt = new SimpleDateFormat( "HH:mm:ss.SSS");
	static SimpleDateFormat hhmm = new SimpleDateFormat( "kkmm");
	static SimpleDateFormat yyyymmdd = new SimpleDateFormat( "yyyyMMdd");
	static SimpleDateFormat todayFmt = new SimpleDateFormat( "yyyy-MM-dd");

	static String now() { 
		return fmt.format( new Date() ); 
	}

	static String today() { 
		return todayFmt.format( new Date() ); 
	}
	
	
	public static void main(String[] args) throws RefException {
	}

	/** Typical format of hours string is:
		20220807:CLOSED
		20220808:0930-20220808:1600
		20220809:0930-20220809:1600
		@return true if we are currently inside of the hours. 
	 * @throws RefException */
	static boolean inside( int conid, String hours, String timeZoneId) throws RefException {
		// simulated trading? always return true
		if (Main.simulate() ) {
			return true;
		}
		
		return inside( new Date(), conid, hours, timeZoneId);
	}
	
	/** These are broken out to facilitate testing. */
	public static boolean inside( Date now, int conid, String hours, String timeZoneIdIn) throws RefException {
		Main.require (hours != null, RefCode.UNKNOWN, "Null trading hours for %s", conid);

		try {
			String timeZoneId = S.isNotNull( timeZoneIdIn) ? timeZoneIdIn : "America/New_York";
			TimeZone zone = TimeZone.getTimeZone(timeZoneId);
			Main.require( zone != null, RefCode.UNKNOWN, "Invalid time zone id %s", timeZoneId);

			SimpleDateFormat yyyymmdd = new SimpleDateFormat( "yyyyMMdd");
			yyyymmdd.setTimeZone(zone);

			SimpleDateFormat hhmm = new SimpleDateFormat( "kkmm");
			hhmm.setTimeZone( zone);			
			
			// put current date/time in same format and time zone as received from server
			// Date now = new Date();
			String curDateTime = String.format( "%s:%s", yyyymmdd.format( now), hhmm.format( now) );  
			
			String[] sessions = hours.split( ";");
			for (String session : sessions) {
				String[] sessionSpan = session.split( "-");
				String sessionStart = sessionSpan[0];
				
				// skip closed sessions, we don't care
				if (sessionStart.toLowerCase().endsWith( "closed") ) {
					continue;
				}
				
				String sessionEnd = sessionSpan[1];
				
				if (curDateTime.compareTo( sessionStart) >= 0 &&
					curDateTime.compareTo( sessionEnd) < 0) {
					
					return true;
				}
			}
			
			return false;
		}
		catch( Exception e) {
			throw new RefException( RefCode.UNKNOWN, "Invalid trading hours for conid %s: %s", conid, hours);
		}
	}
	
//	static boolean between(String today, String nowTime, String sessionStart, String sessionEnd) {
//		String[] startToks = sessionStart.split( ":");
//		String startDate = startToks[0];
//		String startTime = startToks[1];
//		
//		String[] endToks = sessionStart.split( ":");
//		String endDate = startToks[0];
//		String endTime = startToks[1];
//		
//		return compare( today, nowTime, startDate, startTime) >= 0 &&
//			   compare( today)
//		return today.compareTo( startDate) >= 0 &&
//		
//		return 
//		
//			String closeDate = openClose[1].split( ":")[0];  // if close date is tomorrow we should always return true, but this is only needed for ES and such
//			String closeTime = openClose[1].split( ":")[1];
//			
//			return nowTime.compareTo( openTime) >= 0 && 
//				   nowTime.compareTo( closeTime) <= 0;
//		}
//	}
//	

	//static String CRLF = "\r\n";
	public static String formatStr = "\"%s\": \"%s\"";
	public static String formatStrWc = ", \"%s\": \"%s\"";

	/** Create the whole Json message, including the time.
	 *  @param strs tag/value pairs */
	static Json toJsonMsg( Object... strs) { // get rid of this. pas
		JSONObject obj = new JSONObject();
		
		// always start w/ the time
		obj.put( "time", now() );
		
		Object tag = null;
		for (Object val : strs) {
			if (tag == null) {
				tag = val;
			}
			else {
				if (isNumeric( val) ) {
					obj.put( tag, val);
				}
				else if (val != null) {
					obj.put( tag, val.toString() );
				}
				tag = null;
			}
		}
		return new Json( obj);
	}

	/** Numbers we will put without quotation marks; everything else gets quotation marks. */
	private static boolean isNumeric(Object val) {
		return val instanceof Integer || val instanceof Double || val instanceof Long;
	}

	/** Replace all CRLF and LF with ' ' */
	public static String flatten( String str) {
		String flat = str.replaceAll( "\r\n", " ").replaceAll( "\n", " ");
		return flat;
	}
	
	public static double difference(Decimal v1, Decimal v2) {
		return Math.abs( v1.toDouble() - v2.toDouble() );
	}

	/** Round to two decimals. */
	public static double round(double price) {
		return Math.round( price * 100) / 100.0;		
	}
	
	static boolean isNumeric(String str) {
		try {
			Double.valueOf( str);
			return true;
		}
		catch( Exception e) {
			return false;
		}
	}

	static String readResource(Class cls, String filename) throws IOException {
        InputStream is = cls.getClassLoader().getResourceAsStream(filename);
        byte[] data = new byte[100];
        return new String( data, 0, is.read(data, 0, data.length) );
	}
	
	/** Convert hex to decimal and then apply # of decimal digits.
	 *  Could be used to read the transaction size from the logs. */
	public static double hexToDec(String str, int decDigits) {
		// strip off 0x
		if (startsWith( str, "0x") ) {
			str = str.substring( 2);
		}
		
		BigInteger tot = new BigInteger("0", 16);

		for (int i = str.length() - 1; i >= 0; i--) {
			int val = Character.digit(str.charAt(i), 16); // convert hex char to integer 0-15
			if (val != 0) {
	        	BigInteger mult = new BigInteger( "16").pow( str.length() - i - 1);
	        	BigInteger dec = new BigInteger(String.valueOf( val)).multiply( mult);
	        	tot = tot.add( dec);
			}
        }
		
        BigInteger div = new BigInteger("10").pow( decDigits);
        BigDecimal ans = new BigDecimal( tot).divide( new BigDecimal( div) );
        return ans.doubleValue();
    }

	/** Ignores case! str can be null */
	public static boolean startsWith(String str, String str2) {
		return str != null && str.toLowerCase().startsWith( str2.toLowerCase() );
	}

	public static boolean validToken(String token) {
		return token.length() == 42 && startsWith( token, "0x");
	}
	
	public static String tab(int level) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < level * 3; i++) {
			sb.append( " ");
		}
		return sb.toString();
	}
}
