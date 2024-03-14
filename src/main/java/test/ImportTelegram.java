package test;

import java.util.HashSet;

import javax.swing.JFrame;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.JsonModel;
import tw.util.NewLookAndFeel;
import tw.util.S;

public class ImportTelegram {
	static JsonModel m_model = new JsonModel("from,text");
	
	public static void main(String[] args) throws Exception {
		NewLookAndFeel.register();
		String folder = "C:\\Users\\RJJP\\Downloads\\Telegram Desktop\\ChatExport_2024-03-09\\";
		String filename = folder + "result.json";
		JsonArray ar = JsonObject.readFromFile( filename).getArray("messages");
		
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
}
