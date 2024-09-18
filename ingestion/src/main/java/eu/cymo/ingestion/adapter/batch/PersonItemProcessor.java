package eu.cymo.ingestion.adapter.batch;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.batch.item.ItemProcessor;

import eu.cymo.ingestion.domain.BatchPerson;
import eu.cymo.ingestion.domain.Person;

public class PersonItemProcessor implements ItemProcessor<Person, BatchPerson> {
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	
	private final String batchId;
	
	public PersonItemProcessor(String batchId) {
		this.batchId = batchId;
	}

	@Override
	public BatchPerson process(Person item) throws Exception {
		return new BatchPerson(
				batchId,
				item.firstName(),
				item.lastName(),
				LocalDate.parse(item.birthDate(), FORMATTER),
				item.address());
	}

}
