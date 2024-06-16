package com.ib.client;

import static reflection.Main.require;

import java.util.function.Consumer;

import org.json.simple.JsonObject;

import com.ib.client.Types.Action;
import com.ib.client.Types.TimeInForce;
import com.ib.controller.ApiController;

import reflection.RefCode;
import reflection.TradingHours.Session;
import tw.util.S;

/** This gives a view of a single order but actually places two orders,
 *  one on SMART and one on OVERNIGHT. */
public class DualOrder {
	private final SingleOrder m_dayOrder;
	private final SingleOrder m_nightOrder;

	public DualOrder() {
		m_dayOrder = new SingleOrder();
		m_nightOrder = new SingleOrder();
	}
	
	/** Called when restoring from file */
	public DualOrder(JsonObject json) {
		m_dayOrder = new SingleOrder( json);
		m_nightOrder = new SingleOrder( json);
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
	}

	public void orderType(OrderType type) {
		both( order -> order.o().orderType(type) );
	}
	
	private void both(Consumer<SingleOrder> consumer) {
		consumer.accept( m_dayOrder);
		consumer.accept( m_nightOrder);
	}
	
	public void placeOrder( ApiController controller, int conid) throws Exception {
		Contract contract = new Contract();
		contract.conid( conid);
		
		contract.exchange( Session.Smart.toString() );
		m_dayOrder.placeOrder( controller, contract);
		
		contract.exchange( Session.Overnight.toString() );
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
	
}