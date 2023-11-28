package monitor;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Map;

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

	JTextField m_refApi = new JTextField(10);
	JTextField m_fbServer = new JTextField(10);
	JTextField m_mdServer = new JTextField(10);
	JTextField m_aapl = new JTextField(20);

	SouthPanel() {
		add( new JLabel("Ref API:"));
		add( m_refApi);
		add( Box.createHorizontalStrut(10));
		add( new JLabel("FB Server:"));
		add( m_fbServer);
		add( Box.createHorizontalStrut(10));
		add( new JLabel("MD Server:"));
		add( m_mdServer);
		add( Box.createHorizontalStrut(10));
		add( new JLabel("AAPL:"));
		add( m_aapl);
		add( Box.createHorizontalStrut(10));
		
		Util.executeEvery(100, 1000000, () -> update() ); 
	}

	private void update() {
		try {
			test( "/api/ok", m_refApi);
//			test( "/fbserver/ok", m_fbServer);
//			test( "/mdserver/ok", m_mdServer);
			
			
//			Map<String, String> map = Monitor.m_redis.query( jedis -> jedis.hgetAll("265598") );
//			m_aapl.setText( String.format( "%s : %s : %s : %s", map.get("bid"), map.get("ask"), map.get("last"), map.get("time") ) ); 
			
		}
		catch( Exception e) {
			e.printStackTrace();
			m_refApi.setText( "ERROR - NO CONNECTION");
		}
	}

	private void test(String url, JTextField field) {
		long now = System.currentTimeMillis();

		MyClient.getString( Monitor.base + url, data -> {
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
