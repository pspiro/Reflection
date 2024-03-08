package positions;

import static reflection.Main.require;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import common.Util;
import fireblocks.MyServer;
import http.BaseTransaction;
import http.MyClient;
import reflection.Config;
import reflection.RefCode;
import reflection.Stocks;
import test.MyTimer;
import tw.util.S;

// bug: when we received 2, we updated to 2 instead of add
// issue: how to tell if the updates are old or new; you don't
// want to play back old updates

/** This is the WebHook server that receives updates from Moralis.
 *  There are three types of updates: token transfers, native token transfers, and approvals
 *
 */
public class Hook2 {
	static double ten18 = Math.pow(10, 18);
	final static double small = .0001;    // positions less than this will not be reported
	final Config m_config = new Config();
	final Stocks stocks = new Stocks();
	String[] m_allContracts;  // list of contract for which we want to request and monitor position; all stocks plus BUSD and RUSD
	String m_transferStreamId;
	static final long m_started = System.currentTimeMillis(); // timestamp that app was started

	/** Map wallet, lower case to HookWallet */ 
	final Map<String,HookWallet> m_hookMap = new ConcurrentHashMap<>();
	String chain() { return m_config.hookServerChain(); }

	public static void main(String[] args) {
		try {
			Thread.currentThread().setName("Hook");
			S.out( "Starting HookServer");

			if (args.length == 0) {
				throw new Exception( "You must specify a config tab name");
			}

			new Hook2().run(args[0]);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(2);  // we need this because listening on the port will keep the app alive
		}
	}

	void run(String tabName) throws Exception {
		MyClient.filename = "hookserver.http.log";
		m_config.readFromSpreadsheet(tabName);
		stocks.readFromSheet( m_config);
		BaseTransaction.setDebug( true);  // just temporary

		// build list of all contracts that we want to listen for ERC20 transfers
		ArrayList<String> list = new ArrayList<>();  // keep a list as array for speed
		list.addAll( Arrays.asList( stocks.getAllContractsAddresses() ) );
		list.add( m_config.busd().address() );
		list.add( m_config.rusd().address() );
		m_allContracts = list.toArray( new String[list.size()]);

		MyServer.listen( m_config.hookServerPort(), 10, server -> {
			server.createContext("/", exch -> new Trans2(exch, false).handle() ); 
		});

	}

	class Trans2 extends BaseTransaction {
		public Trans2(HttpExchange exchange, boolean debug) {
			super(exchange, debug);
		}
		
		void handle() {
			wrap( () -> { 
				JsonObject obj = MyClient.getJson( "https://live.reflection.trading" + m_exchange.getRequestURI() );
				respond( obj);
			});
		}
	}
}
