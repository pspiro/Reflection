package com.ib.client;

import java.util.function.Consumer;

import org.json.simple.JsonObject;

import com.ib.client.Types.Action;
import com.ib.client.Types.TimeInForce;
import com.ib.controller.ApiController;

import reflection.TradingHours.Session;
import tw.util.S;

/** This gives a view of a single order but actually places two orders,
 *  one on SMART and one on OVERNIGHT. */
public class DualOrder {
	public interface ParentOrder {
		void onCompleted(double totalFilled);
	}
	
	enum Type { Day, Night };  // you could combine this with TradingHours.Session
	
	private final ParentOrder m_parent;
	private final SingleOrder m_dayOrder;
	private final SingleOrder m_nightOrder;
	private int m_quantity;
	boolean m_done;
	private ApiController m_conn;

	public DualOrder( ParentOrder parent, ApiController conn) {
		m_parent = parent;
		m_conn = conn;
		m_dayOrder = new SingleOrder( Type.Day, this);
		m_nightOrder = new SingleOrder( Type.Night, this);
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
	}

	public void orderType(OrderType type) {
		both( order -> order.o().orderType(type) );
	}
	
	private void both(Consumer<SingleOrder> consumer) {
		consumer.accept( m_dayOrder);
		consumer.accept( m_nightOrder);
	}
	
	public void placeOrder( ApiController controller, int conid) throws Exception {
		common.Util.require( controller.isConnected(), "not connected");
		
		Contract contract = new Contract();
		contract.conid( conid);
		
		contract.exchange( Session.Smart.toString() );
		m_dayOrder.o().tif( TimeInForce.GTC);
		m_dayOrder.placeOrder( controller, contract);
		
		contract.exchange( Session.Overnight.toString() );
		m_nightOrder.o().tif( TimeInForce.DAY);
		m_nightOrder.placeOrder( controller, contract);
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

	public void onRecOrderStatus(Type session) {
		double totalFilled = m_dayOrder.filled() + m_nightOrder.filled();

		if (!m_done) {
			if (totalFilled >= m_quantity) {
				both( order -> order.cancel( m_conn) );
				m_parent.onCompleted( totalFilled);
				m_done = true;
			}
			
			else if (!m_dayOrder.isWorking() && !m_nightOrder.isWorking() ) {
				m_parent.onCompleted( totalFilled);
				m_done = true;
			}
		}
		
	}

}
