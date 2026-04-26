package com.example.batch.my_batch_job.config.date;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;

import com.example.batch.my_batch_job.config.JobBaseContextConfig;
import com.example.batch.my_batch_job.date.tasklet.CalendarRegistrationTasklet;

@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan({
    "com.example.batch.my_batch_job.date.tasklet",
    "com.example.batch.my_batch_job.common.parts"
})
//★ これを追加。CalendarMapperがあるパッケージを指定し、共通のセッションファクトリを紐付ける
@MapperScan(
 basePackages = "com.example.batch.my_batch_job.common.repository", 
 sqlSessionFactoryRef = "jobSqlSessionFactory"
)
public class CalendarJobConfig {

    @Bean
    public Job calendarRegistrationJob(JobRepository jobRepository, Step calendarStep) {
        return new JobBuilder("calendarRegistrationJob", jobRepository)
                .start(calendarStep)
                .build();
    }

    @Bean
    public Step calendarStep(JobRepository jobRepository,
                             @Qualifier("jobTransactionManager") PlatformTransactionManager tm,
                             CalendarRegistrationTasklet tasklet) {
        return new StepBuilder("calendarStep", jobRepository)
                .tasklet(tasklet, tm)
                .build();
    }
}
