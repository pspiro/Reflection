package com.ib.client;

import java.util.function.Consumer;

import org.json.simple.JsonObject;

import com.ib.client.SingleOrder.SingleParent;
import com.ib.client.SingleOrder.Type;
import com.ib.client.Types.Action;
import com.ib.client.Types.OcaType;
import com.ib.client.Types.TimeInForce;
import com.ib.controller.ApiController;

import reflection.Prices;
import reflection.TradingHours.Session;
import tw.util.S;

/** This gives a view of a single order but actually places two orders,
 *  one on SMART and one on OVERNIGHT. */
public class DualOrder implements SingleParent {
	public interface DualParent {
		void onCompleted(double totalFilled, DualOrder order);
	}
	
	private final DualParent m_parent;
	private final SingleOrder m_dayOrder;
	private final SingleOrder m_nightOrder;
	private int m_quantity;
	boolean m_done;
	private ApiController m_conn;

	/** prices are only need for sim stop orders on Overnight */ 
	public DualOrder( ApiController conn, DualParent parent, String name) {
		this( conn, parent, name, null);
	}
	
	public DualOrder( ApiController conn, DualParent parent, String name, Prices prices) {
		m_parent = parent;
		m_conn = conn;
		m_dayOrder = new SingleOrder( SingleOrder.Type.Day, prices, name, this);
		m_nightOrder = new SingleOrder( Type.Night, prices, name, this);
	}
	
	public JsonObject toJson() {
		return common.Util.toJson( 
				"day_order", m_dayOrder,
				"night_order", m_nightOrder);
	}
	
	public void quantity( int size) {
		m_quantity = size;
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
	
	public void placeOrder( int conid) throws Exception {
		common.Util.require( m_conn.isConnected(), "not connected");
		
		Contract contract = new Contract();
		contract.conid( conid);
		
		contract.exchange( Session.Smart.toString() );
		m_dayOrder.o().tif( TimeInForce.GTC);
		m_dayOrder.placeOrder( m_conn, contract);
		
		contract.exchange( Session.Overnight.toString() );
		m_nightOrder.o().tif( TimeInForce.DAY);
		m_nightOrder.placeOrder( m_conn, contract);
	}

	/** Set the IB order and listen for updates IF the permId matches */
	public void setOrder(ApiController conn, Order order) {
		if (order.permId() == 0) {
			S.out( "zero");
		}
		m_dayOrder.setOrder( conn, order);
		m_nightOrder.setOrder( conn, order);
	}

	public void display() {
		S.out( "day order: " + m_dayOrder.toString() );
		S.out( "night order: " + m_nightOrder.toString() );
	}

	public void cancel(ApiController conn) {
		both( order -> order.cancel( conn) );
	}

	public void tick(Session session) {
		m_dayOrder.tick( session == Session.Smart);
		m_nightOrder.tick( session == Session.Overnight);
	}

	public double getBalance() {
		return m_dayOrder.getBalance() + m_nightOrder.getBalance();
	}

	public double getPosition() {
		return m_dayOrder.filled() + m_nightOrder.filled();
	}

	/** Called when one of the child orders status updates; could be filled,
	 *  partially filled, or not at all filled. Note that DualOrder can be
	 *  complete even if both children are still work, if the total fill size
	 *  is sufficient */
	@Override public void onStatusUpdated(Type session, int filled) {
		if (!m_done) {
			double totalFilled = m_dayOrder.filled() + m_nightOrder.filled();

			if (totalFilled >= m_quantity || isComplete() ) {
				both( order -> order.cancel( m_conn) );
				m_parent.onCompleted( totalFilled, this);
				m_done = true;
			}
		}
	}

	private boolean isComplete() {
		return m_dayOrder.isComplete() && m_nightOrder.isComplete();
	}

	public void stopPrice(double stopPrice) {
		both( order -> order.o().stopPrice( stopPrice) );
	}

}
