package test;

import java.util.HashSet;

import javax.swing.JFrame;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.JsonModel;
import tw.util.NewLookAndFeel;
import tw.util.S;

public class ImportTelegram {
	static JsonModel m_model = new JsonModel("date,id,from,from_id,reply_to_message_id,text");
	
	public static void main(String[] args) throws Exception {
		NewLookAndFeel.register();
		String folder = "C:/temp/";
		String filename = folder + "result.json";
		JsonArray ar = JsonObject.readFromFile( filename).getArray("messages");
		ar.forEach( obj -> obj.update( "text", textObj -> extract(textObj) ) ); 
		ar.forEach( obj -> obj.update( "date", text -> text.toString().replace( 'T', ' ') ) ); 
		
		JsonArray ar2 = new JsonArray();

		HashSet<String> set = new HashSet<>();
		ar.forEach( rec -> {
			if (set.add( rec.getString("text") ) ) {
				ar2.add( rec);
			}
		});
				
		m_model.setRows( ar2);
		
		JFrame f = new JFrame();
		f.add( m_model.createTable() );
		f.setVisible(true);
		f.setSize( 400, 400);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		ar2.writeToCsv( "c:\\temp\\ama.csv", ',', "from,text");
		S.out( "done");
	}
	
	static Object extract( Object textObj) {
		return textObj instanceof JsonArray && ((JsonArray)textObj).size() > 0
				? ((JsonArray)textObj).get(0)
				: textObj;
	}

}
