package fireblocks;

import tw.util.S;

public class Transfer {
	static String op = "POST"; 
	static String endpoint = "/v1/transactions"; // /v1/vault/accounts_paged";
	static String myDestWallet = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
	
	// this works as of 11/26/22 10:37am in the fireblocks branch
	public static void main(String[] args) throws Exception {
		//Fireblocks.setTestVals();
		Fireblocks.setProdVals();
		
		String id2 = Transfer.transfer( Fireblocks.platformBase, Fireblocks.ownerAcctId, 
				Fireblocks.refWalletAddr, ".3", "transfer BNB");
		S.out(id2);
	}
	
	/** @param asset is the Fireblocks assetId, not the contract address
	 *  @param amt should be the decimal number, e.g. 1.2 transfers one 1.2x10^n where n is the # of decimals
	 *  @return FB trans id */
	public static String transfer(String asset, int srcAccountId, String destAddress, String amount, String note) throws Exception {
		String bodyTemplate = 
				"{" + 
				"'operation': 'TRANSFER'," + 
				"'amount': '%s'," + 
				"'assetId': '%s'," + 
				"'source': {'type': 'VAULT_ACCOUNT', 'id': '%s'}," + 
				"'destination': {" + 
				"   'type': 'ONE_TIME_ADDRESS'," + 
				"   'oneTimeAddress': {'address': '%s'}" + 
				"}," + 
				"'note': '%s'" + 
				"}";

		String body = Fireblocks.toJson( 
				String.format( bodyTemplate, amount, asset, srcAccountId, destAddress, note) );
		String operation = "POST";
		
		Fireblocks fb = new Fireblocks();
		fb.endpoint( endpoint);
		fb.operation( operation);
		fb.body( body);
		return fb.transactToId();
	}
}
