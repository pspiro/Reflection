package tw.util;

public class MyException extends Exception {
	int m_code;
	
	public MyException( String first, Object... params) {
		super( String.format( S.notNull( first), params) );
	}
}
