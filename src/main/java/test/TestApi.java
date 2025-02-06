package test;

import com.ib.client.Contract;
import com.ib.client.Decimal;
import com.ib.client.MarketDataType;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.OrderStatus;
import com.ib.client.TickAttrib;
import com.ib.client.TickType;
import com.ib.client.Types.Action;
import com.ib.client.Types.SecType;
import com.ib.client.Types.TimeInForce;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.ApiParams;
import com.ib.controller.ApiController.IOrderHandler;
import com.ib.controller.ApiController.TopMktDataAdapter;
import com.ib.controller.ConnectionAdapter;

import common.Util;
import tw.util.S;

/*
 * 
 * chart project is in 'Other' at home
 * 
 * 
*/

public class TestApi extends ConnectionAdapter {
	static int clientId = 44;
	static ApiParams prod = new ApiParams( "34.100.227.194", 7393, clientId);
	static ApiParams dev = new ApiParams( "34.125.65.70", 7498, clientId);
	static ApiParams local = new ApiParams( "localhost", 7498, clientId);
	static ApiParams localProd = new ApiParams( "localhost", 9395, clientId);
	
	ApiController m_conn = new ApiController( this, null, null);
	
	public static void main(String[] args) {
		try {
			new TestApi().run(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	void run(String[] args) throws Exception {
		m_conn.connect( dev, "");
	}

	@Override public void onConnected() {
		S.out( "onConnected()");
	}
	
	@Override public void onRecNextValidId(int id) {
		Util.execute( () -> {
		m_conn.reqMktDataType(MarketDataType.DELAYED);

		Contract c = new Contract();
		//c.conid( 756733);
		c.secType( SecType.STK);
		c.currency( "USD");
		c.symbol( "IBM");
		c.exchange( "SMART");
		
		m_conn.reqTopMktData(c, "", false, false, new TopMktDataAdapter() {
			@Override public void tickPrice(TickType tickType, double price, TickAttrib attribs) {
				S.out( "ticked %s %s", tickType, price);
			}
		});
		});
//		m_conn.reqHistoricalData(c, new Date(), 1, DurationUnit.WEEK, BarSize._1_min, WhatToShow.TRADES, false, false, new IHistoricalDataHandler() {
//			
//			@Override
//			public void historicalDataEnd() {
//				S.out( "end");
//			}
//			
//			@Override
//			public void historicalData(Bar bar) {
//				S.out( bar);
//			}
//		});
	}

	public void placeOrder(int id) throws Exception {
		S.out( "rec valid id");
		
		Contract c = new Contract();
		c.conid( 274105);
		c.exchange( "OVERNIGHT");
		
		Order o = new Order();
		o.action(Action.Buy);
		o.roundedQty(1);
		o.lmtPrice(93);
		o.outsideRth(true);
		o.tif( TimeInForce.DAY);
		m_conn.placeOrder( c,  o,  new IOrderHandler() {
			
			@Override public void orderState(OrderState orderState) {
				S.out( "order state %s", orderState);
			}
			
			@Override public void onRecOrderStatus(OrderStatus status, Decimal filled, Decimal remaining, double avgFillPrice, int permId,
					int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {
				S.out( "order status %s", status);
				
			}
			
			@Override public void onRecOrderError(int errorCode, String errorMsg) {
				S.out( "order err %s %s", errorCode, errorMsg);
			}
		});
	}

	@Override
	public void onDisconnected() {
		S.out( "disconnected");
	}

	@Override
	public void error(Exception e) {
		e.printStackTrace();
		
	}

	@Override
	public void message(int id, int errorCode, String errorMsg, String advancedOrderRejectJson) {
		S.out("message: " + errorMsg);
		
	}

	@Override
	public void show(String string) {
		S.out("showing: " + string);
		
	}
}
