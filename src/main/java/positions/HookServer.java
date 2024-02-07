package positions;

import static reflection.Main.require;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import common.Util;
import fireblocks.MyServer;
import http.BaseTransaction;
import reflection.Config;
import reflection.RefCode;
import reflection.Stocks;
import tw.util.S;

public class HookServer {
	final Config m_config = new Config();
	final Stocks stocks = new Stocks();
	String[] m_array;

	/** Map wallet, lower case to HookWal */ 
	final HashMap<String,HookWallet> hookMap = new HashMap<>();
	
	public static void main(String[] args) {
		try {
			Thread.currentThread().setName("Hook");
			S.out( "Starting HookServer");
			
			if (args.length == 0) {
				throw new Exception( "You must specify a config tab name");
			}

			new HookServer().run(args[0]);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(2);  // we need this because listening on the port will keep the app alive
		}
	}
	
	void run(String tabName) throws Exception {
		m_config.readFromSpreadsheet(tabName);
		stocks.readFromSheet( m_config);
		
		Streams.deleteAll();  // not sure I need this
		
		ArrayList<String> list = new ArrayList<>();  // keep a list as array for speed
		list.addAll( Arrays.asList( stocks.getAllContractsAddresses() ) );
		list.add( m_config.busd().address() );
		list.add( m_config.rusd().address() );
		m_array = list.toArray( new String[list.size()]);

		MyServer.listen( m_config.hookServerPort(), 10, server -> {
			server.createContext("/hook/webhook", exch -> new Trans(exch).handleWebhook() );
			server.createContext("/hook/get-stock-positions", exch -> new Trans(exch).handleGetStockPositions() );
			server.createContext("/hook/get-mywallet", exch -> new Trans(exch).handleGetMyWallet() );
			server.createContext("/hook/get-all-positions", exch -> new Trans(exch).handleGetAllPositions() );
			server.createContext("/hook/status", exch -> new Trans(exch).handlesStatus() );
			
			server.createContext("/hook/ok", exch -> new BaseTransaction(exch, false).respondOk() ); 
			server.createContext("/hook/debug-on", exch -> new BaseTransaction(exch, true).handleDebug(true) ); 
			server.createContext("/hook/debug-off", exch -> new BaseTransaction(exch, true).handleDebug(false) );
			
			server.createContext("/", exch -> new Trans(exch).respondOk() );
		});

		//Streams.createStream( list);
	}

	static class HookWallet {
		HashMap<String,Double> m_map = new HashMap<>();
		double nativeTok;
		
		HookWallet( HashMap<String,Double> map) {
			m_map = map;
		}
	}
	
	class Trans extends BaseTransaction {
		public Trans(HttpExchange exchange) {
			super(exchange, true);
		}

		/** Returns wallet lower case */
		public String getWalletFromUri() throws Exception {
			String wallet = Util.getLastToken(m_uri, "/");
			require( Util.isValidAddress(wallet), RefCode.INVALID_REQUEST, "Wallet address is invalid");
			return wallet.toLowerCase();
		}
		
		/*
			Wallet wallet = new Wallet(m_walletAddr);
			
			Util.forEach( wallet.reqPositionsMap(m_main.stocks().getAllContractsAddresses() ).entrySet(), entry -> {
				JsonObject stock = m_main.getStockByTokAddr( entry.getKey() );

				if (stock != null && entry.getValue() >= m_config.minTokenPosition() ) {
					JsonObject resp = new JsonObject();
					resp.put("conId", stock.get("conid") );
					resp.put("symbol", stock.get("symbol") );
					resp.put("price", getPrice(stock) );
					resp.put("quantity", entry.getValue() ); 
					retVal.add(resp);   // alternatively, you could just add the whole stock to the array, but you would need to adjust the column names in the Monitor
				}
			});
			
			retVal.sortJson( "symbol", true);
			respond(retVal);

		 */
		public void handleGetStockPositions() {
			wrap( () -> {
				String walletAddr = getWalletFromUri();
				HookWallet wal = hookMap.get( walletAddr);
				if (wal != null) {
					JsonArray ar = new JsonArray();
					wal.m_map.forEach( (address,position) -> ar.add( Util.toJson(
							"address", address,
							"position", position) ) );
					respond(ar);
				}
				else {
					Wallet wallet = new Wallet(walletAddr);
					HashMap<String, Double> map = wallet.reqPositionsMap(); //m_array);
				}
			});
		}

		public void handleGetMyWallet() {
			wrap( () -> {
				String walletAddr = getWalletFromUri();
			});
		}
		
		// don't forget about syncing
		
		public void handleGetAllPositions() {
			wrap( () -> {
				String walletAddr = getWalletFromUri();
				
				HookWallet hookWallet = hookMap.get( walletAddr);
				if (hookWallet == null) {
					S.out( "requesting positions for %s", walletAddr);
					Wallet wallet = new Wallet( walletAddr);
					HashMap<String, Double> map = wallet.reqPositionsMap(m_array);
					
					hookWallet = new HookWallet( map);
					hookMap.put( walletAddr, hookWallet);
				}
				
				JsonArray ar = new JsonArray();
				hookWallet.m_map.forEach( (address,position) -> ar.add( Util.toJson( 
						"address", address,
						"position", position) ) );

				JsonObject obj = new JsonObject();
				obj.put( "native", hookWallet.nativeTok);
				obj.put( "positions", ar);
				respond( obj);
			});
		}

		public void handlesStatus() {
			wrap( () -> {
			});
		}

		public void handleWebhook() {
			wrap( () -> {
				JsonObject obj = parseToObject();
				obj.getArray("erc20Transfers").forEach( trans -> {
					S.out( "Transferred %s %s", trans.getString("contract"), obj.getBool("confirmed") );
					S.out( "From: %s to %s", trans.getString("from"), trans.getString("to") );
					S.out();
				});
				respondOk();
			});
		}
	}

}
