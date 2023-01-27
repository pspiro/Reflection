package json;

import java.util.Scanner;

import org.json.simple.parser.ParseException;

public class FormatJson {
	public static void main(String[] args) throws Exception {
		Scanner scanner = new Scanner(System.in);
		String input = scanner.nextLine();
		scanner.close();
		
		int i = input.indexOf( "{");
		int j = input.indexOf( "[");

		if (i < j || j == -1) {
			MyJsonObject.parse(input.substring(i)).display();
		}
		else {
			MyJsonArray.parse(input.substring(j)).display();
		}
	}
}
