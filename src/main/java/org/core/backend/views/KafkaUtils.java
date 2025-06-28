package org.utils.backend.utils;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import io.vertx.kafka.client.producer.RecordMetadata;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.vertx.kafka.client.serialization.JsonObjectSerializer;
import io.vertx.kafka.client.serialization.JsonObjectDeserializer;

public class KafkaUtils {
    private static final Logger logger = LoggerFactory.getLogger(KafkaUtils.class);

    private final Vertx vertx;
    private final JsonObject config;
    private KafkaProducer<String, JsonObject> producer;
    private final Map<String, KafkaConsumer<String, JsonObject>> consumers = new ConcurrentHashMap<>();
    private boolean initialized = false;

    public KafkaUtils(Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.config = config;
    }

    public Future<Void> initializeProducer() {
        try {
            Map<String, String> producerProps = getProducerConfig();
            this.producer = KafkaProducer.create(vertx, producerProps,
                new StringSerializer(), new JsonObjectSerializer());
            return Future.succeededFuture();
        } catch (Exception e) {
            return Future.failedFuture(e);
        }
    }

    private Map<String, String> getProducerConfig() {
        Map<String, String> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
            config.getString("bootstrap.servers", "localhost:9092"));
        props.put(ProducerConfig.ACKS_CONFIG,
            config.getString("acks", "1"));
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG,
            config.getString("max.block.ms", "60000"));
        props.put("key.serializer",
            "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer",
            "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("key.deserializer",
            "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer",
            "org.apache.kafka.common.serialization.StringDeserializer");


        // configureSecurity(props);
        return props;
    }

    private Map<String, String> getConsumerConfig(String groupId) {
        Map<String, String> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
            config.getString("bootstrap.servers", "localhost:9092"));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
            config.getString("auto.offset.reset", "latest"));
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG,
            config.getString("max.poll.records", "500"));
        props.put("key.serializer",
            "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer",
            "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("key.deserializer",
            "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer",
            "org.apache.kafka.common.serialization.StringDeserializer");

        // configureSecurity(props);
        return props;
    }

    public Future<Void> createConsumer(String topic) {
        String groupId = config.getString("group.id", "vertx-consumer-group") + "-" + topic;
        Map<String, String> consumerProps = getConsumerConfig(groupId);

        KafkaConsumer<String, JsonObject> consumer = KafkaConsumer.create(vertx,
            consumerProps, new StringDeserializer(), new JsonObjectDeserializer());

        consumers.put(topic, consumer);

        Promise<Void> promise = Promise.promise();
        consumer.subscribe(Collections.singleton(topic))
            .onComplete(ar -> {
                if (ar.succeeded()) {
                    logger.info("Successfully subscribed to topic: {}", topic);
                    promise.complete();
                } else {
                    logger.error("Failed to subscribe to topic: {}", topic, ar.cause());
                    promise.fail(ar.cause());
                }
            });

        return promise.future();
    }

    public Future<RecordMetadata> send(String topic, JsonObject message) {
        return send(topic, null, message);
    }

    public Future<RecordMetadata> send(String topic, String key, JsonObject message) {
        if (!initialized || producer == null) {
            return Future.failedFuture("Kafka producer not initialized");
        }

        KafkaProducerRecord<String, JsonObject> record =
            KafkaProducerRecord.create(topic, key, message);
        return producer.send(record);
    }

    public void registerHandler(String topic, Handler<KafkaConsumerRecord<String, JsonObject>> handler) {
        if (!consumers.containsKey(topic)) {
            logger.warn("No consumer exists for topic {}", topic);
            return;
        }

        consumers.get(topic).handler(record -> {
            try {
                handler.handle(record);
                consumers.get(topic).commit()
                    .onFailure(e -> logger.error("Failed to commit offset for topic {}", topic, e));
            } catch (Exception e) {
                logger.error("Error processing message from topic {}", topic, e);
            }
        });
    }

    public Future<Void> close() {
        List<Future> closeFutures = new ArrayList<>();

        if (producer != null) {
            closeFutures.add(producer.close());
        }

        consumers.forEach((topic, consumer) -> {
            closeFutures.add(consumer.close());
        });

        return CompositeFuture.all(closeFutures)
            .onComplete(r -> {
                initialized = false;
                consumers.clear();
            })
            .mapEmpty();
    }

    public Future<Boolean> healthCheck() {
        if (!initialized) {
            return Future.succeededFuture(false);
        }

        if (producer != null) {
            return send("health-check-topic", new JsonObject().put("timestamp", System.currentTimeMillis()))
                .map(r -> true)
                .otherwise(false);
        }
        return Future.succeededFuture(false);
    }
}