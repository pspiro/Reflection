package fireblocks;

import json.MyJsonArray;
import json.MyJsonObject;
import reflection.Config;
import tw.util.S;

public class Transactions {
	static void displayLastTransactions(int n) throws Exception {
		MyJsonArray ar = getTransactions();
		
		for (int i = 0; i < n; i++) { 
			ar.getJsonObj(i).display();
		}
	}

	public static MyJsonArray getTransactions() throws Exception {
		return Fireblocks.fetchArray( "/v1/transactions/?");
	}

	public static MyJsonArray getSince(long start) throws Exception {
		return Fireblocks.fetchArray( "/v1/transactions/?after=" + start);
	}

	public static MyJsonObject getTransaction(String id) throws Exception {
		return Fireblocks.fetchObject( "/v1/transactions/" + id);
	}

	public static void main(String[] args) throws Exception {
		Config config = new Config();
		config.readFromSpreadsheet("Dt-config");

		long createdAt  = 1685635887444L;
		//long lastUpdated = 1685635938014L;
		//getSince(createdAt).display();
		getTransaction("lkjsdflkj").display();
	}

	public static void getTransactions(String wallet) throws Exception {
		S.out( "Looking for " + wallet);
		
		long since = System.currentTimeMillis() - 120 * 60000;
		
		String admin ; //= Accounts.instance.getAdmin( wallet);
		//int id = Accounts.instance.getId( admin);		
		String url = String.format("/v1/transactions?sourceId=%s&limit=%s&after=%s",
				5, 9, since);
		
		for (MyJsonObject obj : Fireblocks.fetchArray( url) ) {
			obj.getObj("source").display();
			S.out( obj.getString("destinationAddress") );
			S.out( obj.getString("note"));
			S.out( obj.getString("status"));
			S.out( obj.getString("subStatus"));
			
			
		}

//		String url = String.format( "/v1/transactions");
//		
//		Fireblocks fb = new Fireblocks();
//		fb.endpoint( url);
//		fb.operation( "POST");
//		fb.body( TestErrors.toJson( "{sourceId='4'}" ) );
//		String ret = fb.transact();
//		MyJsonArray.parse( ret).display();
	}
	
	
	/*
before	[optional] Unix timestamp in milliseconds. Returns only transactions created before the specified date.
after	[optional] Unix timestamp in milliseconds. Returns only transactions created after the specified date.
status	[optional] You can filter by one of the statuses.
orderBy	[optional] The field to order the results by. Available values : createdAt (default), lastUpdated.
sourceType	[optional] The source type of the transaction. Available values: VAULT_ACCOUNT, EXCHANGE_ACCOUNT, INTERNAL_WALLET, EXTERNAL_WALLET, FIAT_ACCOUNT, NETWORK_CONNECTION, COMPOUND, UNKNOWN, GAS_STATION, OEC_PARTNER.
sourceId	[optional] The source ID of the transaction.
destType	[optional] The destination type of the transaction. Available values: VAULT_ACCOUNT, EXCHANGE_ACCOUNT, INTERNAL_WALLET, EXTERNAL_WALLET, ONE_TIME_ADDRESS, FIAT_ACCOUNT, NETWORK_CONNECTION, COMPOUND.
destId	[optional] The destination ID of the transaction.
assets	[optional] A list of assets to filter by, separated by commas.
txHash	[optional] Returns only results with a specified txHash.
limit	[optional] Limits the number of returned transactions. If not provided, a default of 200 will be returned. The maximum allowed limit is 500.
sort
	 * 
	 */
}
