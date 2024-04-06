package io.camunda.connector.nats.outbound.model;

import io.camunda.connector.generator.dsl.Property;
import io.camunda.connector.generator.java.annotation.TemplateProperty;
import io.camunda.connector.nats.model.NatsAuthentication;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record NatsConnectorRequest(
    @Valid
    NatsAuthentication authentication,

    @Valid
    @NotNull
    NatsMessage message,

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
    Map<String, Object> additionalProperties) {
}
