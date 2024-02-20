package monitor;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.json.simple.JsonObject;

import common.Util;
import http.MyClient;

/** Test the three servers */
public class SouthPanel extends JPanel {
	static SimpleDateFormat fmt = Util.hhmmss;

	private final Timer m_timer = new Timer();

	private final JTextField m_refApi = new JTextField(10);
	private final JTextField m_fbServer = new JTextField(10);
	private final JTextField m_mdServer = new JTextField(10);
	private final JTextField m_hookServer = new JTextField(10);

	SouthPanel() {
		add( new JLabel("Ref API:"));
		add( m_refApi);
		add( Box.createHorizontalStrut(10));
		add( new JLabel("MD Server:"));
		add( m_mdServer);
		add( Box.createHorizontalStrut(10));
		add( new JLabel("FB Server:"));
		add( m_fbServer);
		add( Box.createHorizontalStrut(10));
		add( new JLabel("Hook Server:"));
		add( m_hookServer);
		add( Box.createHorizontalStrut(10));
		
		m_timer.schedule( new TimerTask() {
			@Override public void run() {
				update();
			}
		}, 100, 30000);
	}

	/** Called by the timer task */
	public void update() {
		try {
			test( Monitor.refApiBaseUrl() + "/api/ok", m_refApi);
			test( Monitor.m_config.mdBaseUrl() + "/mdserver/ok", m_mdServer);
			test( Monitor.m_config.fbBaseUrl() + "/fbserver/ok", m_fbServer);
			test( Monitor.m_config.hookBaseUrl() + "/hook/ok", m_hookServer);
		}
		catch( Exception e) {
			e.printStackTrace();
			m_refApi.setText( "ERROR - NO CONNECTION");
		}
	}

	public void stop() {
		m_timer.cancel();
	}

	private void test(String url, JTextField field) {
		long now = System.currentTimeMillis();

		MyClient.getString( url, data -> {
			if (data.equals("OK") || JsonObject.isObject(data) && JsonObject.parse( data).getString("code").equals("OK") ) {
				long elap = System.currentTimeMillis() - now;
				field.setText( String.format( "%s (%s ms)", elap < 500 ? "OK" : "SLOW", elap) );
				field.setBackground(Color.white);
			}
			else {
				field.setText( "ERROR");
				field.setBackground(Color.yellow);
			}
		});
	}
	
}
