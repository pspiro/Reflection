package fireblocks;

import json.MyJsonArray;
import json.MyJsonObject;
import reflection.Util;
import tw.util.IStream;
import tw.util.S;

public class Deploy {

	static String op = "POST"; 

	public static void main(String[] args) throws Exception {
		//Tab tab = NewSheet.getTab(NewSheet.Reflection, "Prod-symbols");

//		for (ListEntry row : tab.fetchRows() ) {
		Fireblocks.setVals();
			deploy();
//		}
	}

	private static void deploy() throws Exception {
		String bodyTemplate = 
				"{" + 
				"'operation': 'CONTRACT_CALL'," + 
				"'amount': '0'," + 
				"'assetId': '%s'," + 
				"'source': {'type': 'VAULT_ACCOUNT', 'id': '0'}," + 
				"'destination': {" + 
				"   'type': 'ONE_TIME_ADDRESS'," + 
				"   'oneTimeAddress': {'address': '0x0'}" + 
				"}," + 
				"'extraParameters': {" +
				"   'contractCallData': '%s'" +
				"}," +
				"'note': 'Deployed From Code'" + 
				"}";

		String bytecode = new IStream( "c:/temp/bytecode.t")
				.readln();
		String params = Fireblocks.encodeParameters( 
				new String[] { "string", "string" }, 
				new Object[] { "jimmy", "bean" }
				);
		
		String body = Fireblocks.toJson( 
				String.format( bodyTemplate, Fireblocks.platformBase, bytecode + params) );  
		
		Fireblocks fb = new Fireblocks();
		fb.endpoint( "/v1/transactions");
		fb.operation( "POST");
		fb.body( body);
		
		String resp = fb.transact();
		Util.require( resp.startsWith( "{"), resp);
		
		MyJsonObject obj = MyJsonObject.parse( resp);
		Util.require( obj.getString("status").equals("SUBMITTED"), resp);

		String id = obj.getString("id");
		Util.require( S.isNotNull( id), resp);
		
		String transactions = Fireblocks.get( "/v1/transactions");
		Util.require( transactions.startsWith("["), transactions);
		
		MyJsonArray ar = MyJsonArray.parse( transactions);
		MyJsonObject trans = find( ar, id);
		Util.require( trans != null, "Error: transaction with id not found: " + id);
		
		S.out( "Object returned:");
		trans.display();
		
		String hash = trans.getString("txHash");  // make a "getRequiredString() 
		
		S.out( "transaction hash is %s, use it on Moralis or Infura", hash);
		
	}

	/** Find the object in the array with "id" = id */
	private static MyJsonObject find(MyJsonArray ar, String id) {
		for (MyJsonObject obj : ar) {
			if (obj.getString("id").equals( id) ) {
				return obj;
			}
		}
		return null;
	}
}
