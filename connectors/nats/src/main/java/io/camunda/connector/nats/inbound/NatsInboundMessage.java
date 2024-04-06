package io.camunda.connector.nats.inbound;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Objects;

public class NatsInboundMessage {
    private final String url;
    private final String subject;
    private final Object message;

    public NatsInboundMessage(String url, String subject, String message) throws JsonProcessingException {
        this.url = url;
        this.subject = subject;
        ObjectMapper mapper = new ObjectMapper();
        this.message = mapper.readTree(message);
    }

    public String getTopic() {
        return subject;
    }

    public String getUrl() {
        return url;
    }

    public Object getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        System.out.println("checking...");
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NatsInboundMessage that = (NatsInboundMessage) o;
        return Objects.equals(subject, that.subject);
    }

    @Override
    public String toString() {
        return "NATSSubscriptionEvent{" +
            "subject='" + subject + '\'' +
            '}';
    }
}
