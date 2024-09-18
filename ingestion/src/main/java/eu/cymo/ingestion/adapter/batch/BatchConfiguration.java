package eu.cymo.ingestion.adapter.batch;

import java.util.Collections;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.kafka.KafkaItemWriter;
import org.springframework.batch.item.kafka.builder.KafkaItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.kafka.core.KafkaTemplate;

import eu.cymo.common.person.PersonFileParsed;
import eu.cymo.common.person.PersonParsed;
import eu.cymo.ingestion.domain.BatchPerson;
import eu.cymo.ingestion.domain.Person;

@Configuration
public class BatchConfiguration {
	
	/**
	 * JOB -----------------------------------------------------------------------
	 */
	
	@Bean
	public Job job(
			JobRepository jobRepository,
			Step ingestFileStep,
			Step produceMessageStep,
			Step produceFileParsedStep) {
		return new JobBuilder("ingestionJob", jobRepository)
				.start(ingestFileStep)
				.next(produceMessageStep)
				.next(produceFileParsedStep)
				.build();
	}
	
	/**
	 * ---------------------------------------------------------------------------
	 */
	
	/**
	 * INGESTION STEP ------------------------------------------------------------
	 */
	
	@Bean
	public Step ingestFileStep(
			JobRepository jobRepository,
			JdbcTransactionManager transactionManager,
			FlatFileItemReader<Person> personCsvReader,
			ItemProcessor<Person, BatchPerson> personItemProcessor,
			ItemWriter<BatchPerson> personTableWriter) {
		return new StepBuilder("ingestFileStep", jobRepository)
				.<Person, BatchPerson>chunk(100, transactionManager)
				.reader(personCsvReader)
				.processor(personItemProcessor)
				.writer(personTableWriter)
				.build();
	}

	@Bean
	@StepScope
	public FlatFileItemReader<Person> personCsvReader(
			@Value("#{jobParameters['input.file']}")
			String inputFile) {
		return new FlatFileItemReaderBuilder<Person>()
				.name("personFileReader")
				.resource(new FileSystemResource(inputFile))
				.delimited()
				.names("firstName", "lastName", "birthDate", "address")
				.targetType(Person.class)
				.linesToSkip(1) // skip header
				.build();
	}

	@Bean
	@StepScope
	public ItemProcessor<Person, BatchPerson> personItemProcessor(
			@Value("#{jobParameters['batch.id']}")
			String batchId) {
		return new PersonItemProcessor(batchId);
	}
	
	@Bean
	public ItemWriter<BatchPerson> personTableWriter(DataSource dataSource) {
		var sql = "insert into PERSON values (:batchId, :firstName, :lastName, :birthDate, :address)";
		return new JdbcBatchItemWriterBuilder<BatchPerson>()
				.dataSource(dataSource)
				.sql(sql)
				.beanMapped()
				.build();
	}
	
	/**
	 * ---------------------------------------------------------------------------
	 */
	
	/**
	 * PRODUCE STEP --------------------------------------------------------------
	 */
	
	@Bean
	public Step produceMessageStep(
			JobRepository jobRepository,
			JdbcTransactionManager transactionManager,
			JdbcCursorItemReader<BatchPerson> personTableReader,
			ItemProcessor<BatchPerson, PersonParsed> batchPersonItemProcessor,
			KafkaItemWriter<String, PersonParsed> personParsedWriter) {
		return new StepBuilder("produceMessageStep", jobRepository)
				.<BatchPerson, PersonParsed>chunk(100, transactionManager)
				.reader(personTableReader)
				.processor(batchPersonItemProcessor)
				.writer(personParsedWriter)
				.build();
	}
	
	@Bean
	@StepScope
	public JdbcCursorItemReader<BatchPerson> personTableReader(
			DataSource dataSource,
			@Value("#{jobParameters['batch.id']}")
			String batchId) {
		var sql = "select * from PERSON where BATCH_ID = '%s'".formatted(batchId);
		return new JdbcCursorItemReaderBuilder<BatchPerson>()
				.name("personTableReader")
				.dataSource(dataSource)
				.sql(sql)
				.rowMapper(new DataClassRowMapper<>(BatchPerson.class))
				.build();
	}
	
	@Bean
	public ItemProcessor<BatchPerson, PersonParsed> batchPersonItemProcessor() {
		return new BatchPersonItemProcessor();
	}
	
	@Bean
	public KafkaItemWriter<String, PersonParsed> personParsedWriter(KafkaTemplate<String, PersonParsed> kafkaTemplate) {
		return new KafkaItemWriterBuilder<String, PersonParsed>()
				.kafkaTemplate(kafkaTemplate)
				.itemKeyMapper(PersonParsed::getBatchId)
				.build();
	}
	
	/**
	 * ---------------------------------------------------------------------------
	 */
	
	/**
	 * BATCH FINISHED MESSAGE STEP -----------------------------------------------
	 */
	
	@Bean
	public Step produceFileParsedStep(
			JobRepository jobRepository,
			JdbcTransactionManager transactionManager,
			ItemReader<String> batchIdItemReader,
			ItemProcessor<String, PersonFileParsed> personFileParsedItemProcessor,
			KafkaItemWriter<String, PersonFileParsed> personFileParsedWriter) {
		return new StepBuilder("produceFileParsedStep", jobRepository)
				.<String, PersonFileParsed>chunk(100, transactionManager)
				.reader(batchIdItemReader)
				.processor(personFileParsedItemProcessor)
				.writer(personFileParsedWriter)
				.build();
	}
	
	@Bean
	@StepScope
	public ItemReader<String> batchIdItemReader(
			@Value("#{jobParameters['batch.id']}")
			String batchId) {
		return new CollectionItemReader<>(Collections.singletonList(batchId));
	}
	
	@Bean
	public PersonFileParsedItemProcessor personFileParsedItemProcessor() {
		return new PersonFileParsedItemProcessor();
	}
	
	@Bean
	public KafkaItemWriter<String, PersonFileParsed> personFileParsedWriter(KafkaTemplate<String, PersonFileParsed> kafkaTemplate) {
		return new KafkaItemWriterBuilder<String, PersonFileParsed>()
				.kafkaTemplate(kafkaTemplate)
				.itemKeyMapper(PersonFileParsed::getBatchId)
				.build();
	}
	
	/**
	 * ---------------------------------------------------------------------------
	 */
	
	
}
