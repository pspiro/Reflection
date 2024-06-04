package reflection;

import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import common.Util;

public class MarginTrans extends MyTransaction {
	static JsonObject marginConfig;

	MarginTrans(Main main, HttpExchange exchange, boolean debug) {
		super(main, exchange, debug);
	}

	public void marginStatic() {
		wrap( () -> {
			if (marginConfig == null) {
				setMarginConfig();
			}
			respond( marginConfig);
		});
	}
	
	private void setMarginConfig() {
		String[] gtc = { "Never, Always" };

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
}
