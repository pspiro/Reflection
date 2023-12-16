package http;

import java.net.URI;

import common.Util;

public class ClientException extends Exception {
	private int m_statusCode;
	private String m_responseText;
	private URI m_uri;
	
	public ClientException( int statusCode, String responseText, URI uri) {
		m_statusCode = statusCode;
		m_responseText = responseText;
		m_uri = uri;
	}
	
	/** @return the responde code and the uri and first 32 chars of response text */
	@Override public String getMessage() {
		return String.format("Error - received status code %s fetching URL %s - %s",
				m_statusCode, m_uri, Util.left( flatText(), 100) );
	}

	private String flatText() {
		return m_responseText.replaceAll("\r\n", " ").replaceAll( "\n", " ");
	}

	public int statusCode() {
		return m_statusCode;
	}
}
