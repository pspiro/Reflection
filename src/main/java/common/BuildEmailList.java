package common;

import java.util.HashSet;

import reflection.Config;
import tw.google.NewSheet;
import tw.util.S;

public class BuildEmailList {
	public static void main(String[] args) throws Exception {
		// read existing emails and add them to a map so we don't add dups
		var tab = NewSheet.getTab( NewSheet.Prefinery, "Full list");
		var rows = tab.fetchRows();
		HashSet<String> emails = new HashSet<>();
		emails.add( "");
		
		for ( var row : rows) {
			String full = row.getString( "Email");
			String email = Util.parseEmailOnly( full);
			emails.add( email);
		}
		
		Config c = Config.readFrom( "Prod-config");

		tab.startTransaction();

		// read and add emails from signup table
		S.out( "adding new records from signup table");
		var recs = c.sqlQuery( "select email, first from signup");
		for (var rec : recs) {
			String email = rec.getString( "email").toLowerCase();
			String first = rec.getString( "first");
			
			if (!emails.contains( email)) {
				emails.add( email);

				String full = String.format( "%s <%s>", first, email);
				S.out( "  adding " + full);
				tab.insert( Util.toJson( "Email", full) );
			}
		}

		// read and add emails from users table
		S.out( "adding new records from users table");
		recs = c.sqlQuery( "select email, first_name from users");
		for (var rec : recs) {
			String email = rec.getString( "email").toLowerCase();
			String first = rec.getString( "first_name");
			
			if (!emails.contains( email)) {
				emails.add( email);

				String full = String.format( "%s <%s>", first, email);
				S.out( "  adding " + full);
				tab.insert( Util.toJson( "Email", full) );
			}
		}
		
		tab.commit();
		
	}
}
