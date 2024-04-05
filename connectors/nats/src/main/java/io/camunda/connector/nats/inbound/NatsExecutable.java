package io.camunda.connector.nats.inbound;

import io.camunda.connector.api.annotation.InboundConnector;
import io.camunda.connector.api.inbound.Health;
import io.camunda.connector.api.inbound.InboundConnectorContext;
import io.camunda.connector.api.inbound.InboundConnectorExecutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;

@InboundConnector(name = "Nats Consumer", type = "io.camunda:connector-nats-inbound:1")
public class NatsExecutable implements InboundConnectorExecutable<InboundConnectorContext> {
    private static final Logger LOG = LoggerFactory.getLogger(NatsExecutable.class);
    private final Function<Properties, Consumer<Object, Object>> consumerCreatorFunction;
    public NatsConnectorConsumer natsConnectorConsumer;
    public JetStreamSubscription subscription;


    public NatsExecutable(
            final Function<Properties, Consumer<Object, Object>> consumerCreatorFunction) {
        this.consumerCreatorFunction = consumerCreatorFunction;
    }

    public NatsExecutable() {
        this(NatsConsumer::new);
    }

    @Override
    public void activate(InboundConnectorContext connectorContext) {
        try {
            KafkaConnectorProperties elementProps =
                    connectorContext.bindProperties(KafkaConnectorProperties.class);
            this.natsConnectorConsumer =
                    new NatsConnectorConsumer(consumerCreatorFunction, connectorContext, elementProps);
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
            this.kafkaConnectorConsumer.stopConsumer();
        } catch (Exception e) {
            LOG.error("Failed to cancel Connector execution: {}", e.getMessage());
        }
    }
}
