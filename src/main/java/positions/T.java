package positions;

import reflection.Config;
import tw.util.S;

public class T {
	static Wallet jitin;
	static Wallet prod;
	static Config config;
	
	public static void main(String[] args) throws Exception {
		
		jitin = new Wallet("0x96531A61313FB1bEF87833F38A9b2Ebaa6EA57ce");
		prod = new Wallet("0x2703161D6DD37301CEd98ff717795E14427a462B");
		
		config = Config.ask();
		
		while(true) {
			S.out( "%s  %s", 
					jitin.getBalance(config.busd().address()),
					prod.getBalance(config.busd().address() ) );
			S.sleep(2000);
		}
	}

	
}

//22:03:27 6:00
//22:01:21 4:00 position change comes
//22:00:27 3:00 unconfirmed   3 minutes to confirm!!!
//21:57:34 0:00 started
//
//
//get request
//if not found, request all positions and map them
//listen to events for all stocks + rusd + busd
//when event occurs, IF we have the wallets, refresh them
//else ignore
//when we get the unconfirmed, mark it stale
//when we get confirmed, delete it; next time, it will request and hold
//anytime you get confirmed, you have to requery or mark it for requery
//let the whole HookServer run as part of RefAPI, but build it separately
//you should maybe adjust the position when we get the unconfirmed trade so that we can more
//quickly return the correct value, and only take a new value if it is different from
//		both the old value and our newly calculated value; but always take the 
//		queried value after a confirmed report comes in