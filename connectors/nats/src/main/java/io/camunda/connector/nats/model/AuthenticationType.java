/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
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
