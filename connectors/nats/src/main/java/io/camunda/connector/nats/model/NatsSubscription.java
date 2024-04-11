/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.connector.nats.model;

import io.camunda.connector.generator.java.annotation.TemplateProperty;

public record NatsSubscription(
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
    @TemplateProperty(
            label = "Subject",
            description = "NATS subject",
            group = "subscription",
            optional = true,
            condition =
                @TemplateProperty.PropertyCondition(
                    property = "subscription.clientType",
                    equals = "nats"))
        String subject,
    @TemplateProperty(
            label = "Stream",
            description = "NATS stream name",
            group = "subscription",
            optional = true,
            condition =
                @TemplateProperty.PropertyCondition(
                    property = "subscription.clientType",
                    equals = "jetStream"))
        String stream,
    @TemplateProperty(
            label = "Consumer Name",
            description = "NATS consumer name",
            group = "subscription",
            optional = true,
            condition =
                @TemplateProperty.PropertyCondition(
                    property = "subscription.clientType",
                    equals = "jetStream"))
        String consumerName) {}
