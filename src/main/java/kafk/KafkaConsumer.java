package kafk;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import kafk.KafkaMain.SessionManager;
import tw.util.S;

/** Read data from kafka and write it to all Websocket clients */
@Component
public class KafkaConsumer {

    @Autowired
    private SessionManager sessionManager;

    @KafkaListener(topics = "market-data", groupId = "market-data-group")
    public void listen(ConsumerRecord<String, String> record) {
        String json = record.value();

        S.out( "received " + json);

        // send data to all sessions
        for (WebSocketSession session : sessionManager.getSessions()) {
            try {
                session.sendMessage(new TextMessage(json));
            	System.out.println( "sent");
            } catch (Exception e) {
            	e.printStackTrace();
            }
        }
    }
}
