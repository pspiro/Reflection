package chain;

import java.util.ArrayList;
import java.util.HashMap;

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
	private ArrayList<StockToken> tokens; // needed? bc
	private final HashMap<Integer,StockToken> mapConid = new HashMap<>();
	private final HashMap<String,StockToken> mapAddress = new HashMap<>();
	
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
		S.out( "------ chain stocks");
		S.out( tokens);
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

	public ArrayList<StockToken> tokens() {  //rename. bc
		return tokens;
	}
	
	public double getApprovedAmt() throws Exception {
		return busd().getAllowance( params().refWalletAddr(), params().rusdAddr() );
	}

	public String blockchainTx(String hash) {
		return String.format( "%s/tx/%s", params().blockchainExpl(), hash);
	}

	public StockToken getAnyStockToken() {
		return new StockToken( tokens.get( 0).address(), this);
	}

	public String[] getAllContractsAddresses() {
		throw new RuntimeException("implement");
	}

	/** read the symbols and create the stock tokens for this chain */ 
	public void readSymbols() throws Exception {
		var symbolsTab = NewSheet.getTab(NewSheet.Reflection, params.symbolsTab() );

		for (var row : symbolsTab.queryToJson() ) {
			StockTokenRec rec = new StockTokenRec(
					row.getInt( "Conid"),
					row.getString( "StartDate"),
					row.getString( "EndDate"),
					row.getDouble( "ConvertsToAmt"),
					row.getString( "ConvertsToAddress"),
					Util.getEnum( row.getString( "Allow"), Allow.values(), Allow.All) 
					);
			
			// create the stock token
			String address = row.getString( "Address");
			StockToken token = new StockToken( address, row.getString( "TokenSymbol"), this, rec);
			
			// add it to the list and maps
			tokens.add( token);
			mapConid.put( row.getInt( "Conid"), token);
			mapAddress.put( address, token);
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
