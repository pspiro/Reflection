package testcase;

import static testcase.TestOrder.orderData;
import static testcase.TestOrder.post;

import json.MyJsonObject;
import junit.framework.TestCase;
import reflection.Prices;
import reflection.RefCode;
import tw.util.S;

public class TestOne extends TestCase {
	public void testEtf1()  throws Exception {
		MyJsonObject map = TestOutsideHours.testHours( TestOutsideHours.QQQ, "4:00");

		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");

		S.out( "testEtf1 " + text);
		assertEquals( RefCode.OK.toString(), ret);
	}
}
