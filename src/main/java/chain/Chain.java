package chain;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import refblocks.Refblocks;
import reflection.Stocks;
import web3.Busd;
import web3.NodeInstance;
import web3.Rusd;

public class Chain {
	ChainParams params;
	NodeInstance node;
	Web3j web3j;
	Refblocks blocks;
	Rusd rusd;
	Busd busd;
	Stocks stocks;
	
	Chain( ChainParams chainIn) throws Exception {
		params = chainIn;
		node = params.createNode();
		web3j = Web3j.build( new HttpService( params.rpcUrl() ) );
		blocks = new Refblocks( params.chainId(), web3j, node);
		rusd = new Rusd( params.rusdAddr(), params.rusdDecimals(), this);
		busd = new Busd( params.busdAddr(), params.busdDecimals(), params.busdName(), this);
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

	/** read stocks on first use 
	 * @throws Exception */
	public Stocks stocks() throws Exception {
		if (stocks == null) {
			stocks = new Stocks();
			stocks.readFromSheet( params.symbolsTab(), this);
		}
		return stocks;
	}
	
	/** Force it to re-read the stocks from the spreadsheet */
	public void clearStocks() {
		stocks = null;
	}

}
