package reflection;

public class RefException extends Exception {
	private RefCode m_code;

	public RefCode code() { return m_code; }

	public RefException( RefCode code, String first, Object... params) {  // my text is the detail message
		super( String.format( first, params) );
		m_code = code;
	}

	public Json toJson() {
		return Util.toJsonMsg( "code", m_code, "text", getMessage() );
	}

}
