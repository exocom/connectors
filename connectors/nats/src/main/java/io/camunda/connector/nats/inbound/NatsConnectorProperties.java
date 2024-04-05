package io.camunda.connector.nats.inbound;

import io.camunda.connector.api.annotation.Secret;
import io.camunda.connector.feel.annotation.FEEL;
import io.camunda.connector.generator.java.annotation.TemplateProperty;
import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

public class NatsConnectorProperties {
    @NotEmpty
    @TemplateProperty(
            label = "Servers",
            description = "Comma-separated list of NATS servers (e.g., nats://localhost:4222)",
            group = "nats",
            type = TemplateProperty.PropertyType.Text)
    private String servers;

    @NotEmpty
    @TemplateProperty(label = "Subject", description = "NATS subject to subscribe to", group = "nats")
    private String subject;

    @TemplateProperty(
            label = "Queue Group",
            description = "Optional queue group name for load balancing",
            group = "nats",
            optional = true)
    private String queueGroup;

    @TemplateProperty(
            label = "Durable Name",
            description = "Optional durable name for consumer",
            group = "nats",
            optional = true)
    private String durableName;

    @FEEL
    @TemplateProperty(
            label = "Additional options",
            description = "Provide additional NATS consumer options in JSON",
            group = "nats",
            optional = true,
            feel = TemplateProperty.FeelMode.required)
    private Map<String, Object> additionalOptions;

    @FEEL
    @TemplateProperty(
            label = "Activation condition",
            description = "Condition under which the connector triggers. Leave empty to catch all events",
            group = "activation",
            optional = true,
            feel = TemplateProperty.FeelMode.required)
    private String activationCondition;

    @TemplateProperty(
            label = "Result variable",
            description = "Name of variable to store the result of the connector in",
            group = "variable-mapping",
            optional = true)
    private String resultVariable;

    @FEEL
    @TemplateProperty(
            label = "Result expression",
            description = "Expression to map the inbound payload to process variables",
            group = "variable-mapping",
            type = TemplateProperty.PropertyType.Text,
            feel = TemplateProperty.FeelMode.required)
    private String resultExpression;

    // Getters and setters for all properties

}