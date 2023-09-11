package monitor;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.json.simple.JsonObject;

import common.Util;

public class SouthPanel extends JPanel {
	static SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");

	JLabel m_refApi = new JLabel();
	JLabel m_mktDataServer = new JLabel();

	SouthPanel() {
		
		add( new JLabel("RefAPI:"));
		add( m_refApi);
		add( Box.createHorizontalStrut(10));
		add( new JLabel("MktDataServer:"));
		add( m_mktDataServer);
		Util.executeEvery(100, 5000, () -> update() ); 
	}

	private void update() {
	    AsyncHttpClient client = new DefaultAsyncHttpClient();  //might you need the cursor here as well?
		client
			.prepare("GET", "https://reflection.trading/api/ok")
		  	.execute()
		  	.toCompletableFuture()
		  	.thenAccept( obj -> {
		  		try {
		  			client.close();
		  			JsonObject json = JsonObject.parse( obj.getResponseBody() );
		  			if (json.getString("code").equals("OK") ) {
		  				m_refApi.setText( fmt.format( new Date() ) );
		  			}
		  			else {
		  				m_refApi.setText( "Error");
		  			}
		  		}
		  		catch (Exception e) {
		  			e.printStackTrace();
		  		}
		  	});
	}
}
