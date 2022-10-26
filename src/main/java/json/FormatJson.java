package json;

import java.util.Scanner;

import org.json.simple.parser.ParseException;

public class FormatJson {
	public static void main(String[] args) throws ParseException {
		Scanner scanner = new Scanner(System.in);
		String input = scanner.nextLine();
		scanner.close();
		
		int i = input.indexOf( "{");
		String str = input.substring( i);
		MyJsonObj obj = MyJsonObj.parse(str);
		obj.display();  // figure out long vs integer in the Jsonobj
	}
}
