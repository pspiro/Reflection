package common;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.ib.controller.ApiController;
import com.ib.controller.ApiController.ApiParams;
import com.ib.controller.ApiController.IConnectionHandler;

import tw.util.S;

/** Base class for TWS connection manager for RefAPI and MdServer */
public abstract class ConnectionMgrBase implements IConnectionHandler {
	protected final ApiParams m_apiParams;
	protected long m_reconnectInterval; // in ms
	protected Timer m_timer;
	protected boolean m_ibConnection;  // status of TWS connection to IB
	protected final ApiController m_controller = new ApiController( this, null, null);
	
	public boolean ibConnection() { return m_ibConnection; }
	public ApiController controller() { return m_controller; }
	
	public ConnectionMgrBase(String host, int port, int clientId, long reconnectInterval) {
		this( new ApiParams( host, port, clientId), reconnectInterval);
	}

	/** @param reconnectInterval in ms */
	public ConnectionMgrBase(ApiParams apiParams, long reconnectInterval) {
		m_apiParams = apiParams;
		m_reconnectInterval = reconnectInterval;
	}
	
	public ApiParams apiParams() {
		return m_apiParams;
	}
	
	/** Attempt to connect every n seconds */
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

	/** Called by timer every n seconds */
	protected final void onTimer() {
		try {
			connectNow();
			S.out( "  connect() success");
		}
		catch( Exception e) {
			e.printStackTrace();
		}
	}
	
	/** Attempt to connect now */
	private synchronized void connectNow() throws Exception {
		if (!m_controller.connect(m_apiParams, "") ) {
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
	
	/** Dump top market data subscriptions */
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
	
	/** Update IB connection status and handle some common codes */
	@Override public void message(int id, int errorCode, String errorMsg, String advancedOrderRejectJson) {
		switch (errorCode) {
			case 1100:
				m_ibConnection = false;
				onIbConnectionUpdated( m_ibConnection);
				break;
			case 1102:
				m_ibConnection = true;
				onIbConnectionUpdated( m_ibConnection);
				break;
			case 10197:
				S.out( "You can't get market data in your paper account while logged into your production account");
				break;
			case 502:
				S.out( "Received 502. It may be that EITHER another client is connected with the same client ID OR TWS is not accepting connections from this IP address");
			default:
				S.out( "Code %s: %s %s", errorCode, errorMsg, advancedOrderRejectJson);
		}
	}
	
	/** Override if desired */
	protected void onIbConnectionUpdated(boolean connected) {
	}
}
