package io.camunda.connector.nats.model;

import io.camunda.connector.generator.java.annotation.TemplateProperty;

public record NatsAuthentication(
    @TemplateProperty(
        label = "Authentication type",
        description = "Select the type of authentication to use for the connection",
        group = "authentication",
        type = TemplateProperty.PropertyType.Dropdown,
        defaultValue = "NONE",
        choices = {
            @TemplateProperty.DropdownPropertyChoice(label = "None", value = "none"),
            @TemplateProperty.DropdownPropertyChoice(label = "username/password", value = "username_password"),
            @TemplateProperty.DropdownPropertyChoice(label = "token", value = "token"),
            @TemplateProperty.DropdownPropertyChoice(label = "jwt/nkey", value = "jwt")
        }
    )
    AuthenticationType type,

    @TemplateProperty(
        label = "Username",
        description = "Provide the username (must have permissions to subscribe to the topic)",
        group = "authentication",
        optional = true,
        condition = @TemplateProperty.PropertyCondition(property = "authenticationType", equals = "username_password"))
    String username,

    @TemplateProperty(
        label = "Password",
        description = "Provide a password for the user",
        group = "authentication",
        optional = true,
        condition = @TemplateProperty.PropertyCondition(property = "authenticationType", equals = "username_password"))
    String password,

    @TemplateProperty(
        label = "Token",
        description = "Provide a token for authentication",
        group = "authentication",
        optional = true,
        condition = @TemplateProperty.PropertyCondition(property = "authenticationType", equals = "token"))
    String token,

    @TemplateProperty(
        label = "JWT",
        description = "user JWT for authentication",
        group = "authentication",
        optional = true,
        condition = @TemplateProperty.PropertyCondition(property = "authenticationType", equals = "jwt"))
    String jwt,

    @TemplateProperty(
        label = "NKey seed",
        description = "If a seed is provided, the public key, and signature are calculated",
        group = "authentication",
        type = TemplateProperty.PropertyType.String,
        optional = true,
        condition = @TemplateProperty.PropertyCondition(property = "authenticationType", equals = "jwt"))
    String nKeySeed

) {
  @Override
  public String toString() {
    return "NatsAuthentication{type='" + this.type + "', username='[REDACTED]', password='[REDACTED]', token='[REDACTED]', jwt='[REDACTED]', nKeySeed='[REDACTED]'}";
  }
}
