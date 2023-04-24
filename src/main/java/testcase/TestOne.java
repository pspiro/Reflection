package testcase;

import java.lang.reflect.Field;

import org.json.simple.JSONArray;

import json.MyJsonObject;
import junit.framework.TestCase;
import reflection.Config;
import reflection.RefCode;
import tw.util.S;

public class TestOne extends TestCase {
	int a;
	private int b;
	protected int c;
	
	public static void main(String[] args) throws Exception {
	}

	public void testFillBuy() throws Exception {
		MyJsonObject obj = TestOrder.orderData( 3, "BUY", 10);
		obj.remove("noFireblocks");
		
		MyJsonObject map = TestOrder.sendData(obj);
		String code = map.getString( "code");
		String text = map.getString( "text");
		S.out( "fill buy %s %s", code, text);
		assertEquals( RefCode.OK.toString(), code);
		double filled = map.getDouble( "filled");
		assertEquals( 10.0, filled);
	}
}
