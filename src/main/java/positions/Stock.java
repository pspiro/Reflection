package positions;

class Stock {
	private String symbol;
	private String token;
	private int conid;
	
	String symbol() { return symbol; }
	String token() { return token; }
	int conid() { return conid; }
	
	Stock( String sym, String tok, int con) {
		symbol = sym;
		token = tok;
		conid = con;
	}
}