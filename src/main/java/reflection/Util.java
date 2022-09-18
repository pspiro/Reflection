package reflection;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.simple.JSONObject;

import com.ib.client.Decimal;

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
	static boolean inside( int conid, String hours) throws RefException {
		return inside( new Date(), conid, hours);
	}
	
	/** These are broken out to facilitate testing. */
	public static boolean inside( Date now, int conid, String hours) throws RefException {
		Main.require (hours != null, RefCode.UNKNOWN, "Null trading hours for %s", conid);

		try {
			// put current date/time in same format as received from server
			//Date now = new Date();
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
	static Json toJsonMsg( Object... strs) {
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
}

