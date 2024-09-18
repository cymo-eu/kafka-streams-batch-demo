package eu.cymo.ingestion.domain;

import java.time.LocalDate;

public record BatchPerson(
		String batchId,
		String firstName,
		String lastName,
		LocalDate birthDate,
		String address) {
	
}
