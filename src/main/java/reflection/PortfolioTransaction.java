package reflection;

import static reflection.Main.m_config;

import java.util.HashMap;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import common.Util;
import http.MyClient;
import reflection.Statements.PnlMap;

public class PortfolioTransaction extends MyTransaction {
	private static HashMap<String,PnlMap> pnls = new HashMap<>();

	public PortfolioTransaction(Main main, HttpExchange exch, boolean debug) {
		super(main, exch, debug);
	}
	
	/** You'll see exceptions here when the HookServer is restarting */
	public void handleReqPositionsNew() {
		wrap( () -> {
			// read wallet address into m_walletAddr (last token in URI)
			parseMsg();
			getWalletFromUri();
			setChainFromHttp();

			String url = String.format( "%s/get-wallet/%s",
					chain().params().localHook(),
					m_walletAddr.toLowerCase() );

			// get or create the PnlMap for this wallet
			var pnlMap = Util.getOrCreateEx( pnls, m_walletAddr.toLowerCase(), () ->  // access to map is synchronized
				new PnlMap( m_config.getCompletedTransactions( m_walletAddr) ) );
			
			JsonArray positions = new JsonArray();
			
			for (JsonObject pos : MyClient.getJson( url).getArray( "positions") ) {   // returns the following keys: native, approved, positions, wallet
				double position = pos.getDouble("position");
				
				var token = chain().getTokenByAddress( pos.getString("address") );
				
				if (token != null && position >= m_config.minTokenPosition() ) {
					var stock = m_main.stocks().getStockByConid( token.conid() );

					if (stock != null) {
						var pair = pnlMap.get( token.rec().conid() );
						double pnl = pair != null ? pair.getPnl( stock.markPrice(), position) : 0;
						
						JsonObject resp = new JsonObject();
						resp.put("conId", stock.conid() );
						resp.put("symbol", stock.symbol() );
						resp.put("price", stock.markPrice() );
						resp.put("quantity", position); 
						resp.put("pnl", pnl);
						positions.add(resp);
					}
				}
			}
			
			positions.sortJson( "symbol", true);
			respond(positions);
		});
	}


	/** Incorporate this trade into the map IFF there is already an entry for this wallet
	 *  @param transaction is the json representation of an entry in the transactions table */ 
	public static void processTrans(String wallet, JsonObject transaction) throws Exception {
		// lookup(), not getOrCreate() 
		Util.lookup( pnls.get( wallet.toLowerCase() ), pnlMap -> pnlMap.process( transaction) );
	}
	
}
