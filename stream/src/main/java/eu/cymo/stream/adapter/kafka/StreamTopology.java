package eu.cymo.stream.adapter.kafka;

import java.time.LocalDate;

import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.cymo.common.person.Adult;
import eu.cymo.common.person.PersonParsed;

@Component
public class StreamTopology {
	private AvroSerdeFactory avroSerdeFactory;
	
	public StreamTopology(AvroSerdeFactory avroSerdeFactory) {
		this.avroSerdeFactory = avroSerdeFactory;
	}

	@Autowired
	public void configure(StreamsBuilder builder) {
		builder.stream("persons", Consumed.with(Serdes.String(), avroSerdeFactory.specificAvroValueSerde()))
			.process(AgeFilterProcessor::new)
			.to("adults", Produced.with(Serdes.String(), avroSerdeFactory.specificAvroValueSerde()));
	}
	
	private class AgeFilterProcessor implements Processor<String, SpecificRecord, String, SpecificRecord> {
		private ProcessorContext<String, SpecificRecord> context;
		
		@Override
		public void init(ProcessorContext<String, SpecificRecord> context) {
			this.context = context;
		}

		@Override
		public void process(Record<String, SpecificRecord> record) {
			var value = record.value();
			if(value instanceof PersonParsed person) {
				if(person.getBirthDate().plusYears(18).isBefore(LocalDate.now())) {
					context.forward(record.withValue(Adult.newBuilder()
							.setFirstName(person.getFirstName())
							.setLastName(person.getLastName())
							.setBatchId(person.getBatchId())
							.setAddress(person.getAddress())
							.build()));
				}
			}
			else {
				context.forward(record);
			}
		}
		
	}
	
}
