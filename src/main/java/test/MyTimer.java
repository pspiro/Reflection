package test;

import tw.util.S;

/** Call next(), next(), etc., done() */
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
	
	/** Use this for custom "done" messages */
	public long time() {
		return System.currentTimeMillis() - start;
	}

	/** Pass in the name of the item that was completed */
	public void done(String str) {
		S.out( "  completed %s in %s ms", str, time() );
		start = 0;
	}
	
	public void done() {
		S.out( "  done in %s ms", time() );
		start = 0;
	}
}
