/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.camunda.connector.runtime.core.inbound.correlation;

import io.camunda.connector.api.error.ConnectorCorrelationException;
import io.camunda.connector.api.error.ConnectorCorrelationException.CorrelationErrorReason;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.error.ConnectorInputException;
import io.camunda.connector.feel.FeelEngineWrapper;
import io.camunda.connector.feel.FeelEngineWrapperException;
import io.camunda.connector.runtime.core.ConnectorHelper;
import io.camunda.connector.runtime.core.inbound.InboundConnectorDefinitionImpl;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.command.ClientStatusException;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.client.api.response.PublishMessageResponse;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Component responsible for calling Zeebe to report an inbound event */
public class InboundCorrelationHandler {

  private static final Logger LOG = LoggerFactory.getLogger(InboundCorrelationHandler.class);

  private final ZeebeClient zeebeClient;
  private final FeelEngineWrapper feelEngine;

  public InboundCorrelationHandler(ZeebeClient zeebeClient, FeelEngineWrapper feelEngine) {
    this.zeebeClient = zeebeClient;
    this.feelEngine = feelEngine;
  }

  public void correlate(InboundConnectorDefinitionImpl definition, Object variables) {
    correlate(definition, variables, null);
  }

  public void correlate(
      InboundConnectorDefinitionImpl definition, Object variables, String messageId) {

    var correlationPoint = definition.correlationPoint();

    if (correlationPoint instanceof StartEventCorrelationPoint startCorPoint) {
      triggerStartEvent(definition, startCorPoint, variables);
    } else if (correlationPoint instanceof MessageCorrelationPoint msgCorPoint) {
      triggerMessage(
          definition,
          msgCorPoint.messageName(),
          msgCorPoint.correlationKeyExpression(),
          variables,
          resolveMessageId(msgCorPoint.messageIdExpression(), messageId, variables));
    } else if (correlationPoint instanceof MessageStartEventCorrelationPoint msgStartCorPoint) {
      triggerMessageStartEvent(definition, msgStartCorPoint, variables);
    } else if (correlationPoint
        instanceof BoundaryEventCorrelationPoint boundaryEventCorrelationPoint) {
      triggerMessage(
          definition,
          boundaryEventCorrelationPoint.messageName(),
          boundaryEventCorrelationPoint.correlationKeyExpression(),
          variables,
          resolveMessageId(
              boundaryEventCorrelationPoint.messageIdExpression(), messageId, variables));
    } else {
      throw new ConnectorException(
          "Process correlation point "
              + correlationPoint.getClass()
              + " is not supported by Runtime");
    }
  }

  protected void triggerStartEvent(
      InboundConnectorDefinitionImpl definition,
      StartEventCorrelationPoint correlationPoint,
      Object variables) {

    if (!isActivationConditionMet(definition, variables)) {
      LOG.info("Activation condition didn't match: {}", correlationPoint);
      return;
    }
    Object extractedVariables = extractVariables(variables, definition);

    try {
      ProcessInstanceEvent result =
          zeebeClient
              .newCreateInstanceCommand()
              .bpmnProcessId(correlationPoint.bpmnProcessId())
              .version(correlationPoint.version())
              .tenantId(definition.tenantId())
              .variables(extractedVariables)
              .send()
              .join();

      LOG.info("Created a process instance with key" + result.getProcessInstanceKey());

    } catch (ClientStatusException e1) {
      LOG.info("Failed to publish message: ", e1);
      throw new ConnectorCorrelationException(CorrelationErrorReason.FAULT_ZEEBE_CLIENT_STATUS, e1);
    } catch (Exception e2) {
      throw new ConnectorCorrelationException(
          "Failed to publish process message for subscription: " + correlationPoint, e2);
    }
  }

  protected void triggerMessageStartEvent(
      InboundConnectorDefinitionImpl definition,
      MessageStartEventCorrelationPoint correlationPoint,
      Object variables) {

    if (!isActivationConditionMet(definition, variables)) {
      LOG.info("Activation condition didn't match: {}", correlationPoint);
      return;
    }

    String messageId = extractMessageId(correlationPoint.messageIdExpression(), variables);
    if (correlationPoint.messageIdExpression() != null
        && !correlationPoint.messageIdExpression().isBlank()
        && messageId == null) {
      LOG.debug(
          "Wasn't able to obtain idempotency key for expression {}.",
          correlationPoint.messageIdExpression());
      throw new ConnectorCorrelationException(CorrelationErrorReason.FAULT_IDEMPOTENCY_KEY);
    }

    Object extractedVariables = extractVariables(variables, definition);

    try {
      var correlationKey =
          extractCorrelationKey(correlationPoint.correlationKeyExpression(), variables);
      PublishMessageResponse result =
          zeebeClient
              .newPublishMessageCommand()
              .messageName(correlationPoint.messageName())
              // correlation key must be empty to start a new process, see:
              // https://docs.camunda.io/docs/components/modeler/bpmn/message-events/#message-start-events
              .correlationKey(correlationKey.orElse(""))
              .messageId(messageId)
              .tenantId(definition.tenantId())
              .variables(extractedVariables)
              .send()
              .join();

      LOG.info("Published message with key: " + result.getMessageKey());

    } catch (ClientStatusException e1) {
      LOG.info("Failed to publish message: ", e1);
      throw new ConnectorCorrelationException(CorrelationErrorReason.FAULT_ZEEBE_CLIENT_STATUS, e1);
    } catch (Exception e2) {
      throw new ConnectorCorrelationException(
          "Failed to publish process message for subscription: " + correlationPoint, e2);
    }
  }

  protected void triggerMessage(
      InboundConnectorDefinitionImpl definition,
      String messageName,
      String correlationKeyExpression,
      Object variables,
      String messageId) {
    if (!isActivationConditionMet(definition, variables)) {
      LOG.info("Activation condition didn't match: {}", definition.correlationPoint());
      return;
    }
    String correlationKey =
        extractCorrelationKey(correlationKeyExpression, variables)
            .orElseThrow(
                () ->
                    new ConnectorException(
                        "Correlation key not resolved: " + correlationKeyExpression));

    Object extractedVariables = extractVariables(variables, definition);
    try {
      PublishMessageResponse response =
          zeebeClient
              .newPublishMessageCommand()
              .messageName(messageName)
              .correlationKey(correlationKey)
              .messageId(messageId)
              .tenantId(definition.tenantId())
              .variables(extractedVariables)
              .send()
              .join();

      LOG.info("Published message with key: " + response.getMessageKey());
    } catch (ClientStatusException e1) {
      LOG.info("Failed to publish message: ", e1);
      throw new ConnectorCorrelationException(CorrelationErrorReason.FAULT_ZEEBE_CLIENT_STATUS, e1);
    } catch (Exception e2) {
      throw new ConnectorCorrelationException(
          "Failed to publish process message for subscription: " + definition.correlationPoint(),
          e2);
    }
  }

  protected boolean isActivationConditionMet(
      InboundConnectorDefinitionImpl definition, Object context) {

    var maybeCondition = definition.activationCondition();
    if (maybeCondition == null || maybeCondition.isBlank()) {
      LOG.debug("No activation condition specified for connector");
      return true;
    }
    try {
      Object shouldActivate = feelEngine.evaluate(maybeCondition, context);
      return Boolean.TRUE.equals(shouldActivate);
    } catch (FeelEngineWrapperException e) {
      throw new ConnectorInputException(e);
    }
  }

  protected Optional<String> extractCorrelationKey(
      String correlationKeyExpression, Object context) {
    Optional<String> correlationKey;
    if (correlationKeyExpression != null && !correlationKeyExpression.isBlank()) {
      try {
        correlationKey =
            Optional.ofNullable(
                feelEngine.evaluate(correlationKeyExpression, context, String.class));
      } catch (Exception e) {
        correlationKey = Optional.empty();
      }
    } else {
      correlationKey = Optional.empty();
    }
    return correlationKey;
  }

  protected String extractMessageId(String messageIdExpression, Object context) {
    if (messageIdExpression == null || messageIdExpression.isBlank()) {
      return "";
    }
    try {
      return feelEngine.evaluate(messageIdExpression, context, String.class);
    } catch (Exception e) {
      throw new ConnectorInputException(e);
    }
  }

  protected Object extractVariables(
      Object rawVariables, InboundConnectorDefinitionImpl definition) {
    return ConnectorHelper.createOutputVariables(
        rawVariables, definition.resultVariable(), definition.resultExpression());
  }

  private String resolveMessageId(String messageIdExpression, String messageId, Object context) {
    if (messageId == null) {
      if (messageIdExpression != null) {
        return extractMessageId(messageIdExpression, context);
      } else {
        return UUID.randomUUID().toString();
      }
    }
    return messageId;
  }
}
