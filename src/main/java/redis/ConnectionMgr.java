package redis;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.ib.controller.ApiController;
import com.ib.controller.ApiController.IConnectionHandler;

import tw.util.S;
import util.LogType;

/** Manage the connection from this client to TWS. */
// share this with Main; need logging support, reconnect interval. pas
class ConnectionMgr implements IConnectionHandler {
	private String m_host;
	private int m_port;
	private int m_clientId;
	private Timer m_timer;
	private boolean m_ibConnection;
	private final LogType m_logType;
	private final ApiController m_controller = new ApiController( this, null, null);
	boolean ibConnection() { return m_ibConnection; }

	ConnectionMgr(LogType logType) {
		m_logType = logType;
	}
	
	public ApiController controller() { 
		return m_controller;
	}

	void connect(String host, int port) {
		int clientId = 1; //MktDataServer.rnd.nextInt( Integer.MAX_VALUE) + 1; // use random client id, but not zero
		S.out( "%s connecting to TWS on %s:%s with client id %s", m_logType, host, port, clientId);
		
		m_host = host;
		m_port = port;
		m_clientId = clientId;
		startTimer();
		
		//S.out( "  done");
	}

	synchronized void startTimer() {
		if (m_timer == null) {
			
			m_timer = new Timer();
			m_timer.schedule(new TimerTask() {
				@Override public void run() {
					onTimer();
				}
			}, 0, MktDataServer.m_config.reconnectInterval() );
		}
	}

	synchronized void stopTimer() {
		if (m_timer != null) {
			m_timer.cancel();
			m_timer = null;
		}
	}

	synchronized void onTimer() {
		S.out( "%s trying...", m_logType);
		if (!m_controller.connect(m_host, m_port, m_clientId, "") ) {
			S.out( "%s failed", m_logType);
		}
		else {
			S.out( "%s success", m_logType);
		}
	}

	/** We are connected and have received server version */
	public boolean isConnected() {
		return m_controller.isConnected();
	}
	
	/** Called when we receive server version. We don't always receive nextValidId. */
	@Override public void onConnected() {
		MktDataServer.log( m_logType, "Connected to TWS");
		m_ibConnection = true; // we have to assume it's connected since we don't know for sure
		
		stopTimer();
	}
	
	/** Ready to start sending messages. */  // anyone that uses requestid must check for this
	@Override public synchronized void onRecNextValidId(int id) {
		// we really don't care if we get this because we are using random
		// order id's; it's because sometimes, after a reconnect or if TWS
		// is just startup up, or if we tried and failed, we don't ever receive
		// it
		MktDataServer.log( m_logType, "Received next valid id %s ***", id);  // why don't we receive this after disconnect/reconnect? pas
	}

	@Override public synchronized void onDisconnected() {
		if (m_timer == null) {
			MktDataServer.log( m_logType, "Disconnected from TWS");
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
				S.out( "You can't get market data in your paper account while logged into your production account");
				break;
		}
	
		S.out( "RECEIVED %s %s %s", id, errorCode, errorMsg);
	}

	@Override public void show(String string) {
		S.out( "Show: " + string);
	}

	/** Simulate disconnect to test reconnect */
	public void disconnect() {
		m_controller.disconnect();
	}
	
	public void dump() {
		m_controller.dump();
	}
	
}