/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.connector.nats;

import io.camunda.connector.nats.model.NatsAuthentication;
import io.camunda.connector.nats.model.NatsConnection;
import io.nats.client.AuthHandler;
import io.nats.client.NKey;
import io.nats.client.Options;
import io.nats.client.support.SSLUtils;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;
import javax.net.ssl.SSLContext;

public class NatsConnectionOptions {

  public static Options getConnectionOptions(
      NatsAuthentication authentication, NatsConnection connection, String processIdAndVersion) {
    SSLContext ctx = null;
    try {
      ctx = SSLUtils.createTrustAllTlsContext();
    } catch (GeneralSecurityException e) {
      throw new RuntimeException(e);
    }

    String connectionName =
        Optional.ofNullable(connection.connectionName()).orElse(processIdAndVersion);
    Options.Builder builder =
        new Options.Builder()
            .server(connection.servers())
            .connectionName(connectionName)
            .sslContext(ctx);

    switch (authentication.type()) {
      case USERNAME_PASSWORD -> builder.userInfo(
          authentication.username(), authentication.password());
      case TOKEN -> builder.token(authentication.token().toCharArray());
      case JWT -> {
        NKey nKey = NKey.fromSeed(authentication.nKeySeed().toCharArray());
        builder.authHandler(
            new AuthHandler() {
              public char[] getID() {
                try {
                  return nKey.getPublicKey();
                } catch (GeneralSecurityException | IOException | NullPointerException ex) {
                  return null;
                }
              }

              public byte[] sign(byte[] nonce) {
                try {
                  return nKey.sign(nonce);
                } catch (GeneralSecurityException | IOException | NullPointerException ex) {
                  return null;
                }
              }

              public char[] getJWT() {
                return authentication.jwt().toCharArray();
              }
            });
      }
    }

    return builder.build();
  }
}
