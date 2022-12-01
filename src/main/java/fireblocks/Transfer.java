package fireblocks;

public class Transfer {
	static String op = "POST"; 
	static String endpoint = "/v1/transactions"; // /v1/vault/accounts_paged";
	static String myDestWallet = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
	
	// this works as of 11/26/22 10:37am in the fireblocks branch
	public static void main(String[] args) throws Exception {
		Fireblocks.setVals();
		
		String bodyTemplate = 
				"{" + 
				"'operation': 'TRANSFER'," + 
				"'amount': '.00001'," + 
				"'assetId': '%s'," + 
				"'source': {'type': 'VAULT_ACCOUNT', 'id': '%s'}," + 
				"'destination': {" + 
				"   'type': 'ONE_TIME_ADDRESS'," + 
				"   'oneTimeAddress': {'address': '%s'}" + 
				"}," + 
//				"'priorityFee': '1'," + 
//				"'maxFee': '15'," + 
				"'note': 'Deployed from code'" + 
				"}";

		String accountId = "0";
		String destAddress = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
		String body = Fireblocks.toJson( 
				String.format( bodyTemplate, Fireblocks.platformBase, accountId, destAddress) );
		String operation = "POST";
		
		Fireblocks fb = new Fireblocks();
		fb.endpoint( endpoint);
		fb.operation( operation);
		fb.body( body);
		fb.transact();
	}
}
