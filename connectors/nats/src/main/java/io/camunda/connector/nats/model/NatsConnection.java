/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.connector.nats.model;

import io.camunda.connector.feel.annotation.FEEL;
import io.camunda.connector.generator.java.annotation.TemplateProperty;
import jakarta.validation.constraints.NotEmpty;

public record NatsConnection(
    @FEEL
        @NotEmpty
        @TemplateProperty(
            group = "connection",
            label = "Servers",
            description = "Provide connection server(s), comma-delimited if there are multiple")
        String servers,
    @TemplateProperty(
            group = "connection",
            label = "Connection name",
            description =
                "Provide a name for the connection if left empty processInstanceKey will be used",
            optional = true)
        String connectionName) {}
