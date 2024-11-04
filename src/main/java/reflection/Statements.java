package reflection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import chain.Chain;
import chain.Stocks;
import common.SmtpSender;
import common.Util;
import tw.util.S;
import web3.NodeInstance;

// you could create an easier way for this, where you pass Json and it finds the replacement tags based on json tags
// pass a json array for multiple
// and don't require splitting the text into multiple strings, or use
// color the text based on buy or sell, or add a round icon
// you'll need to use GraphQl, I think , as you will send 2000 wallets x 40 contracts
// = 80,000 requests

// add users who are in jotform with status=Funded and wallet and email

// ******************

public class Statements {
	private record Position( String name, double quantity, double price, double unreal) {}

	private Config m_config;
	private Chain m_chain;
	private List<String> m_list;
	private boolean m_testing;
	private Stocks m_stocks;

	public static void main(String[] args) {
		try {
			SingleChainConfig c = SingleChainConfig.ask();
			new Statements( c, c.chain(), true, new Stocks() )  // don't set to false; there will be no prices
				.generateSummaries( 1);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	Statements( Config config, Chain chain, boolean testing, Stocks stocks) throws Exception {
		m_config = config;
		m_chain = chain;
		m_testing = testing;
		m_stocks = stocks;

		// set fake stock prices for testing
//		if (m_testing) {
//			for ( var stock : chain.stocks() ) {
//				stock.setPrices( new Prices( Util.toJson( "last", 83.82) ) );
//			}
//		}

		// get list of all stock contract addresses plus RUSD and BUSD
		m_list = Util.toList( chain.getAllContractsAddresses() );
		m_list.add( chain.params().rusdAddr().toLowerCase() );
		m_list.add( chain.params().busdAddr().toLowerCase() );
	}

	/** generate the daily summary emails */
	void generateSummaries( int maxToSend) throws Exception {
		S.out( "Generating daily email summaries");

		// get wallet and names from users table
		S.out( "querying users table");
		JsonArray users = m_config.sqlQuery( "select first_name, last_name, email, wallet_public_key from users");
		
		int count = 0;
		for (var userRec : users) {
			String wallet = userRec.getString( "wallet_public_key");
			
			if (m_testing ){
				wallet = NodeInstance.prod.toLowerCase();
				userRec = users.find( "wallet_public_key", wallet);
				Util.require( userRec != null, "Error");
			}
			
			try {
				if (generateSummary( wallet, userRec) && ++count == maxToSend) {
					break;
				}
				// EuroDNS allows only 500 per hour per account; switch to AWE SES
				// or use multiple mail accounts to go faster
				S.sleep( 7200);   
			}
			catch( Exception e) {
				S.out( "Error while generating summary for wallet %s", wallet);
				e.printStackTrace();
			}
		}

		S.out( "Sent %s summary emails", count);
	}


	/** @param walletLc lower case
	 * @param map maps wallet to record with first and last name, if we have it
	 * @return true if we sent an email  */
	private boolean generateSummary( String walletLc, JsonObject userRec) throws Exception {
		String email = userRec.getString( "email");
		
		if (!Util.isValidEmail( email) ) {
			return false;
		}

		S.out( "generating summary for %s", walletLc);

		// get positions from Blockchain
		// doesn't work for Pulsechain: HashMap<String, Double> positionsMap = MoralisServer.reqPositionsMap(       // you could also try graphql or 
				//walletLc, m_list.toArray( new String[0]) );
		HashMap<String, Double> positionsMap = m_chain.node().reqPositionsMap(       // you could also try graphql or 
				walletLc, m_list.toArray( new String[0]), 0);

		var transactions = m_config.getCompletedTransactions( walletLc); 

		// build average cost map
		var pnlMap = new PnlMap( transactions);

		// build array of Positions
		ArrayList<Position> positions = new ArrayList<>();
		
		boolean missedOne = false;

		// add stocks
		for ( var token : m_chain.tokens() ) {
			var stock = m_stocks.getStockByConid( token.conid() );
			var balance = positionsMap.get( token.address().toLowerCase() );
			if (balance != null && balance > 0) {
				// calculate pnl
				var pair = pnlMap.get( token.conid() );
				double pnl = pair != null ? pair.getPnl( stock.markPrice(), balance) : 0;
				
				positions.add( new Position( stock.symbol(), balance, stock.markPrice(), pnl) );

				if (!stock.prices().validLast() ) {
					S.out( "Error: no valid last for %s when generating statement", stock.symbol() );
					missedOne = true;
				}
			}
		}
		
		// sort positions by symbol
		positions.sort( (p1, p2) -> p1.name().compareTo( p2.name() ) );

		// add RUSD and BUSD at the bottom
		Util.iff( positionsMap.get( m_chain.params().rusdAddr() ), pos -> 
			positions.add( new Position( m_chain.rusd().name() + " (stablecoin)", (double)pos, 1, 0) ) );
		Util.iff( positionsMap.get( m_chain.params().busdAddr() ), pos -> 
			positions.add( new Position( m_chain.busd().name() + " (stablecoin)", (double)pos, 1, 0) ) );

		// build the positions section
		StringBuilder posRows = new StringBuilder();
		double total = 0;
		for (Position pos : positions) {
			double value = pos.quantity() * pos.price();
			if (value >= .01 || pos.price() == 0. && pos.quantity > .001) {
				String item = Text.portRow
						.replace( "#token#", pos.name() )
						.replace( "#quantity#", S.fmt2( pos.quantity() ) )
						.replace( "#price#", S.fmt2( pos.price() ) )
						.replace( "#value#", S.fmt2( value) )
						.replace( "#pnl#", S.fmt2( pos.unreal() ) )
						;
				posRows.append( item); 
				total += value;		
			}
		}

		// build the activity rows
		StringBuilder actRows = new StringBuilder();


		for (var trans : transactions) { 
			boolean buy = trans.getString( "action").equals( "Buy");

			String date = Util.left( trans.getString( "created_at"), 10);  // add a column for commission
			String action = buy ? "Bought" : "Sold";
			double quantity = trans.getDouble( "quantity");
			double price = trans.getDouble( "price");
			double comm = trans.getDouble( "commission");
			double amount =  quantity * price + (buy ? comm : -comm);

			String description = String.format( "%s %s %s at %s",
					action, S.fmt2( quantity), trans.getString( "symbol"), S.fmt2( price) );

			String actRow = Text.actRow
					.replace( "#date#", date)
					.replace( "#description#", description)
					.replace( "#amount#", S.fmt2( amount) );

			actRows.append( actRow);
		}

		// build the email
		if (posRows.length() > 0 || actRows.length() > 0) {
			String name = Util.combine( userRec.getString( "first_name"), userRec.getString( "last_name")).trim();  

			String totalStr = missedOne ? "" : S.fmt2( total);  // don't display total if we were missing prices as it won't be accurate
			
			String html = Text.email
					.replace( "#name#", name)
					.replace( "#wallet#", walletLc)
					.replace( "#blockchain#", m_chain.params().name() )
					.replace( "#portrows#", posRows.toString() )
					.replace( "#actrows#", actRows.toString() )
					.replace( "#total#", totalStr);

//			Auth.auth().getMail().send(
//					"Reflection", 
//					"josh@reflection.trading",  // must be a valid "from" address in gmail; display name is supported 
//					email, 
//					"Reflection Account Statement", 
//					html, 
//					true);
			
			SmtpSender.Ses.send( 
					"Reflection",
					"josh@reflection.trading", 
					email, 
					"Reflection Account Statement", 
					html,
					SmtpSender.Type.Statement); 

			return true;
		}
		
		return false;
	}

	static class PnlPair {
		double size;
		double avgPrice;
		
		void increment(JsonObject trans) {
			double prevSpent = size * avgPrice;
			
			double qty = trans.getDouble( "quantity");
			double price = trans.getDouble( "price");
			double newSpent = qty * price + trans.getDouble( "commission");
			
			size += qty;
			avgPrice = (prevSpent + newSpent) / size;
		}

		void decrement(double qty) {
			size = Math.max( size - qty, 0);  // don't go negative
		}
		
		public double getPnl(double markPrice, double balance) {
			return markPrice > 0 ? balance * (markPrice - avgPrice) : 0;
		}
		
		@Override public String toString() {
			return S.format( "[%s / %s]", size, avgPrice); 
		}
	}
	
	/** The pnl map of conid -> Pair for a single wallet */
	static class PnlMap extends HashMap<Integer,PnlPair> {
		/** Create PnlMap for a single wallet */
		PnlMap(JsonArray transactions) {
			transactions.forEach( trans -> process( trans) );
		}

		/** incorporate a single transaction into the pnl map for this wallet and conid */ 
		synchronized void process( JsonObject transaction) {
			var pair = Util.getOrCreate( this, transaction.getInt( "conid"), () -> new PnlPair() );   // access to map is synchronized
			
			if (transaction.getString( "action").equals( "Buy") ) {
				pair.increment( transaction);
			}
			else {
				pair.decrement( transaction.getDouble( "quantity") );
			}
		}
	}
}
