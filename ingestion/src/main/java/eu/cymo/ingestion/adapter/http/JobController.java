package eu.cymo.ingestion.adapter.http;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/job")
public class JobController {

	private final JobLauncher jobLauncher;
	private final Job job;
	
	public JobController(
			JobLauncher jobLauncher,
			Job job) {
		this.jobLauncher = jobLauncher;
		this.job = job;
	}
	
	@PostMapping("/execute")
	public ResponseEntity<JobExecution> execute(
			@RequestParam("file")
			MultipartFile file) throws Exception {
		var batchId = UUID.randomUUID().toString();
		var target = Path.of("files", batchId);
		
		Files.createDirectories(target.getParent());
		Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
		
		var params = new JobParametersBuilder()
				.addString("batch.id", batchId)
				.addString("input.file", target.toString())
				.toJobParameters();
		
		var jobExecution = jobLauncher.run(job, params);
		
		return ResponseEntity.ok(jobExecution);
	}
	
}
