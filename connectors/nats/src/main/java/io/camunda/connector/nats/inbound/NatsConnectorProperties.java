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
import io.camunda.connector.nats.model.NatsConnection;
import io.camunda.connector.nats.model.NatsSubscription;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record NatsConnectorProperties(
    @Valid @NotNull NatsAuthentication authentication,
    @Valid @NotNull NatsConnection connection,
    @Valid @NotNull NatsSubscription subscription,
    @FEEL
        @TemplateProperty(
            label = "Additional options",
            description = "Provide additional NATS consumer options in JSON",
            group = "nats")
        Map<String, Object> additionalOptions,
    @FEEL
        @TemplateProperty(
            label = "Activation condition",
            description =
                "Condition under which the connector triggers. Leave empty to catch all events",
            group = "activation")
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
            type = TemplateProperty.PropertyType.Text)
        String resultExpression) {}
