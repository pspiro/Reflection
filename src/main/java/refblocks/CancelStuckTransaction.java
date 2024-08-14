package refblocks;

import reflection.Config;
import tw.util.S;

public class CancelStuckTransaction {
	public static void main(String[] args) throws Exception {
		Config c = Config.read();
		Refblocks.cancelStuckTransaction( c.ownerAddr() );

		S.out( c.ownerAddr() );
		
	}
}
