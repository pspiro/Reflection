package reflection;

import static reflection.Main.m_config;
import static reflection.Main.require;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import common.Util;
import web3.Stablecoin;

public class MarginTrans extends MyTransaction {
	static JsonObject marginConfig;
	private Stablecoin m_stablecoin;

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
	
	// we could remember the set of orders for each user and only resend the changes
	public void marginDynamic() {
		wrap( () -> {
			parseMsg();
			
			m_walletAddr = m_map.getWalletAddress();
			
			validateCookie( "margin-static");
			
			int conid = m_map.getRequiredInt("conid");
			
			JsonObject resp = Util.toJson(
					"bid", 83.83,
					"ask", 84.84,
					"orders", getOrders() );
			
			respond( resp);
		});
	}

	private JsonArray getOrders() throws Exception {
		JsonArray ar = m_config.sqlQuery( "select * from orders where wallet_public_key = '%s'", m_walletAddr.toLowerCase() );
		Util.forEach( ar, order -> {
			Stock stock = m_main.getStock( order.getInt( "conid") );
			Util.require( stock != null, "order id %s has invalid conid %s", order.getString( "orderId"), order.getInt( "conid") );

			Prices prices = stock.prices();
			order.put( "bidPrice", prices.bid() );
			order.put( "askPrice", prices.ask() );
		});
			
		ar.add( createFakeMarginOrder() );
		
		return ar;
	}

	JsonObject createFakeMarginOrder() {
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

	enum GoodUntil {
		Immediately,
		OneHour,
		EndOfDay,
		EndOfWeek,
		Never
	}
	
	public void marginOrder() {
		wrap( () -> {
			parseMsg();
			
			m_walletAddr = m_map.getWalletAddress();
			
			validateCookie( "margin-order");
			
			int conid = m_map.getRequiredInt("conid");
			Stock stock = m_main.getStock(conid);
			require( stock != null, RefCode.INVALID_REQUEST, "Invalid conid");

			double amountToSpend = m_map.getRequiredDouble( "amountToSpend");
			require( amountToSpend >= 0 && amountToSpend <= m_config.maxOrderSize(), RefCode.INVALID_REQUEST, "Order size is too large");
			require( amountToSpend >= m_config.minOrderSize(), RefCode.INVALID_REQUEST, "Order size is too small");

			double leverage = m_map.getRequiredDouble( "leverage");
			require( leverage >= 1 && leverage <= 20, RefCode.INVALID_REQUEST, "Leverage is out of range");
			
			double profitTakerPrice = m_map.getRequiredDouble( "profitTakerPrice");
			require( profitTakerPrice > 0, RefCode.INVALID_REQUEST, "profitTakerPrice is invalid");
			
			double entryrice = m_map.getRequiredDouble( "entryPrice");
			require( entryrice > 0, RefCode.INVALID_REQUEST, "entryPrice is invalid");
			
			double stopLossPrice = m_map.getRequiredDouble( "stopLossPrice");
			require( stopLossPrice > 0, RefCode.INVALID_REQUEST, "stopLossPrice is invalid");

			GoodUntil goodUntil = m_map.getEnumParam( "goodUntil", GoodUntil.values() );
			
			String currency = m_map.getRequiredString( "currency");
			require( currency.equals( m_config.rusd().name() ) || currency.equals( m_config.busd().name() ),
					RefCode.INVALID_REQUEST,
					"currency is invalid");
			
			m_stablecoin = currency.equals( m_config.rusd().name() ) ? m_config.rusd() : m_config.busd();
			
			String orderId = Util.uid( 10);
			
			JsonObject json = Util.toJson(
					"wallet_public_key", m_walletAddr.toLowerCase(),
					"orderId", orderId,
					"conid", conid,
					"action", "Buy",
					//"quantity", quantity,
					"amountToSpend", amountToSpend,
					"leverage", leverage,
					"entryPrice", entryrice,
					"profitTakerPrice", profitTakerPrice,
					"stopLossPrice", stopLossPrice,
					"goodUntil", goodUntil,
					"currency", currency
					);
			
			m_config.sqlCommand( sql -> sql.insertJson( "orders", json) );

			respond( code, RefCode.OK, "orderId", orderId);
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
