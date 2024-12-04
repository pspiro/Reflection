package monitor;

import java.awt.BorderLayout;

import javax.swing.JTextField;

import common.Util;
import http.MyClient;
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
	JTextField f16 = new JTextField(14);
	JTextField f17 = new JTextField(14);
	JTextField f18 = new JTextField(14);
	JTextField f19 = new JTextField(14);
	JTextField f20 = new JTextField(14);
	JTextField f21 = new JTextField(14);

	StatusPanel() {
		super( new BorderLayout() );
		
		VerticalPanel p = new VerticalPanel();
		p.addHeader( "RefAPI");
		p.add( "RefAPI", f1);
		p.add( "TWS", f2);
		p.add( "IB", f3);
		p.add( "Started", f4);
		p.add( "Built", f4a);
		p.addHeader( "MdServer");
		p.add( "MdServer", f5);
		p.add( "TWS", f6);
		p.add( "IB", f7);
		p.add( "Started", f8);
		p.addHeader( "HookServer");
		p.add( "HookServer", f16);
		p.add( "Started", f21);
		p.add( "Transfer stream", f17, f18);
		p.add( "Approval stream", f19, f20);
		
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
		
		MyClient.getJson(
				Monitor.chain().params().getHookServerUrl(), json -> {
					
			f16.setText( S.format( "%s (%s ms)", json.getString("code"), System.currentTimeMillis() - now) );
			f21.setText( json.getTime("started", Util.yToS) );
			setStreamStatus( f17, f18, "transfer-");
			setStreamStatus( f19, f20, "approval-");
		});
		
	}

	private void setStreamStatus(JTextField fullName, JTextField status, String prefix) {
		wrap( () -> {
//			String name = prefix + Monitor.m_config.getHookNameSuffix();
//			fullName.setText( name);
//			status.setText( MoralisStreams.getStreamStatus( name) );
		});
	}
}