/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.connector.nats.outbound;

import static io.camunda.connector.nats.NatsConnectionOptions.getConnectionOptions;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.connector.api.annotation.OutboundConnector;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.json.ConnectorsObjectMapperSupplier;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import io.camunda.connector.generator.java.annotation.ElementTemplate;
import io.camunda.connector.nats.outbound.model.NatsConnectorRequest;
import io.camunda.connector.nats.outbound.model.NatsConnectorResponse;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.api.PublishAck;

@OutboundConnector(
    name = "NATS Producer",
    inputVariables = {
      "authentication",
      "connection",
      "subject",
      "message",
      "additionalProperties",
    },
    type = "io.camunda:connector-nats:1")
@ElementTemplate(
    id = "io.camunda.connectors.NATS.v1",
    name = "NATS Outbound Connector",
    description = "Produce NATS message",
    inputDataClass = NatsConnectorRequest.class,
    version = 1,
    propertyGroups = {
      @ElementTemplate.PropertyGroup(id = "authentication", label = "Authentication"),
      @ElementTemplate.PropertyGroup(id = "connection", label = "Connection"),
      @ElementTemplate.PropertyGroup(id = "nats", label = "NATS"),
    },
    documentationRef = "",
    icon = "icon.svg")
public class NatsConnectorFunction implements OutboundConnectorFunction {

  private static final ObjectMapper objectMapper =
      ConnectorsObjectMapperSupplier.getCopy().enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);

  @Override
  public Object execute(final OutboundConnectorContext context) {
    var connectorRequest = context.bindVariables(NatsConnectorRequest.class);
    return executeConnector(connectorRequest, context);
  }

  private NatsConnectorResponse executeConnector(
      final NatsConnectorRequest request, final OutboundConnectorContext context) {
    String processIdAndVersion =
        context.getJobContext().getBpmnProcessId()
            + " v"
            + context.getJobContext().getProcessDefinitionVersion();
    Options connectionOptions =
        getConnectionOptions(request.authentication(), request.connection(), processIdAndVersion);
    try (Connection nc = Nats.connect(connectionOptions)) {
      PublishAck publishAck = null;
      switch (request.clientType()) {
        case JET_STREAM_CLIENT -> {
          JetStream js = nc.jetStream();
          publishAck = js.publish(request.subject(), request.message().getBytes());
          if (publishAck.isDuplicate()) {
            throw new ConnectorException(
                "DUPLICATE",
                "Error during Nats Producer execution; error message: [Duplicate message]");
          }

          return new NatsConnectorResponse(
              request.subject(),
              publishAck.getSeqno(),
              publishAck.getStream(),
              publishAck.getDomain());
        }
        case NATS_CLIENT -> nc.publish(request.subject(), request.message().getBytes());
      }

      return new NatsConnectorResponse(request.subject(), null, null, null);

    } catch (Exception e) {
      throw new ConnectorException(
          "FAIL",
          "Error during Nats Producer execution; error message: [" + e.getMessage() + "]",
          e);
    }
  }

  public static String transformData(Object data) throws JsonProcessingException {
    return data instanceof String ? (String) data : objectMapper.writeValueAsString(data);
  }
}
