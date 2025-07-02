package kafk;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/** Read data from kafka and write it to all Websocket clients.
 *  Clients will get only the updates UNLESS we haven't received the initial response yet.
 *  We could not allow clients until after initial response is received.
 *  Also, initial response should not contain so much data, I think */
@SpringBootApplication
@EnableScheduling
public class MyKakfa {

	public static void main(String[] args) {
		System.out.println("SLF4J in use: " + LoggerFactory.getILoggerFactory().getClass());

		SpringApplication.run(MyKakfa.class, args);
	}


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

}
