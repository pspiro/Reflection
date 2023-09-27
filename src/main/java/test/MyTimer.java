package test;

import tw.util.S;

public class MyTimer {
	long start;

	public MyTimer() {
	}
	
	public void next(String format, Object... params) {
		if (start > 0) {
			done();
		}
		
		S.out(format, params);
		start = System.currentTimeMillis();
	}

	public void done(String str) {
		S.out( "  completed %s in %s ms", str, time() );
		start = 0;
	}
	
	public long time() {
		return System.currentTimeMillis() - start;
	}
	
	public void done() {
		S.out( "  done in %s ms", System.currentTimeMillis() - start );
		start = 0;
	}
}
