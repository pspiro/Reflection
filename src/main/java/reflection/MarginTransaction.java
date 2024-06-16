package reflection;

import static reflection.Main.m_config;
import static reflection.Main.require;

import com.ib.client.DualOrder;
import com.ib.client.Types.Action;
import com.sun.net.httpserver.HttpExchange;

public class MarginTransaction extends MyTransaction {
	public static MarginOrderMgr mgr = new MarginOrderMgr();

	MarginTransaction(Main main, HttpExchange exchange) {
		super(main, exchange);
	}

	/** Used when creating for test program */
	public MarginTransaction(DualOrder o) {  //remove this. pas
		super( null, null);
	}

	void marginOrder() {
		wrap( () -> {
			parseMsg();
			m_walletAddr = m_map.getWalletAddress();
			validateCookie("marginOrder");
			
			require( m_main.orderController().isConnected(), RefCode.NOT_CONNECTED, "Not connected; please try your order again later");
			require( m_main.orderConnMgr().ibConnection() , RefCode.NOT_CONNECTED, "No connection to broker; please try your order again later");
			require( m_config.allowTrading().allow(Action.Buy), RefCode.TRADING_HALTED, "Trading is temporarily halted; please try your order again later");
			require( m_main.validWallet( m_walletAddr, Action.Buy), RefCode.ACCESS_DENIED, "Your order cannot be processed at this time (L9)");  // make sure wallet is not blacklisted

			MarginOrder order = new MarginOrder( m_main, m_map, m_uid);
			order.parse( m_main);
			mgr.add( order);
			mgr.write();
			
			respond( code, RefCode.OK); // Frontend will display a message which is hard-coded in Frontend
		});
	}
}
// you have to check how a GTC night order is handled; does it really stay around forever?
// you have to set up the day/night oca orders to reduce size on fill
// you have to code this for a market crash, but maybe not until you have bigger sizes