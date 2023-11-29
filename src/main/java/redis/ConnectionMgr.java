package redis;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.ib.controller.ApiController;
import com.ib.controller.ApiController.IConnectionHandler;

import tw.util.S;

/** Manage the connection from this client to TWS. */
// share this with Main; need logging support, reconnect interval. pas
class ConnectionMgr implements IConnectionHandler {
	private MdServer m_main;
	private String m_host;
	private int m_port;
	private int m_clientId;
	private Timer m_timer;
	private boolean m_ibConnection;
	private final ApiController m_controller = new ApiController( this, null, null);
	private long m_reconnectInterval;
	
	boolean ibConnection() { return m_ibConnection; }

	public ApiController controller() { 
		return m_controller;
	}
	
	ConnectionMgr( MdServer main, String host, int port, int clientId, long reconnectInterval) {
		m_main = main;
		m_host = host;
		m_port = port;
		m_clientId = clientId;
		m_reconnectInterval = reconnectInterval;
	}

	synchronized void startTimer() {
		if (m_timer == null) {
			
			m_timer = new Timer();
			m_timer.schedule(new TimerTask() {
				@Override public void run() {
					onTimer();
				}
			}, 500, m_reconnectInterval);  // give initial 500ms delay because we can go into a loop where we connect/disconnect (502 error)
		}
	}

	synchronized void stopTimer() {
		if (m_timer != null) {
			m_timer.cancel();
			m_timer = null;
		}
	}

	void onTimer() {
		try {
			connectNow();
			m_main.log( "connect() success");
		}
		catch( Exception e) {
			m_main.log( "connect() failure");
			m_main.log(e);
		}
	}
	
	synchronized void connectNow() throws Exception {
		m_main.log( "Connecting to TWS on %s:%s with client id %s...", m_host, m_port, m_clientId);
		if (!m_controller.connect(m_host, m_port, m_clientId, "") ) {
			throw new Exception("Could not connect to TWS");
		}
	}

	/** We are connected and have received server version */
	public boolean isConnected() {
		return m_controller.isConnected();
	}
	
	/** Called when we receive server version. We don't always receive nextValidId. */
	@Override public void onConnected() {
		m_main.log( "Connected to TWS");
		m_ibConnection = true; // we have to assume it's connected since we don't know for sure
		
		stopTimer();
	}
	
	/** Ready to start sending messages.
	 *  Overridden in subclass
	 *  Anyone that uses requestid must check for this  */
	@Override public synchronized void onRecNextValidId(int id) {
		// we really don't care if we get this because we are using random
		// order id's; it's because sometimes, after a reconnect or if TWS
		// is just startup up, or if we tried and failed, we don't ever receive
		// it
		m_main.log( "Received next valid id %s ***", id);  // why don't we receive this after disconnect/reconnect? pas
	}

	@Override public synchronized void onDisconnected() {
		if (m_timer == null) {
			m_main.log( "Disconnected from TWS");
			startTimer();
		}
	}

	@Override public void accountList(List<String> list) {
	}

	@Override public void error(Exception e) {
		e.printStackTrace();
	}

	@Override public void message(int id, int errorCode, String errorMsg, String advancedOrderRejectJson) {
		switch (errorCode) {
			case 1100: 
				m_ibConnection = false; 
				break;
			case 1102: 
				m_ibConnection = true; 
				break;
			case 10197:
				m_main.log( "You can't get market data in your paper account while logged into your production account");
				break;
			case 502:
				m_main.log( "Received 502 - there may be another client connected to TWS with the same client ID");
		}
	
		m_main.log( "Received from TWS %s %s %s", id, errorCode, errorMsg);
	}

	@Override public void show(String string) {
		S.out( "Show: " + string);
	}

	public void disconnect() {
		m_controller.disconnect();
	}
	
	public void dump() {
		m_controller.dump();
	}
}