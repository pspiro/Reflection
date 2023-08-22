package tw.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.StringTokenizer;
import java.util.TimeZone;


/** NOTE: If you use set() to set any fields, you must call computeTime().
 *        If you add members to this class, please add a test to JtsCalendarTest.
 *        
 *  NOTE: when using SimpleDateFormat:
 *        use y for year, not Y which is some "week-year" shit;
 *        use M for month, not m which is minute;
 *        use d for day in the month, not D which is day in the year;
 *       */
public class JtsCalendar extends GregorianCalendar {

    // For Serialization (serialized by ChartSettings)
    private static final String SINGLE_SPACE_PAD = " ";
    private static final String DOUBLE_SPACE_PAD = "  ";

    // constants
    public static final long MILLIS_IN_DAY = 24 * 60 * 60 * 1000L;
    public static final String GMT = "GMT";
    public static final String EST = "EST";
    public static final String EMPTY_STRING = "";
	public static final long MILLISECONDS_IN_DAY = 0;

    public static long CCP_TIME_OFFSET = 0; // in ms, ccp time offset; shared between all instances
    private static JtsCalendar LOCAL_CALENDAR = new JtsCalendar(); // used for quicker calculation of values
    private static JtsCalendar GMT_CALENDAR = new JtsCalendar(); // used for quicker calculation of values
    private String m_prefTimeZone; // this is for display purposes only and does not affect how the date/time is stored

    // get
    String prefTimeZone() { return m_prefTimeZone; } // for testing only

    static {
        // this must be called before getCCPBasedTimeInMillis() is called
        GMT_CALENDAR.setTimeZone( TimeZone.getTimeZone( GMT) );
    }

    public JtsCalendar() {
    }
    
    @Override public boolean equals(Object obj) {
    	return getTimeInMillis() == ((JtsCalendar)obj).getTimeInMillis();
    }

    public JtsCalendar(long timeInMillis) {
        setTimeInMillis( timeInMillis);
    }

    public JtsCalendar( int year, int month, int day) {
    	clear();
        set( year, month-1, day);
    }

    public static JtsCalendar createGmtInstance(long timeInMillis) {
        JtsCalendar cal = new JtsCalendar(timeInMillis);
        cal.setTimeZone(TimeZone.getTimeZone(GMT));
        return cal;
    }

    /** similar to parent class - use local milliseconds */
    public static JtsCalendar newSyncedInstance() {
        return new JtsCalendar(getCCPBasedTimeInMillis());
    }

    private static long getCCPBasedTimeInMillis() {
		return System.currentTimeMillis();
	}

	/** Override setTimeZone to automatically compute fields. If you don't do this,
     *  the fields are still set to the old time zone. */
    @Override public void setTimeZone(TimeZone zone) {
        super.setTimeZone(zone);
        computeFields();
    }

    // add and set do not automatically compute the millis value.
    public void addWithComputeTime(int field, int units) {
        super.add(field, units);
        computeTime();
    }

    /** @return HH:MM */
    public String getHHMM() {
        if( getTimeInSec() == 0 ) {
            return "??:??";
        }
        if( getTimeInSec() == 1) {
            return "00:00";
        }
        return getHHMMprim();
    }

    private String getHHMMprim() {
        return pad(get(Calendar.HOUR_OF_DAY)) + ":" +
               pad(get(Calendar.MINUTE));
    }

    public static String getHHMMSS( int sec) {
        int min = sec / 60;
        sec -= min * 60;
        int hour = min / 60;
        min -= hour * 60;
        return pad( hour) + ":" + pad( min) + ":" + pad( sec);
    }

    /** @return HH:MM:SS */
    public String getHHMMSS() {
        return pad(get(Calendar.HOUR_OF_DAY)) + ":" +
               pad(get(Calendar.MINUTE)) + ":" +
               pad(get(Calendar.SECOND));
    }

    public String getHHMMSS_() {
        return pad(get(Calendar.HOUR_OF_DAY)) +
               pad(get(Calendar.MINUTE)) +
               pad(get(Calendar.SECOND));
    }


    /** @return HH:MM:SS:MS */
    public String getHHMMSSMS() {
        return pad(get(Calendar.HOUR_OF_DAY)) + ":" +
               pad(get(Calendar.MINUTE)) + ":" +
               pad(get(Calendar.SECOND)) + ":" +
               pad3(get(Calendar.MILLISECOND));
    }

    /** @return HHMMSS.MMM */
    public String getHHMMSSdotMMM() {
        return pad(get(Calendar.HOUR_OF_DAY)) +
               pad(get(Calendar.MINUTE)) +
               pad(get(Calendar.SECOND)) + "." +
               pad3(get(Calendar.MILLISECOND));
    }

    /** @return YYYYMMDD */
    public static String getYYYYMMDD(GregorianCalendar date) {
        return "" + date.get(Calendar.YEAR) +
                    pad( date.get(Calendar.MONTH)+1) +
                    pad( date.get(Calendar.DAY_OF_MONTH) );
    }

    /** @return YYYY-MM-DD. This is good for adding to Google Sheets because it is recongnized as a date. */
    public String getYYYYdashMMdashDD() {
        return S.format( "%s-%s-%s",
        			this.get(Calendar.YEAR),
                    pad( this.get(Calendar.MONTH)+1),
                    pad( this.get(Calendar.DAY_OF_MONTH) ) );
    }

    /** @return YYYYMMDD */
    public String getYYYYMMDD() {
        return getYYYYMMDD(this);
    }

    /** @return YYYY-MM-DD */
    public String getYYYYMMDD2() {
        return "" + get(Calendar.YEAR) + '-' +
               pad( get(Calendar.MONTH)+1) + '-' +
               pad( get(Calendar.DAY_OF_MONTH) );
    }

    /** @return YYYYMM */
    public String getYYYYMM() {
        return "" + get(Calendar.YEAR) +
              pad( get(Calendar.MONTH)+1);
    }

    public String getYYYY() {
        return "" + get(Calendar.YEAR);
    }

    /** @return DD */
    public String getDD() {
        return pad( get(Calendar.DAY_OF_MONTH) );
    }

    /** @return HHMMSSMMM */
    public String getHHMMSSMMM() {
        return "" +  pad(get(Calendar.HOUR_OF_DAY)) +
               pad(get(Calendar.MINUTE)) +
               pad(get(Calendar.SECOND)) +
               pad3(get(Calendar.MILLISECOND));
    }

    /** @return YYYYMMDDHHMMSS */
    public String getYYYYMMDDHHMMSS() {
        return "" + get(Calendar.YEAR) + '-' +
               pad( get(Calendar.MONTH)+1) + '-' +
               pad( get(Calendar.DAY_OF_MONTH) ) + ' ' +
               pad(get(Calendar.HOUR_OF_DAY)) + ':' +
               pad(get(Calendar.MINUTE)) + ':' +
               pad(get(Calendar.SECOND));
    }

    /** @return MMDD */
    public String getMMDD() {
        return pad( get(Calendar.MONTH)+1) +
               pad( get(Calendar.DAY_OF_MONTH) );
    }

    /** Should be used for all API String date/time reports,
     *  except for historical data format types >= 3 that are date strings.
     *  @return YYYYMMDD  HH:MM:SS local time. */
    public String getExecStr() {
        return getYYYYMMDD() + DOUBLE_SPACE_PAD + getHHMMSS();
    }

    // used only for API historical data format 3.  Use for any other purpose is discouraged.
    public String getMMDDHHMMSS() {
        return getMMDD() + SINGLE_SPACE_PAD + getHHMMSS();
    }

    // used only for API historical data format 4.  Use for any other purpose is discouraged.
    public String getMMDDHHMM() {
        return getMMDD() + SINGLE_SPACE_PAD + getHHMMprim();
    }

    /** @return HH:MM:SS TMZ local time */
    public String getTimeStr() {
        return getHHMMSS() + SINGLE_SPACE_PAD + getShortTimeZone();
    }

    /** Return the short time zone unless it is ambiguous (like GMT+10),
     *  in which case return the long time zone. */
    public String getShortTimeZone() {
        TimeZone timeZone = getTimeZone();
        return timeZone.getDisplayName( false, TimeZone.SHORT);
    }

    /** @return HH:MM TMZ local time */
    public String getTimeNoSecStr() {
        return getHHMM() + SINGLE_SPACE_PAD + getShortTimeZone();
    }

    /** return YYYYMMDD-HH:MM:SS in local zone */
    public String getFixStrLocalZone() {
        return getYYYYMMDD() + "-" + getHHMMSS() + SINGLE_SPACE_PAD + getShortTimeZone();
    }

    /** @return YYYYMMDD-HH:MM:SS in GMT */
    public String getFixStr() {
        JtsCalendar cal = copy();
        cal.setTimeZone( TimeZone.getTimeZone( GMT) );
        return cal.getYYYYMMDD() + "-" + cal.getHHMMSS();
    }

    /** i.e. 12:14:15 on July 11, 2005 (local time) */
    public String toHumanFormat() {
        return getHHMMSS() + " on " + toHumanDate();
    }

    /** i.e. hh:mm:ss tz */
    public String toHumanTime() {
        return getHHMMSS() + " " + getTimeZone().getDisplayName();
    }

    /** July 11, 2005 (local time) */
    public String toHumanDate() {
        return DateFormat.getDateInstance(DateFormat.LONG).format( getTime() );
    }

    /** for debug */
    public static String toString(long time) {
        return (time == Long.MIN_VALUE || time == Long.MAX_VALUE)
            ? "N/A"
            : new JtsCalendar(time).getFixStr() + " (" + time + ")";
    }

    /** @return time in seconds */
    public int getTimeInSec() {
        return (int)(super.getTimeInMillis() / 1000);
    }

    /** set time in seconds */
    public void setTimeInSec( int sec) {
        super.setTimeInMillis( (long)sec * 1000);
    }

    /** Return a copy for the same time. DO NOT USE CLONE */
    public JtsCalendar copy() {
        JtsCalendar res = new JtsCalendar(getTimeInMillis());
        res.setTimeZone(getTimeZone());
        return res;
    }

    /** Used for tooltip for last price cell.
     *  @return time if this object is today, or date and time for
     *  any other day */
    public static String getDateIndicatingTime(long systimeInSec) {
        JtsCalendar tmp = new JtsCalendar(systimeInSec*1000);
        return tmp.get(Calendar.YEAR) == LOCAL_CALENDAR.get(Calendar.YEAR) &&
               tmp.get(Calendar.DAY_OF_YEAR) == LOCAL_CALENDAR.get(Calendar.DAY_OF_YEAR)
                ? tmp.getHHMMSS()
                : tmp.getExecStr();
    }


    /*-----------------------------------------------------
        static member functions
      -----------------------------------------------------*/

    /** @return HH:MM */
    public static String formatHHMM( int timeInSec) {
        JtsCalendar calendar = new JtsCalendar();
        calendar.setTimeInSec( timeInSec);
        return calendar.getHHMM();
    }

    /** takes in a string hh:mm, h:mm, hh:m or h:m and returns hh:mm (or EMPTY_STRING if improperly formatted)*/
    public static String formatHHMM(String str, boolean use24Hour) {
        DateFormat df = new SimpleDateFormat(use24Hour ? "HH:mm" : "hh:mm");
        df.setLenient(false);
        try {
            return df.format(df.parse(str));
        } catch(Exception e) {
            return EMPTY_STRING;
        }
    }

    /** @return HH:MM:SS time in the given time zone */
    public static String getTimeInHHMMSS(TimeZone tz) {
        JtsCalendar calendar = new JtsCalendar();
        calendar.setTimeZone(tz);
        return calendar.getHHMMSS();
    }

    /** @return 2 character string */
    public static String pad( int val) {
        return val < 10 ? "0" + val : "" + val;
    }

    /** @return 3 character string */
    public static String pad3( int val) {
        return (val < 10 ? "00" : val < 100 ? "0" : "") + val;
    }

    /** Incoming format is YYYYMMDD or YYYY-MM-DD */
    public static JtsCalendar createFromYYYYMMDD( String str) {
        // null str passed in?
        if( str == null) {
            return null;
        }

        try {
        	String fmtStr = str.indexOf( "-") != -1 ? "yyyy-MM-dd" : "yyyyMMdd";
        	Date date = new SimpleDateFormat( fmtStr).parse( str);
    	
        	JtsCalendar cal = new JtsCalendar( date.getTime() );
        
//        cal.setTimeZone( TimeZone.getDefault() );
//        cal.set( year, month, day, 11, 59);
//        cal.computeTime();

        	return cal;
        }
        catch( Exception e) {
        	e.printStackTrace();
        	return null;
        }
    }

    /** Incoming format is: YYYYMMDD HH:MM:SS */
    public static JtsCalendar createFromYYYYMMDD_HHMMSS( String str) {

        StringTokenizer st = new StringTokenizer( str, " ");

        String date = st.nextToken();
        String time = st.nextToken();

        // parse date values
        int year   = Integer.parseInt( date.substring( 0, 4) );
        int month  = Integer.parseInt( date.substring( 4, 6) ) - 1; // month is 0 based
        int day    = Integer.parseInt( date.substring( 6, 8) );

        // parse time string
        StringTokenizer st2 = new StringTokenizer( time, ":");
        String hh = st2.nextToken();
        String mm = st2.nextToken();
        String ss = st2.hasMoreTokens() ? st2.nextToken() : null;

        // parse time values
        int hour   = Integer.parseInt( hh);
        int minute = Integer.parseInt( mm);
        int second = ss != null ? Integer.parseInt( ss) : 0;


        // create calendar with date/time passed in
        JtsCalendar cal = new JtsCalendar();
        cal.setTimeZone( TimeZone.getDefault() );
        cal.set( year, month, day, hour, minute, second);
        cal.computeTime();

        return cal;
    }
    
    @Override
    public String toString() {
    	return getYYYYMMDDHHMMSS();
    }

    /** @return HH:MM:SS local time */
    public static String getLocalTimeInHHMMSS() {
        LOCAL_CALENDAR.setTimeInMillis( getCCPBasedTimeInMillis() );
        return LOCAL_CALENDAR.getHHMMSS();
    }

    /** @return HH:MM:SS:MS local time */
    public static String getLocalTimeInHHMMSSMS() {
        LOCAL_CALENDAR.setTimeInMillis( getCCPBasedTimeInMillis() );
        return LOCAL_CALENDAR.getHHMMSSMS();
    }

    /** This runs 5 times faster than the non-static version and creates
     *  no new objects.
     *  @return current time YYYYMMDD-HH:MM:SS in GMT */
    public static String getFixStr2() {
        GMT_CALENDAR.setTimeInMillis( getCCPBasedTimeInMillis() );
        return GMT_CALENDAR.getYYYYMMDD() + "-" + GMT_CALENDAR.getHHMMSS();
    }

    public static JtsCalendar create(int daysFromToday) {
        return new JtsCalendar( getCCPBasedTimeInMillis() + MILLIS_IN_DAY * daysFromToday );
    }

    public static String getTodayDate() {
        return LOCAL_CALENDAR.getYYYYMMDD();
    }

    public static JtsCalendar getLocalCalendar() {
        return LOCAL_CALENDAR;
    }

    public JtsCalendar addDays(int days) {
        return new JtsCalendar( getTimeInMillis() + MILLIS_IN_DAY * days );
    }

    public JtsCalendar addMinutes(int minutes){
        JtsCalendar ret = new JtsCalendar( getTimeInMillis() );

        ret.add(Calendar.MINUTE, minutes);

        return ret;
    }

    public JtsCalendar toDayEnd() {
        GregorianCalendar cal = new GregorianCalendar();
        cal.set(Calendar.YEAR, get(Calendar.YEAR));
        cal.set(Calendar.DAY_OF_YEAR, get(Calendar.DAY_OF_YEAR));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 59);

        return new JtsCalendar( cal.getTimeInMillis() );
    }

    /** @return true if this object has today's date */
    public boolean isToday() {
        return JtsCalendar.getTodayDate().equals( getYYYYMMDD() );
    }

    /** this definitely could be performance improved */
    public boolean isSameDay(JtsCalendar calendar) {
        return getYYYYMMDD().equals( calendar.getYYYYMMDD() );
    }

    /** this definitely could be performance improved */
    public boolean isSameSecond( JtsCalendar calendar) {
        return getTimeInSec() == calendar.getTimeInSec();
    }

    /** this definitely could be performance improved */
    public boolean isSameMonth(JtsCalendar calendar) {
        return getYYYYMM().equals( calendar.getYYYYMM() );
    }

	public boolean isSameYear(JtsCalendar calendar) {
		return getYYYY().equals( calendar.getYYYY() );
	}
	
    public boolean isBetween(JtsCalendar from, JtsCalendar to){
        return (getTimeInMillis() >= from.getTimeInMillis() && getTimeInMillis() <= to.getTimeInMillis());
    }

    /** return true if calendar representing day is in daylight
     * if time-only use "today" as ref date */
    public boolean isDaylightTime() {
        // when parsing just time day will be first day of epoch
        boolean isDaylightTime = false;
        if (getTimeInMillis() <= MILLIS_IN_DAY) {
            isDaylightTime = getTimeZone().inDaylightTime(new Date());
        } else {
            isDaylightTime = getTimeZone().inDaylightTime(new Date(getTimeInMillis()));
        }
        return isDaylightTime;
    }

    public static JtsCalendar create(SimpleDateFormat formatter, String dateTime) {
        JtsCalendar ret = null;

        try {
            Date date = formatter.parse(dateTime);
            ret = new JtsCalendar(date.getTime());
        } catch (ParseException e) {
        }

        return ret;
    }

    /** removes timezone suffix if default timezone used */
    public static String cutDefaultTimeZone(String str) {
        if (str != null) {
            String timeZoneName = TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT);
            if (str.endsWith(' ' + timeZoneName)) {
                String ret = str.substring(0, str.length() - timeZoneName.length());
                return ret;
            }
        }
        return str;
    }
	public boolean isSameWeek(JtsCalendar other) {
		return getSunday().equals( other.getSunday() );
	}

	/** Return Sunday of this week. */
	private String getSunday() {
		int today = get( DAY_OF_WEEK);
		int past = today - getFirstDayOfWeek();
		JtsCalendar sunday = new JtsCalendar( getTimeInMillis() - past * MILLIS_IN_DAY);
		return sunday.getYYYYMMDD();
	}

	public String getExcelDate() {
        return get(Calendar.YEAR) + "-" + 
               pad( get(Calendar.MONTH)+1) + "-" + 
               pad( get(Calendar.DAY_OF_MONTH) );
	}

	public String getExcelDateTime() {
		return getExcelDate() + " " + getHHMMSS(); 
	}
	
	public int daysSpanned( JtsCalendar other) {
		double ms = other.getTimeInMillis() - getTimeInMillis();
		return (int)Math.abs( Math.round( ms / MILLIS_IN_DAY) );
	}
	
	/** @param date is YYYYMMDD */
	public static int getDaysSince(String date) {
		return JtsCalendar.createFromYYYYMMDD( date).daysSpanned( new JtsCalendar() );
	}

	static JtsCalendar YESTERDAY = null;
	
	/** Returns today or Friday if today is a weekend. */
	public static JtsCalendar getYesterday() {
		if( YESTERDAY == null) {
			YESTERDAY = new JtsCalendar( LOCAL_CALENDAR.getTimeInMillis() - MILLIS_IN_DAY);
		}
		return YESTERDAY;
	}
	
	public static String getYears(String begin, String end) {
		if( begin != null && end != null) {
			JtsCalendar beginCal = JtsCalendar.createFromYYYYMMDD( begin);
			JtsCalendar endCal = JtsCalendar.createFromYYYYMMDD( end);
			long dif = endCal.getTimeInMillis() - beginCal.getTimeInMillis();
			long days = dif / JtsCalendar.MILLIS_IN_DAY;
			double years = days / 365.0;
			return Util.fmt1( years);
		}
		return "";
	}

	public static JtsCalendar createFromLongStr(String fullTime, boolean b) {
		throw new RuntimeException();
	}
	
	public static void main(String[] args) {
		String str = "1921-11-22";
	    SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd");
		try {
			Date date = FMT.parse( str);
			JtsCalendar c = new JtsCalendar( date.getTime() );
			S.out( "" + c.getYYYYMMDD() );
			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
