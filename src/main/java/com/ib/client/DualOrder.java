package com.ib.client;

import java.util.HashMap;
import java.util.function.Consumer;

import org.json.simple.JsonObject;

import com.ib.client.SingleOrder.SingleParent;
import com.ib.client.SingleOrder.Type;
import com.ib.client.Types.Action;
import com.ib.client.Types.OcaType;
import com.ib.client.Types.TimeInForce;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.LiveOrder;

import reflection.Prices;
import reflection.TradingHours.Session;
import tw.util.S;

/** This gives a view of a single order but actually places two orders,
 *  one on SMART and one on OVERNIGHT. */
public class DualOrder implements SingleParent {
	public interface DualParent {
		void onStatusUpdated(DualOrder order, int permId, Action action, int totalFilled, double avgPrice);
	}
	
	private ApiController m_conn;
	private final DualParent m_parent;
	private final SingleOrder m_dayOrder;
	private final SingleOrder m_nightOrder;
	private String m_name;  // for debug only; could change to an enum

	/** prices are only need for sim stop orders on Overnight */ 
	public DualOrder( 
			ApiController conn, 
			Prices prices, 
			String name, 
			String key,
			DualParent parent 
			) {
		m_parent = parent;
		m_conn = conn;
		m_dayOrder = new SingleOrder( conn, prices, SingleOrder.Type.Day, name, key + " day", this);
		m_nightOrder = new SingleOrder( conn, prices, Type.Night, name, key + " night", this);
		m_name = name;
	}
	
	public String name() {
		return m_name;
	}
	
	public JsonObject toJson() {
		return common.Util.toJson( 
				"day_order", m_dayOrder,
				"night_order", m_nightOrder);
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

	public void ocaGroup(String group) {
		both( order -> order.o().ocaGroup(group) );
		both( order -> order.o().ocaType(OcaType.ReduceWithBlocking) );
	}

	public void orderType(OrderType type) {
		both( order -> order.o().orderType(type) );
	}
	
	private void both(Consumer<SingleOrder> consumer) {
		consumer.accept( m_dayOrder);
		consumer.accept( m_nightOrder);
	}

	/** really place or restore order */
	public void placeOrder( int conid, HashMap<String,LiveOrder> liveOrders) throws Exception {
		common.Util.require( m_conn.isConnected(), "not connected");
		
		// we have two choices; either save the permId for the single order that was last active,
		// or look for the correct order based on orderId, side, and, for sell, also name (profit/stop)
		
		Contract contract = new Contract();
		contract.conid( conid);
		
		contract.exchange( Session.Smart.toString() );
		m_dayOrder.o().tif( TimeInForce.GTC);
		m_dayOrder.placeOrder( contract, liveOrders);
		
		contract.exchange( Session.Overnight.toString() );
		m_nightOrder.o().tif( TimeInForce.DAY);
		m_nightOrder.placeOrder( contract, liveOrders);
	}

	public void display() {
		S.out( "day order: " + m_dayOrder.toString() );
		S.out( "night order: " + m_nightOrder.toString() );
	}

	public void cancel() {
		both( order -> order.cancel() );
	}

	/** Called when one of the child orders status updates; could be filled,
	 *  partially filled, or not at all filled. Note that DualOrder can be
	 *  complete even if both children are still work, if the total fill size
	 *  is sufficient */
	@Override public void onStatusUpdated(SingleOrder single, int permId, Action action, int filled, double avgPrice) {
		S.out( "DualOrder status  name=%s  permId=%s  action=%s  filled=%s  avgPrice=%s",
				single.name(), permId, action, filled, avgPrice);
		
		m_parent.onStatusUpdated( this, permId, action, filled, avgPrice); 
	}

	private boolean isComplete() {
		return m_dayOrder.isComplete() && m_nightOrder.isComplete();
	}

	public void stopPrice(double stopPrice) {
		both( order -> order.o().stopPrice( stopPrice) );
	}

}
