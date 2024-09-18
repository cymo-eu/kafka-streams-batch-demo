package eu.cymo.ingestion.adapter.batch;

import org.springframework.batch.item.ItemProcessor;

import eu.cymo.common.person.PersonFileParsed;

public class PersonFileParsedItemProcessor implements ItemProcessor<String, PersonFileParsed> {

	@Override
	public PersonFileParsed process(String item) throws Exception {
		return PersonFileParsed.newBuilder()
				.setBatchId(item)
				.build();
	}

}
