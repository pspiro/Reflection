package test;

import tw.util.S;

public class MyTimer {
	long start;

	public MyTimer() {
		start = System.currentTimeMillis();  // the time is negligible ~ .000005 ms
	}
	
	public void next(String format, Object... params) {
		if (start > 0) {
			done();
		}
		S.out(format, params);
		start = System.currentTimeMillis();
	}

	public void done() {
		S.out( "  done in %s ms", System.currentTimeMillis() - start );
		start = 0;
	}
	
	public static void main(String[] args) {
		MyTimer t = new MyTimer();

		for (int a = 0; a < 1000000; a++) {
		}
		
		t.next("mid");
		
		for (int a = 0; a < 1000000; a++) {
			System.currentTimeMillis();
		}
		t.done();
	}
}
