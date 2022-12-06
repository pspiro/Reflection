package json;

import java.util.Scanner;

import org.json.simple.parser.ParseException;

public class FormatJson {
	public static void main(String[] args) throws ParseException {
		Scanner scanner = new Scanner(System.in);
		String input = scanner.nextLine();
		scanner.close();
		
		int i = input.indexOf( "{");
		int j = input.indexOf( "[");

		if (i < j || j == -1) {
			MyJsonObject obj = MyJsonObject.parse(input.substring(i));
			obj.display();
		}
		else {
			MyJsonAr ar = MyJsonAr.parse(input.substring(j));
			// ar.display(); fix this after merge 
		}
	}
}
