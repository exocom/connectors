package io.camunda.connector.nats.inbound;

import io.camunda.connector.api.annotation.Secret;
import io.camunda.connector.feel.annotation.FEEL;
import io.camunda.connector.generator.java.annotation.TemplateProperty;
import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

public class NatsConnectorProperties {

    @TemplateProperty(
            label = "Authentication type",
            description = "username/password or token or jwt/nkey",
            group = "authentication",
            type = TemplateProperty.PropertyType.Dropdown,
            value = "NONE",
            choices = {
                    @TemplateProperty.DropdownPropertyChoice(name = "None", value = "NONE"),
                    @TemplateProperty.DropdownPropertyChoice(name = "username/password", value = "credentials"),
                    @TemplateProperty.DropdownPropertyChoice(name = "token", value = "token"),
                    @TemplateProperty.DropdownPropertyChoice(name = "jwt/nkey", value = "jwt")
            },
            binding = @TemplateProperty.PropertyBinding(type = "zeebe:property", name = "authenticationType")
    )
    private String authenticationType;

    @TemplateProperty(
            label = "Username",
            description = "Provide the username (must have permissions to subscribe to the topic)",
            group = "authentication",
            type = TemplateProperty.PropertyType.String,
            optional = true,
            binding = @TemplateProperty.PropertyBinding(type = "zeebe:property", name = "authentication.username"),
            condition = @TemplateProperty.PropertyCondition(property = "authenticationType", equals = "credentials")
    )
    private String username;

    @Secret
    @TemplateProperty(
            label = "Password",
            description = "Provide a password for the user",
            group = "authentication",
            type = TemplateProperty.PropertyType.String,
            optional = true,
            binding = @TemplateProperty.PropertyBinding(type = "zeebe:property", name = "authentication.password"),
            condition = @TemplateProperty.PropertyCondition(property = "authenticationType", equals = "credentials")
    )
    private String password;

    @Secret
    @TemplateProperty(
            label = "Token",
            description = "Provide a token for authentication",
            group = "authentication",
            type = TemplateProperty.PropertyType.String,
            optional = true,
            binding = @TemplateProperty.PropertyBinding(type = "zeebe:property", name = "authentication.token"),
            condition = @TemplateProperty.PropertyCondition(property = "authenticationType", equals = "token")
    )
    private String token;

    @TemplateProperty(
            label = "JWT",
            description = "user JWT for authentication",
            group = "authentication",
            type = TemplateProperty.PropertyType.String,
            optional = true,
            binding = @TemplateProperty.PropertyBinding(type = "zeebe:input", name = "authentication.jwt"),
            condition = @TemplateProperty.PropertyCondition(property = "authenticationType", equals = "jwt")
    )
    private String jwt;

    @Secret
    @TemplateProperty(
            label = "NKey seed",
            description = "If a seed is provided, the public key, and signature are calculated.",
            group = "authentication",
            type = TemplateProperty.PropertyType.String,
            optional = true,
            binding = @TemplateProperty.PropertyBinding(type = "zeebe:input", name = "authentication.nkey"),
            condition = @TemplateProperty.PropertyCondition(property = "authenticationType", equals = "jwt")
    )
    private String nkeySeed;

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

    @Secret
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