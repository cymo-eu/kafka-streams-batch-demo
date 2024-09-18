package eu.cymo.batch.adapter.batch;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.support.JdbcTransactionManager;

import eu.cymo.common.person.Adult;

@Configuration
public class BatchConfiguration {

	
	/**
	 * JOB -----------------------------------------------------------------------
	 */

	@Bean
	public Job job(
			JobRepository jobRepository,
			Step writeFileStep) {
		return new JobBuilder("WriteFileJob", jobRepository)
				.start(writeFileStep)
				.build();
	}
	
	/**
	 * ---------------------------------------------------------------------------
	 */
	
	/**
	 * TO FILE STEP --------------------------------------------------------------
	 */

	@Bean
	public Step writeFileStep(
			JobRepository jobRepository,
			JdbcTransactionManager transactionManager,
			JdbcCursorItemReader<Adult> personTableReader,
			FlatFileItemWriter<Adult> billingDataFileWriter) {
		return new StepBuilder("writeFileStep", jobRepository)
				.<Adult, Adult>chunk(100, transactionManager)
				.reader(personTableReader)
				.writer(billingDataFileWriter)
				.build();
	}
	
	@Bean
	@StepScope
	public JdbcCursorItemReader<Adult> personTableReader(
			DataSource dataSource,
			@Value("#{jobParameters['batch.id']}")
			String batchId) {
		var sql = "select * from ADULT where BATCH_ID = '%s'".formatted(batchId);
		return new JdbcCursorItemReaderBuilder<Adult>()
				.name("adultTableReader")
				.dataSource(dataSource)
				.sql(sql)
				.rowMapper(new DataClassRowMapper<>(Adult.class))
				.build();
	}
	
	@Bean
	@StepScope
	public FlatFileItemWriter<Adult> billingDataFileWriter(
			@Value("#{jobParameters['output.file']}")
			String outputFile) {
		return new FlatFileItemWriterBuilder<Adult>()
				.resource(new FileSystemResource(outputFile))
				.name("adultFileWriter")
				.delimited()
				.names("firstName", "lastName", "address")
				.build();
	}
	
	/**
	 * ---------------------------------------------------------------------------
	 */
}
