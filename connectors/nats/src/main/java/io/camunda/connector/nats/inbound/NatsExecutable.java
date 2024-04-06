package io.camunda.connector.nats.inbound;

import io.camunda.connector.api.annotation.InboundConnector;
import io.camunda.connector.api.inbound.Health;
import io.camunda.connector.api.inbound.InboundConnectorContext;
import io.camunda.connector.api.inbound.InboundConnectorExecutable;
import io.camunda.connector.generator.dsl.BpmnType;
import io.camunda.connector.generator.java.annotation.ElementTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@InboundConnector(name = "NATS Consumer", type = "io.camunda:connector-nats-inbound:1")
@ElementTemplate(
    id = "io.camunda.connectors.webhook",
    name = "NATS Event Connector",
    icon = "icon.svg",
    version = 1,
    inputDataClass = NatsConnectorProperties.class,
    description = "Consume NATS messages",
    documentationRef = "https://docs.nats.io/",
    propertyGroups = {
        @ElementTemplate.PropertyGroup(id = "authentication", label = "Authentication"),
        @ElementTemplate.PropertyGroup(id = "nats", label = "NATS"),
    },
    elementTypes = {
        @ElementTemplate.ConnectorElementType(
            appliesTo = BpmnType.START_EVENT,
            elementType = BpmnType.START_EVENT,
            templateIdOverride = "io.camunda.connectors.inbound.NATS.StartEvent.v1",
            templateNameOverride = "NATS Start Event Connector"),
        // TODO : correlation
        @ElementTemplate.ConnectorElementType(
            appliesTo = BpmnType.START_EVENT,
            elementType = BpmnType.MESSAGE_START_EVENT,
            templateIdOverride = "io.camunda.connectors.inbound.NATS.MessageStart.v1",
            templateNameOverride = "NATS Message Start Event Connector"),
        // TODO: correlation
        @ElementTemplate.ConnectorElementType(
            appliesTo = {BpmnType.INTERMEDIATE_THROW_EVENT, BpmnType.INTERMEDIATE_CATCH_EVENT},
            elementType = BpmnType.INTERMEDIATE_CATCH_EVENT,
            templateIdOverride = "io.camunda.connectors.inbound.NATS.Intermediate.v1",
            templateNameOverride = "NATS Intermediate Catch Event Connector"),
        // TODO: correlation
        @ElementTemplate.ConnectorElementType(
            appliesTo = BpmnType.BOUNDARY_EVENT,
            elementType = BpmnType.BOUNDARY_EVENT,
            templateIdOverride = "io.camunda.connectors.inbound.NATS.Boundary.v1",
            templateNameOverride = "NATS Boundary Event Connector")
    })
public class NatsExecutable implements InboundConnectorExecutable<InboundConnectorContext> {
    private static final Logger LOG = LoggerFactory.getLogger(NatsExecutable.class);
    private NatsConnectorConsumer natsConnectorConsumer;

    @Override
    public void activate(InboundConnectorContext connectorContext) {
        try {
            NatsConnectorProperties elementProps = connectorContext.bindProperties(NatsConnectorProperties.class);
            this.natsConnectorConsumer = new NatsConnectorConsumer(connectorContext, elementProps);
            this.natsConnectorConsumer.startConsumer();
        } catch (Exception ex) {
            connectorContext.reportHealth(Health.down(ex));
            throw ex;
        }
    }

    @Override
    public void deactivate() {
        LOG.info("Subscription deactivation requested by the Connector runtime");
        try {
            this.natsConnectorConsumer.stopConsumer();
        } catch (Exception e) {
            LOG.error("Failed to cancel Connector execution: {}", e.getMessage());
        }
    }
}
