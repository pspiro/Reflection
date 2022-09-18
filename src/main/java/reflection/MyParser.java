package reflection;

import tw.util.S;

public class MyParser {
	static Object[] parse( String s, String delims) {
		char c = delims.charAt( 0);
		String rem = delims.substring( 1);
		String[] parts = s.split( "" + c);

		if (rem.length() == 0) {
			return parts;
		}
		
		Object[] list = new Object[parts.length];
		for (int i = 0; i < parts.length; i++) {
			list[i] = parse( parts[i], rem);
		}
		return list;
	}
	
	static void show( Object[] list, String delims) {
		char c = delims.charAt( 0);
		String rem = delims.substring( 1);

		S.out( "[");
		for (Object obj : list) {
			if (obj instanceof String) {
				S.out( obj);
			}
			else {
				show( (Object[])obj, rem);
			}
			S.out( c);
		}
		S.out( "]");
	}
	
	public static void main(String[] args) {
		String str = "a,b;e,f;i,j-k,l";
		String delims = ";-,";
		Object[] list = parse( str, delims);
		show( list, delims);
	}
}
