/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.camunda.connector.api.inbound;

/**
 * Base subscription interface for inbound connectors. This is an interface that
 * environment-specific Connector Runtime uses to control the inbound Connectors.
 *
 * <p>NB: For custom inbound Connectors implementation, please consider extending a more specific
 * type, such as {@link io.camunda.connector.impl.inbound.SubscriptionInboundConnector} rather than
 * this interface.
 */
public interface InboundConnectorExecutable {

  /**
   * Activation trigger for the subscription
   *
   * @param properties Properties to be used by subscription
   * @param context Runtime-specific information
   */
  void activate(InboundConnectorProperties properties, InboundConnectorContext context)
      throws Exception;

  /**
   * Gentle shutdown hook for inbound connectors. Must release all resources used by the
   * subscription.
   */
  void deactivate() throws Exception;
}