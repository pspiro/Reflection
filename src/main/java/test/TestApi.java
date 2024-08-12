package test;

import java.util.HashMap;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.TickAttrib;
import com.ib.client.TickType;
import com.ib.client.Types.Action;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.LiveOrder;
import com.ib.controller.ApiController.TopMktDataAdapter;
import com.ib.controller.ConnectionAdapter;

import reflection.Config;
import reflection.TradingHours;
import tw.util.S;

public class TestApi extends ConnectionAdapter {
	ApiController m_conn = new ApiController( this, null, null);
	
	public static void main(String[] args) {
		try {
			new TestApi().run(args);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	void run(String[] args) throws Exception {
		Config m_config = Config.ask();
		
		m_conn.connect( m_config.twsOrderHost(), m_config.twsOrderPort(), 776, "");
		
		TradingHours hours = new TradingHours( m_conn, m_config);
		hours.startQuery();
		
		S.sleep( 5000);
		hours.getHours().display();
		S.out( hours.mustLiquidate(10));
		
		S.sleep( 5000);
		//m_conn.connect("34.125.231.254", 7498, 838, null);  // dev
		//m_conn.connect("34.100.227.194", 7393, 838, null);  // prod
	}

	@Override
	public void onConnected() {
		S.out( "onConnected()");
	}
	
	@Override
	public void onRecNextValidId(int id) {
		S.out( "rec valid id");
		
//		m_conn.reqPositions( new IPositionHandler() {
//			
//			@Override
//			public void positionEnd() {
//				S.out( "end");
//			}
//			
//			@Override
//			public void position(String account, Contract contract, Decimal pos, double avgCost) {
//				S.out( "pos %s ", pos);
//				
//			}
//		});
		
		Contract c = new Contract();
		c.conid( 265598);
		c.exchange( "SMART");
		
		m_conn.reqTopMktData(c, null, false, false, new TopMktDataAdapter() {
			@Override public void tickPrice(TickType tickType, double price, TickAttrib attribs) {
				S.out( "%s %s", tickType, price);
			}
	
		});
		
		
//		Order o = new Order();
//		o.action(Action.Buy);
//		o.roundedQty(1);
//		o.lmtPrice(150);
//		o.transmit(true);
//		o.outsideRth(true);
//		o.orderRef("ZZZZZZZZ");
//
//		try {
//			HashMap<Integer, LiveOrder> map = m_conn._reqLiveOrderMap();
//			map.values().forEach( ord -> show(ord) );
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		m_conn.reqTopMktData(c, null, false, false, new TopMktDataAdapter() {
//			@Override public void tickPrice(TickType tickType, double price, TickAttrib attribs) {
//				S.out( "%s %s", tickType, price);
//			}
//		});
	}

	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub
		
	}

	private void show(LiveOrder ord) {
		S.out( "id=%s  status=%s  filled=%s  price=%s",
				ord.orderId(), ord.status(), ord.filled(), ord.avgPrice() );
	}

	@Override
	public void error(Exception e) {
		e.printStackTrace();
		
	}

	@Override
	public void message(int id, int errorCode, String errorMsg, String advancedOrderRejectJson) {
		S.out(errorMsg);
		
	}

	@Override
	public void show(String string) {
		S.out(string);
		
	}
}
