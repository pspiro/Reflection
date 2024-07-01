package common;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.SynchronousQueue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.swing.JOptionPane;

import org.json.simple.JsonObject;

import com.ib.client.Decimal;

import reflection.RefCode;
import reflection.RefException;
import tw.util.S;
import web3.CreateKey;

/** note use Keys.toChecksumAddress() to get EIP55 mixed case address */
public class Util {
	public static final int MINUTE = 60 * 1000;
	public static final int HOUR = 60 * MINUTE;
	public static final int DAY = HOUR * 24;
	
	// hh  // 12 hr, useless, use w/ am/pm
	// HH  // 24 hr, midnight is 00
	// kk  // 24 hr, midnight is 24
	public static Random rnd = new Random();
	public static SimpleDateFormat hhmmss = new SimpleDateFormat("HH:mm:ss");
	public static SimpleDateFormat yToS = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss");  // 24 hr clock
	// there are more formats in S class

	static {
		TimeZone zone = TimeZone.getTimeZone( "America/New_York" );
		hhmmss.setTimeZone( zone);
		yToS.setTimeZone( zone);
	}		
		
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
	
	// use Supplier if you need to return a value, or BiConsumer for two args

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
	 *  Note that enums are added as object but deserialized as strings.
	 *  Null values are not added.
	 *  @param strs tag/value pairs */
	public static JsonObject toJson( Object... strs) {
		JsonObject obj = new JsonObject();
		
		Object tag = null;
		for (Object val : strs) {
			if (tag == null) {
				tag = val;
			}
			else {
				obj.putIf( tag.toString(), val);
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
	public static void executeAndWrap( ExRunnable runnable) {
		execute( () -> wrap( () -> runnable.run() ) );
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

	/** Use this in more places. */  // try String.join(), dummy!
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
	public static String easyJson(String format, Object... params) {
		return String.format(format, params).replaceAll( "\\'", "\"");
	}
	
	public static boolean isValidAddress( String str) {
		return str != null && str.length() == 42 && str.toLowerCase().startsWith("0x"); 
	}
	
	public static String reqValidAddress(String str) throws Exception {
		require( isValidAddress(str), "Invalid address: %s", str);
		return str;
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

	/** Return string all the way to the end */
	public static String substring(String str, int start) {
		return substring( str, start, Integer.MAX_VALUE);
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

	/** get or create, no exception */
	public static <Tag,Val> Val getOrCreate(Map<Tag,Val> map, Tag tag, Supplier<Val> creator) {
		synchronized(map) {
			Val val = map.get(tag);
			if (val == null) {
				val = creator.get();
				map.put( tag, val);  // could use putIfAbsent() here
			}
			return val;
		}
	}
	
	public interface ExSupplier<T> {
	    T get() throws Exception;
	}

	/** get or create; create can throw an exception */
	public static <Tag,Val> Val getOrCreateEx(Map<Tag,Val> map, Tag tag, ExSupplier<Val> creator) throws Exception {
		synchronized(map) {
			Val val = map.get(tag);
			if (val == null) {
				val = creator.get();
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
			e.printStackTrace();  // calls toString() which prints exception type and message
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

	public interface ExBiConsumer<K,V> {
	    void accept(K k, V v) throws Exception;
	}
			
	/** My version of forEach that propogates up an exception */ 
	public static <T,V> void forEach(Map<T,V> map, ExBiConsumer<T,V> consumer) throws Exception {
        Objects.requireNonNull(consumer);
		for (Entry<T,V> entry : map.entrySet() ) {
        	consumer.accept( entry.getKey(), entry.getValue() );
        }
	}
	
	/** Wait for user to press enter */
	public static void pause() {
		try(Scanner s = new Scanner(System.in)) {
			s.nextLine();
		}
	}
	
	/** Send email from google */
//	public static void sendEmail(String username, String password, 
//			String fromName, String to, String subject, String text, boolean isHtml) throws Exception {
//
//		Auth.auth().getMail().send(
//				fromName, 
//				username, 
//				to, 
//				subject, 
//				text, 
//				isHtml);
//	}
//
//    /** Send an email using SMTP */
//	public static void sendEmail(String username, String password, String fromName, String to, String subject, String text, boolean isHtml) throws Exception {
//		Properties props = new Properties();
//		props.put("mail.smtp.auth", "true");
//		props.put("mail.smtp.tlsv1.2.enable", "true");  // tls also works but not starttls
//		props.put("mail.smtp.host", "smtp.openxchange.eu");  // put any smpt server here
//		props.put("mail.smtp.port", "587");
//		
//		Session session = Session.getInstance( props,
//				new javax.mail.Authenticator() {
//					protected PasswordAuthentication getPasswordAuthentication() {
//						return new PasswordAuthentication(username, password);
//					}
//				});
//
//		MimeMessage message = new MimeMessage(session);
//		message.setFrom( toEmail( fromName, username) );
//		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
//		message.setSubject(subject);
//		message.setText(text, null, isHtml ? "html" : "plain");
//
//		Transport.send(message);
//		S.out( "Sent email '%s' to %s", subject, to);
//	}
//
//	
//	/** Return email address in this format: "Peter Spiro <peteraspiro@gmail.com>" */
//	private static Address toEmail(String name, String email) throws AddressException {
//		return new InternetAddress( String.format( "%s <%s>", name, email) );
//	}

	/** Convert Throwable to Exception */
	public static Exception toException(Throwable e) {
		return e instanceof Exception ? (Exception)e : new Exception(e);
	}

	/** Pop up a dialog, beep, and get user input */ 
	public static String ask(String prompt, Object... params) {
		java.awt.Toolkit.getDefaultToolkit().beep();
		return JOptionPane.showInputDialog( String.format( prompt, params) );
	}

	/** Pop up a dialog, beep, and get user input (double) */ 
	public static double askForVal(String prompt) {
		String val = ask(prompt);
		return S.isNull(val) ? 0 : Double.parseDouble(val);
	}
	
	/** Show message, beep, and wait for user input */
	public static boolean confirm(Component parent, String format, Object... params) {
		java.awt.Toolkit.getDefaultToolkit().beep();
		return JOptionPane.showConfirmDialog( 
				parent, String.format(format,params), "Confirm", JOptionPane.YES_NO_OPTION) == 0;
	}

	/** Show a message and make a beep */
	public static void inform(Component parent, String message, Object... params) {
		java.awt.Toolkit.getDefaultToolkit().beep();
		JOptionPane.showMessageDialog( parent, String.format( S.notNull( message), params) );
	}

	/** Compare two doubles */
	public static int comp( double v1, double v2) {
		return v1 < v2 ? -1 : v1 > v2 ? 1 : 0;
	}

	/** Compare two Comparables but allow for one or both to be null */
	@SuppressWarnings("unchecked")
	public static int compare(Comparable v1, Comparable v2) {
		return v1 != null && v2 != null
				? v1.compareTo(v2)
				: v1 == null && v2 == null
					? 0
					: v1 == null
						? -1 : 1;
	}

	/** Handle null objects */
	public static String toString(Object obj) {
		return obj == null ? "" : obj.toString();
	}

	public static String toMsg(Throwable e) {
		return S.isNotNull( e.getMessage() ) ? e.getMessage() : e.toString(); 
	}

	public static void browse(String url) {
		S.out( "Browsing " + url);
		wrap( () -> Desktop.getDesktop().browse(new URI(url) ) );
	}

	public static void show(HashMap<String,?> m_map) {
		S.out( "[");
		m_map.forEach( (key, val) -> S.out( "%s : %s", key, val) );
		S.out( "]");
	}

	/** Remove all items that return false; could be improved such that
	 *  the filter takes tag/val */
	public static <T,V> void filter(HashMap<T,V> map, Predicate<V> filter) {
		for (Iterator<Entry<T,V>> iter = map.entrySet().iterator(); iter.hasNext(); ) {
			if (!filter.test( iter.next().getValue() ) ) {
				iter.remove();
			}
		}
	}
	
	/** Convert vararg to array */
	@SafeVarargs public static <T> T[] toArray( T... ts) {
		return ts;
	}
	
	/** convert to decimal; accepts null */
	public static double toDouble( Double v) {
		return v == null ? 0 : v;
	}

	/** handles null object and empty string */
	public static double toDouble( Object v) {
		return v == null || S.isNull( v.toString() ) ? 0 : Double.parseDouble( v.toString() );
	}


	/** Copy obj.toString() to clipboard
	 * @param obj can be null */
	public static void copyToClipboard(Object obj) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
        		new StringSelection( toString(obj) ), null);
	}
	
	/** @return value if not null, or default if null */
	public static String valOr( String value, String def) {
		return S.isNotNull(value) ? value : def;
	}
	
	/** return fake EIP-55 address; don't send crypto here, it can never be recovered */
	public static String createFakeAddress() {
		StringBuilder sb = new StringBuilder("0x");
		for (int i = 0; i < 40; i++) {
			sb.append( String.format( "%x", rnd.nextInt(16) ) );
		}
		return sb.toString();  // change to EIP-55 address 
	}

	/** return a wallet private key */
	public static String createPrivateKey() {
		return CreateKey.createPrivateKey();
	}
	
	/** Use this when you want to create an object or retrieve a value and
	 *  then take some action on a single line
	 *
	 *  e.g.
	 *  tweak( new JLabel(text), lab -> lab.set);
	 *  
	 *  doAnd( map.lookup(key), val -> process(val) );
	 *  
	 *  instead of:
	 *    JLabel lab = new JLabel( text);
	 *    lab.setHorizontalAlignment( SwingConstants.CENTER);
	 */
	public static <T> T tweak( T t, Consumer<T> consumer) {
		consumer.accept( t);
		return t;
	}

	/** Retrieve a value and process and return it if not null */ 
	public static <T> T lookup( T t, ExConsumer<T> consumer) throws Exception {
		if (t != null) {
			consumer.accept( t);
		}
		return t;
	}

	/** "if not null"; execute block if object is not null and not empty string
	 *  DOES NOT return a value */
	public static <T> void iff( T obj, ExConsumer<T> consumer) throws Exception {
		if (obj instanceof String ? S.isNotNull((String)obj) : obj != null) {
			consumer.accept( obj);
		}
	}

	/** "if not null"; execute block if object is not null and not empty string
	 *  DOES NOT return a value */
	public static <T> void ifff( T obj, Consumer<T> consumer) {
		if (obj instanceof String ? S.isNotNull((String)obj) : obj != null) {
			consumer.accept( obj);
		}
	}

	/** wrap text like this <tag>text</tag> */
	public static String wrapHtml( String tag, String text) {
		return String.format( "<%s>%s</%s>", tag, text, tag);
	}

	/** append <tag><body></tag> where body is supplied by supplier */
	public static void wrapHtml( StringBuilder sb, String tag, String body) {
		sb.append( wrapHtml( tag, body.toString() ) );
	}
	
	/** append <tag> and </tag> and let consumer add the body text */
	public static void appendHtml( StringBuilder sb, String tag, Runnable consumer) {
		sb.append( String.format( "<%s>", tag) );
		consumer.run();
		sb.append( String.format( "</%s>", tag) );
	}

	/** 12 digits */
	public static boolean isValidAadhaar(String aadhaar) {
		return aadhaar.replaceAll("-", "").matches( "^\\d{12}$");
	}

	public static boolean isValidPan(String pan) {
		return pan.toUpperCase().matches("^[A-Z]{5}[0-9]{4}[A-Z]$");
	}
	
	public static SimpleDateFormat getDateFormatter( String format, TimeZone zone) {
		SimpleDateFormat fmt = new SimpleDateFormat( format);
		fmt.setTimeZone( zone);
		return fmt;
	}

	public static String initialCap(String name) {
		name = name.trim();
		return left( name, 1).toUpperCase() + substring(name, 1).toLowerCase();
	}
	
	public static String unescHtml(String html) {
		return html
				.replaceAll( "%20", " ")
				.replaceAll( "%22", "\"")
				.replaceAll( "%2[cC]", ",")
				.replaceAll( "%2[fF]", "/")
				.replaceAll( "%3[aA]", ":")
				.replaceAll( "%40", "@")
				.replaceAll( "%7[bB]", "{")
				.replaceAll( "%7[dD]", "}")
				;
	}

	/** Return true if it is a valid private key. This only checks length; you could
	 *  check for valid characters as well */
	public static boolean isValidKey(String privateKey) {
		return privateKey != null && privateKey.length() == 64;
	}

	public static void reqValidKey(String privateKey) throws Exception {
		require( isValidKey( privateKey), "%s is not a valid private key", privateKey);
	}
	
	public static String toHex( long val) {
		return "0x" + Long.toHexString( val);
	}

	public static long getLong(String str) {
		return S.isNotNull( str) 
				? str.startsWith( "0x")
						? Long.parseLong( str.substring( 2), 16)
						: Long.parseLong( str)
				: 0;
	}

	/** @param email is in this format: 'name <email>'
	    @return array first name, then email */
	public static String[] parseEmail( String email) {
		String[] parts = email.replace( '<', '>').split( "\\>");
		if (parts.length >= 2) {
			parts[0] = parts[0].trim();
			parts[1] = parts[1].trim().toLowerCase();
			return parts;
		}
		return new String[] { "", "" };
	}

	/** Return full email address w/ name and email */
	public static String formatEmail( String name, String email) {
		return String.format( "%s <%s>", name, email);
	}

	/** wait n seconds for supplier to return true, then fail 
	 *  @return number of seconds, or -1 if it failed */
	public static int waitFor( int sec, ExSupplier<Boolean> sup) throws Exception {
		for (int i = 0; i < sec; i++) {
			S.out( i);
			if (sup.get() ) {
				return i;
			}
			S.sleep(1000);
		}
		return -1;
	}

	/** Sum up all the items in the list where func returns the value of each item */
	public static <T> double sum( Iterable<T> iter, Function<T,Double> func) {
		double sum = 0;
		for (T item : iter) {
			sum += func.apply( item);
		}
		return sum;
	}

	/** Use as follows:
	 *
		sync( queue -> {
			requestSomething( () -> queue.put( "") );
		});
		
		you call queue.put() when done processing to complete the sync block
		
		note that you CANNOT call queue.put() in the same thread as both
		put() AND take() both block; to change this, switch to a ArrayBlockingQueue
		that has at least one item capacity 
	 * 
	 */
	public static void sync( ExConsumer<SynchronousQueue<String>> consumer) throws Exception {
		SynchronousQueue<String> q = new SynchronousQueue<>();
		consumer.accept(q);
		q.take();
	}

	public static void main(String[] args) throws Exception {
		S.out( "start");

		sync( queue -> Util.executeIn(1000, () -> {
			try {
				queue.put( "");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}));

		S.out( "done");
	}
			
}
