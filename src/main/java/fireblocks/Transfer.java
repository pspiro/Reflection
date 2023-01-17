package fireblocks;

public class Transfer {
	static String op = "POST"; 
	static String endpoint = "/v1/transactions"; // /v1/vault/accounts_paged";
	static String myDestWallet = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
	
	// this works as of 11/26/22 10:37am in the fireblocks branch
	public static void main(String[] args) throws Exception {
		Fireblocks.setTestVals();
		transfer("0", "0xb016711702D3302ceF6cEb62419abBeF5c44450e", "transfer platform base");
	}
	
	static void transfer(String accountId, String destAddress, String note) throws Exception {
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
				"'note': '%s'" + 
				"}";

		String body = Fireblocks.toJson( 
				String.format( bodyTemplate, Fireblocks.platformBase, accountId, destAddress, note) );
		String operation = "POST";
		
		Fireblocks fb = new Fireblocks();
		fb.endpoint( endpoint);
		fb.operation( operation);
		fb.body( body);
		fb.transact();
	}
}
