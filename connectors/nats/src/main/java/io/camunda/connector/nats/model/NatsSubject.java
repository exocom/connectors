/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.connector.nats.model;

import io.camunda.connector.generator.java.annotation.TemplateProperty;
import jakarta.validation.constraints.NotEmpty;

public record NatsSubject(
    @NotEmpty
    @TemplateProperty(
        group = "nats",
        label = "Subject",
        description = "NATS subject to subscribe to")
    String bootstrapServers) {
}
