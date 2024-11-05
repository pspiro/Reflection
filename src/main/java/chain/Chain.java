package chain;

import java.util.Collection;
import java.util.HashMap;

import org.json.simple.JsonArray;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import common.Util;
import refblocks.Refblocks;
import tw.google.NewSheet;
import tw.util.S;
import web3.Busd;
import web3.NodeInstance;
import web3.Rusd;
import web3.StockToken;
import web3.StockToken.StockTokenRec;

public class Chain {
	private ChainParams params;
	private NodeInstance node;
	private Web3j web3j;
	private Refblocks blocks;
	private Rusd rusd;
	private Busd busd;
	private final HashMap<Integer,StockToken> mapConid = new HashMap<>();
	private final HashMap<String,StockToken> mapAddress = new HashMap<>();
	private String[] m_allAddresses;
	
	Chain( ChainParams chainIn) throws Exception {
		params = chainIn;
		node = params.createNode();
		web3j = Web3j.build( new HttpService( params.rpcUrl() ) );
		blocks = new Refblocks( params.chainId(), web3j, node);
		rusd = new Rusd( params.rusdAddr(), params.rusdDecimals(), this);
		busd = new Busd( params.busdAddr(), params.busdDecimals(), params.busdName(), this);
	}
	
	public void dump() throws Exception {
		S.out( "------ chain params");
		S.out( params);
	}
	
	public ChainParams params() {
		return params;
	}

	public NodeInstance node() {
		return node;
	}

	public Rusd rusd() {
		return rusd;
	}

	public Busd busd() {
		return busd;
	}
	
	public Web3j web3j() {
		return web3j;
	}
	
	public Refblocks blocks() {
		return blocks;
	}

	public Collection<StockToken> tokens() {
		return mapConid.values();
	}
	
	public double getApprovedAmt() throws Exception {
		return busd().getAllowance( params().refWalletAddr(), params().rusdAddr() );
	}

	public String blockchainTx(String hash) {
		return String.format( "%s/tx/%s", params().blockchainExpl(), hash);
	}

	public StockToken getAnyStockToken() {
		return new StockToken( tokens().iterator().next().address(), this);
	}

	public String[] getAllContractsAddresses() {
		if (m_allAddresses == null) {
			m_allAddresses = new String[mapConid.size()];
			
			int i = 0;
			for (StockToken token: mapConid.values() ) {
				m_allAddresses[i++] = token.address();
			}
		}
		return m_allAddresses;
	}
	
	/** for the get-all-stocks query which is for dropdown on trading page */ 
	public JsonArray getAllStocks(Stocks stocks) throws Exception {
		JsonArray ar = new JsonArray();
		
		for (var token : mapConid.values() ) {
			var stock = stocks.getStockByConid( token.conid() );
			
			// stock could be null if it is not 'Active' on the Master-symbols page
			if (stock != null) {
				ar.add( Util.toJson(
						"smartcontractid", token.address(), 
						"symbol", stock.symbol(),
						"description", stock.rec().description(),
						"conid", "" + token.conid(),
						"exchangeStatus", "unknown", // currently ignore, we could bring it back. 
						"tradingView", stock.rec().tradingView(),
						"tokenSymbol", token.name()
						) );
			}
		}
		
		return ar;
	}

	/** read the symbols and create the stock tokens for this chain */ 
	public void readSymbols() throws Exception {
		var symbolsTab = NewSheet.getTab(NewSheet.Reflection, params.symbolsTab() );
		S.out( "reading chain %s from tab %s", params().name(), params.symbolsTab() );

		for (var row : symbolsTab.queryToJson() ) {
			String address = row.getString( "TokenAddress");
			
			if (row.getInt( "Conid") > 0 && S.isNotNull( address) ) {
				StockTokenRec rec = new StockTokenRec(
						row.getInt( "Conid"),
						row.getString( "StartDate"),
						row.getString( "EndDate"),
						row.getDouble( "ConvertsToAmt"),
						row.getString( "ConvertsToAddress"),
						Util.getEnum( row.getString( "Allow"), Allow.values(), Allow.All) 
						);
				
				// create the stock token
				Util.require( Util.isValidAddress(address), "Invalid address '%s' for conid %s on tab %s", 
						address, row.getInt( "Conid"), params.symbolsTab() );
				
				StockToken token = new StockToken( address, row.getString( "TokenSymbol"), this, rec);
				
				// add it to the list and maps
				mapConid.put( row.getInt( "Conid"), token);
				mapAddress.put( address, token);
			}
		}
	}

	/** This is called frequently so a map is good */
	public StockToken getTokenByAddress(String address) {
		return mapAddress.get( address);
	}

	/** This is called frequently so a map is good */
	public StockToken getTokenByConid(int conid) {
		return mapConid.get( conid);
	}
}
// tokensupply on monitor, do a batch query. bc
// symbol not displaying on Monitor/Token tab