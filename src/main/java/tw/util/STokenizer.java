package tw.util;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


enum State { QUOTED, NON };

/** For reading Excel .csv files. */
public class STokenizer {
	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat( "MM/dd/yyyy");
	static final char C = ',';
	
	public static DecimalFormat FORMAT = new DecimalFormat( "0.00");

	private String m_line;

	public STokenizer(String line) {
		m_line = line;
	}

	/** Used to remove commas from within the text. */
	public String nextToken() {
		if( m_line.length() == 0) {
			return "";
		}

		State state = State.NON;

		StringBuffer buf = new StringBuffer();

		int i = 0;

		char c;


		while( i < m_line.length() ) {
			c = m_line.charAt( i++);

			if( c == '"') {
				state = state == State.QUOTED ? State.NON : State.QUOTED;
				continue;
			}

			if( state == State.NON && c == C) {
				break;
			}

			// ignore quoted commas - this is needed so "2,300" is read as "2300"
//			if( c != C) {
				buf.append( c);
//			}
		}

		m_line = m_line.substring( i);
		return repair( buf.toString().trim() );
	}

	/** Change (2.00) to -2.00. */
	private String repair(String str) {
		if (str.length() > 2 && str.charAt( 0) == '(' && str.charAt( str.length() - 1) == ')') {
			return "-" + str.substring( 1, str.length() - 1);
		}
		return str;
	}

	public boolean hasMoreTokens() {
		return m_line.length() > 0;
	}

	/** Return ####.##, no commas. */
	public String nextDoubleStr() throws Exception {
		return S.formatDoubleNoCommas( nextToken() );
	}

	/** Can handle commas. Null string returns zero. */
	public double nextDouble() throws Exception {
		return S.parseDouble( nextToken() );
	}
	
	/** Can handle commas. Null string returns zero. */
	public int nextInt() throws Exception {
		return Integer.parseInt( nextToken() );
	}
	
	public double nextDoubleDollar() throws Exception {
		return S.parseDouble( nextToken().replaceAll( "\\$", "") );
	}

	/** Return date in mm/dd/yyyy format. */
	public String nextDate() {
		try {
			Date date = DATE_FORMAT.parse( nextToken() );
			return DATE_FORMAT.format( date);
		}
		catch( Exception e) {
			e.printStackTrace();
			return "";
		}
		
	}
}
