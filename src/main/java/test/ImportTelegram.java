package test;

import javax.swing.JFrame;

import org.json.simple.JsonArray;

import common.JsonModel;

public class ImportTelegram {
	static JsonModel m_model = new JsonModel("date,from,text");
	
	public static void main(String[] args) throws Exception {
		JsonArray ar = JsonArray.readFromFile( "C:\\Users\\peter\\Downloads\\Telegram Desktop\\ChatExport_2024-03-07\\result.json");
		
		m_model.setRows( ar);
		
		JFrame f = new JFrame();
		f.add( m_model.createTable() );
		f.setVisible(true);
		f.setSize( 400, 400);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
}
