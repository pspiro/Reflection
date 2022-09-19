package test;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;

import reflection.MySqlConnection;
import tw.google.GTable;
import tw.google.NewSheet;
import tw.google.NewSheet.Book.Tab;
import tw.google.NewSheet.Book.Tab.ListEntry;

public class Query {
	public static void main(String[] args) {
		try {
			new Query().run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	void run() throws Exception {
		GTable queryTab = new GTable(NewSheet.Reflection, "Query", "Tag", "Value");

		// connect to db
		MySqlConnection con = new MySqlConnection();
		con.connect(queryTab.get( "Url"), queryTab.get( "Username"), queryTab.get( "Password") );

		// run query
		ResultSet rs = con.query( queryTab.get( "Query") );
		ResultSetMetaData meta = rs.getMetaData();
		
		Tab tab = NewSheet.getTab( NewSheet.Reflection, "Query");
		
		// get column names
		ArrayList<String> colNames = new ArrayList<String>(); 
		for (int i = 1; i <= meta.getColumnCount(); i++) {      // columns in psql are 1-based
			colNames.add( meta.getColumnName( i ) );  
		}

		// add column names to sheet
		ListEntry titleRow = tab.newListEntry();
		for (String col : colNames) {
			titleRow.addValue( col);
		}
		titleRow.insert();
		
		// add data rows
		while( rs.next() ) {
			ListEntry dataRow = tab.newListEntry();
			for (String col : colNames) {
				dataRow.addValue( rs.getString( col) );
			}
			dataRow.insert();
		}
	}
}
