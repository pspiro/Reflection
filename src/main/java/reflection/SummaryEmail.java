package reflection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import tw.util.S;
import web3.NodeServer;
import web3.StockToken;

public class SummaryEmail {
	record Pos( String name, double quantity, double price) {}
	
	private Config m_config;
	private Stocks m_stocks;
	
	public static void main(String[] args) {
		try {
			Config c = Config.ask();
			var sum = new SummaryEmail( c, c.readStocks() );
			sum.generateSummaries();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	SummaryEmail( Config config, Stocks stocks) {
		m_config = config;
		m_stocks = stocks;
	}
int i = 0;

	// you'll need to use GraphQl, I think , as you will send 2000 wallets x 40 contracts
	// = 80,000 requests

	/** generate the daily summary emails */
	void generateSummaries() throws Exception {
		S.out( "Generating daily email summaries");
		
		// get wallet and names from users table
		S.out( "querying users table");
		JsonArray users = m_config.sqlQuery( "select first_name, last_name, wallet_public_key from users");

		// create map of wallet to user rec
		var map = users.getMap( "wallet_public_key");
		
		S.out( "getting RUSD balances");
		HashMap<String, Double> rusdMap = m_config.rusd().getAllBalances(); // map wallet to RUSD balance
		
		// create list of wallets from users table and RUSD holders
		HashSet<String> all = new HashSet<>();
		all.addAll( users.getArrayOf( "wallet_public_key") );
		all.addAll( rusdMap.keySet() );
		S.out( "created list of %s wallets", all.size() );

		// make sure they are all lower case
		
		for (var wallet : all) {
			try {
				generateSummary( wallet, map, Util.toDouble( rusdMap.get( wallet) ) );
			}
			catch( Exception e) {
				S.out( "Error while generating summary for wallet %s", wallet);
				e.printStackTrace();
			}
			if (++i == 100)	break;
		}
	}

	/** @param d 
	 * @param map 
	 * @param map maps wallet to record with first and last name */
	private void generateSummary( String wallet, HashMap<String, JsonObject> map, double rusdPos) throws Exception {
		S.out( "generating summary for %s", wallet);

		// get stock balances
		HashMap<String, Double> posMap = NodeServer.reqPositionsMap( 
				wallet, m_stocks.getAllContractsAddresses(), StockToken.stockTokenDecimals);
		
		if (posMap.size() == 0) {
			return;
		}
		
		// create array of positions
		ArrayList<Pos> positions = new ArrayList<>();
		for (Stock stock : m_stocks) {
			double bal = Util.toDouble( posMap.get( stock.getSmartContractId().toLowerCase() ) );
			positions.add( new Pos( stock.tokenSmbol(), bal, stock.prices().last() ) );
		}

		// add RUSD and BUSD at the bottom
		positions.add( new Pos( m_config.rusd().name(), rusdPos, 1) );
		
		positions.add( new Pos( 
				m_config.busd().name(), 
				m_config.busd().getPosition( wallet), 
				1) );

		// build the positions section
		StringBuilder posSection = new StringBuilder();
		double total = 0;
		for (Pos pos : positions) {
			double value = pos.quantity() * pos.price();
			if (value >= .05) {
				String item = itemTempl
						.replaceFirst( "#token#", pos.name() )
						.replaceFirst( "#quantity#", S.fmt2( pos.quantity() ) )
						.replaceFirst( "#price#", S.fmt2( pos.price() ) )
						.replaceFirst( "#value#", S.fmt2( value) );
				posSection.append( item); 
				total += value;		
			}
		}
		
		// build the email
		var userRec = Util.notNull( map.get( wallet), () -> new JsonObject() );
		String name = Util.combine( userRec.getString( "first_name"), userRec.getString( "last_name"));  
		
		String email = emailTempl
				.replaceFirst( "#name#", name)
				.replaceFirst( "#wallet#", wallet)
				.replaceFirst( "#portfolio#", posSection.toString() )
				.replaceFirst( "#total#", S.fmt2( total) );
		
		S.out( "here is the email");
		S.out( email);
	}
	
	static String emailTempl = "name: #name#\nwallet: #wallet#\nportfolio:\n#portfolio# \n total: #total#";
	static String itemTempl = "#token#  #quantity#  #price#  #value#\n";
}
