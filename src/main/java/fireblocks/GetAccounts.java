package fireblocks;

import json.MyJsonArray;
import json.MyJsonObject;
import tw.util.S;

/** Works for prod and test. */
public class GetAccounts {

	public static void main(String[] args) throws Exception {
		Fireblocks.setVals();

		//Fireblocks.get( "/v1/vault/accounts_paged");
		Fireblocks.getVaultAccounts().display();
		
		// Fireblocks.getTransactions().display();
		
		//Fireblocks.getTransaction( "a769ace6-6c35-492d-96e4-8f5588c1ee87").display(); 
//		displayLastTransaction();

	}

	static void displayLastTransaction() throws Exception {
		MyJsonArray ar = Fireblocks.getTransactions();
		ar.getJsonObj(0).display();
	}
}


/*
{
"accounts":[
	{"id":"5","name":"Goerli","hiddenOnUI":false,"autoFuel":false,"assets":[]},
	{"id":"4","name":"Reflection Owner","hiddenOnUI":false,"autoFuel":false,"assets":[{"id":"BNB_BSC","total":"0.094165606","balance":"0.094165606","lockedAmount":"0","available":"0.094165606","pending":"0","frozen":"0","staked":"0","blockHeight":"23247848"}]},
	{"id":"3","name":"Ref Wallet","hiddenOnUI":false,"autoFuel":false,"assets":[]},
	{"id":"2","name":"Peter Spiro","hiddenOnUI":false,"autoFuel":false,"assets":[{"id":"ETH","total":"3.308474040701385829","balance":"3.308474040701385829","lockedAmount":"0","available":"3.308474040701385829","pending":"0","frozen":"0","staked":"0","blockHeight":"16014752"},{"id":"AVAX","total":"109.233459018621976894","balance":"109.233459018621976894","lockedAmount":"0","available":"109.233459018621976894","pending":"0","frozen":"0","staked":"0","blockHeight":"21903561"},{"id":"BNB_BSC","total":"0","balance":"0","lockedAmount":"0","available":"0","pending":"0","frozen":"0","staked":"0","blockHeight":"22851766"},{"id":"USDT_BSC","total":"19040.36557501","balance":"19040.36557501","lockedAmount":"0","available":"19040.36557501","pending":"0","frozen":"0","staked":"0","blockHeight":"22746838"},{"id":"LUNA2","total":"551.669873","balance":"551.669873","lockedAmount":"0","available":"551.669873","pending":"0","frozen":"0","staked":"0","blockHeight":"2192453"},{"id":"LUNA","total":"98.907382","balance":"98.907382","lockedAmount":"0","available":"98.907382","pending":"0","frozen":"0","staked":"0","blockHeight":"10000435"},{"id":"TERRA_USD","total":"27665.657509","balance":"27665.657509","lockedAmount":"0","available":"27665.657509","pending":"0","frozen":"0","staked":"0","blockHeight":"10000424"},{"id":"WMEMO_AVAX","total":"0.031127325394880677","balance":"0.031127325394880677","lockedAmount":"0","available":"0.031127325394880677","pending":"0","frozen":"0","staked":"0","blockHeight":"21632363"},{"id":"USDT_ERC20","total":"47152.554302","balance":"47152.554302","lockedAmount":"0","available":"47152.554302","pending":"0","frozen":"0","staked":"0","blockHeight":"15940689"},{"id":"SOL","total":"122.77560717","balance":"122.77560717","lockedAmount":"0","available":"122.77560717","pending":"0","frozen":"0","staked":"0","blockHeight":"160324773","blockHash":"FbyinQxnTp3v7HYWMkc998oqcFENPxp9jNSyB1QuYqUT"},{"id":"BTC","total":"0.2316455","balance":"0.2316455","lockedAmount":"0","available":"0.2316455","pending":"0","frozen":"0","staked":"0","blockHeight":"760605"},{"id":"FTM_FANTOM","total":"7459.517972671257912","balance":"7459.517972671257912","lockedAmount":"0","available":"7459.517972671257912","pending":"0","frozen":"0","staked":"0","blockHeight":"50736259"},{"id":"USDC","total":"0","balance":"0","lockedAmount":"0","available":"0","pending":"0","frozen":"0","staked":"0","blockHeight":"15870107"},{"id":"LINK","total":"0","balance":"0","lockedAmount":"0","available":"0","pending":"0","frozen":"0","staked":"0","blockHeight":"15970843"}]},
	{"id":"1","name":"Network Deposits","hiddenOnUI":false,"autoFuel":false,"assets":[{"id":"BTC","total":"0","balance":"0","lockedAmount":"0","available":"0","pending":"0","frozen":"0","staked":"0","blockHeight":"-1"},{"id":"ETH","total":"0","balance":"0","lockedAmount":"0","available":"0","pending":"0","frozen":"0","staked":"0","blockHeight":"-1"}]},{"id":"0","name":"Default","hiddenOnUI":false,"autoFuel":false,"assets":[{"id":"BTC","total":"0","balance":"0","lockedAmount":"0","available":"0","pending":"0","frozen":"0","staked":"0","blockHeight":"-1"},{"id":"ETH","total":"0","balance":"0","lockedAmount":"0","available":"0","pending":"0","frozen":"0","staked":"0","blockHeight":"-1"},{"id":"USDC","total":"0","balance":"0","lockedAmount":"0","available":"0","pending":"0","frozen":"0","staked":"0","blockHeight":"15727505"},{"id":"BNB_BSC","total":"0","balance":"0","lockedAmount":"0","available":"0","pending":"0","frozen":"0","staked":"0","blockHeight":"-1"},{"id":"AVAX","total":"0.000525","balance":"0.000525","lockedAmount":"0","available":"0.000525","pending":"0","frozen":"0","staked":"0","blockHeight":"21903561"}]}
	],
	"paging":{}
}

[
	{
	"id":"f89b1116-943c-5c25-db3f-f2a9c1a55426",
	"name":"Binance - Peter Spiro",
	"type":"BINANCEUS",
	"assets":[ 
		{ "id":"BUSD", "balance":"57.78922818", "total":"57.78922818", "available":"57.78922818", "lockedAmount":"0.00000000" }, 
		{ "id":"USD", "balance":"29.9370", "total":"29.9370", "available":"29.9370", "lockedAmount":"0.00000000" }
	],
	"isSubaccount":false,
	"status":"APPROVED",
	"tradingAccounts":[],
	"fundableAccountType":"SPOT" 
	}
]
 */