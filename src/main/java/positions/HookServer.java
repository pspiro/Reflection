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

// bug: when we received 2, we updated to 2 instead of add
// issue: how to tell if the updates are old or new; you don't
// want to play back old updates

public class HookServer {
	final static double small = .0001;    // positions less than this will not be reported
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
//		list.addAll( Arrays.asList( stocks.getAllContractsAddresses() ) );
		list.add( m_config.busd().address() );
		list.add( m_config.rusd().address() );
		m_array = list.toArray( new String[list.size()]);

		MyServer.listen( m_config.hookServerPort(), 10, server -> {
			server.createContext("/webhook", exch -> new Trans(exch, true).handleWebhook() );
			server.createContext("/hook/webhook", exch -> new Trans(exch, true).handleWebhook() );
//			server.createContext("/hook/get-stock-positions", exch -> new Trans(exch, true).handleGetStockPositions() );
//			server.createContext("/hook/get-mywallet", exch -> new Trans(exch).handleGetMyWallet() );
			server.createContext("/hook/get-all-positions", exch -> new Trans(exch, false).handleGetAllPositions() );
//			server.createContext("/hook/status", exch -> new Trans(exch).handlesStatus() );
			server.createContext("/hook/dump", exch -> new Trans(exch, false).handleDump() );
			
			server.createContext("/hook/ok", exch -> new BaseTransaction(exch, false).respondOk() ); 
			server.createContext("/hook/debug-on", exch -> new BaseTransaction(exch, true).handleDebug(true) ); 
			server.createContext("/hook/debug-off", exch -> new BaseTransaction(exch, true).handleDebug(false) );
			
			server.createContext("/", exch -> new Trans(exch, false).respondOk() );
		});

		Streams.createStreamWithAddresses( Streams.erc20Transfers, list);
	}

	class Trans extends BaseTransaction {
		public Trans(HttpExchange exchange, boolean debug) {
			super(exchange, debug);
		}

		/** Returns wallet lower case */
		public String getWalletFromUri() throws Exception {
			String wallet = Util.getLastToken(m_uri, "/");
			require( Util.isValidAddress(wallet), RefCode.INVALID_REQUEST, "Wallet address is invalid");
			return wallet.toLowerCase();
		}
		
		public void handleGetStockPositions() {
			wrap( () -> {
				String walletAddr = getWalletFromUri();
				HookWallet wal = hookMap.get( walletAddr);
				if (wal != null) {
					JsonArray ar = new JsonArray();
					
//					wal.m_map.forEach( (address,position) -> ar.add( Util.toJson(
//							"address", address,
//							"position", position) ) );
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
				
				HookWallet hookWallet;
				
				synchronized( hookMap) {
					hookWallet = hookMap.get( walletAddr);
					if (hookWallet == null) {
						S.out( "Querying all positions for %s", walletAddr);
	
						HashMap<String, Double> positions = new Wallet( walletAddr)
								.reqPositionsMap(m_array);
						
						hookWallet = new HookWallet( walletAddr, positions);
						hookMap.put( walletAddr, hookWallet);
					}
				}
				
				respond( hookWallet.getAllJson() );
			});
		}

		public void handlesStatus() {
			wrap( () -> {
			});
		}

		public void handleWebhook() {
			wrap( () -> {
				JsonObject obj = parseToObject();
				S.out( "Received " + obj);
				
				obj.getArray("erc20Transfers").forEach( trans -> {
					String contract = trans.getString("contract").toLowerCase();
					double amt = trans.getDouble("valueWithDecimals");
					String from = trans.getString("from").toLowerCase(); 
					String to = trans.getString("to").toLowerCase();
					

					S.out( "Received hook: %s transferred %s from %s to %s (%s)", 
							contract, 
							amt,
							from,
							to,
							obj.getBool("confirmed"));

					// "confirmed" is our signal that something changed recently,
					// it is completely done and we should re-query;
					// one loophole is if two transactions are entered before the
					// first one is confirmed
					boolean confirmed = obj.getBool("confirmed");
					adjust( from, contract, -amt, confirmed);
					adjust( to, contract, amt, confirmed);
				});
				respondOk();
			});
		}
		
		void handleDump() {
			S.out( "Dumping");
			S.out( hookMap);
		}

		private void adjust(String wallet, String contract, double amt, boolean confirmed) {
			HookWallet hookWallet = hookMap.get( wallet);
			if (hookWallet != null) {
				hookWallet.adjust( contract, amt, confirmed);
			}
		}
	}
}
