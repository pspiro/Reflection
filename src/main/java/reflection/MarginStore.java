package reflection;

import java.io.IOException;
import java.util.HashMap;

import org.json.simple.JsonArray;
import org.json.simple.TsonArray;

import com.ib.client.Contract;
import com.ib.client.Execution;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.LiveOrder;

import common.Alerts;
import common.NiceTimer;
import tw.util.S;

class MarginStore extends TsonArray<MarginOrder> {
	private String m_filename ;
	private boolean m_started;
	private ApiController m_conn;
	private final NiceTimer m_processTimer = new NiceTimer();  // check every ten sec
	private final NiceTimer m_saveTimer = new NiceTimer(); // save up to every
	private final Runnable m_saver = () -> saveNow(); 
	
	public MarginStore(String filename, ApiController conn) {
		m_filename = filename;
		m_conn = conn;
	}

	/** Called after store is created or read from disk */
	public void postInit() {
		forEach( order -> order.postInit() );		
	}

	public void saveLater() {
		m_saveTimer.schedule( 500, m_saver);
	}
	
	void saveNow() {
		try {
			S.out( "Writing margin store");
			writeToFile(m_filename);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/** Return orders for the specified wallet address */
	public JsonArray getOrders(String walletAddr) {
		JsonArray ar = new JsonArray();
		
		for (MarginOrder order : this) {
			if (order.wallet().equalsIgnoreCase( walletAddr) ) {
				ar.add( order);
			}
		}
		
		return ar;
	}

	public MarginOrder getById(String orderId) {
		for (MarginOrder order : this) {
			if (order.orderId().equals( orderId) ) {
				return order;
			}
		}
		return null;
	}

	/** connection to TWS has been established; could be the first time or a subsequent time */
	public synchronized void onReconnected() {
		try {
			// fetch all live orders from TWS
			m_conn.reqLiveOrderMap( permIdMap -> onRecMap( permIdMap) );
		} 
		catch (Exception e1) {
			Alerts.alert( "RefAPI", "COULD NOT GET LIVE ORDER MAP", "");
			S.out( "Could not get live order map; we should probably reset the connection to TWS. Will try again in 30 seconds");
			e1.printStackTrace();

			onRecMap(new HashMap<Integer, LiveOrder>() );
		}
	}
	
	public synchronized void onRecMap(HashMap<Integer, LiveOrder> permIdMap) {
		
		// build map of order ref to live orders
		HashMap<String, LiveOrder> orderRefMap = getOrderRefMap( permIdMap);  // map orderRef to LiveOrder; better would be map orderId to list of orders with with that orderId
		
		// call this every time we reconnect to update the margin orders with the
		// current live IB orders; use executeEvery to put this in the same thread
		// as the calls to MarginOrder.process() so they don't overlap
		m_processTimer.executeEvery( 0, 0, () -> 
			forEach( order -> order.onReconnected( orderRefMap) ) );
		
		// start the thread only once, after the first successful connection to TWS, then it runs forever
		if (!m_started) {
			m_started = true;
			
			//forEach( order -> order.restart() );
			m_processTimer.executeEvery( 0, 10000, () -> 
				forEach( order -> order.process() ) );
		}
	}

	public static HashMap<String, LiveOrder> getOrderRefMap(HashMap<Integer, LiveOrder> permIdMap) {
		HashMap<String, LiveOrder> orderRefMap = new HashMap<>();
		for (var liveOrder : permIdMap.values() ) {
			if (S.isNotNull( liveOrder.orderRef() ) ) {
				orderRefMap.put( liveOrder.orderRef(), liveOrder);
			}
		}
		return orderRefMap;
	}

}
