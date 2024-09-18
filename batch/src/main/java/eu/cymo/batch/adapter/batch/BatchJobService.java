package eu.cymo.batch.adapter.batch;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import eu.cymo.batch.domain.JobService;

@Component
public class BatchJobService implements JobService {
    private final JobLauncher jobLauncher;
    private final Job job;
    
    public BatchJobService(
    		JobLauncher jobLauncher,
    		Job job) {
        this.jobLauncher = jobLauncher;
        this.job = job;
    }
    
    @Async
	@Override
	public void execute(String batchId) {
		try {
			var target = Path.of("files", batchId);

			Files.createDirectories(target.getParent());

			var params = new JobParametersBuilder()
					.addString("batch.id", batchId)
					.addString("output.file", target.toString())
					.toJobParameters();
			
			jobLauncher.run(job, params);
		}
		catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

}
