package testcase;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.Date;

import org.json.simple.JSONArray;

import json.MyJsonObject;
import junit.framework.TestCase;
import reflection.Config;
import reflection.RefCode;
import tw.util.S;

public class TestOne extends TestCase {
	public void testEtf4() throws Exception {
		MyJsonObject map = TestOutsideHours.testHours( TestOutsideHours.QQQ, "20:00");

		String ret = (String)map.getString("code");
		String text = (String)map.getString("message");

		S.out( "testEtf4 " + text);
		assertEquals( RefCode.OK.toString(), ret);
	}	
}
