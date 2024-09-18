package eu.cymo.ingestion.adapter.batch;

import org.springframework.batch.item.ItemProcessor;

import eu.cymo.common.person.PersonParsed;
import eu.cymo.ingestion.domain.BatchPerson;

public class BatchPersonItemProcessor implements ItemProcessor<BatchPerson, PersonParsed> {

	@Override
	public PersonParsed process(BatchPerson item) throws Exception {
		return PersonParsed.newBuilder()
				.setBatchId(item.batchId())
				.setFirstName(item.firstName())
				.setLastName(item.lastName())
				.setBirthDate(item.birthDate())
				.setAddress(item.address())
				.build();
	}

}
