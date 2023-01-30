package util;

import java.text.SimpleDateFormat;
import java.util.Date;

import tw.util.FileUtilities;
import tw.util.OStream;
import tw.util.S;

/** Log file that creates a new file every day with the date in the name. */
public class DateLogFile {
	static SimpleDateFormat fmt = new SimpleDateFormat( "HH:mm:ss.SSS");
	static SimpleDateFormat todayFmt = new SimpleDateFormat( "yyyy-MM-dd");

	private String m_prefix;
	private OStream m_log; // log file for requests and responses
	int m_date;
	
	public DateLogFile( String prefix) {
		m_prefix = prefix;
	}

	static String today() {
		return todayFmt.format( new Date() ); 
	}
	
	static String now() { 
		return fmt.format( new Date() ); 
	}

	public synchronized void log(LogType type, String text, Object... params) {
		try {
			// if date has changed since last log msg, close the log file and create a new one
			if (m_date != new Date().getDate() ) {
				resetLogFile();
				m_date = new Date().getDate();
			}
			String str = String.format( "%s %s %s", now(), type, String.format( S.notNull( text), params) );
			S.out( str.substring(13) );
			m_log.writeln( str);
		}
		catch( Exception e) {
			e.printStackTrace();
		}
	}

	private void resetLogFile() {
		try {
			String fname = String.format( "logs/%s.%s.log", m_prefix, today() );
			
			if (m_log != null) {
				m_log.close();
				m_log = null;
				S.out( "Resetting log to %s", fname);
			}

			FileUtilities.createDir( "logs");
			m_log = new OStream( fname, true);			
		}
		catch( Exception e) {
			e.printStackTrace();
		}
	}
}
