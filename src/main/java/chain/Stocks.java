package chain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import reflection.Prices;
import tw.google.NewSheet;
import tw.google.NewSheet.Book;
import tw.util.S;

public class Stocks {
	private ArrayList<MiniStock> hotList = new ArrayList<>(); // for hot stocks
	private ArrayList<MiniStock> fullList = new ArrayList<>(); // for watch list
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
			String exchange,
			String tradingView,
			String startDate,
			String endDate
			) {
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
			prices = pricesIn;
			
			jstock.put( "put", prices.anyBid() );
			jstock.put( "call", prices.anyAsk() );
			
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
		S.out( "Reading stocks from %s");

		// clear out exist data; this is needed in case refreshConfig() is being called
		hotList.clear();
		fullList.clear();
		conidMap.clear();

		// read master tab
		ArrayList<StockRec> list = book.getTab( "M2").queryToRecList( StockRec.class);

		// create master lists
		for (var rec : list) {
			if (rec.active() ) {
				MiniStock jstock = new MiniStock();
				jstock.put( "conid", rec.conid() );  
				jstock.put( "symbol", rec.symbol() );  
	
				fullList.add( jstock);
	
				if (rec.isHot() ) {
					hotList.add( jstock);
				}
	
				Stock stock = new Stock( rec, jstock);
				conidMap.put( rec.conid(), stock);
			}
		}
		
		hotList.sort(null);  // fix this. pas
		fullList.sort(null);
	}
	
	public Collection<Stock> stocks() {  //rename to list(). bc
		return conidMap.values();
	}

	public static void main(String[] args) throws Exception {
		Stocks stocks = new Stocks();
		stocks.readFromSheet();
		stocks.show();
	}

	public ArrayList<MiniStock> watchList() {
		return fullList;
	}
	
	public Stock getStockByConid( int conid) {
		return conidMap.get( conid);
	}
	
	void show() {
		S.out( "---- hot list");
		S.out( JsonArray.toJSONString( hotList) );
		S.out( "---- full list");
		S.out( JsonArray.toJSONString( fullList) );
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
