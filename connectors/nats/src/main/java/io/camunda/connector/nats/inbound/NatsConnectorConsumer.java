package io.camunda.connector.nats.inbound;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.scala.DefaultScalaModule$;
import io.nats.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.camunda.connector.api.inbound.Health;
import io.camunda.connector.api.inbound.InboundConnectorContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NatsConnectorConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(NatsConnectorConsumer.class);

    private final InboundConnectorContext context;

    private final ExecutorService executorService;

    public CompletableFuture<?> future;

    private Connection connection;
    private JetStreamSubscription jetStreamSubscription;

    NatsConnectorProperties elementProps;

    private Health consumerStatus = Health.up();

    boolean shouldLoop = true;

    public static ObjectMapper objectMapper =
        new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(DefaultScalaModule$.MODULE$)
            .registerModule(new JavaTimeModule())
            // deserialize unknown types as empty objects
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature());

    private ObjectReader avroObjectReader;

    public NatsConnectorConsumer(
        final InboundConnectorContext connectorContext,
        final NatsConnectorProperties elementProps) {
        this.context = connectorContext;
        this.elementProps = elementProps;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void startConsumer() {
        this.future =
            CompletableFuture.runAsync(
                () -> {
                    prepareConsumer();
                    consume();
                },
                this.executorService);
    }

    private void prepareConsumer() {
        Options.Builder builder = new Options.Builder();
        builder.server(elementProps.servers);

        switch (elementProps.authentication.type()) {
            case USERNAME_PASSWORD ->
                builder.userInfo(elementProps.authentication.username(), elementProps.authentication.password());
            case TOKEN -> builder.token(elementProps.authentication.token().toCharArray());
            case JWT -> builder.authHandler(new AuthHandler() {
                @Override
                public byte[] sign(byte[] bytes) {
                    return new byte[0];
                }

                public char[] getID() {
                    return elementProps.authentication.nKeySeed().toCharArray();
                }

                public char[] getJWT() {
                    return elementProps.authentication.jwt().toCharArray();
                }
            });
        }

        try (Connection nc = Nats.connect(builder.build())) {
            this.connection = nc;
            JetStream jetStream = nc.jetStream();
            // TODO: streaming
            // NOTE: This is for polling
            this.jetStreamSubscription = jetStream.subscribe(elementProps.subject);
            reportUp();
        } catch (Exception ex) {
            LOG.error("Failed to initialize connector: {}", ex.getMessage());
            context.reportHealth(Health.down(ex));
            try {
                throw ex;
            } catch (IOException | InterruptedException | JetStreamApiException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void consume() {
        while (shouldLoop) {
            try {
                pollAndPublish();
                reportUp();
            } catch (Exception ex) {
                reportDown(ex);
                throw ex;
            }
        }
        LOG.debug("Kafka inbound loop finished");
    }

    private void pollAndPublish() {
        LOG.debug("Polling the subject: {}", jetStreamSubscription.getSubject());
        try {
            Message nextMessage = jetStreamSubscription.nextMessage(Duration.ofMillis(500));
            handleMessage(nextMessage);
            nextMessage.ack();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleMessage(Message message) {
        LOG.trace("NATS message received: subject = {}, value = {}", message.getSubject(), new String(message.getData(), StandardCharsets.UTF_8));
        try {
            NatsInboundMessage mappedMessage = new NatsInboundMessage(message.getConnection().getConnectedUrl(), message.getSubject(), new String(message.getData(), StandardCharsets.UTF_8));
            this.context.correlateWithResult(mappedMessage);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void stopConsumer() throws ExecutionException, InterruptedException {
        if (this.future != null && !this.future.isDone()) {
            this.future.get();
        }
        if (this.executorService != null) {
            this.executorService.shutdownNow();
        }
    }

    private void reportUp() {
        var details = new HashMap<String, Object>();
        details.put("url", connection.getConnectedUrl());
        details.put("servers", connection.getServers());
        // TODO : if polling
        details.put("subject", jetStreamSubscription.getSubject());
        var newStatus = Health.up(details);
        if (!newStatus.equals(consumerStatus)) {
            consumerStatus = newStatus;
            context.reportHealth(Health.up(details));
            LOG.info(
                "NATS Consumer status changed to UP, process {}, version {}, element {} ",
                context.getDefinition().bpmnProcessId(),
                context.getDefinition().version(),
                context.getDefinition().elementId());
        }
    }

    private void reportDown(Throwable error) {
        var newStatus = Health.down(error);
        if (!newStatus.equals(consumerStatus)) {
            consumerStatus = newStatus;
            context.reportHealth(Health.down(error));
            LOG.error(
                "NATS Consumer status changed to DOWN, process {}, version {}, element {}",
                context.getDefinition().bpmnProcessId(),
                context.getDefinition().version(),
                context.getDefinition().elementId(),
                error);
        }
    }
}