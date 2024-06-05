package reflection;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import common.Util;
import static reflection.Main.require;

public class MarginTrans extends MyTransaction {
	static JsonObject marginConfig;

	MarginTrans(Main main, HttpExchange exchange, boolean debug) {
		super(main, exchange, debug);
	}

	public void marginStatic() {
		wrap( () -> {
			getWalletFromUri();
			
			if (marginConfig == null) {
				setMarginConfig();
			}
			respond( marginConfig);
		});
	}
	
	private void setMarginConfig() {
		String[] gtc = "Never,Always,Sometimes".split( ",");

		//			JsonArray stocks = new JsonArray();
		//			
		//			m_main.stocks().forEach( stock -> {
		//				if (stock.canMargin() ) {
		//					stocks.add( Util.toJson( 
		//							""

		marginConfig = Util.toJson(
				"maxLeverage", 5.0,
				"goodUntil", gtc,
				"refreshInterval", 5000,
				"stocks", m_main.stocks().marginStocks().toArray( new Stock[0])
				);
	}

	// write test scripts for these
	
	public void marginDynamic() {
		wrap( () -> {
			parseMsg();
			
			m_walletAddr = m_map.getWalletAddress();
			
			validateCookie( "margin-static");
			
			int conid = m_map.getRequiredInt("conid");
			
			JsonArray orders = new JsonArray();
			orders.add( createMarginOrder() );
			orders.add( createMarginOrder() );
			
			JsonObject resp = Util.toJson(
					"bid", 83.83,
					"ask", 84.84,
					"orders", orders);
			
			respond( resp);
		});
	}

	private JsonObject createMarginOrder() {
		return Util.toJson(
				"conid", "265598",
				"orderId", "73827383728",
				"symbol", "APPL (Apple)",
				"sharesToBuy", 100.12,
				"sharesHeld", 100.12,
				"value", 2600.12,
				"loanAmount", 2500.12,
				"liquidationPrice", 50.12,
				"stopLossPrice", 60.12,
				"entryPrice", 70.12,
				"profitTakerPrice", 80.12,
				"bidPrice", 65.12,
				"askPrice", 66.12
				);
	}

	public void marginOrder() {
		wrap( () -> {
			parseMsg();
			
			m_walletAddr = m_map.getWalletAddress();
			
			validateCookie( "margin-static");
			
			int conid = m_map.getRequiredInt("conid");
			double amt = m_map.getRequiredDouble( "amountToSpend");

			double leverage = m_map.getRequiredDouble( "leverage");
			require( leverage >= 1 && leverage <= 20, RefCode.INVALID_REQUEST, "Leverage is out of range");
			
			double profitTakerPrice = m_map.getRequiredDouble( "profitTakerPrice");
			double lmtPrice = m_map.getRequiredDouble( "entryPrice");
			double stopLossPrice = m_map.getRequiredDouble( "stopLossPrice");
			String goodUntil = m_map.getRequiredString( "goodUntil");
			require( "SometimesAlwaysNever".indexOf( goodUntil) != -1, RefCode.INVALID_REQUEST, "Invalid goodUntil");
			String currency = m_map.getRequiredString( "currency");

			respondOk();
		});
	}

	public void marginCancel() {
		wrap( () -> {
			parseMsg();
			
			m_walletAddr = m_map.getWalletAddress();
			
			validateCookie( "margin-static");

			String id = m_map.getRequiredString("orderId");
			boolean liquidate = m_map.getBool("liquidate");
			

			respondOk();
		});
	}
}
