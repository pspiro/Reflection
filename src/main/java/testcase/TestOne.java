package testcase;

import java.lang.reflect.Field;

import org.json.simple.JSONArray;

import json.MyJsonObject;
import junit.framework.TestCase;
import reflection.Config;
import reflection.RefCode;
import tw.util.S;

public class TestOne extends TestCase {
	public void testFracShares()  throws Exception {
		new TestOutsideHours().testEtf4();
	}
}
