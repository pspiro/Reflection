package refblocks;

import reflection.Config;
import tw.util.S;

/** This actually works as of 8/19/24 on pulsechain */
public class CancelStuckTransaction {
	public static void main(String[] args) throws Exception {
		Config c = Config.ask();

		String pk = "";
		Refblocks.cancelStuckTransaction( pk, 0);

		S.out( c.ownerAddr() );
		
	}
}
