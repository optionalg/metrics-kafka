package ly.stealth.kafka.metrics;

import com.codahale.metrics.*;
import com.google.gson.Gson;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.SortedMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class KafkaReporter extends ScheduledReporter {
    private static final Logger log = LoggerFactory.getLogger(KafkaReporter.class);

    private final Producer<String, String> kafkaProducer;
    private final String kafkaTopic;
    private final Gson mapper = new Gson();

    private KafkaReporter(MetricRegistry registry,
                          String name,
                          MetricFilter filter,
                          TimeUnit rateUnit,
                          TimeUnit durationUnit,
                          String kafkaTopic,
                          Properties kafkaProperties) {
        super(registry, name, filter, rateUnit, durationUnit);
        this.kafkaTopic = kafkaTopic;
        kafkaProducer = new Producer<String, String>(new ProducerConfig(kafkaProperties));
    }

    @Override
    public synchronized void report(SortedMap<String, Gauge> gauges,
                                    SortedMap<String, Counter> counters,
                                    SortedMap<String, Histogram> histograms,
                                    SortedMap<String, Meter> meters,
                                    SortedMap<String, Timer> timers) {
        log.info("Trying to report metrics to Kafka kafkaTopic {}", kafkaTopic);
        String report = mapper.toJson(new KafkaMetricsReport(gauges, counters, histograms, meters, timers));
        log.debug("Created metrics report: {}", report);
        kafkaProducer.send(new KeyedMessage<String, String>(kafkaTopic, report));
        log.info("Metrics were successfully reported to Kafka kafkaTopic {}", kafkaTopic);
    }

    public static Builder builder(MetricRegistry registry, String brokerList, String kafkaTopic) {
        return new Builder(registry, kafkaTopic, brokerList);
    }

    public static class Builder {
        private MetricRegistry registry;
        private String kafkaTopic;
        private String brokerList;

        private String clientId = UUID.randomUUID().toString();
        private boolean synchronously = true;
        private String compressionCodec = "gzip";
        private int batchSize = 200;
        private int messageSendMaxRetries = 3;
        private int requestRequiredAcks = -1;
        private String name = "KafkaReporter";
        private MetricFilter filter;
        private TimeUnit rateUnit;
        private TimeUnit durationUnit;

        public Builder(MetricRegistry registry, String topic, String brokerList) {
            this.registry = registry;
        }

        public String getKafkaTopic() {
            return kafkaTopic;
        }

        public Builder setKafkaTopic(String kafkaTopic) {
            this.kafkaTopic = kafkaTopic;
            return this;
        }

        public String getBrokerList() {
            return brokerList;
        }

        public Builder setBrokerList(String brokerList) {
            this.brokerList = brokerList;
            return this;
        }

        public String getClientId() {
            return clientId;
        }

        public Builder setClientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public boolean isSynchronously() {
            return synchronously;
        }

        public Builder setSynchronously(boolean synchronously) {
            this.synchronously = synchronously;
            return this;
        }

        public String getCompressionCodec() {
            return compressionCodec;
        }

        public Builder setCompressionCodec(String compressionCodec) {
            this.compressionCodec = compressionCodec;
            return this;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public Builder setBatchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public int getMessageSendMaxRetries() {
            return messageSendMaxRetries;
        }

        public Builder setMessageSendMaxRetries(int messageSendMaxRetries) {
            this.messageSendMaxRetries = messageSendMaxRetries;
            return this;
        }

        public int getRequestRequiredAcks() {
            return requestRequiredAcks;
        }

        public Builder setRequestRequiredAcks(int requestRequiredAcks) {
            this.requestRequiredAcks = requestRequiredAcks;
            return this;
        }

        public MetricRegistry getRegistry() {
            return registry;
        }

        public Builder setRegistry(MetricRegistry registry) {
            this.registry = registry;
            return this;
        }

        public String getName() {
            return name;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public MetricFilter getFilter() {
            return filter;
        }

        public Builder setFilter(MetricFilter filter) {
            this.filter = filter;
            return this;
        }

        public TimeUnit getRateUnit() {
            return rateUnit;
        }

        public Builder setRateUnit(TimeUnit rateUnit) {
            this.rateUnit = rateUnit;
            return this;
        }

        public TimeUnit getDurationUnit() {
            return durationUnit;
        }

        public Builder setDurationUnit(TimeUnit durationUnit) {
            this.durationUnit = durationUnit;
            return this;
        }

        public KafkaReporter build() {
            Properties props = new Properties();
            props.put("compression.codec", compressionCodec);
            props.put("producer.type", synchronously ? "sync" : "async");
            props.put("metadata.broker.list", brokerList);
            props.put("batch.num.messages", batchSize);
            props.put("message.send.max.retries", messageSendMaxRetries);
            props.put("require.requred.acks",requestRequiredAcks);
            props.put("client.id", clientId);

            return new KafkaReporter(registry, name, filter, rateUnit, durationUnit, kafkaTopic, props);
        }
    }
}