package fireblocks;

import java.math.BigDecimal;
import java.math.BigInteger;

import reflection.Main;
import reflection.RefCode;
import reflection.RefException;
import reflection.Util;
import tw.util.S;

public class Rusd {
	static String prodAddr = "0x30072dccf2ac894ee7c28e233a2d222cc5123f78"; // contract address
	static String testAddr = "0x8a694956F724097ecE8Bf9A5B9d80ed8e05b66e2"; // contract address
	

	static int decimals = 5;
	static BigDecimal mult = new BigDecimal( 10).pow(decimals);
	static String buyKecaccac =  "58e78a85";
	static String sellKecaccac = "5948f1f0";
	
	public static void main(String[] args) throws Exception {
		Fireblocks.setVals();
		
		String myWallet = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
		
		buy( myWallet, StockToken.qqq, 69.1, 69.1); 
	}
	
	static void buy( String userAddr, String stockTokenAddr, double stablecoinAmt, double stockTokenAmt) throws Exception {
		// keccaks calculated as:
		// buyStock(address,address,address,uint256,uint256)
		// sellStock(address,address,address,uint256,uint256)
		
		String bodyTemplate = 
				"{" + 
				"'operation': 'CONTRACT_CALL'," +
				"'amount': '0'," + 
				"'assetId': '%s'," + 
				"'source': {'type': 'VAULT_ACCOUNT', 'id': '%s'}," + 
				"'destination': {" + 
				"   'type': 'ONE_TIME_ADDRESS'," + 
				"   'oneTimeAddress': {'address': '%s'}" + 
				" }," + 
				"'extraParameters': {" +
				"   'contractCallData': '%s'" +
				" }," +
				"'note': 'called from Rusd.java'" +
				"}";
		
//        address _userAddress,
//        address _stableCoinAddress,
//        address _stockTokenAddress,
//        uint256 _stableCoinAmount,
//        uint256 _stockTokenAmount

        String callParams = String.format( "0x%s%s%s%s%s%s",
				buyKecaccac,
				Fireblocks.padAddr( userAddr),
				Fireblocks.padAddr( Fireblocks.rusdAddress),
				Fireblocks.padAddr( stockTokenAddr),
				Fireblocks.padDouble( stablecoinAmt, mult),
				Fireblocks.padDouble( stockTokenAmt, mult) );
        
				
		// take the first 4 bytes i think of the kkcac256 of the method signature
		// and then all of the parameters, each some fixed number of bytes
		
		String accountId = "0";
		String body = Fireblocks.toJson( bodyTemplate, Fireblocks.platformBase, accountId, 
						Fireblocks.rusdAddress, callParams);
		
		Fireblocks fb = new Fireblocks();
		fb.endpoint("/v1/transactions");
		fb.operation( "POST");
		fb.body( body);
		fb.transact();
	}
}
