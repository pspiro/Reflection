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
		new Sub().test();
	}
	
	static class Sub extends TestOne {
		int d;
	}

	public void test() throws Exception {
		JSONArray list = new JSONArray();
		
		for (Field field : TestOne.class.getDeclaredFields() ) {
			Object obj = field.get(this);
			if (obj != null && isPrimitive(obj.getClass()) ) {
				list.add( field.getName() );
				list.add(obj);
			}
		}

		S.out( list);
	}

	private boolean isPrimitive(Class clas) {
		return clas == String.class || clas == Integer.class || clas == Double.class || clas == Long.class;
	}

}
