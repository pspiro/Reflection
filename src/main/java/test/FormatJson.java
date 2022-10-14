package test;

import java.util.Scanner;

import org.json.simple.parser.ParseException;

import http.MyJsonObj;

public class FormatJson {
	public static void main(String[] args) throws ParseException {
		Scanner scanner = new Scanner(System.in);
		String input = scanner.next();
		scanner.close();
		
		int i = input.indexOf( "{");
		String str = input.substring( i == -1 ? 0 : i);
		MyJsonObj obj = MyJsonObj.parse(str);
		obj.display();
	}
}
