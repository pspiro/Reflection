package tw.util;

import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.LinkedBlockingQueue;

import common.Util;
import tw.grep.DirProcessor;
import tw.grep.FileProcessor;


public class S {
	public static Format PRICE = new DecimalFormat( "$#,##0.00");
	public static Format FMT2D = new DecimalFormat( "0.00");  // two dec.
	public static Format FMT3 = new DecimalFormat( "0.0##");  // 1-3 dec
	public static Format FMT4 = new DecimalFormat( "0.0###");  // 1-4 dec
	public static Format FMT6 = new DecimalFormat( "0.0#####");  // 1-6 dec
	public static Format FMT2DC = new DecimalFormat( "#,##0.00");  // two dec. plus comma
	public static Format FMT0 = new DecimalFormat( "#,##0");
	public static Format FMTPCT = new DecimalFormat( "0.0%");
	public static Format USER_DATE = new SimpleDateFormat( "MM/dd/yy"); // see DateFormatSymbols class
	static final char C = ':';
	static final char COMMA = ',';
	public static final double SMALL = -Double.MAX_VALUE;
	public static final Random RND = new Random( System.currentTimeMillis() );
	private static final String NONE = "<none>";
	public static long DAY = 1000*60*60*24;
	private static SimpleDateFormat yyyymmdd = new SimpleDateFormat( "yyyy/MM/dd");
	public static SimpleDateFormat timeFmt = new SimpleDateFormat( "HH:mm:ss.SSS.dd");  // used for S.out() only

	static {
		// display time and date in NY time
		TimeZone zone = TimeZone.getTimeZone( "America/New_York" );
		yyyymmdd.setTimeZone( zone);
		timeFmt.setTimeZone( zone);
	}
	
	/** Return a random number. When called repeatedly, numbers in 
	 *  the list will have a mean of zero and a stddev as specified. */
	public static double next( double stddev) {
		return Math.sqrt( -2 * Math.log( RND.nextDouble() ) ) * Math.cos( 2 * Math.PI * RND.nextDouble() ) * stddev;
	}

	/** Return YYYYMMDD. */
	public static String dateAsStr( long time) {
		Calendar cal = GregorianCalendar.getInstance();
		cal.setTimeInMillis( time);
		return cal.get( Calendar.YEAR) + pad( cal.get( Calendar.MONTH) + 1) + pad( cal.get( Calendar.DAY_OF_MONTH) );
	}

	/** @return MM/DD/YY */
	public static String userDate( Date date) {
		return USER_DATE.format( date);
	}
	
	/** @param date YYYYMMDD
	 *  @return MM/DD/YY */
	public static String userDate( String date) {
		return isNull( date) 
				? "" 
				: String.format( "%s/%s/%s", date.substring( 0, 4), date.substring( 4, 6), date.substring( 6) );
	}
	
	public static int dayOfYear( long time) {
		Calendar cal = GregorianCalendar.getInstance();
		cal.setTimeInMillis( time);
		return cal.get( Calendar.DAY_OF_YEAR);
	}		
	
	public static boolean isValid( double val) {
		return val != Double.MAX_VALUE && val != SMALL;
	}
	
	public static String now() {
		return timeAsStr( System.currentTimeMillis() );
	}

	/** return hh:mm:ss military time */
	public static String timeAsStr( long timeInMillis) {
		Calendar cal = GregorianCalendar.getInstance();
		cal.setTimeInMillis( timeInMillis);
		int hour = cal.get( Calendar.HOUR_OF_DAY);
		hour = hour == 0 ? 12 : hour;
		return pad( hour) + C + pad( cal.get( Calendar.MINUTE) ) + C + pad( cal.get( Calendar.SECOND) );
	}

	private static String pad(int hours) {
		return hours < 10 ? "0" + String.valueOf( hours) : String.valueOf( hours);
	}

	/** Double vals get two decimal places and commas.
	 *  Integer.MAX_VALUE -> <none>
	 *  Can take null but never returns null */
	public static String format(String string, Object... params) {
		if (params.length == 0) {
			return notNull(string);
		}
		
		for (int i = 0; i < params.length; i++) {
			if (params[i] instanceof Double) {
				params[i] = fmt2( ((Double)params[i]).doubleValue() );
			}
			else if (params[i] instanceof Integer && ((int)params[i]) == Integer.MAX_VALUE) {
				params[i] = NONE;
			}
		}
		
		try {
			return String.format( notNull( string), params);
		}
		catch( Exception e) {
			if (params != null && params.length > 0) {
				e.printStackTrace();
				return string + " (bad format)";
			}
			return string;  // this means caller did not intend for the string to be formatted
		}
	}

	/** Double get two decimal places. */
	public static void out( String str, Object... params) {
		out( format( str, params) );
	}
	
	public static void outt( String str, Object... params) {
		outt( format( str, params) );
	}

	public static void outt( Object str) {
		out( "");
		out( str);
	}

	public static void out() {
		out("");
	}

	public static void out( Object str) {
		System.out.println( String.format( "%s %3s %s", 
				timeFmt.format( new Date() ), Thread.currentThread().getName(), str) );  
	}

	public static void deleteFile(String pnlFilename) {
		File file = new File( pnlFilename);
		file.delete();
	}
	
	public static void sleep(int ms) {
		try {
			Thread.sleep( ms);
		} 
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void writeToFile(String filename, String string) {
		try {
			OStream os = new OStream( filename, true);
			os.writeln( string);
			os.close();
		}
		catch( Exception e) {
			out( e.toString() );
		}
	}

	public static void writeToFileNoln(String filename, String string) {
		try {
			OStream os = new OStream( filename, true);
			os.write( string);
			os.close();
		}
		catch( Exception e) {
			out( e.toString() );
		}
	}

	public static double stddev(Collection<Double> vals) {
		double avg = average( vals);
		return stddev( vals, avg);
	}
	
	public static double stddev(Collection<Double> vals, double avg) {
		List<Double> squares = new ArrayList<Double>();
		
		for( double high : vals) {
			double dif = high - avg;
			double sqr = Math.pow( dif, 2);
			squares.add( sqr);
		}
		
		double avgSquare = average( squares);
		double root = Math.sqrt( avgSquare);
		return root;
	}

	public static double average(Collection<Double> vals) {
		double total = 0;
		for( double val : vals) {
			total += val;
		}
		return total / vals.size();
	}

	public static double average(double... vals) {
		double total = 0;
		for( double high : vals) {
			total += high;
		}
		return total / vals.length;
	}

	/** @param list must be sorted */
	public static double mean(List<Double> list) {
		int i = list.size();
		if( i % 2 == 0) {
			int half = i / 2;
			double v1 = list.get( half - 1);
			double v2 = list.get( half);
			return (v1 + v2) / 2;
		}
		int half = i / 2;
		return list.get( half + 1);
	}
	
	/** Enter time in format: YYYYMMDD HH:MM:SS
	 *  Return time in millis */
    public static long getLongTime( String str) { 
        // parse date/time passed in 
        int year   = Integer.parseInt( str.substring( 0, 4) ); 
        int month  = Integer.parseInt( str.substring( 4, 6) ) - 1; // month is 0 based 
        int day    = Integer.parseInt( str.substring( 6, 8) ); 
        int hour   = Integer.parseInt( str.substring( 9, 11) ); 
        int minute = Integer.parseInt( str.substring( 12, 14) ); 
        int second = Integer.parseInt( str.substring( 15, 17) ); 

        // create calendar with date/time passed in 
        GregorianCalendar cal = new GregorianCalendar(); 
        cal.set( year, month, day, hour, minute, second); 
        cal.set( GregorianCalendar.MILLISECOND, 0); 
        
        return cal.getTimeInMillis();
    }

    /** All whitespace is considered null.
     *  Adding separate versions for Object to avoid mistakes */
	public static boolean isNullObj( Object obj) {
		return obj == null || obj.toString().trim().equals( "");
	}
	
    /** All whitespace is considered null.
    *  Adding separate versions for Object to avoid mistakes */
	public static boolean isNotNullObj( Object obj) {
		return !isNullObj( obj);
	}
	
    /** All whitespace is considered null. */
	public static boolean isNull( String str) {
		return str == null || str.trim().equals( "");
	}
	
    /** All whitespace is considered null. */
	public static boolean isNotNull( String str) {
		return !isNull( str);
	}
	
	public static String notNull( String str) {
		return str == null ? "" : str;
	}
	
	public static String notNull( String str, String def) {
		return str == null ? def : str;
	}
	
	public static double min(double v1, double v2) {
		if( !isValid( v1) && !isValid( v2) ) {
			return Double.MAX_VALUE;
		}
		if( !isValid( v1) ) {
			return v2;
		}
		if( !isValid( v2) ) {
			return v1;
		}
		return Math.min( v1, v2);
	}
	
	public static double max(double v1, double v2) {
		if( !isValid( v1) && !isValid( v2) ) {
			return Double.MAX_VALUE;
		}
		if( !isValid( v1) ) {
			return v2;
		}
		if( !isValid( v2) ) {
			return v1;
		}
		return Math.max( v1, v2);
	}
	
	public static void clearDir( String dirname) {
		File dir = new File( dirname);
		File[] files = dir.listFiles();
		if( files != null) {
			for( File file : files) {
				file.delete();
			}
		}
	}

	public static String concat( Object... objs) {
		StringBuffer buf = new StringBuffer();
		for( Object obj : objs) {
			if( obj instanceof Double) {
				buf.append( fmt2( (Double)obj) );
			}
			else {
				buf.append( obj);
			}
			buf.append( COMMA);
		}
		return buf.toString();
	}
	
	public static void excel( String param) {
		try {
			Runtime.getRuntime().exec( "C:\\Program Files (x86)\\Microsoft Office\\Office12\\EXCEL.EXE " + param);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/** take string with , format with , and two decimals */
	public static String fmt( String v) {
		return v != null ? fmt2( Double.parseDouble( v.replaceAll( ",", "") ) ) : null;
	}

	/** Format with comma and two decimals. */
	public static String fmt2( double v) { 
		return v == Double.MAX_VALUE ? NONE : FMT2DC.format( v); 
	}
	
	/** Format with 1-3 decimals, no comma */
	public static String fmt3( double v) { 
		return FMT3.format( v);   // faster than String.format("%.3f")
	}
	
	/** Format with 1-4 decimals, no comma */
	public static String fmt4( double v) { 
		return FMT4.format( v);   // faster than String.format("%.3f")
	}
	
	/** Format with 1-6 decimals, no comma */
	public static String fmt6( double v) { 
		return FMT6.format( v);   // faster than String.format("%.3f")
	}
	
	/** Format with two decimals, no comma. */
	public static String fmt2d( double v) { 
		return FMT2D.format( v); 
	}
	
	/** Format with comma. */
	public static String iFmt( int v) { 
		return v == Integer.MAX_VALUE ? NONE : FMT0.format( v); 
	}
	
	/** Format with no decimals (rounded to int). */
	public static String fmt0( double v) { 
		return FMT0.format( v); 
	}
	
	/** Format with comma and two decimals. */
	public static String fmtPct( double v) { 
		return FMTPCT.format( v); 
	}
	
	public static String formatPrice( double v) { 
		return PRICE.format( v); 
	}

	/** Round to nearest penny. */
	public static double round(double d) {
		double v = d * 100 + .5;
		return Math.floor( v) / 100;
	}

	public static String getTempDir() {
//		if( m_tempDir == null) {
//			m_tempDir = System.getProperty( "java.io.tmpdir");
//		}
//		return m_tempDir;
		return "c:\\temp";
	}

	/** This can handle strings formatted with commas and/or dollar signs.
	 *  Null or empty string returns zero.
	 *  Throws no exceptions. */
	public static double parseDouble2( String str) {
		try {
			return parseDouble( str);
		}
		catch( Exception e) {
			return 0;
		}
	}

	/** This can handle strings formatted with commas and/or dollar signs.
	 *  Null string returns zero. */
	public static double parseDouble( String str) {
		return S.isNotNull( str) ? Double.parseDouble( strip( str) ) : 0.0;
	}

	/** This can handle strings formatted with commas.
	 *  Null string returns Double.MAX_VALUE. */
	public static double parseDoubleMV( String str) {
		return S.isNotNull( str) ? Double.parseDouble( strip( str) ) : Double.MAX_VALUE;
	}

	/** Returns a string in the format ####.## (no commas). */ 
	public static String formatDoubleNoCommas(String str) {
		return S.isNull( str) ? null : formatDoubleNoCommas( parseDouble( str) );
	}

	/** Returns a string in the format ####.## (no commas). */ 
	public static DecimalFormat FORMAT = new DecimalFormat( "0.00");
	public static String formatDoubleNoCommas(double d) {
		return FORMAT.format( d);
	}

	private static String strip(String str) {
		return str.replaceAll( ",", "").replaceAll( "\\$", "");
	}

//	public static void main(String[] args) {
//		try {
//			SimpleDateFormat fmt = new SimpleDateFormat( "d MMM yyyy");
//			Date date = new Date();
//			S.out( fmt.format( date) );
//			
//			
//			Date d3 = fmt.parse( "22 September 2017");
//			S.out( fmt.format( d3) );
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}

	public static String combine(String... strs) {
		StringBuilder sb = new StringBuilder();
		for (String str : strs) {
			sb.append( str);
			sb.append( ' ');
		}
		return sb.toString();
	}

	public static double between(double num, double min, double max) {
		return Math.min( Math.max( num, min), max);
	}

	public static double cos(double deg) {
		return Math.cos( Math.toRadians( deg) );
	}

	public static double sin(double deg) {
		return Math.sin( Math.toRadians( deg) );
	}

	public static boolean exists(String filename) {
		return new File( filename).exists();
	}

	public static boolean dirExists(String filename) {
		File file = new File( filename);
		return file.exists() && file.isDirectory();
	}

	/** Show value of all member variables.
	 *  NOTE: it won't show member variables of lists or sets */
	public static void debug(Object e) {
		try {
			debug( e, e.getClass(), "", new HashSet<Object>(), false);
		}
		catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	static String space = "   ";
	private static void debug(Object obj, Class cls, String tab, HashSet<Object> set, boolean force) throws Exception {
		if (obj instanceof SoftReference) {
			return;
		}
		
		if (obj instanceof String) {
			S.out( "%s%s", tab, obj);
			return;
		}
	
		if (!force) {
			if (set.contains( obj) ) {
				return;
			}
			set.add( obj);
		}

		if (cls.getSuperclass() != null) {
			debug( obj, cls.getSuperclass(), tab, set, true);
		}
		
		Field[] fields = cls.getDeclaredFields();
		for (Field field : fields) {
			if (Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			
			field.setAccessible(true);
			Object val = field.get( obj);
			
			if (isPrimitive( field.getType() ) || val == null ) {
				S.out( "%s%s %s=%s", tab, field.getType().getSimpleName(), field.getName(), val);
			}
			else if (field.getType().isArray() ) {
				S.out( "%s%s %s", tab, field.getType().getSimpleName(), field.getName() );
				try {
					for (Object item : (Object[])val) {
						if (item != null) {
							debug( item, item.getClass(), tab + space, set, false);
						}
					}
				}
				catch( Exception ee) {
					// array of primitives
				}
			}
			else if (val instanceof Collection) {
				S.out( "%s%s<%s> %s", tab, field.getType().getSimpleName(), val.getClass().getSimpleName(), field.getName() );
				for (Object item : (Collection)val) {
					debug( item, item.getClass(), tab + space, set, false);
				}
			}
			else {
				S.out( "%s%s<%s> %s", tab, field.getType().getSimpleName(), val.getClass().getSimpleName(),field.getName() );
				debug( val, val.getClass(), tab + space, set, false);
			}
		}
	}

	/** These objects will show using toString() instead of recursion. */
	static HashSet<Class> m_set = new HashSet<Class>();
	static {
		m_set.add( Integer.class);
		m_set.add( Double.class);
		m_set.add( Character.class);
		m_set.add( String.class);
		m_set.add( Color.class);
		m_set.add( Byte.class);
		m_set.add( Float.class);
		m_set.add( Boolean.class);
		m_set.add( Class.class);
	}
	
	private static boolean isPrimitive(Class cls) {
		return cls.isPrimitive() || m_set.contains( cls);
	}

	public static List<String> list(String... strings) {
		ArrayList<String> list = new ArrayList<String>();
		for (String string : strings) {
			list.add( string);
		}
		return list;
	}

    /** return true if a file or directory with the specified name exists */
    static public boolean fileExists( String str) {
        return new File( str).exists();
    }

    /** Return true if n is even. */
	public static boolean isEven(int n) {
		return n % 2 == 0;
	}
	
	public static String substrNoEx( String str, int start) {
		try {
			return str.substring( start);
		}
		catch( Exception e) {
			return "";
		}
	}

	/** Return sorted list of subfolders. */
	public static String[] getSubfolders( String path) {
		File dir = new File( path);
		if (!dir.isDirectory() ) {
			return null;
		}
		
        File[] files = dir.listFiles();

        if( files == null) {
        	return new String[0];
        }
        
        ArrayList<String> list = new ArrayList<String>();
        for( File file : files) {
        	if( file.isDirectory() ) {
        		list.add( file.getName() );
        	}
        }
        
        list.sort( new Comparator<String> () {
			@Override public int compare(String o1, String o2) {
				return o1.compareTo( o2);
			}
        });
        
        return list.toArray( new String[0] ); 
		
	}

	public static void readURLtoFile( String url, String filename) throws IOException {
		InputStream os = new URL(url).openStream();
		BufferedInputStream in = new BufferedInputStream(os);

		FileOutputStream fileOutputStream = new FileOutputStream( filename);

		byte dataBuffer[] = new byte[1024];
		int bytesRead;
		while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
			fileOutputStream.write(dataBuffer, 0, bytesRead);
		}

		fileOutputStream.close();
		in.close();
	}

	public static boolean isInt(String id) {
		try {
			Integer.valueOf( id);
			return true;
		}
		catch( Exception e) {
			return false;
		}
	}

	/** Return the matching file with the latest date/timestamp. */
	static File latest;
	public static File getLatest(String folder, String wildcard) throws Exception {
		latest = null;
		DirProcessor p = new DirProcessor(wildcard, false, false, false, new FileProcessor() {
			@Override public void process(File file) {
				if (latest == null || file.lastModified() > latest.lastModified() ) {
					latest = file;
				}
			}
		});
		p.process( folder);
		return latest;
	}

	public static File[] getFiles(File dir, String wildcard) throws Exception {
		ArrayList<File> ar = new ArrayList<File>();
		
		DirProcessor p = new DirProcessor(wildcard, false, false, false, new FileProcessor() {
			@Override public void process(File file) {
				ar.add( file);
			}
		});
		p.process( dir);
		
		return ar.toArray( new File[ar.size()] );
	}
	
	/** Takes date in any format with - or / separator. */
	public static Date parseDate(String date) throws Exception {
		if (date.indexOf( "," ) != -1) {
			return new SimpleDateFormat("MMM dd, yyyy").parse( date);
		}
		
		char sep = date.indexOf( '-') != -1 ? '-' : '/';
		
		String format = date.charAt( 1) == sep || date.charAt( 2) == sep
			? String.format( "MM%sdd%syy", sep, sep)
			: String.format( "yyyy%sMM%sd", sep, sep);

		return new SimpleDateFormat( format).parse( date);
	}
	
	/** Convert date from one format to another. 
	 * @throws Exception */
	public static String formatDate(String date, DateFormat fmt) throws Exception {
		return fmt.format( parseDate( date) );
	}
	
	/** Return date in yyyy-mm-dd format. */
	public static String formatDate(String date) throws Exception {
		return formatDate( date, yyyymmdd);
	}
	
	public static boolean compareDates( String date1, String date2) throws Exception {
		return parseDate( date1).equals( parseDate( date2) );
	}
	
	public static void main(String[] args) {
		S.out( format( "%s %s", 1234.5323, 8383) );
	}

	

	/** @param endsWith is case-insensitive; we need this to avoid tmp files created in the downloads folder. */
	@SuppressWarnings("unchecked")
	public static void watchFolder( String folder, String endsWith, LinkedBlockingQueue<Path> queue) throws Exception {
		Path dir = new File( folder).toPath();
		WatchService watcher = FileSystems.getDefault().newWatchService();        		
		dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE);

		while (true) {

			// wait for key to be signaled
			WatchKey key = watcher.take();

			for (WatchEvent<?> event : key.pollEvents() ) {
				WatchEvent.Kind<?> kind = event.kind();

				// skip OVERFLOW events
				if (kind == StandardWatchEventKinds.OVERFLOW) {
					continue;
				}
				
				// The filename is the context of the event.
				Path filename = ((WatchEvent<Path>)event).context();
				
				if (S.isNull( endsWith) || filename.toString().toUpperCase().endsWith(endsWith.toUpperCase() ) ) {
					Path child = dir.resolve(filename); // Resolve the filename against the directory. If the filename is "test" and the directory is "foo", the resolved name is "test/foo".
					queue.put( child);
				}
			}
			
			// Reset the key -- this step is critical if you want to
			// receive further watch events.  If the key is no longer valid,
			// the directory is inaccessible so exit the loop.
			boolean valid = key.reset();
			if (!valid) {
				break;
			}
		}		
	}

	public static void err(String str, Exception e) {
		out( str + " - " + Util.toMsg(e) ); 
	}
}
