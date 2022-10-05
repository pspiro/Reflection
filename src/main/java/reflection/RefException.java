package reflection;

import tw.util.S;

public class RefException extends Exception {
	private RefCode m_code;

	public RefCode code() { return m_code; }

	public RefException( RefCode code, String first, Object... params) {  // my text is the detail message
		super( String.format( S.notNull( first), params) );
		m_code = code;
	}
	
	@Override public String toString() {
		return m_code + " " + getMessage();
	}

	public Json toJson() {
		return Util.toJsonMsg( "code", m_code, "text", getMessage() );
	}

}
