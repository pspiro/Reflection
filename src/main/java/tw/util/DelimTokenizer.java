package tw.util;

/** Parses a line with fixed width fields. See also STokenizer. */
public class DelimTokenizer {
	private String m_strs;
	private int[] m_delimss;
	private int m_counter = 0;
	
	public DelimTokenizer( String str, int[] delims) {
		m_strs = str;
		m_delimss = delims;
	}
	
	public String nextString() {
		int start = m_delimss[m_counter];
		int end = m_delimss[m_counter++ + 1];
		return m_strs.substring( start, end).trim();
	}

	public boolean hasNext() {
		int end = m_delimss[m_counter + 1];
		return m_strs.length() >= end;
	}

	public double nextDouble() {
		return Double.valueOf( nextString() );
	}

	public int nextInt() {
		return Integer.valueOf( nextString() );
	}
}
