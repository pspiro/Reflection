package fireblocks;

import tw.util.IStream;
import tw.util.S;

public class Call {
	public static void main(String[] args) throws Exception {
		Fireblocks.setVals();
		String getfirstKeccak = "65ad0967";
		call( "0x14d7e955b156d9f294daea57b7b119db54d71124", getfirstKeccak);
	}
//id":"59cd81db-f776-4d34-8dac-3243ffa48829","status":"SUBMITTED"}

	// add calling w/ parameters
	// add picking up the return value
	private static void call(String addr, String keccak) throws Exception {
		String bodyTemplate = 
				"{" + 
						"'operation': 'CONTRACT_CALL'," + 
						"'amount': '0'," + 
						"'assetId': '%s'," + 
						"'source': {'type': 'VAULT_ACCOUNT', 'id': '0'}," + 
						"'destination': {" + 
						"   'type': 'ONE_TIME_ADDRESS'," + 
						"   'oneTimeAddress': {'address': '%s'}" + 
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
				String.format( bodyTemplate, Fireblocks.platformBase, addr, keccak) );  

		Fireblocks fb = new Fireblocks();
		fb.endpoint( "/v1/transactions");
		fb.operation( "POST");
		fb.body( body);
		S.out( fb.transact() );
	}
}

