package test;

import java.util.HashSet;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import reflection.Config;
import tw.google.GTable;
import tw.google.NewSheet;
import tw.util.OStream;
import tw.util.S;

public class CreateEmailList {
	static JsonArray all = new JsonArray();
	static HashSet<String> emails = new HashSet<>();

	public static void main(String[] args) throws Exception {

		// add excluded emails to set
		new GTable( NewSheet.Prefinery, "Exclude", "Exclude", null)
			.keySet().forEach( email -> emails.add( email) );
		
		
		// add existing emails to set; convert full format to email only
		// to rebuild the whole list (with exclusions), add Json object to addAll
		new GTable( NewSheet.Prefinery, "Full list", "Email", null)
			.keySet().forEach( full -> 
				emails.add( Util.parseEmail(full)[1]) );
		
		Config.ask().sqlCommand( sql -> {
			JsonArray ar = new JsonArray();

			// add from signup table
			ar = sql.queryToJson( "select email as Email, first as FirstName from signup");
			S.out( "got %s from signup", ar.size() );
			all.addAll( ar);

			// add from users table
			ar = sql.queryToJson( "select email as Email, first_name as FirstName from users");
			S.out( "got %s from users", ar.size() );
			all.addAll( ar);

			// add from jotform
			ar = NewSheet.getTab(NewSheet.Prefinery, "Jotform").queryToJson("Email,FirstName");
			S.out( "got %s from Jotform tab", ar.size() );
			all.addAll( ar);
		});

		OStream os = new OStream( "c:/temp/emails.t");

		// better, write it directly to "Full list" tab
		int count = 0;
		for (JsonObject row : all) {
			String email = row.getString( "Email").toLowerCase().trim();

			if (S.isNotNull( email) && emails.add( email) ) {
				String first = Util.initialCap( row.getString( "FirstName"));
				os.writeln( Util.formatEmail( first, email) );
				count++;
			}
		}

		os.close();
		S.out( "wrote %s", count);
	}

}
