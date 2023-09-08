package reflection;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.json.simple.JsonArray;

import common.Util;
import fireblocks.StockToken;
import redis.ConfigBase;
import tw.google.NewSheet;
import tw.google.NewSheet.Book;
import tw.google.NewSheet.Book.Tab.ListEntry;

public class Stocks implements Iterable<Stock> {
	private static Stock NULL = new Stock();
	private final HashMap<Integer,Stock> m_stockMap = new HashMap<Integer,Stock>(); // map conid to JSON object storing all stock attributes; prices could go here as well if desired. pas
	private final JsonArray m_stocks = new JsonArray(); // all Active stocks as per the Symbols tab of the google sheet; array of JSONObject
	private final JsonArray m_hotStocks = new JsonArray(); // all Active stocks as per the Symbols tab of the google sheet; array of JSONObject
	
	public void readFromSheet(ConfigBase config) throws Exception {
		readFromSheet( NewSheet.getBook(NewSheet.Reflection), config);
	}

	/** Use this version for better performance when reading multiple tabs from same sheet */
	public void readFromSheet(Book book, ConfigBase config) throws Exception {
		// clear out exist data; this is needed in case refreshConfig() is being called
		m_stocks.clear();
		m_stockMap.clear();
		m_hotStocks.clear();
		
		// read master list of symbols and map conid to entry
		HashMap<Integer,ListEntry> map = readMasterSymbols(book);
		
		for (ListEntry row : book.getTab( config.symbolsTab() ).fetchRows(true) ) {  // we must pass "true" for formatted so we get the start and end dates in the right format (yyyy-mm-dd); if that's a problem, write a getDate() method
			Stock stock = new Stock();
			if ("Y".equals( row.getString( "Active") ) ) {
				int conid = Integer.valueOf( row.getString("Conid") );

				stock.put( "conid", String.valueOf( conid) );
				
				String address = row.getString("TokenAddress");
				Util.require( Util.isValidAddress(address), "stock address is invalid: " + address);
				stock.put( "smartcontractid", address);
				stock.put( "startDate", row.getString("Start Date") );
				stock.put( "endDate", row.getString("End Date") );
				stock.put( "convertsToAmt", row.getDouble("Converts To Amt") );
				stock.put( "convertsToAddress", row.getString( "Converts To Address") );
				stock.put( "allow", row.getString("Allow") );
				
				
				ListEntry masterRow = map.get(conid);
				Util.require( masterRow != null, "No entry in Master-symbols for conid " + conid);
				stock.put( "symbol", masterRow.getString("Symbol") );
				stock.put( "description", masterRow.getString("Description") );
				stock.put( "type", masterRow.getString("Type") ); // Stock, ETF, ETF-24
				stock.put( "exchange", masterRow.getString("Exchange") );
				stock.put( "is24hour", masterRow.getBool("24-Hour") );
				stock.put( "isHot", masterRow.getBool("Hot") );
				stock.put( "tradingView", masterRow.getString("Trading View") );

				m_stocks.add( stock);
				m_stockMap.put( conid, stock);

				if (stock.isHot() ) {
					m_hotStocks.add( stock);
				}
			}
		}
		
		m_stocks.sort(null);
		m_hotStocks.sort(null);
	}

	/** @return map of conid -> ListEntry */
	private static HashMap<Integer, ListEntry> readMasterSymbols(Book book) throws Exception {
		HashMap<Integer,ListEntry> map = new HashMap<>();
		for (ListEntry entry : book.getTab( "Master-symbols").fetchRows(false) ) {
			map.put( entry.getInt("Conid"), entry);
		}
		return map;
	}

	public JsonArray stocks() {
		return m_stocks;
	}
	
	public HashMap<Integer, Stock> stockMap() {
		return m_stockMap;
	}
	
	public Collection<Stock> stockSet() {
		return m_stockMap.values();
	}
	
	public JsonArray hotStocks() {
		return m_hotStocks;
	}

	@Override public Iterator<Stock> iterator() {
		return stockSet().iterator();
	}

	/** Return smart contract address of any stock 
	 * @throws Exception */
	public StockToken getAnyStockToken() throws Exception {
		return new StockToken( m_stocks.get(0).getLowerString("smartcontractid") );
	}

	public Stock getStock(int conid) {
		Stock stock = m_stockMap.get(conid);
		return stock != null ? stock : NULL;
	}
}
