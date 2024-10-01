package reflection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import tw.util.S;
import web3.MoralisServer;
import web3.NodeInstance;

// you could create an easier way for this, where you pass Json and it finds the replacement tags based on json tags
// pass a json array for multiple
// and don't require splitting the text into multiple strings, or use
// color the text based on buy or sell, or add a round icon
// you'll need to use GraphQl, I think , as you will send 2000 wallets x 40 contracts
// = 80,000 requests

// add users who are in jotform with status=Funded and wallet and email

// ******************

public class SummaryEmail {
	private record Position( String name, double quantity, double price) {}

	private Config m_config;
	private Stocks m_stocks;
	private List<String> m_list;
	private boolean m_testing;

	int count = 1;
	int max = 30;

	public static void main(String[] args) {
		try {
			Config c = Config.ask();
			new SummaryEmail( c, c.readStocks(), true)  // don't set to false; there will be no prices
			.generateSummaries();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	SummaryEmail( Config config, Stocks stocks, boolean testing) {
		m_config = config;
		m_stocks = stocks;
		m_testing = testing;

		if (m_testing) {
			for ( Stock stock : m_stocks) {  //remove. pas
				stock.setPrices( new Prices( Util.toJson(
						"last", 83.82) ) );
			}
		}

		// get list of all stock contract addresses plus RUSD and BUSD
		m_list = Util.toList( m_stocks.getAllContractsAddresses() );
		m_list.add( m_config.rusdAddr().toLowerCase() );
		m_list.add( m_config.busdAddr().toLowerCase() );
	}

	/** generate the daily summary emails */
	void generateSummaries() throws Exception {
		S.out( "Generating daily email summaries");

		// get wallet and names from users table
		S.out( "querying users table");
		JsonArray users = m_config.sqlQuery( "select first_name, last_name, email, wallet_public_key from users");

		// wallets must be all lower case

		if (m_testing ){
			String wallet = NodeInstance.prod.toLowerCase();
			var userRec = users.find( "wallet_public_key", wallet);
			Util.require( userRec != null, "Error");
			generateSummary( wallet, userRec);
			System.exit( 0);
		}

		for (var userRec : users) {
			String wallet = userRec.getString( "wallet_public_key");
			try {
				if (userRec.has( "email")) {
					generateSummary( wallet, userRec);
					if (++count == max)	break;
				}
			}
			catch( Exception e) {
				S.out( "Error while generating summary for wallet %s", wallet);
				e.printStackTrace();
			}
		}
	}


	/** @param wallet lower case
	 * @param map maps wallet to record with first and last name, if we have it  */
	private void generateSummary( String wallet, JsonObject userRec) throws Exception {
		S.out( "generating summary for %s", wallet);

		HashMap<String, Double> posMap = MoralisServer.reqPositionsMap(       // you could also try graphql or 
				wallet, m_list.toArray( new String[0]) );

		// build array of Positions
		ArrayList<Position> positions = new ArrayList<>();

		// add stocks
		for ( Stock stock : m_stocks) {
			Util.iff( posMap.get( stock.getSmartContractId().toLowerCase() ), balance -> {
				if (balance > 0) {
					if (stock.prices().validLast() ) {
						positions.add( new Position( stock.symbol(), balance, stock.prices().last() ) );
					}
					else {
						S.out( "Error: cannot add %s to statement, no valid last", stock.symbol() );
					}
				}
			});
		}
		
		// sort positions by symbol
		positions.sort( (p1, p2) -> p1.name().compareTo( p2.name() ) );

		// add RUSD and BUSD at the bottom
		Util.iff( posMap.get( m_config.rusdAddr() ), pos -> 
			positions.add( new Position( m_config.rusd().name() + " (stablecoin)", (double)pos, 1) ) );
		Util.iff( posMap.get( m_config.busdAddr() ), pos -> 
			positions.add( new Position( m_config.busd().name() + " (stablecoin)", (double)pos, 1) ) );

		// WARNING: if price is zero, item will not display
		
		// build the positions section from list of Pos
		StringBuilder posRows = new StringBuilder();
		double total = 0;
		for (Position pos : positions) {
			double value = pos.quantity() * pos.price();
			if (value >= .01) {
				String item = Text.portRow
						.replace( "#token#", pos.name() )
						.replace( "#quantity#", S.fmt2( pos.quantity() ) )
						.replace( "#price#", S.fmt2( pos.price() ) )
						.replace( "#value#", S.fmt2( value) );
				posRows.append( item); 
				total += value;		
			}
		}

		// build the activity rows
		StringBuilder actRows = new StringBuilder();

		for (var trans : m_config.sqlQuery( "select * from transactions where wallet_public_key = '%s' and status = 'COMPLETED' order by created_at", wallet) ) {
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

			String html = Text.email
					.replace( "#name#", name)
					.replace( "#wallet#", wallet)
					.replace( "#portrows#", posRows.toString() )
					.replace( "#actrows#", actRows.toString() )
					.replace( "#total#", S.fmt2( total) );

			String to = S.isNotNull( name)
					? String.format( "%s <%s>", name, userRec.getString( "email") )
							: userRec.getString( "email");


			to = userRec.getString( "email");  // display name not supported when sending w/ gmail

//			Auth.auth().getMail().send(
//					"Reflection", 
//					"josh@reflection.trading",  // must be a valid "from" address in gmail; display name is supported 
//					to, 
//					"Reflection Account Statement", 
//					html, 
//					true);


			S.out( "sent email for " + wallet + "\n" + html);
		}
	}
}
