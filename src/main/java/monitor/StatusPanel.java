package monitor;

import java.awt.BorderLayout;

import javax.swing.Box;
import javax.swing.JTextField;

import common.Util;
import http.MyClient;
import monitor.Monitor.MonPanel;
import tw.util.S;
import tw.util.VerticalPanel;

class StatusPanel extends MonPanel {
	JTextField f1 = new JTextField(7);
	JTextField f2 = new JTextField(7);
	JTextField f3 = new JTextField(7);
	JTextField f4 = new JTextField(14);
	JTextField f4a = new JTextField(14);
	JTextField f5 = new JTextField(7);
	JTextField f6 = new JTextField(7);
	JTextField f7 = new JTextField(7);
	JTextField f8 = new JTextField(14);
	JTextField f10 = new JTextField(14);
	JTextField f11 = new JTextField(14);
	JTextField f12 = new JTextField(14);
	JTextField f13 = new JTextField(14);
	JTextField f14 = new JTextField(14);
	JTextField f15 = new JTextField(14);

	StatusPanel() {
		super( new BorderLayout() );
		
		VerticalPanel p = new VerticalPanel();
		p.add( "RefAPI", f1);
		p.add( "TWS", f2);
		p.add( "IB", f3);
		p.add( "Started", f4);
		p.add( "Built", f4a);
		p.add( Box.createVerticalStrut(10) );
		p.add( "MdServer", f5);
		p.add( "TWS", f6);
		p.add( "IB", f7);
		p.add( "Started", f8);
		p.add( Box.createVerticalStrut(10) );
		p.add( "FbServer", f10);
		p.add( "Map size", f12);
		p.add( "Started", f11);
		p.add( "Last error", f13);
		p.add( "Last successful fetch", f14);
		p.add( "Last successful put", f15);
		
		add( p);
	}
	
	@Override public void refresh() throws Exception {
		long now = System.currentTimeMillis();

		MyClient.getJson( Monitor.m_config.baseUrl() + "/api/status", json -> {
			f1.setText( S.format( "%s (%s ms)", json.getString("code"), System.currentTimeMillis() - now) );
			f2.setText( json.getBool("TWS") ? "OK" : "ERROR" );
			f3.setText( json.getBool("IB") ? "OK" : "ERROR" );
			f4.setText( json.getTime("started", Util.yToS) );
			f4a.setText( json.getString("built") );
		});

		MyClient.getJson( Monitor.m_config.mdBaseUrl() + "/mdserver/status", json -> {
			f5.setText( S.format( "%s (%s ms)", json.getString("code"), System.currentTimeMillis() - now) );
			f6.setText( json.getBool("TWS") ? "OK" : "ERROR" );
			f7.setText( json.getBool("IB") ? "OK" : "ERROR" );
			f8.setText( json.getTime("started", Util.yToS) );
		});
		
		MyClient.getJson( Monitor.m_config.fbBaseUrl() + "/fbserver/status", json -> {
			f10.setText( S.format( "%s (%s ms)", json.getString("code"), System.currentTimeMillis() - now) );
			f11.setText( json.getTime("started", Util.yToS) );
			f12.setText( json.getString("mapSize").toString() );
			f14.setText( json.getTime( "lastSuccessfulFetch", Util.hhmmss) );
			f14.setText( json.getTime( "lastSuccessfulPut", Util.hhmmss) );
		});
	}
}