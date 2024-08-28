package tw.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.function.Consumer;

public class IStream implements Closeable {
	private BufferedReader m_br;
	private String m_peek;
	
	public IStream( String filename) throws FileNotFoundException {
		m_br = new BufferedReader( new FileReader( filename) );
	}
	
	/** Return the next line that will be returned by readln(). */
	public String peek() {
		if (m_peek == null) {
			m_peek = readln();
		}
		return m_peek;
	}
	
	public boolean hasNext() {
		return peek() != null;
	}
	
	public String readln() {
		if (m_peek != null) {
			try {
				return m_peek;
			}
			finally {
				m_peek = null;
			}
		}
		
		try {
			return m_br.readLine();
		}
		catch( Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void close() {
		try {
			m_br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** Reads the whole file into a single string and close the stream. */

	public static String readAll(String filename) {
		try (IStream is = new IStream( filename) ) {
			StringBuilder sb = new StringBuilder();

			int c = is.m_br.read();
			while( c != -1) {
				sb.append( (char)c);
				c = is.m_br.read();
			}
			return sb.toString();
		}
		catch( Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public String readcsvln() throws Exception {
		// keep building builder with more lines until there is an even number of double-quotes
		String line = readln();
		
		// nothing more to read?
		if (line == null) {
			return null;
		}

		// keep appending lines until we have an even number of double-quotes
		StringBuilder builder = new StringBuilder();
		while( line != null) {
			builder.append( line);
			if (S.isEven( countQuote( builder) ) ) {
				return builder.toString();
			}
			builder.append( " <p> ");
			line = readln();
		}

		throw new Exception( "Error: CSV file ends without cell closure");
	}

	private static int countQuote(StringBuilder builder) {
		int n = 0;
		for (int i = 0; i < builder.length(); i++) {
			n += builder.charAt( i) == '"' ? 1 : 0;
		}
		return n;
	}
	
	public void process( Consumer<String> consumer) {
		String str;
		
		while( (str = readln() ) != null) {
			consumer.accept( str);
		}
	}

	public void processAsCsv( Consumer<String> consumer) throws Exception {
		String str;
		
		while( (str = readcsvln() ) != null) {
			consumer.accept( str);
		}
	}

	public static void main(String[] args) throws Exception {
		S.out( readLine( "c:/work/reflection/config.txt"));
	}

	/** Return the first line of the file as a string */
	public static String readLine(String filename) throws Exception {
		try (IStream is = new IStream( filename) ) {
			return is.readln();
		}
	}
}
