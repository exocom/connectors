/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.connector.nats.inbound;

import static io.camunda.connector.nats.NatsConnectionOptions.getConnectionOptions;

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
import io.camunda.connector.api.inbound.Health;
import io.camunda.connector.api.inbound.InboundConnectorContext;
import io.camunda.connector.nats.model.ClientType;
import io.nats.client.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NatsConnectorConsumer {
  private static final Logger LOG = LoggerFactory.getLogger(NatsConnectorConsumer.class);

  private final InboundConnectorContext context;

  private final ExecutorService executorService;

  public CompletableFuture<?> future;

  private Connection connection;
  private ClientType clientType;

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
      final InboundConnectorContext connectorContext, final NatsConnectorProperties elementProps) {
    this.context = connectorContext;
    this.elementProps = elementProps;
    this.executorService = Executors.newSingleThreadExecutor();
  }

  public void startConsumer() {
    this.future =
        CompletableFuture.runAsync(
            () -> {
              clientType = elementProps.subscription().clientType();
              String processIdAndVersion =
                  context.getDefinition().bpmnProcessId()
                      + " v"
                      + context.getDefinition().version();
              Options connectionOptions =
                  getConnectionOptions(
                      elementProps.authentication(),
                      elementProps.connection(),
                      processIdAndVersion);
              if (elementProps.subscription().clientType() == ClientType.JET_STREAM_CLIENT) {
                consumeJetStream(connectionOptions);
              }
            },
            this.executorService);
  }

  private void consumeJetStream(Options options) {
    try (Connection nc = Nats.connect(options)) {
      this.connection = nc;
      JetStream jetstream = nc.jetStream();
      reportUp();

      try {
        String stream = elementProps.subscription().stream();
        String consumerName = elementProps.subscription().consumerName();
        ConsumerContext consumerContext = jetstream.getConsumerContext(stream, consumerName);

        try (IterableConsumer consumer = consumerContext.iterate()) {
          while (shouldLoop) {
            try {
              Message nextMessage = consumer.nextMessage(Duration.ofMillis(500));
              if (nextMessage == null) {
                continue;
              }
              handleMessage(nextMessage);
              nextMessage.ack();
            } catch (Exception ex) {
              reportDown(ex);
              throw new RuntimeException(ex);
            }
          }
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      } catch (IOException | JetStreamApiException e) {
        throw new RuntimeException(e);
      }
    } catch (Exception ex) {
      LOG.error("Failed to initialize connector: {}", ex.getMessage());
      context.reportHealth(Health.down(ex));
      try {
        throw ex;
      } catch (IOException | InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void handleMessage(Message message) {
    LOG.trace(
        "NATS message received: subject = {}, value = {}",
        new String(message.getData(), StandardCharsets.UTF_8));
    try {
      NatsInboundMessage mappedMessage =
          new NatsInboundMessage(
              message.getConnection().getConnectedUrl(),
              message.getSubject(),
              new String(message.getData(), StandardCharsets.UTF_8),
              objectMapper);
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
    //    details.put("url", connection.getConnectedUrl());
    //    details.put("servers", connection.getServers());
    //    if (clientType == ClientType.JET_STREAM_CLIENT) {
    //      details.put("stream", elementProps.subscription().stream());
    //      details.put("consumerName", elementProps.subscription().consumerName());
    //    }
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
