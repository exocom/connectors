package io.camunda.connector.nats.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum AuthenticationType {
  @JsonProperty("none")
  NONE,
  @JsonProperty("username_password")
  USERNAME_PASSWORD,
  @JsonProperty("token")
  TOKEN,
  @JsonProperty("jwt")
  JWT,
}
