/*
 * $Id: Std.java,v 1.4 2012/02/21 21:06:39 jsant Exp $
 * 
 * @author Pete 
 * @since Sep 28, 2005
 * 
 * $Log: Std.java,v $
 * Revision 1.4  2012/02/21 21:06:39  jsant
 * int bz9999 OrderStats: added code to include commission of previous and next day
 *
 * Revision 1.3  2010/05/19 16:02:49  smangla
 * int dp9575 Extract authentication phase from TWS
 *
 * Revision 1.2  2009/08/20 14:09:21  jsant
 * int bz9999 Changed the audit system to generate report for the previous day instead of 2 days ago
 *
 * Revision 1.1  2009/04/30 13:03:45  ptitov
 * int dp0000 fix common alias that breaks builds
 *
 * Revision 1.6  2008/02/14 19:23:43  pspiro
 * int bz9999 changes to audit stats collection program
 *
 * Revision 1.5  2007/07/16 20:58:47  pspiro
 * int bz9999 add aggregator
 *
 * Revision 1.4  2007/07/02 13:43:15  pspiro
 * int bz9999 reord stats
 *
 * Revision 1.3  2007/05/30 14:45:09  pspiro
 * int bz9999 order stats
 *
 * Revision 1.2  2007/05/16 22:11:38  pspiro
 * int bz9999 add os support
 *
 * Revision 1.1  2007/02/14 22:49:32  linux24g
 * relocated utils
 *
 * Revision 1.1  2007/02/09 22:46:00  linux24g
 * Java files from pspiro/programs
 *
 */
package tw.util;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

/** this is utils- utility class*/
public class Std {
    public static void err(String string) {
        System.out.println( string);
    }

    public static void stderr(String string) {
        System.err.println( string);
    }

    public static boolean isNull(String str) {
        return str == null || str.length() == 0;
    }

    public static boolean isNotNull(String str) {
        return str != null && str.length() > 0;
    }

    public static String padLeft(String tag, int i) {
        return padLeft( tag, ' ', i);
    }
    
    public static String padLeft(String tag, char c, int i) {
        StringBuffer buf = new StringBuffer( tag);
        while( buf.length() < i) {
            buf.insert( 0, c);
        }
        return buf.toString();
    }
    
    public static String padRight(String tag, int i) {
        return padRight( tag, i, ' ');
    }
    
    public static String padRight(String tag, int i, char c) {
        StringBuffer buf = new StringBuffer( tag);
        while( buf.length() < i) {
            buf.append( c);
        }
        return buf.toString();
    }
    
    private static final long ONE_DAY = 1000 * 60 * 60 * 24;
    
    public static String getTwoDaysAgo() {
        final long twoDaysAgo = System.currentTimeMillis() - 2 * ONE_DAY;
        return getYYYYmmdd(twoDaysAgo);
    }
    
    public static String getOneDayAgo() {
        final long oneDayAgo = System.currentTimeMillis() - 1 * ONE_DAY;
        return getYYYYmmdd(oneDayAgo);
    }

    private static String getYYYYmmdd(final long timeInMillis) {
        GregorianCalendar date = new GregorianCalendar();
        date.setTimeInMillis(timeInMillis);
        
        return "" + padLeft( "" + date.get( Calendar.YEAR), '0', 4) +
                    padLeft( "" + (date.get( Calendar.MONTH) + 1), '0', 2) +
                    padLeft( "" + date.get( Calendar.DAY_OF_MONTH), '0', 2);
    }

    static final Locale FORMATTER_LOCALE = new Locale("en", "us");
    static NumberFormat s_decimalFormatter = NumberFormat.getNumberInstance(FORMATTER_LOCALE);
    static {
        s_decimalFormatter.setMinimumFractionDigits( 1);
        s_decimalFormatter.setMaximumFractionDigits( 1);
    }

    /** Right-justified with 1 decimal place, e.g.:  32.4% */
    public static String pct( long num, long den) {
        if( den == 0) {
            return "";
        }
        double val = (double)num / den * 100;
        return padLeft( s_decimalFormatter.format( val) + "%", 6);
    }
    
    /* returns 20120215 if date is 20120216 */
    public static String getPreviousDay(String date) throws ParseException {
        return getNDaysLater(date, -1);
    }

    /* returns 20120217 if date is 20120216 */
    public static String getNextDay(String date) throws ParseException {
        return getNDaysLater(date, 1);
    }


    /* returns 20120215 if date is 20120217 and n is -2 */
    private static String getNDaysLater(String date, int n) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        Date dt = sdf.parse(date);
        Calendar cal = Calendar.getInstance();
        cal.setTime(dt);
        cal.add(Calendar.DATE, n);
        return sdf.format(cal.getTime());
    }

}
