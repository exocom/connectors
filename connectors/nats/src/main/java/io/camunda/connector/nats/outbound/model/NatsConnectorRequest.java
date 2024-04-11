/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.connector.nats.outbound.model;

import io.camunda.connector.generator.dsl.Property;
import io.camunda.connector.generator.java.annotation.TemplateProperty;
import io.camunda.connector.nats.model.ClientType;
import io.camunda.connector.nats.model.NatsAuthentication;
import io.camunda.connector.nats.model.NatsConnection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record NatsConnectorRequest(
    @Valid @NotNull NatsAuthentication authentication,
    @Valid @NotNull NatsConnection connection,
    @TemplateProperty(
            label = "Client type",
            description = "Select the type of client to use for the connection",
            group = "subscription",
            type = TemplateProperty.PropertyType.Dropdown,
            defaultValue = "nats",
            choices = {
              @TemplateProperty.DropdownPropertyChoice(label = "NATS", value = "nats"),
              @TemplateProperty.DropdownPropertyChoice(label = "JetStream", value = "jetStream"),
            })
        ClientType clientType,
    @NotEmpty
        @TemplateProperty(
            label = "Subject",
            description = "Provide the subject to send the message to",
            group = "nats")
        String subject,
    @NotEmpty
        @TemplateProperty(
            label = "Message",
            description = "Provide the message to send",
            group = "nats")
        String message,
    @TemplateProperty(
            group = "nats",
            label = "Headers",
            optional = true,
            feel = Property.FeelMode.required,
            description = "Provide NATS producer headers in JSON")
        Map<String, String> headers,
    @TemplateProperty(
            group = "nats",
            label = "Additional properties",
            optional = true,
            feel = Property.FeelMode.required,
            description = "Provide additional NATS producer properties in JSON")
        Map<String, Object> additionalProperties) {}
