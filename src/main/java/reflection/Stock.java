package reflection;

import java.util.Objects;

import org.json.simple.JsonObject;

import common.Util;
import web3.StockToken;

/** Represents the static stock as downloaded from the google sheets
 * 
 *  All values are string, including conid, except bid and ask
 *  which are doubles. This object lives in the m_stockMap
 *  map and also in the m_stocks array. Each stock is itself
 *  a map (JSONObject) with keys "bid" and "ask". This is so
 *  we don't need to recreate the array every time the client
 *  queries for the prices, which is often.
 *  
 *   Type could be Stock, ETF, or ETF-24
 *   
 * The tags are:
	"smartcontractid": "0xd3383F039bef69A65F8919e50d34c7FC9e913e20",
	"symbol": "IBM",
	"description": "International Business Machines",
	"conid": "8314",
	"exchange": "SMART",
	"type": "Stock",
	"is24hour": boolean
	"exchangeStatus",
	"exchangeTime",
	
	"bid": 128.5
	"ask": 128.6,
	
    
 *    */
public class Stock extends JsonObject {
	private Prices m_prices = Prices.NULL;  // this does not get serialized into the json

	void setPrices( Prices prices) {
		Objects.requireNonNull(prices, "prices cannot be null");
		m_prices = prices;

		put( "bid", Main.round( prices.anyBid() ) );  // for front-end display
		put( "ask", Main.round( prices.anyAsk() ) );
	}

	Prices prices() { 
		return m_prices; 
	}

	/** @return stock token contract address */
	public String getSmartContractId() {
		return getString("smartcontractid");
	}

	/** From master tab, e.g. AAPL (Apple) */
	public String symbol() {
		return getString("symbol");
	}

	/** From master tab */
	public String description() {
		return getString("description");
	}

	/** From specific tab, e.g. AAPL.r */
	public String tokenSmbol() {
		return getString("tokenSymbol");
	}

	/** Returns zero if not found */
	public int conid() {
		return getInt("conid");
	}
	
	public boolean is24Hour() {
		return getBool("is24hour");
	}

	public boolean isHot() {
		return getBool("isHot");
	}

	@Override public int compareTo(JsonObject o) {
		return getString("symbol").compareTo(o.getString("symbol"));
	}

	public double queryTotalSupply() throws Exception {
		return new StockToken( getSmartContractId() ).queryTotalSupply();
	}
	
	public String getStartDate() {
		return getString("startDate");
	}

	public String getEndDate() {
		return getString("endDate");
	}
	
	public double getConvertsToAmt() { 
		return getDouble("convertsToAmt");
	}

	public String getConvertsToAddress() { 
		return getString("convertsToAddress");
	}

	public Allow allow() {
		return Util.getEnum( getString("allow"), Allow.values(), Allow.All);
	}
	
	public StockToken getToken() throws Exception {
		return new StockToken(getSmartContractId());
	}

	public String getType() {
		return getString("type");
	}

	/** This is used for mkt data subscription */
	public String getMdExchange() {
		return getType().equals("Crypto") ? "PAXOS" : "SMART";
	}
}
