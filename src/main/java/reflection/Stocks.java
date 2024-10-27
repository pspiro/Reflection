package reflection;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.json.simple.JsonArray;

import chain.Chain;
import common.Util;
import tw.google.NewSheet;
import tw.google.NewSheet.Book;
import tw.google.NewSheet.Book.Tab.ListEntry;
import tw.util.S;
import web3.NodeInstance;
import web3.StockToken;

public class Stocks implements Iterable<Stock> {
	private final HashMap<Integer,Stock> m_conidMap = new HashMap<Integer,Stock>(); // map conid to JSON object storing all stock attributes; prices could go here as well if desired. pas
	private final HashMap<String,Stock> m_tokenAddrMap = new HashMap<String,Stock>(); // map contract address (lower case) to JSON object storing all stock attributes
	private final JsonArray m_stocks = new JsonArray(); // all Active stocks as per the Symbols tab of the google sheet; array of JsonObject
	private final JsonArray m_hotStocks = new JsonArray(); // hot stocks as per the spreadsheet
	
	/** @param chain could be null when read from MdServer */
	public void readFromSheet(String symbolsTab, Chain chain) throws Exception {
		readFromSheet( NewSheet.getBook(NewSheet.Reflection), symbolsTab, chain);
	}

	/** Use this version for better performance when reading multiple tabs from same sheet
	    @param chain could be null when read from MdServer */
	public void readFromSheet(Book book, String symbolsTab, Chain chain) throws Exception {
		S.out( "Reading stocks from %s", symbolsTab);
		
		// clear out exist data; this is needed in case refreshConfig() is being called
		m_stocks.clear();
		m_conidMap.clear();
		m_hotStocks.clear();
		m_allAddresses = null;
		
		// read master list of symbols and map conid to entry
		HashMap<Integer,ListEntry> masterList = readMasterSymbols(book);
		
		for (ListEntry row : book.getTab( symbolsTab).fetchRows(true) ) {  // we must pass "true" for formatted so we get the start and end dates in the right format (yyyy-mm-dd); if that's a problem, write a getDate() method
			Stock stock = new Stock(chain);
			if ("Y".equals( row.getString( "Active") ) ) {
				int conid = Integer.valueOf( row.getString("Conid") );

				stock.put( "conid", String.valueOf( conid) );
				
				// read fields from from specific tab 
				String address = row.getString("TokenAddress");
				Util.require( Util.isValidAddress(address), "stock address is invalid: " + address);
				stock.put( "smartcontractid", address);
				stock.put( "startDate", row.getString("Start Date") );
				stock.put( "endDate", row.getString("End Date") );
				stock.put( "convertsToAmt", row.getDouble("Converts To Amt") );
				stock.put( "convertsToAddress", row.getString( "Converts To Address") );
				stock.put( "allow", row.getString("Allow") );
				stock.put( "tokenSymbol", row.getString("Token Symbol"));
				stock.put( "isHot", row.getBool("Hot") );
				
				// read fields from Master tab
				ListEntry masterRow = masterList.get(conid);
				Util.require( masterRow != null, "No entry in Master-symbols for conid " + conid);
				stock.put( "symbol", masterRow.getString("Symbol") );  // e.g. AAPL (Apple)
				stock.put( "description", masterRow.getString("Description") );
				stock.put( "type", masterRow.getString("Type") ); // Stock, ETF, ETF-24
				stock.put( "exchange", masterRow.getString("Exchange") );
				stock.put( "is24hour", masterRow.getBool("24-Hour") );
				stock.put( "tradingView", masterRow.getString("Trading View") );

				m_stocks.add( stock);
				m_conidMap.put( conid, stock);
				m_tokenAddrMap.put( address.toLowerCase(), stock);

				if (stock.isHot() ) {
					m_hotStocks.add( stock);
				}
			}
		}
		
		m_stocks.sort(null);
		m_hotStocks.sort(null);
		
		// pre-fill decimals map to avoid unnecessary queries
		// really only HookServer needs this because the other apps know how
		// many decimals there are
		NodeInstance.setDecimals( 18, getAllContractsAddresses() );
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
		return m_conidMap;
	}
	
	public Collection<Stock> stockSet() {
		return m_conidMap.values();
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
		var stock = m_conidMap.entrySet().iterator().next().getValue();
		return stock.getToken();
	}

	/** Return the stock or null */
	public Stock getStockByConid(int conid) {
		return m_conidMap.get(conid);
	}

	/** takes the Token Symbol from the release-specific tab, e.g. AAPL.r */ 
	public Stock getStockBySymbol(String tokenSymbol) throws Exception {
		for (Stock stock : this) {
			if (tokenSymbol.equals(stock.tokenSymbol() ) ) {
				return stock;
			}
		}
		throw new Exception("Stock not found");
	}

	/** Takes the token symbol from the release-specific tab;
	 *  could return null */ 
	public Stock getStockByTokenAddr(String address) throws Exception {
		return m_tokenAddrMap.get( address.toLowerCase() );
	}

	/** Used for querying for stock positions */
	private String[] m_allAddresses;
	
	/** Return array of all stock contract addresses */
	public String[] getAllContractsAddresses() {
		if (m_allAddresses == null) {
			m_allAddresses = new String[m_conidMap.size()];
			
			int i = 0;
			for (Stock stock : m_conidMap.values() ) {
				m_allAddresses[i++] = stock.getSmartContractId();
			}
		}
		return m_allAddresses;
	}

	/** Return time of most recent last price in ms */
	public long getLatest() {
		long time = 0;
		for (var stock : this) {
			time = Math.max( time, stock.lastTime() );
		}
		return time;
	}
}
