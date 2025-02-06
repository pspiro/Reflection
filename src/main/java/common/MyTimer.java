package common;

import tw.util.S;

/** 
 *   INSTRUCTIONS: Call next(), next(), etc., done() 
 *   
 *   always call next() to start
 *   
 *   */
public class MyTimer {
	long start;

	public MyTimer() {
	}

	/** start here */
	public MyTimer start() {
		start = System.currentTimeMillis();
		return this;
	}
	
	/** or start here */
	public MyTimer next(String format, Object... params) {
		if (start > 0) {
			done();
		}
		
		S.out(format, params);
		start = System.currentTimeMillis();
		return this;
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
