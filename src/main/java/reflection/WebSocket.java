package reflection;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/** create the WS server to handle mkt data requests;
 *  there is no request, actually; client connects, sends no data;
 *  we push all market data down
 */
public class WebSocket {
	@Configuration
	@EnableWebSocket
	public static class WebSocketConfig implements WebSocketConfigurer {

	    private final WsHandler wsHandler;

	    @Autowired
	    public WebSocketConfig(WsHandler wsHandler) {
	        this.wsHandler = wsHandler;
	    }

	    @Override
	    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
	        registry.addHandler(wsHandler, "/ws").setAllowedOrigins("*");
	    }
	}

	@Component
	public static class WsHandler extends TextWebSocketHandler {
		@Autowired
		private SessionManager sessionManager;

		@Override
		public void afterConnectionEstablished(WebSocketSession session) throws Exception {
			sessionManager.add(session);
		}

		@Override
		public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
			sessionManager.remove(session);
		}

		@Override
		protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
			// Optional: echo user messages (not required)
			session.sendMessage(new TextMessage("You said: " + message.getPayload()));
		}
	}


	@Component
	public static class SessionManager {
		private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

		public void add(WebSocketSession session) {
			sessions.add(session);
		}

		public void remove(WebSocketSession session) {
			sessions.remove(session);
		}

		public Set<WebSocketSession> getSessions() {
			return Collections.unmodifiableSet(sessions);
		}
	}

	/** we do not modify or even read the data, we just pass the json record from kafka to
	 *  all connected clients */
	@Component
	public static class MarketDataKafkaConsumer {

	    @Autowired
	    private SessionManager sessionManager;

	    @KafkaListener(topics = "market-data", groupId = "market-data-group")
	    public void listen(ConsumerRecord<String, String> record) {
	        String json = record.value();
	    	System.out.println( "read data");
	        for (WebSocketSession session : sessionManager.getSessions()) {
	            try {
	                session.sendMessage(new TextMessage(json));
	            	System.out.println( "wrote data");
	            } catch (Exception e) {
	            	e.printStackTrace();
	            }
	        }
	    }
	}


}
