package tw.util;

public class MyException extends Exception {
	public MyException( String first, Object... params) {
		super( String.format( S.notNull( first), params) );
	}
}
