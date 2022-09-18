package test;

import java.util.HashMap;
import java.util.Random;

import tw.util.S;

public class TestMany {
	public static void main(String[] args) {
		for (int i = 0; i < 10; i++) {
			new Thread( () -> {
				try {
					sendOrder();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}).start();
			S.sleep( 300);
		}
	}

	static Random r = new Random(System.currentTimeMillis());
	
	static Object lock = new Object();
	
	private static void sendOrder() throws Exception {
		
		boolean side = r.nextBoolean();
		
		String cryptoId = String.valueOf( r.nextInt() );
		
		double priceOffset = -2;
		
		String data = TestOrder.orderData( priceOffset, side ? "sell" : "sell", cryptoId);
		HashMap<String, Object> map = TestErrors.sendData( data);
		synchronized( lock) {
			S.out( map);
		}
//		String code = (String)map.get( "code");
//		String text = (String)map.get( "text");		
	}
}
