package refblocks;

import reflection.Config;
import tw.util.S;

public class CancelStuckTransaction {
	public static void main(String[] args) throws Exception {
		Config c = Config.ask( "Dt2");
//		Refblocks.showNonce( c.ownerAddr(), DefaultBlockParameterName.FINALIZED);
//		Refblocks.showNonce( c.ownerAddr(), DefaultBlockParameterName.LATEST);
//		Refblocks.showNonce( c.ownerAddr(), DefaultBlockParameterName.PENDING);
//		Refblocks.showNonce( c.ownerAddr(), DefaultBlockParameterName.SAFE);
		
		S.out( c.ownerAddr() );
		
	}
}
