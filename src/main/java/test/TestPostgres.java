package test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import tw.util.S;

/** Just test that you can connect to the database. */
public class TestPostgres {
	static int i = 0;
	public static void main(String[] args) throws Exception {
		Map<String,String> map = new ConcurrentHashMap<>();
		
		map.put( "a", "e");
		map.put( "b", "f");
		map.put( "c", "g");
		map.put( "d", "h");
		
		map.entrySet().forEach( item -> {
			map.clear();

			String key = item.getKey();
			String val = item.getValue();
		
			S.out( "%s %s", key, val);
			i++;
		});
	}
}
