package chain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import reflection.Prices;
import tw.google.NewSheet;
import tw.google.NewSheet.Book;
import tw.util.S;

/* the stocks are read in from the 'Master-symbols' tab. the stock represents a stock
 * on the exchange or at IB, but has no association with a blockchain;
 * see also StockToken */ 
public class Stocks {
	private JsonArray hotStocks = new JsonArray(); // for hot stocks
	private JsonArray watchList = new JsonArray(); // for watch list
	private HashMap<Integer,Stock> conidMap = new HashMap<>(); // map conid to Stock, all stocks

	/** json stock with conid, symbol, put and call, for sending to frontend */
	static class MiniStock extends JsonObject { }

	/** Holds the parameters from the spreadsheet */
	public static record StockRec(
			int conid,
			boolean active,
			boolean isHot,
			boolean is24Hour, 
			String symbol, 
			String description, 
			String type,
			// String exchange,  not needed by frontend
			String tradingView,
			String startDate,
			String endDate
			) implements Comparable<StockRec> {

		/** return Json for the get-all-stocks query */
		public JsonObject toJson() {
			return Util.toJson(
					"conid", "" + conid,
					"symbol", symbol,
					"description", description,
					"tradingView", tradingView);
		}

		@Override public int compareTo(StockRec other) {
			return symbol.compareTo( other.symbol);
		}
	}

	/** top-level stock w/ no blockchain info */
	public static class Stock {
		private StockRec rec;
		private Prices prices = Prices.NULL;
		private final MiniStock jstock; // sent to frontend for hot stocks and watch list

		public Stock( StockRec trecIn, MiniStock jstockIn) {
			rec = trecIn;
			jstock = jstockIn;
		}

		public void setPrices(Prices pricesIn) {
			// maintain the existing 'last' price if the new prices do not have a valid last
			if (!pricesIn.validLast() ) {
				pricesIn.last( prices.last() );
			}
			
			prices = pricesIn;
			
			jstock.put( "bid", prices.anyBid() );
			jstock.put( "ask", prices.anyAsk() );
			
			// we never delete a valid last price
//			if (prices.last() > 0) {
//				jstock.put( "last", prices.); // I think it's wrong and Frontend doesn't use this pas
//			}

		}
		
		public int conid() {
			return rec.conid();
		}

		public String symbol() {
			return rec.symbol();
		}
		
		public StockRec rec() {
			return rec;
		}

		public Prices prices() {
			return prices;
		}

		public String getMdExchange() {
			return "SMART";
		}

		public double markPrice() {
			return prices.markPrice();
		}
	}

	public void readFromSheet() throws Exception {
		readFromSheet( NewSheet.getBook( NewSheet.Reflection) );
	}

	/** Use this version for better performance when reading multiple tabs from same sheet
	    @param chain could be null when read from MdServer */
	public void readFromSheet(Book book) throws Exception {
		S.out( "Reading stocks from %s", book.name() );

		// clear out exist data; this is needed in case refreshConfig() is being called
		hotStocks.clear();
		watchList.clear();
		conidMap.clear();

		// read master tab
		ArrayList<StockRec> list = book.getTab( "M2").queryToRecList( StockRec.class);
		list.sort( null);

		// create master lists
		for (var rec : list) {
			if (rec.active() ) {
				MiniStock jstock = new MiniStock();
				jstock.put( "conid", "" + rec.conid() );  
				jstock.put( "symbol", rec.symbol() );  
	
				watchList.add( jstock);
	
				if (rec.isHot() ) {
					hotStocks.add( jstock);
				}
				
				Stock stock = new Stock( rec, jstock);
				conidMap.put( rec.conid(), stock);
			}
		}
	}
	
	public Collection<Stock> stocks() {
		return conidMap.values();
	}

	public static void main(String[] args) throws Exception {
		Stocks stocks = new Stocks();
		stocks.readFromSheet();
		stocks.show();
	}

	public JsonArray watchList() {
		return watchList;
	}
	
	public JsonArray hotStocks() {
		return hotStocks;
	}

	public Stock getStockByConid( int conid) {
		return conidMap.get( conid);
	}
	
	public void show() {
		S.out( "---- hot list");
		S.out( JsonArray.toJSONString( hotStocks) );
		S.out( "---- full list");
		S.out( JsonArray.toJSONString( watchList) );
		S.out( "---- top map");
		S.out( JsonObject.toJSONString( conidMap) );
	}

	/** Return time of most recent last price in ms */
	public long getLatest() {
		long time = 0;
		for (var stock : stocks() ) {
			time = Math.max( time, stock.prices().time() );
		}
		return time;
	}
}
