package reflection;

import java.io.IOException;
import java.util.HashMap;

import org.json.simple.TsonArray;

import com.ib.controller.ApiController;
import com.ib.controller.ApiController.LiveOrder;

import common.Alerts;
import common.NiceTimer;
import common.Util;
import reflection.MarginOrder.Status;
import tw.util.S;

class MarginStore extends TsonArray<MarginOrder> {
	private String m_filename ;
	private boolean m_started;
	private ApiController m_conn;
	private final NiceTimer m_processingThread = new NiceTimer( "MarginStore");  // check every ten sec
	private final Runnable m_saver = () -> saveNow();
	private final long m_pruneInterval;
	
	public MarginStore(String filename, ApiController conn, long pruneInterval) {
		m_filename = filename;
		m_conn = conn;
		m_pruneInterval = pruneInterval;
	}

	/** Called after store is created or read from disk */
	public void postInit() {
		forEach( order -> order.postInit() );		
	}

	public void saveLater() {
		m_processingThread.schedule( 500, m_saver);
	}
	
	private synchronized void saveNow() {
		try {
			S.out( "Writing margin store");
			writeToFile(m_filename);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/** Return orders for the specified wallet address */
	public synchronized TsonArray<MarginOrder> getOrders(String walletAddr) {
		var ar = new TsonArray<MarginOrder>();
		
		for (MarginOrder order : this) {
			if (order.wallet().equalsIgnoreCase( walletAddr) ) {
				ar.add( order);
			}
		}
		
		return ar;
	}

	/** wrong; this whole thing should be a map; or at least build a map after read it */
	public synchronized MarginOrder getById(String orderId) {
		for (MarginOrder order : this) {
			if (order.orderId().equals( orderId) ) {
				return order;
			}
		}
		return null;
	}

	/** Called when a new margin order is received from user */
	public void startOrder(MarginOrder order) {
		// add order to margin store and save
		synchronized( this) {
			add( order);
			saveNow();
		}
		
		// initiate process to accept stablecoin payment
		// this executes in its own thread since it can take a while 
		order.acceptPayment();
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
	
	/** Called on reconnect 
	 *  @param pruneInterval completed orders should be removed after this much time */
	private synchronized void onRecMap(HashMap<Integer, LiveOrder> permIdMap) {
		// build map of order ref to live orders
		HashMap<String, LiveOrder> orderRefMap = getOrderRefMap( permIdMap);  // map orderRef to LiveOrder; better would be map orderId to list of orders with with that orderId
		
		// call this every time we reconnect to update the margin orders with the
		// current live IB orders; use executeEvery to put this in the same thread
		// as the calls to MarginOrder.process() so they don't overlap
		m_processingThread.execute( () -> forEach( order -> order.onReconnected( orderRefMap) ) );
		
		// start the thread only once, after the first successful connection to TWS, then it runs forever
		if (!m_started) {
			S.out( "Starting margin thread to check each order every ten seconds");
			m_started = true;
			
			m_processingThread.executeEvery( 0, 10000, () -> 
				forEach( order -> order.onProcess() ) );

			m_processingThread.executeEvery( 0, Util.MINUTE * 10, this::prune);
		}
	}

	/** Called in processing thread every ten minutes */
	private synchronized void prune() {
		for (var iter = iterator(); iter.hasNext(); ) {
			var order = iter.next();
			if (order.shouldPrune( m_pruneInterval) ) {
				S.out( "Pruning margin order " + order);
				iter.remove();
			}
		}
	}
	
	/** Build map of order ref to LiveOrder; order ref should be unique for each IB order */
	public static HashMap<String, LiveOrder> getOrderRefMap(HashMap<Integer, LiveOrder> permIdMap) {
		HashMap<String, LiveOrder> orderRefMap = new HashMap<>();
		for (var liveOrder : permIdMap.values() ) {
			if (S.isNotNull( liveOrder.orderRef() ) ) {
				orderRefMap.put( liveOrder.orderRef(), liveOrder);
			}
		}
		return orderRefMap;
	}

	public void cancelAll() {
		m_processingThread.execute( () -> {
			S.out( "Canceling all orders");
			forEach( order -> {
				order.systemCancel("SystemCancelAll");
				S.sleep( 10);  // don't break TWS pacing
			});
		});
	}

	/** Return false if there is an open order for the same wallet and conid */
	public boolean canPlace(String walletAddr, int conid) {
		for (MarginOrder order : this) {
			if (order.wallet().equalsIgnoreCase( walletAddr) &&
				order.conid() == conid &&
				(order.status() == Status.Monitoring || order.status() == Status.Liquidation) ) {
				return false;
			}
		}
		return true;
	}

}
