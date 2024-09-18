package eu.cymo.stream.adapter.kafka;

import org.apache.avro.specific.SpecificRecord;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.stereotype.Component;

import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;

@Component
public class AvroSerdeFactory {
    private final KafkaProperties kafkaProperties;
    private final ObjectProvider<SslBundles> sslBundles;

    public AvroSerdeFactory(
            KafkaProperties kafkaProperties,
            ObjectProvider<SslBundles> sslBundles) {
        this.kafkaProperties = kafkaProperties;
        this.sslBundles = sslBundles;
    }
    
    public <T extends SpecificRecord> SpecificAvroSerde<T> specificAvroValueSerde() {
    	return specificAvroSerde(false);
    }
    
    public <T extends SpecificRecord> SpecificAvroSerde<T> specificAvroSerde(boolean isKey) {
        var properties = kafkaProperties.buildStreamsProperties(sslBundles.getIfAvailable());
        properties.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        
        var serde = new SpecificAvroSerde<T>();
        serde.configure(properties, isKey);
        return serde;
    }

}
