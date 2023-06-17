package json;

import java.util.Scanner;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

public class FormatJson {
	public static void main(String[] args) throws Exception {
		Scanner scanner = new Scanner(System.in);
		String input = scanner.nextLine();
		scanner.close();
		
		int i = input.indexOf( "{");
		int j = input.indexOf( "[");

		if (i < j || j == -1) {
			JsonObject.parse(input.substring(i)).display();
		}
		else {
			JsonArray.parse(input.substring(j)).display();
		}
	}
}
