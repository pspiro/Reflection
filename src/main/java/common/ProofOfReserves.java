package common;

import reflection.SingleChainConfig;
import tw.util.OStream;
import tw.util.S;

/** show RUSD balance of all accounts */
public class ProofOfReserves {
	
	public static void main(String[] args) throws Exception {
		try (OStream os = new OStream( "c:/temp/file.csv") ) {
			S.out( "RUSD Wallet Balances as of ");
			S.out( "Wallet Address,RUSD Balance");
			SingleChainConfig.ask( "prod").rusd().getAllBalances().forEach( (addr,bal) ->
				os.writeln( S.format( "%s,%s", addr, "" + bal) ) );
		}
	}

}
