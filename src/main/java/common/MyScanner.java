package common;

import java.io.Closeable;
import java.io.IOException;
import java.util.Scanner;

import tw.util.S;

public class MyScanner implements Closeable {
	
	private Scanner scanner;
	
	public MyScanner() {
		scanner = new Scanner( System.in);
	}

	@Override public void close() throws IOException {
		scanner.close();
	}
	
	public String input( String prompt) {
		S.out( prompt);
		return scanner.nextLine();
	}
}
