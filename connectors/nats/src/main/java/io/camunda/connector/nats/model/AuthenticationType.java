package io.camunda.connector.nats.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum AuthenticationType {
  @JsonProperty("none")
  NONE,
  @JsonProperty("credentials")
  CREDENTIALS,
  @JsonProperty("token")
  TOKEN,
  @JsonProperty("jwt")
  JWT,
}
