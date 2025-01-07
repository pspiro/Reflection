package common;

import java.io.Closeable;
import java.io.IOException;
import java.util.Scanner;

import tw.util.S;

/** It seems you can only create this once per application;
 *  when you create the second one, it returns immediately */
public class MyScanner implements Closeable {
	
	private Scanner scanner;
	
	public MyScanner() {
		scanner = new Scanner( System.in);
	}

	@Override public void close() throws IOException {
		scanner.close();
	}
	
	public int getInt( String prompt) {
		return Integer.parseInt( getString( prompt));
	}

	public String getString( String prompt, String def) {
		S.out( prompt);
		String str = scanner.nextLine();
		return S.isNull( str) ? def : str;
	}
	
	public String getString( String prompt) {
		S.out( prompt);
		return scanner.nextLine();
	}
}
