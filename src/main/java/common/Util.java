package common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.BooleanSupplier;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.json.simple.JsonObject;

import com.ib.client.Decimal;

import reflection.RefCode;
import reflection.RefException;
import tw.util.S;

public class Util {
	public static final int HOUR = 60*60*1000;
	// hh  // 12 hr, useless, use w/ am/pm
	// HH  // 24 hr, midnight is 00
	// kk  // 24 hr, midnight is 24
	static Random rnd = new Random();
	public static SimpleDateFormat yyyymmdd = new SimpleDateFormat( "yyyyMMdd");
	public static SimpleDateFormat hhmmss = new SimpleDateFormat("HH:mm:ss");
	//static SimpleDateFormat yToS = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss");  12 hr clock, useless 
	public static SimpleDateFormat yToS = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss");  // 24 hr clock
	//private static final SimpleDateFormat hhmmEST = new SimpleDateFormat( "kk:mm:ss");  // what is this?
    //static final DateFormat dateAndTime = new SimpleDateFormat( "MM/dd/yy HH:mm"); 
	//SimpleDateFormat yyyymmdd = new SimpleDateFormat( "yyyy-MM-dd");

	/** Use this to return values from asynchronous methods */
	public static class ObjectHolder<T> {
		public T val;
	}
	
	/** Runnable, returns void, throws Exception */
	public interface ExRunnable {
		void run() throws Exception;
	}

	/** Same as Consumer but allows exception */
	public interface ExConsumer<T> {
		void accept(T t) throws Exception;
	}

	/** Do a decimal compare down to six digits */
	public static boolean isGtEq(double larger, double smaller) {
		return larger - smaller >= -.000001;
	}

	/** Do a decimal compare down to six digits */
	public static boolean isLtEq(double smaller, double larger) {
		return isGtEq(larger, smaller);
	}

	/** Do a decimal compare down to four digits */
	public static boolean isEq(double d1, double d2, double tolerance) {
		return Math.abs(d1 - d2) < tolerance;
	}

	/** Typical format of hours string is:
		20220807:CLOSED
		20220808:0930-20220808:1600
		20220809:0930-20220809:1600
		@return true if we are currently inside of the hours. 
	 * @throws RefException */

	/** These are broken out to facilitate testing. */
	public static boolean inside( Date now, int conid, String hours, String timeZoneIdIn) throws Exception {
		require(hours != null, "Null trading hours for %s", conid);

		try {
			String timeZoneId = S.isNotNull( timeZoneIdIn) ? timeZoneIdIn : "America/New_York";
			TimeZone zone = TimeZone.getTimeZone(timeZoneId);
			require( zone != null, "Invalid time zone id %s", timeZoneId);

			SimpleDateFormat yyyymmdd = new SimpleDateFormat( "yyyyMMdd");
			yyyymmdd.setTimeZone(zone);

			SimpleDateFormat hhmm = new SimpleDateFormat( "kkmm");
			hhmm.setTimeZone( zone);			
			
			// put current date/time in same format and time zone as received from server
			// Date now = new Date();
			String curDateTime = String.format( "%s:%s", yyyymmdd.format( now), hhmm.format( now) );  
			
			String[] sessions = hours.split( ";");
			for (String session : sessions) {
				// protect against strings starting with ; (which I saw once) or having ;; or ; ;
				if (session == null || S.isNull(session.trim()) ) {
					continue;
				}
				
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
	
	
	public static void main(String[] args) throws Exception {
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
	 * Note that enums are added as object but deserialized as strings
	 *  @param strs tag/value pairs */
	public static JsonObject toJson( Object... strs) {
		JsonObject obj = new JsonObject();
		
		Object tag = null;
		for (Object val : strs) {
			if (tag == null) {
				tag = val;
			}
			else {
				obj.put( tag.toString(), val);
				tag = null;
			}
		}
		return obj;
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

	public static String readResource(Class cls, String filename) throws IOException {
        InputStream is = cls.getClassLoader().getResourceAsStream(filename);
        byte[] data = new byte[100];
        return new String( data, 0, is.read(data, 0, data.length) );
	}
	
	/** Convert hex to integer to decimal and then apply # of decimal digits.
	 *  Could be used to read the transaction size from the logs.
	 *  @param hexVal can start with 0x or not */
	public static double hexToDec(String hexVal, int decDigits) {
		// strip off 0x
		if (startsWith( hexVal, "0x") ) {
			hexVal = hexVal.substring( 2);
		}
		
		BigInteger tot = new BigInteger(hexVal, 16);
        BigInteger div = new BigInteger("10").pow( decDigits);
        BigDecimal ans = new BigDecimal( tot).divide( new BigDecimal( div) );
        return ans.doubleValue();
    }

	/** Ignores case! str can be null */
	public static boolean startsWith(String str, String str2) {
		return str != null && str.toLowerCase().startsWith( str2.toLowerCase() );
	}
	
	public static boolean validToken(String token) {
		return token != null && token.length() == 42 && startsWith( token, "0x");
	}
	
	public static String tab(int level) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < level * 3; i++) {
			sb.append( " ");
		}
		return sb.toString();
	}
	
	/** Execute the runnable in a new thread aka invokeLater.
	 *  Consider using ThreadQueue if you want all to execute in the same thread */
	public static void execute( String name, Runnable runnable) {
		new Thread(runnable, name).start();
	}
	
	/** Execute the runnable in a new thread aka invokeLater.
	 *  Consider using ThreadQueue if you want all to execute in the same thread */
	public static void execute( Runnable runnable) {
		new Thread(runnable).start();
	}
	
	/** Execute the runnable in a new thread after waiting ms. */
	public static void executeIn( int ms, Runnable runnable) {
		new Thread( () -> {
			S.sleep( ms);
			runnable.run();   // we should catch the exception here and log to our datelogger
		}).start();
	}

	static Timer m_timer;
	
	/** Execute the runnable in a new thread now and every period ms forever. */
	public static synchronized void executeEvery( int wait, int period, Runnable runnable) {
		if (m_timer == null) {
			m_timer = new Timer();
		}
		
		TimerTask task = new TimerTask() {
			@Override public void run() {
				runnable.run();
			}
		};
		
		m_timer.schedule( task, wait, period);
	}

	public static String getenv(String env) throws Exception {
		String str = System.getenv(env);
		require( S.isNotNull( str), "Missing environment variable %s", env);
		return str;
	}
	
	public static String padLeft( String str, int n, char c) {
		StringBuilder sb = new StringBuilder(str);
		while (sb.length() < n) {
			sb.insert( 0, c);
		}
		return sb.toString();
	}
	
	public static String padRight( String str, int n, char c) {
		StringBuilder sb = new StringBuilder(str);
		while (sb.length() < n) {
			sb.append( c);
		}
		return sb.toString();
	}

	public static void require(boolean test, String text, Object... params) throws Exception {
		if (!test) {
			throw new Exception( String.format( S.notNull( text), params) );
		}
	}

	/** Use this in more places. */
	public static String concatenate(char separator, String... values) {
		StringBuilder builder = new StringBuilder();
		for (String value : values) {
			if (builder.length() > 0) {
				builder.append(separator);
			}
			builder.append( value);
		}
		return builder.toString();
	}

	/** Replace single-quotes with double-single-quotes.
	 *  This is needed when inserting or updating SQL records. */
	public static String dblQ(String sql) {
		return sql.replaceAll( "'", "''");  
	}

	public static class Ex extends Exception {
		public Ex( String format, Object... params) {
			super( String.format( format, params) );
		}
	}

	/** Replace single-quotes with double-quotes
	 *  @deprecated, use toJsonMsg() instead */
	public static String fmtJson(String str) {  // not a good name
		return str.replaceAll( "\\'", "\"");
	}
	
	public static boolean isValidAddress( String str) {
		return str.length() == 42 && str.toLowerCase().startsWith("0x"); 
	}
	
	public static void reqValidAddress(String str) throws Exception {
		require( isValidAddress(str), "Invalid address: %s", str);
	}
	
	public static String getLastToken(String str, String sep) {
		String[] ar = str.split(sep);
		return ar[ar.length-1];
	}

	public static String isoNow() {
		return DateTimeFormatter.ISO_INSTANT.format( Instant.now() );
	}

	public static boolean isPrimitive(Class clas) {
		return clas == String.class || clas == Integer.class || clas == Double.class || clas == Long.class || clas == Boolean.class || clas == Float.class;
	}

	/** Numbers we will put without quotation marks; everything else gets quotation marks. */
//	public static boolean isNumeric(Object val) {
//		return val instanceof Integer || val instanceof Double || val instanceof Long || val instanceof Float;
//	}
	
	static boolean isDouble(String str) {
		try {
			Double.valueOf( str);
			return true;
		}
		catch( Exception e) {
			return false;
		}
	}

	public static boolean isInteger(String conidStr) {
		try {
			Integer.valueOf(conidStr);
			return true;
		}
		catch( Exception e) {
			return false;
		}
	}

	
	
//	public static String toLowerCase(String address) {
//		return notNull(address).toLowerCase();
//	}
	
	/** Clip the bounds, don't throw exception, handle null string */
	public static String left( String str, int max) {
		str = S.notNull(str);
		return str.substring( 0, Math.min(str.length(), max) );
	}

	/** Clip the bounds, don't throw exception, handle null string */
	public static String right(String str, int max) {
		str = S.notNull(str);
		return str.substring( str.length() - Math.min(str.length(), max) );
	}

	/** Clip the bounds, don't throw exception, handle null string */
	public static String substring(String str, int start, int end) {
		str = S.notNull(str);
		return str.substring(
				Math.min(start, str.length() ),
				Math.min(end, str.length() ) );
	}
	
	/** Default value is used for null input only */ 
	public static <T extends Enum<T>> T getEnum(String text, T[] values, T defVal) throws IllegalArgumentException {
		return S.isNull(text) ? defVal : getEnum(text, values);
	}

	/** Lookup enum by ordinal. Use Enum.valueOf() to lookup by string. */
	public static <T extends Enum<T>> T getEnum(String text, T[] values) throws IllegalArgumentException {
		for (T val : values) {
			if (val.toString().equalsIgnoreCase(text) ) {
				return val;
			}
		}
		String str = String.format( "'%s' is not a valid value for enum %s", text, values[0].getClass().getName() );
		throw new IllegalArgumentException( str);
	}
		
	public static String allEnumValues(Object[] values) {
		return Arrays.asList(values).toString();
	}

	/** Return an id of n chars where each char is between a and z */
	public static String uid(int n) {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < n; i++) 
			b.append( (char)('A' + rnd.nextInt(26) ) );
		return b.toString();
	}

	/** Return an id of n chars where each char is between 0 and 9 */
	public static String uin(int n) {  //note that it could begin with a zero
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < n; i++) 
			b.append( (char)('0' + rnd.nextInt(10) ) );
		return b.toString();
	}

	public interface Creator<T> {
		T instance();
	}

	public static <Tag,Val> Val getOrCreate(Map<Tag,Val> map, Tag tag, Creator<Val> creator) {
		synchronized(map) {
			Val val = map.get(tag);
			if (val == null) {
				val = creator.instance();
				map.put( tag, val);  // could use putIfAbsent() here
			}
			return val;
		}
	}
	
	/** Put if absent and return the value currently stored in the map;
	 *  use this on synchronized maps */
	// not used
//	public static <Tag,Val> Val putIfAbsent( Map<Tag,Val> map, Tag tag, Val newVal) { 
//		Val oldVal = map.putIfAbsent(tag, newVal);
//		return oldVal != null ? oldVal : newVal;
//	}

	/** Simple wrapper which prints stack trace */
	public static void wrap(ExRunnable runner) {
		try {
			runner.run();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	/** Truncate n to a number of decimal digits */
	public static double truncate(double n, int digits) throws Exception {
		require( n >= 0, "Cannot truncate negative numbers"); // you could fix this if needed
		double mult = Math.pow(10, digits);
		return Math.floor( n * mult) / mult;
	}

	static SimpleDateFormat fmt = new SimpleDateFormat( "yyyy/MM/dd kk:mm:ss");
	public static String fmtTime(long t) {
		return t == 0 ? "" : fmt.format( new Date(t) );
	}

	/** Must be in format of a@b.c */
	public static boolean isValidEmail(String email) {
		int i = email.indexOf("@");
		return i >= 1 && email.lastIndexOf(".") > i + 1 && email.length() >= 5;
	}
	
	/** My version of forEach that propogates up an exception */ 
	public static <T> void forEach(Iterable<T> iter, ExConsumer<? super T> action) throws Exception {
        Objects.requireNonNull(action);
        for (T t : iter) {
            action.accept(t);
        }
	}

	/** Wait for user to press enter */
	public static void pause() {
		try(Scanner s = new Scanner(System.in)) {
			s.nextLine();
		}
	}

	public static void iff(BooleanSupplier test, Runnable r1, Runnable r2) {
		if (test.getAsBoolean() )
			r1.run();
		else
			r2.run();
	}
	
	public static void ifEx(BooleanSupplier test, ExRunnable r1, ExRunnable r2) throws Exception {
		if (test.getAsBoolean() )
			r1.run();
		else
			r2.run();
	}

    /** Send an email using SMTP */
	public static void sendEmail(String username, String password, String fromName, String to, String subject, String text, boolean isHtml) throws Exception {
		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.tlsv1.2.enable", "true");  // tls also works but not smarttls
		props.put("mail.smtp.host", "smtp.openxchange.eu");  // put any smpt server here
		props.put("mail.smtp.port", "587");
		
		Session session = Session.getInstance( props,
				new javax.mail.Authenticator() {
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(username, password);
					}
				});

		MimeMessage message = new MimeMessage(session);
		message.setFrom( toEmail( fromName, username) );
		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
		message.setSubject(subject);
		message.setText(text, null, isHtml ? "html" : "plain");

		Transport.send(message);
		S.out( "Sent email '%s' to %s", subject, to);
	}

	
	/** Return email address in this format: "Peter Spiro <peteraspiro@gmail.com>" */
	private static Address toEmail(String name, String email) throws AddressException {
		return new InternetAddress( String.format( "%s <%s>", name, email) );
	}

	/** Convert Throwable to Exception */
	public static Exception toException(Throwable e) {
		return e instanceof Exception ? (Exception)e : new Exception(e);
	}

	/** Show prompt and input string; Scanner does not work */
	public static String input(String str) {
		S.out( str);
		try (BufferedReader buf = new BufferedReader(new InputStreamReader(System.in) ) ) {
			return buf.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
}
