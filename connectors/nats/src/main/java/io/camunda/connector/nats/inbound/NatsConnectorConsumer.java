/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
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
import io.camunda.connector.api.inbound.Health;
import io.camunda.connector.api.inbound.InboundConnectorContext;
import io.camunda.connector.nats.model.ClientType;
import io.nats.client.*;
import io.nats.client.support.SSLUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.SSLContext;
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
    //              consume();
    this.future = CompletableFuture.runAsync(this::prepareConsumer, this.executorService);
  }

  private void prepareConsumer() {
    SSLContext ctx = null;
    try {
      ctx = SSLUtils.createTrustAllTlsContext();
    } catch (GeneralSecurityException e) {
      throw new RuntimeException(e);
    }

    String connectionName =
        Optional.ofNullable(elementProps.connection().connectionName())
            .orElse(
                String.valueOf(
                    context.getDefinition().bpmnProcessId()
                        + " v"
                        + context.getDefinition().version()));
    Options.Builder builder =
        new Options.Builder()
            .server(elementProps.connection().servers())
            .connectionName(connectionName)
            .sslContext(ctx);

    switch (elementProps.authentication().type()) {
      case USERNAME_PASSWORD -> builder.userInfo(
          elementProps.authentication().username(), elementProps.authentication().password());
      case TOKEN -> builder.token(elementProps.authentication().token().toCharArray());
      case JWT -> {
        NKey nKey = NKey.fromSeed(elementProps.authentication().nKeySeed().toCharArray());
        builder.authHandler(
            new AuthHandler() {
              public char[] getID() {
                try {
                  return nKey.getPublicKey();
                } catch (GeneralSecurityException | IOException | NullPointerException ex) {
                  return null;
                }
              }

              public byte[] sign(byte[] nonce) {
                try {
                  return nKey.sign(nonce);
                } catch (GeneralSecurityException | IOException | NullPointerException ex) {
                  return null;
                }
              }

              public char[] getJWT() {
                return elementProps.authentication().jwt().toCharArray();
              }
            });
      }
    }

    clientType = elementProps.subscription().clientType();
    try (Connection nc = Nats.connect(builder.build())) {
      this.connection = nc;
      if (elementProps.subscription().clientType() == ClientType.JET_STREAM_CLIENT) {
        JetStream jetstream = nc.jetStream();
        ////        JetStreamManagement jsm = nc.jetStreamManagement();
        ////        jsm.addStream(
        ////            StreamConfiguration.builder()
        ////                .name("stream")
        ////                .subjects(elementProps.subscription().subject())
        ////                .build());
        ////        jetStreamSubscription =
        ////            jsm.addConsumer(
        ////                ConsumerConfiguration.builder()
        ////                    .stream("stream")
        ////                    .durable(elementProps.subscription().durableName())
        ////                    .queue(elementProps.subscription().queueGroup())
        ////                    .build());
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

  //  public void consume() {
  //    if (clientType == ClientType.JET_STREAM_CLIENT) {
  //
  //    }
  //    //        while (shouldLoop) {
  //    //            try {
  //    //                if (clientType == ClientType.JET_STREAM_CLIENT) {
  //    //                    pollJetStream();
  //    //                }
  //    //                reportUp();
  //    //            } catch (Exception ex) {
  //    //                reportDown(ex);
  //    //                throw ex;
  //    //            }
  //    //        }
  //    //        LOG.debug("NATS inbound loop finished");
  //  }

  //  private void pollJetStream() {
  //    try {
  //      Message nextMessage = consumer.nextMessage(Duration.ofMillis(500));
  //      handleMessage(nextMessage);
  //      nextMessage.ack();
  //    } catch (InterruptedException
  //        | IOException
  //        | JetStreamApiException
  //        | JetStreamStatusCheckedException e) {
  //      throw new RuntimeException(e);
  //    }
  //  }

  private void handleMessage(Message message) {
    LOG.trace(
        "NATS message received: subject = {}, value = {}",
        new String(message.getData(), StandardCharsets.UTF_8));
    try {
      NatsInboundMessage mappedMessage =
          new NatsInboundMessage(
              message.getConnection().getConnectedUrl(),
              message.getSubject(),
              new String(message.getData(), StandardCharsets.UTF_8));
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
