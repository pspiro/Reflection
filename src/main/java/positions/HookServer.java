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
	final HashMap<String,HookWallet> m_hookMap = new HashMap<>();
	
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
			server.createContext("/webhook", exch -> new Trans(exch, true).handleWebhook() );
			server.createContext("/hook/webhook", exch -> new Trans(exch, true).handleWebhook() );
//			server.createContext("/hook/get-stock-positions", exch -> new Trans(exch, true).handleGetStockPositions() );
			server.createContext("/hook/mywallet", exch -> new Trans(exch, false).handleMyWallet() );
			server.createContext("/api/mywallet", exch -> new Trans(exch, false).handleMyWallet() );
			server.createContext("/hook/get-all-positions", exch -> new Trans(exch, false).handleGetAllPositions() );
//			server.createContext("/hook/status", exch -> new Trans(exch).handlesStatus() );
			server.createContext("/hook/dump", exch -> new Trans(exch, false).handleDump() );
			
			server.createContext("/hook/ok", exch -> new BaseTransaction(exch, false).respondOk() ); 
			server.createContext("/hook/debug-on", exch -> new BaseTransaction(exch, true).handleDebug(true) ); 
			server.createContext("/hook/debug-off", exch -> new BaseTransaction(exch, true).handleDebug(false) );
			
			server.createContext("/", exch -> new Trans(exch, false).respondOk() );
		});

		//Streams.createStreamWithAddresses( Streams.erc20Transfers, list);
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
				HookWallet wal = m_hookMap.get( walletAddr);
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

		public void handleMyWallet() {
			wrap( () -> {
				String walletAddr = getWalletFromUri();
				
				Wallet wallet = new Wallet(walletAddr);
				HookWallet hookWallet = getOrCreateHookWallet( walletAddr);
				
				JsonObject rusd = new JsonObject();
				rusd.put( "name", "RUSD");
				rusd.put( "balance", hookWallet.getBalance( m_config.rusdAddr() ) );
				rusd.put( "tooltip", m_config.getTooltip(Config.Tooltip.rusdBalance) );
				rusd.put( "buttonTooltip", m_config.getTooltip(Config.Tooltip.redeemButton) );
				
				double approved = hookWallet.getAllowance(m_config.busd(), walletAddr, m_config.rusdAddr() );

				// fix a display issue where some users approved a huge size by mistake
				approved = Math.min(1000000, approved);
				
				JsonObject busd = new JsonObject();
				busd.put( "name", m_config.busd().name() );
				busd.put( "balance", hookWallet.getBalance( m_config.busd().address() ) );
				busd.put( "tooltip", m_config.getTooltip(Config.Tooltip.busdBalance) );
				busd.put( "buttonTooltip", m_config.getTooltip(Config.Tooltip.approveButton) );
				busd.put( "approvedBalance", approved);
				busd.put( "stablecoin", true);
				
				JsonObject base = new JsonObject();
				base.put( "name", m_config.nativeTok() );
				base.put( "balance", hookWallet.getNativeBalance() );
				base.put( "tooltip", m_config.getTooltip(Config.Tooltip.baseBalance) );
				
				JsonArray ar = new JsonArray();
				ar.add(rusd);
				ar.add(busd);
				ar.add(base);
				
				JsonObject obj = new JsonObject();
				obj.put( "refresh", m_config.myWalletRefresh() );
				obj.put( "tokens", ar);
				respond(obj);
			});
		}
		
		// don't forget about syncing
		
		public void handleGetAllPositions() {
			wrap( () -> {
				respond( getOrCreateHookWallet( getWalletFromUri() ).getAllJson() );
			});
		}

		private HookWallet getOrCreateHookWallet(String walletAddr) throws Exception {
			return Util.getOrCreateEx(m_hookMap, walletAddr, () -> {
				S.out( "Querying all positions for %s", walletAddr);
				HashMap<String, Double> positions = new Wallet( walletAddr)
						.reqPositionsMap(m_array);
				
				S.out( "Querying approval for %s", walletAddr);
				double approved = m_config.busd().getAllowance(walletAddr, m_config.rusdAddr() );
				
				S.out( "Querying native balance for %s", walletAddr);
				double nativeBal = MoralisServer.getNativeBalance( walletAddr);
				
				return new HookWallet( walletAddr, positions, approved, nativeBal);
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
				
				for (JsonObject trans : obj.getArray("approvals") ) {
				}
				
				for (JsonObject trans : obj.getArray("erc20Transfers") ) {
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
				};
				respondOk();
			});
		}

		private void adjust(String wallet, String contract, double amt, boolean confirmed) throws Exception {
			HookWallet hookWallet = m_hookMap.get( wallet);
			if (hookWallet != null) {
				hookWallet.adjust( contract, amt, confirmed);
			}
			// if no hookWallet found, it means we are not yet tracking the positions
			// for this wallet, and we would query all positions if a request comes in
		}
		
		void handleDump() {
			S.out( "Dumping");
			S.out( m_hookMap);
		}
	}
}
