package testcase;

import java.util.concurrent.CountDownLatch;

import org.json.simple.JsonObject;

import reflection.RefCode;
import tw.util.S;

public class TestTwoOrdersSameUser extends MyTestCase {
	CountDownLatch latch = new CountDownLatch(1);
	boolean ok = true;
	
	public void testFill() throws InterruptedException {
		
		new Thread( () -> buy1("a") ).start();
		S.sleep(10);
//		new Thread( () -> buy1("b") ).start();
		
		latch.await();
		assertTrue(ok);
	}
	
	void buy1(String id) {
		try {
			JsonObject map = postOrderToObj( orderData( 3, "BUY", 2) );
			
			String code = map.getString( "code");
			String message = map.getString("message");
			S.out( "fill buy1 %s %s", code, message);
			
			assertEquals( RefCode.OK.toString(), code);
			assertEquals( 2.0, map.getDouble( "filled") );
		} catch (Throwable e) {
			e.printStackTrace();
			ok = false;
		}
		finally {
			S.out( "finished %s", id);
			
			latch.countDown();
		}
	}

	private JsonObject orderData(int offset, String side, int qty) throws Exception {
		JsonObject data = TestOrder.createOrder(side, qty, offset);
		data.remove("noFireblocks");
		return data;
	}
}
