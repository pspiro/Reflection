package monitor;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.json.simple.JsonObject;

import common.Util;

public class SouthPanel extends JPanel {
	static SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");

	JLabel m_refApi = new JLabel();

	SouthPanel() {

		add( new JLabel("RefAPI:"));
		add( m_refApi);
		add( Box.createHorizontalStrut(10));
		Util.executeEvery(100, 5000, () -> update() ); 
	}

	private void update() {
		long now = System.currentTimeMillis();

		MyAsyncClient.get( "https://reflection.trading/api/ok", data -> {
			long elap = System.currentTimeMillis() - now;
			if (JsonObject.parse( data).getString("code").equals("OK") ) {
				String str = String.format( "%s in %s ms", fmt.format( new Date() ), elap); 
				m_refApi.setText( str);
			}
			else {
				m_refApi.setText( "Error");
			}
		});
	}
}

