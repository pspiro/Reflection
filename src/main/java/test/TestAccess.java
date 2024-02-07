package test;

import java.util.HashMap;

/** Compare map access to member variable access */
public class TestAccess {
	HashMap<String,Integer> map = new HashMap<>();
	int a = 3;
	
	TestAccess() {
		map.put( "a", 3);
	}
	public static void main(String[] args) {
		int n = 3000000;
		MyTimer t = new MyTimer();
		
		TestAccess m = new TestAccess();
		
		// 10x faster
		t.next("direct");
		for (int i = 0; i < n; i++) {
			int b = m.a + 3;
		}
		
		t.next("table");
		for (int i = 0; i < n; i++) {
			int b = m.map.get("a") + 3;
		}
		
		t.done();
		
		
	}
		
}
