package fireblocks;

import http.SimpleTransaction;
import tw.util.S;

public class Webhook {
	public static void main(String[] args) {
		if (args.length != 2) {
			S.out( "usage: Webhook <host> <port>");
			return;
		}
		
		SimpleTransaction.listen(args[0], Integer.valueOf(args[1]), trans -> process(trans) );
	}

	private static Object process(SimpleTransaction trans) {
		// TODO Auto-generated method stub
		return null;
	}
}
