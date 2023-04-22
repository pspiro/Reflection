package test;

import tw.util.S;

public class MyTimer {
	static long start;
	public static void start() {
		start = System.currentTimeMillis();
	}
	public static void tick() {
		long now = System.currentTimeMillis();
		long intv = now - start;
		S.out( "Interval: " + intv);
		start = now;
	}
	public static void next(String format, Object... params) {
		if (start > 0) {
			done();
		}
		S.out(format, params);
		start = System.currentTimeMillis();
	}

	public static void done() {
		S.out( "  done in %s ms", System.currentTimeMillis() - start );
		start = 0;
	}
}
