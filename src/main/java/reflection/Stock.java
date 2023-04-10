package reflection;

import org.json.simple.JSONObject;

/** All values are string, including conid, except bid and ask
 *  which are doubles. This object lives in the m_stockMap
 *  map and also in the m_stocks array. Each stock is itself
 *  a map (JSONObject) with keys "bid" and "ask". This is so
 *  we don't need to recreate the array every time the client
 *  queries for the prices, which is often.
 *  
 *   Type could be Stock, ETF, or ETF-24 */
class Stock extends JSONObject {
	Prices m_prices = Prices.NULL;

	void setPrices( Prices prices) {
		m_prices = prices;

		put( "bid", Main.round( prices.anyBid() ) );  // for front-end display
		put( "ask", Main.round( prices.anyAsk() ) );
	}

	Prices prices() { return m_prices; }

	public String getString(String key) {
		return (String)super.get(key);
	}
}