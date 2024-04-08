/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.connector.nats.inbound;

import io.camunda.connector.feel.annotation.FEEL;
import io.camunda.connector.generator.java.annotation.TemplateProperty;
import io.camunda.connector.nats.model.NatsAuthentication;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record NatsConnectorProperties(
    @Valid @NotNull NatsAuthentication authentication,
    @FEEL
        @NotEmpty
        @TemplateProperty(
            group = "nats",
            label = "Servers",
            description = "Provide connection server(s), comma-delimited if there are multiple")
        String servers,
    @NotEmpty
        @TemplateProperty(
            group = "nats",
            label = "Subject",
            description = "NATS subject to subscribe to")
        String subject,
    @TemplateProperty(
            label = "Queue Group",
            description = "Optional queue group name for load balancing",
            group = "nats",
            optional = true)
        String queueGroup,
    @TemplateProperty(
            label = "Durable Name",
            description = "Optional durable name for consumer",
            group = "nats",
            optional = true)
        String durableName,
    @FEEL
        @TemplateProperty(
            label = "Additional options",
            description = "Provide additional NATS consumer options in JSON",
            group = "nats",
            optional = false)
        Map<String, Object> additionalOptions,
    @FEEL
        @TemplateProperty(
            label = "Activation condition",
            description =
                "Condition under which the connector triggers. Leave empty to catch all events",
            group = "activation",
            optional = false)
        String activationCondition,
    @TemplateProperty(
            label = "Result variable",
            description = "Name of variable to store the result of the connector in",
            group = "output",
            optional = true)
        String resultVariable,
    @FEEL
        @TemplateProperty(
            label = "Result expression",
            description = "Expression to map the inbound payload to process variables",
            group = "output",
            optional = false,
            type = TemplateProperty.PropertyType.Text)
        String resultExpression) {}
