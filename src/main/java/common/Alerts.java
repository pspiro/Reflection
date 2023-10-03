package common;

import java.util.HashMap;

import tw.google.Auth;
import tw.google.TwMail;
import tw.util.S;

/** Don't send the same alert (with same subject) more than once every n minutes */
public class Alerts {
	static HashMap<String,Long> m_map = new HashMap<String,Long>();
	static long min = 60000;
	static long min_interval = 3 * min;
	
	public static void alert(String from, String subject, String body) {
		Long time = m_map.get(subject);
		if (time == null || System.currentTimeMillis() - time > min_interval) {
			alert_( from, subject, body);
			m_map.put(subject, System.currentTimeMillis() );
		}
	}
	
	protected static void alert_(String from, String subject, String body) {
		try {
			TwMail mail = Auth.auth().getMail();
			mail.send(
					from, 
					"peteraspiro@gmail.com", 
					"peteraspiro@gmail.com",
					subject,
					body,
					"plain");
			S.out( "Sending alert %s - %s", subject, body);
		}
		catch( Exception e) {
			e.printStackTrace();
		}
	}

}
