package common;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.ib.controller.ApiController;
import com.ib.controller.ApiController.IConnectionHandler;

import tw.util.S;

/** Base class for TWS connection manager for RefAPI and MdServer */
public abstract class ConnectionMgrBase implements IConnectionHandler {
	protected String m_host;
	protected int m_port;
	protected int m_clientId;
	protected long m_reconnectInterval;
	protected Timer m_timer;
	protected boolean m_ibConnection;  // status of TWS connection to IB
	protected final ApiController m_controller = new ApiController( this, null, null);
	
	public boolean ibConnection() { return m_ibConnection; }
	public ApiController controller() { return m_controller; }

	public ConnectionMgrBase(String host, int port, int clientId, long reconnectInterval) {
		m_host = host;
		m_port = port;
		m_clientId = clientId;
		m_reconnectInterval = reconnectInterval;
	}

	/** Attempto to connect every n seconds */
	public synchronized void startTimer() {
		if (m_timer == null) {
			m_timer = new Timer();
			m_timer.schedule(new TimerTask() {
				@Override public void run() {
					onTimer();
				}
			}, 500, m_reconnectInterval);  // give initial 500ms delay because we can go into a loop where we connect/disconnect (502 error)
		}
	}
	
	/** Called from onConnected */
	private synchronized void stopTimer() {
		if (m_timer != null) {
			m_timer.cancel();
			m_timer = null;
		}
	}

	/** Called by the timer every n seconds */
	protected final void onTimer() {
		try {
			connectNow();
			S.out( "  connect() success");
		}
		catch( Exception e) {
			e.printStackTrace();
		}
	}

	/** Attempt to connect now (synchronized) */
	private synchronized void connectNow() throws Exception {
		S.out( "Connecting to TWS on %s:%s with client id %s", m_host, m_port, m_clientId);
		if (!m_controller.connect(m_host, m_port, m_clientId, "") ) {
			throw new Exception("Could not connect to TWS");
		}
	}

	/** Called when connection is established */
	@Override public void onConnected() {
		stopTimer();
		m_ibConnection = true; // we have to assume it's connected since we don't know for sure
	}

	/** Call to disconnect */
	public final void disconnect() {
		m_controller.disconnect();
	}
	
	public final void dump() {
		m_controller.dump();
	}

	/** Remove final and override if desired */
	@Override final public void show(String string) {
		S.out( "Show: " + string);
	}
	
	/** Remove final and override if desired */
	@Override final public void accountList(List<String> list) {
	}
	
	/** Remove final and override if desired */
	@Override final public void error(Exception e) {
		e.printStackTrace();
	}	

	/** We are connected and have received server version */
	public final boolean isConnected() {
		return m_controller.isConnected();
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
			case 502:
				S.out( "Received 502. It may be that EITHER another client is connected with the same client ID OR TWS is not accepting connections from this IP address");
		}
	}
}
