package positions;

import java.io.FileNotFoundException;

import reflection.Util;
import tw.util.S;

public class TestMoralis {
	static String chain = "0x5";
	static String apple = "0x29c6f774536dFc3343e2e8D804Ed233690083299";
	
	public static void main(String[] args) throws FileNotFoundException {
		String abi = Util.toJson( "{'abi': [{'inputs': [],'name': 'totalSupply','outputs': [{'internalType': 'uint256','name': '','type': 'uint256'}],'stateMutability': 'view','type': 'function'}],'params': {}}");
		S.out( MoralisServer.contractCall( apple, "totalSupply", abi) );
	}
	

	
}
