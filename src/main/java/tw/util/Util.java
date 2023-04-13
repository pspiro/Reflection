package tw.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;


public class Util {
	static NumberFormat PERCENT0 = new DecimalFormat( "0%");
	static NumberFormat PERCENT = new DecimalFormat( "0.00%");
	static NumberFormat DECIMAL = new DecimalFormat( "0.00");
	static NumberFormat DECIMAL1 = new DecimalFormat( "0.0");
	static NumberFormat NICE = new DecimalFormat( "#,##0.00");
	static NumberFormat SHORT = new DecimalFormat( "#,##0");

	public static double stddev(List<Double> vals) {
		double avg = average( vals);
		return stddev( vals, avg);
	}
	
	public static double stddev(List<Double> vals, double avg) {
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

	public static double average(List<Double> vals) {
		double total = 0;
		for( double val : vals) {
			total += val;
		}
		return total / vals.size();
	}

	public static String fmtPct(double val) {
		return PERCENT.format( val);
	}

	public static String fmtPct0(double val) {
		return PERCENT0.format( val);
	}

	/** Format no comma and two decimals. */
	public static String fmt(double val) {
		return DECIMAL.format( val);
	}

	/** Format with commas and no decimals. */
	public static String fmtShort(double val) {
		return SHORT.format( val);
	}

	/** Format with commas and two decimals. */
	public static String fmtNice(double val) {
		return NICE.format( val);
	}

	/** Returns empty string for zero. */
	public static String fmtNice2(double val) {
		return val == 0 ? "" : NICE.format( val);
	}

	public static String fmt1(double val) {
		return DECIMAL1.format( val);
	}

	public static String underline(String str) {
		return String.format( "<html><u>%s</html>", str);
	}
	
	public static boolean between(double m_x, int x1, int x2) {
		return m_x >= x1 && m_x <= x2;
	}

	public static void closeOnEsc(JDialog ticketDlg) {
		// write the code
		throw new RuntimeException();
		
	}

	public static void sleep(int i) {
		// write the code
		throw new RuntimeException();
		
	}
	
	// to convert 1 or more items to a List, use Arrays.asList()

}
//move into S. ps