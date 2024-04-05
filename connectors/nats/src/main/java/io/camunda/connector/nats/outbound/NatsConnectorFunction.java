package io.camunda.connector.nats.outbound;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.avro.AvroMapper;
import com.fasterxml.jackson.dataformat.avro.AvroSchema;
import io.camunda.connector.api.annotation.OutboundConnector;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.json.ConnectorsObjectMapperSupplier;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import io.camunda.connector.generator.java.annotation.ElementTemplate;
import org.apache.avro.Schema;
import org.apache.commons.text.StringEscapeUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@OutboundConnector(
    name = "NATS Producer",
    inputVariables = {
      "authentication",
      "topic",
      "message",
      "additionalProperties",
      "headers",
    },
    type = "io.camunda:connector-nats:1")
@ElementTemplate(
    id = "io.camunda.connectors.NATS.v1",
    name = "NATS Outbound Connector",
    description = "Produce NATS message",
    inputDataClass = NatsConnectorRequest.class,
    version = 4,
    propertyGroups = {
      @ElementTemplate.PropertyGroup(id = "authentication", label = "Authentication"),
      @ElementTemplate.PropertyGroup(id = "nats", label = "NATS"),
      @ElementTemplate.PropertyGroup(id = "message", label = "Message")
    },
    documentationRef = "",
    icon = "icon.svg")
public class NatsConnectorFunction implements OutboundConnectorFunction {

  private final Function<Properties, Producer<String, Object>> producerCreatorFunction;

  private static final ObjectMapper objectMapper =
      ConnectorsObjectMapperSupplier.getCopy().enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);

  public NatsConnectorFunction() {
    this(NatsProducer::new);
  }

  public NatsConnectorFunction(
      final Function<Properties, Producer<String, Object>> producerCreatorFunction) {
    this.producerCreatorFunction = producerCreatorFunction;
  }

  @Override
  public Object execute(final OutboundConnectorContext context) {
    var connectorRequest = context.bindVariables(NatsConnectorRequest.class);
    return executeConnector(connectorRequest);
  }

  public static byte[] produceAvroMessage(final NatsConnectorRequest request) throws Exception {
    var schemaString = StringEscapeUtils.unescapeJson(request.avro().schema());
    Schema raw = new Schema.Parser().setValidate(true).parse(schemaString);
    AvroSchema schema = new AvroSchema(raw);
    AvroMapper avroMapper = new AvroMapper();
    Object messageValue = request.message().value();
    if (messageValue instanceof String messageValueAsString) {
      messageValue = objectMapper.readTree(StringEscapeUtils.unescapeJson(messageValueAsString));
    }
    return avroMapper.writer(schema).writeValueAsBytes(messageValue);
  }

  private NatsConnectorResponse executeConnector(final NatsConnectorRequest request) {
    Properties props = NatsPropertiesUtil.assembleNatsClientProperties(request);
    try (Producer<String, Object> producer = producerCreatorFunction.apply(props)) {
      Map<String, String> headers =
          (request.headers() != null) ? request.headers() : new HashMap<>();
      ProducerRecord<String, Object> producerRecord = createProducerRecord(request);
      addHeadersToProducerRecord(producerRecord, headers);
      Future<RecordMetadata> natsResponse = producer.send(producerRecord);
      return constructNatsConnectorResponse(natsResponse.get(45, TimeUnit.SECONDS));
    } catch (Exception e) {
      throw new ConnectorException(
          "FAIL",
          "Error during Nats Producer execution; error message: [" + e.getMessage() + "]",
          e);
    }
  }

  private ProducerRecord<String, Object> createProducerRecord(final NatsConnectorRequest request)
      throws Exception {
    Object transformedValue;
    if (request.avro() != null) {
      transformedValue = produceAvroMessage(request);
    } else {
      transformedValue = transformData(request.message().value());
    }
    String transformedKey = transformData(request.message().key());
    return new ProducerRecord<>(
        request.topic().topicName(), null, null, transformedKey, transformedValue);
  }

  public static String transformData(Object data) throws JsonProcessingException {
    return data instanceof String ? (String) data : objectMapper.writeValueAsString(data);
  }

  private void addHeadersToProducerRecord(
      ProducerRecord<String, Object> producerRecord, Map<String, String> headers) {
    if (headers != null) {
      for (Map.Entry<String, String> header : headers.entrySet()) {
        producerRecord
            .headers()
            .add(header.getKey(), header.getValue().getBytes(StandardCharsets.UTF_8));
      }
    }
  }

  private NatsConnectorResponse constructNatsConnectorResponse(RecordMetadata recordMetadata) {
    return new NatsConnectorResponse(
        recordMetadata.topic(),
        recordMetadata.timestamp(),
        recordMetadata.offset(),
        recordMetadata.partition());
  }
}
