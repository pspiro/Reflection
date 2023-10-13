package monitor;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import redis.clients.jedis.Response;
import tw.util.S;

public class RedisPanel extends JsonPanel {
	RedisPanel() {
		super( new BorderLayout(), "symbol,conid,bid,ask,last,time,close,from");
		
		add( m_model.createTable() );
	}
	
	@Override public void refresh() throws Exception {
		S.out( "Refreshing Redis panel");
		m_model.refresh();
		S.out( "  done");
	}
	
	@Override JsonModel createModel(String allNames) {
		return new Model(allNames);
	}
	
	static class RedisQuery {
		String key;
		Response<Map<String, String>> resp;  // this is specifically an "hgetall" query

		RedisQuery(String v1, Response<Map<String, String>> v2) {
			key = v1;
			resp = v2;
		}
	}
	
	class Model extends JsonModel {
		Model(String allNames) {
			super(allNames);
			justify("llrrrlr");
		}
	
		void refresh() throws Exception {
			super.refresh();
			Set<String> keys = Monitor.m_redis.query( jedis -> jedis.keys("*") );

			ArrayList<RedisQuery> list = new ArrayList<>();
			
			Monitor.m_redis.pipeline( pipe -> {
				for (String key : keys) {
					list.add( new RedisQuery( key, pipe.hgetAll(key) ) );  // we have to remember the key or we can't get it
				}
			});
			
			JsonArray ar = new JsonArray();
			
			list.forEach( query -> {
				JsonObject obj = new JsonObject();
				obj.put("conid", query.key); 
				query.resp.get().forEach( (key,val) -> obj.put( key, val) );
				ar.add( obj);
				
				obj.put("symbol", Monitor.stocks.getStock(Integer.parseInt(query.key)).getSymbol() ); // lookup symbol
				obj.update("time", val -> Util.fmtTime(Long.parseLong((String)val) ) );  // format date 
			});
			
			m_ar = ar;
			fireTableDataChanged();
		}
		
//		@Override protected Object format(String key, Object val) {
//			return key.equals("bid") || key.equals("ask")
//					? fmt((String)val) : val;
//		}
//
//		private String fmt(String val) {
//			try {
//				S.parseDouble
//			}
//			catch( Exception e) {
//				return val;
//			}
//		}
	}
	
	@Override public void activated() {
		try {
			refresh();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override public void closed() {
	}
}
