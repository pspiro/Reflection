package fireblocks;

import java.math.BigDecimal;
import java.math.BigInteger;

import reflection.Main;
import reflection.RefCode;
import reflection.RefException;
import reflection.Util;
import tw.util.S;

public class StockToken {
	// method signature: buy(address,uint256,uint256,address)	
	static String qqq = "0xb402C11973Bcb15149b765e93E2553a688668f93";
	
	static int decimals = 5;
	static BigDecimal mult = new BigDecimal( 10).pow(decimals);
	static String buyKeccak = "3f60b633";
	
	public static void main(String[] args) throws Exception {
		Fireblocks.setVals();
		
		String myWallet = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
		
		buy( myWallet, qqq, 69.1, 69.1); 
	}
	
	static void buy( String userAddr, String stockTokenAddr, double stablecoinAmt, double stockTokenAmt) throws Exception {
		
		// buyStock(address,address,address,uint256,uint256)
		// sellStock(address,address,address,uint256,uint256)
		
		
		String bodyTemplate = 
				"{" + 
				"'assetId': '%s'," + 
				"'source': {'type': 'VAULT_ACCOUNT', 'id': '%s'}," + 
				"'destination': {" + 
				"   'type': 'ONE_TIME_ADDRESS'," + 
				"   'oneTimeAddress': {'address': '%s'}" + 
				"    }," + 
				"'amount': '0'," + 
				"'note': 'called from Rusd.java'," +
				"'operation': 'CONTRACT_CALL'," +
				"'extraParameters': {" +
				"   'contractCallData': %s" +
				"   }" +
				"}";
		
//        address _userAddress,
//        address _stableCoinAddress,
//        address _stockTokenAddress,
//        uint256 _stableCoinAmount,
//        uint256 _stockTokenAmount

        String callParams = String.format( "0x%s%s%s%s%s%s",
				buyKeccak,
				padAddr( userAddr),
				padAddr( Fireblocks.rusdAddress),
				padAddr( stockTokenAddr),
				pad( stablecoinAmt),
				pad( stockTokenAmt) );
        
				
		// take the first 4 bytes i think of the kkcac256 of the method signature
		// and then all of the parameters, each some fixed number of bytes
		
		String accountId = "4";
		String destAddress = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
		String body = Fireblocks.toJson( bodyTemplate, 
				Fireblocks.platformBase, accountId, Fireblocks.rusdAddress, callParams);
		
		
		Fireblocks fb = new Fireblocks();
		fb.endpoint("/v1/transactions");
		fb.operation( "POST");
		fb.body( body);
		
		S.out( body);
		fb.transact();
	}

	
	private static String pad(double amt) {
		BigInteger big = new BigDecimal(amt).multiply(mult).toBigInteger();
		S.out( "big: %s", big);
		return pad( String.format( "%x", big) );
	}
	
	private static String padAddr(String addr) throws RefException {
		Main.require( addr != null && addr.length() == 42, RefCode.UNKNOWN, "Invalid address %s", addr);
		return pad( addr.substring(2) );
	}

	private static String pad(String str) {
		return Util.padLeft( str, 64, '0'); 
	}
}
