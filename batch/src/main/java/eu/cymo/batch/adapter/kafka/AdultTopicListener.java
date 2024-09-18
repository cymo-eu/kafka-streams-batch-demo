package eu.cymo.batch.adapter.kafka;

import java.util.ArrayList;

import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import eu.cymo.batch.domain.JobService;
import eu.cymo.common.person.Adult;
import eu.cymo.common.person.PersonFileParsed;

@Component
public class AdultTopicListener {
    private final JdbcTemplate jdbcTemplate;
    private final JobService jobService;

    public AdultTopicListener(
    		JdbcTemplate jdbcTemplate,
    		JobService jobService) {
        this.jdbcTemplate = jdbcTemplate;
        this.jobService = jobService;
    }

	@Transactional
	@KafkaListener(topics = "adults", batch = "true")
	public void process(ConsumerRecords<String, SpecificRecord> records) {
		var sql = "insert into ADULT values (? , ?, ? , ?)";
		var args = new ArrayList<Object[]>();
	
		for(var record : records) {
			var value = record.value();
			if(value instanceof Adult adult) {
				args.add(new Object[] {
					adult.getBatchId(),
					adult.getFirstName(),
					adult.getLastName(),
					adult.getAddress()
				});
				
			}
			else if(value instanceof PersonFileParsed parsed) {
				TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
					
					@Override
					public void afterCommit() {
						jobService.execute(parsed.getBatchId());
					}
					
				});
			}
		}
		
		jdbcTemplate.batchUpdate(sql, args);
	}
	
}
