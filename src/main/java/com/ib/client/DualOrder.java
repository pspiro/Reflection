package com.ib.client;

import java.util.HashMap;
import java.util.function.Consumer;

import com.ib.client.SingleOrder.SingleParent;
import com.ib.client.SingleOrder.Type;
import com.ib.client.Types.Action;
import com.ib.client.Types.OcaType;
import com.ib.client.Types.TimeInForce;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.LiveOrder;

import reflection.Prices;
import tw.util.S;

/** This gives a view of a single order but actually places two orders,
 *  one on SMART and one on OVERNIGHT. */
public class DualOrder implements SingleParent {
	public interface DualParent {
		void onStatusUpdated(DualOrder order, OrderStatus ibOrderStatus, int permId, Action action, int totalFilled, double avgPrice);
	}
	
	private ApiController m_conn;
	private final DualParent m_parent;
	public final SingleOrder m_dayOrder;
	private final SingleOrder m_nightOrder;
	private String m_name;  // for debug only; could change to an enum

	/** prices are only need for sim stop orders on Overnight */ 
	public DualOrder( 
			ApiController conn, 
			Prices prices, 
			String name, 
			String key,
			int conid,
			DualParent parent 
			) {
		m_parent = parent;
		m_conn = conn;
		m_dayOrder = new SingleOrder( conn, prices, Type.Day, name, key + " day", conid, this);
		m_nightOrder = new SingleOrder( conn, prices, Type.Night, name, key + " night", conid, this);
		m_name = name;
	}
	
	public void rehydrate(HashMap<String, LiveOrder> orderRefMap) {
		m_dayOrder.rehydrate( orderRefMap);
		m_nightOrder.rehydrate( orderRefMap);
	}

	public String name() {
		return m_name;
	}
	
	public void quantity( int size) {
		both( order -> order.o().roundedQty( size) );
	}
	
	public void tif( TimeInForce tif) {
		both( order -> order.o().tif( tif) );
	}

	public void action(Action action) {
		both( order -> order.o().action(action) );
	}

	public void lmtPrice(double price) {
		both( order -> order.o().lmtPrice(price) );
	}

	public void transmit(boolean b) {
		both( order -> order.o().transmit(b) );
	}

	public void outsideRth(boolean b) {
		both( order -> order.o().outsideRth(b) );
	}

	public void orderRef(String uid) {
		both( order -> order.o().orderRef(uid) );
	}

	public void ocaGroup(String group) {  // not used
		both( order -> order.o().ocaGroup(group) );
		both( order -> order.o().ocaType(OcaType.ReduceWithBlocking) );
	}

	public void orderType(OrderType type) {
		both( order -> order.o().orderType(type) );
	}

	public void stopPrice(double stopPrice) {
		both( order -> order.o().stopPrice( stopPrice) );
	}

	public void checkOrder( int quantity) {
		both( order -> order.checkOrder( quantity) );
	}
	
	public void checkCanceled() {
		both( order -> order.checkCanceled() );
	}

	public void cancel() {
		both( order -> order.cancel() );
	}

	private void both(Consumer<SingleOrder> consumer) {
		consumer.accept( m_dayOrder);
		consumer.accept( m_nightOrder);
	}

	/** really place or restore order */
	public void placeOrder( int conid) throws Exception {
		common.Util.require( m_conn.isConnected(), "not connected");

		m_dayOrder.placeOrder();
		m_nightOrder.placeOrder();
	}

	public void display() {
		S.out( "day order: " + m_dayOrder.toString() );
		S.out( "night order: " + m_nightOrder.toString() );
	}

	/** Called when one of the child orders status updates; could be filled,
	 *  partially filled, or not at all filled. Note that DualOrder can be
	 *  complete even if both children are still work, if the total fill size
	 *  is sufficient */
	@Override public void onStatusUpdated(SingleOrder single, OrderStatus status, int permId, Action action, int filled, double avgPrice) {
		S.out( "DualOrder onStatus  name=%s  status=%s  permId=%s  action=%s  filled=%s  avgPrice=%s",
				single.name(), status, permId, action, filled, avgPrice);
		
		m_parent.onStatusUpdated( this, status, permId, action, filled, avgPrice); 
	}

	/** Called when the user updates the order price */
	public void resubmit() throws Exception {
		m_dayOrder.resubmit();
		m_nightOrder.resubmit();
	}



}
