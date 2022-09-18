package tw.util;

public class MyException extends Exception {  // move this to lib. pas
	int m_code;
	
	public MyException( String first, String... params) {
		super( String.format( first, params) );
	}
}
