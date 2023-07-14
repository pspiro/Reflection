package fireblocks;

import http.SimpleTransaction;
import tw.util.S;

public class Webhook {
	public static void main(String[] args) {
		if (args.length != 2) {
			S.out( "usage: Webhook <host> <port>");
			return;
		}
		
		//WebhookConfig config = WebhookConfig.readFrom(null);
		
		SimpleTransaction.listen(args[0], Integer.valueOf(args[1]), trans -> process(trans) );
	}

	private static Object process(SimpleTransaction trans) {
		return null;
	}
}
