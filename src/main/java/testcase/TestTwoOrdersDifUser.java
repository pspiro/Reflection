package testcase;

import json.MyJsonObject;
import reflection.RefCode;
import tw.util.S;

// finish this
public class TestTwoOrdersDifUser extends TestOrder {
	boolean done1 = true;
	boolean done2 = true;
	
	static {
		//m_noFireblocks = false;
	}

	public static void main(String[] args) throws InterruptedException {
		new TestTwoOrdersDifUser().testFill();
	}
	
	public void testFill() throws InterruptedException {
		new Thread( () -> buy1() ).start();
		
		for (int i = 0; i < 6; i++) {
			new Thread( () -> getPrice() ).start();
			S.sleep(1000);
		}
		
//		while (!done1 || !done2) {
//			wait();
//		}
	}
	
	// try one more time, then move the auto-fill up
// they are held up until after the order is filled
	
	void buy1() {
		try {
			MyJsonObject map = postDataToObj(orderData( 3, "BUY", 2));
			String code = map.getString( "code");
			S.out( "fill buy1 %s %s", code, map.getString("message") );
			assertEquals( RefCode.OK.toString(), code);
			double filled = map.getDouble( "filled");
			assertEquals( 2.0, filled);
			S.out( "SUCCESS 1");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void buy2() {
		try {
			MyJsonObject map = postDataToObj(orderData( 3, "BUY", 3));
			String code = map.getString( "code");
			S.out( "fill buy2 %s %s", code, map.getString("message") );
			assertEquals( RefCode.OK.toString(), code);
			double filled = map.getDouble( "filled");
			assertEquals( 3.0, filled);
			S.out( "SUCCESS 2");
			notify();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	void getPrice() {
		try {
			S.out( "get-stock-with-price");
			cli().get( "/api/get-stock-with-price/8314").readMyJsonObject().display();
			S.out( "got-stock-with-price");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}

//keep going with this, get the tests to pass; you still have to find out why getPrice() hangs