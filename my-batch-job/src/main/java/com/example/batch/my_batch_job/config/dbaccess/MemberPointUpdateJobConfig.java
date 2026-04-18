package com.example.batch.my_batch_job.config.dbaccess;

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

import com.example.batch.my_batch_job.common.listener.JobExitCodeChangeListener;
import com.example.batch.my_batch_job.config.JobBaseContextConfig;
import com.example.batch.my_batch_job.dbaccess.tasklet.MemberCountTasklet;
import com.example.batch.my_batch_job.dbaccess.tasklet.MemberSearchTasklet;
import com.example.batch.my_batch_job.dbaccess.tasklet.PointAddTasklet;

@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan({"com.example.batch.my_batch_job.dbaccess.tasklet"
	,"com.example.batch.my_batch_job.common.listener"})
@MapperScan(basePackages = "com.example.batch.my_batch_job.common.repository", sqlSessionFactoryRef = "jobSqlSessionFactory")
public class MemberPointUpdateJobConfig {

    // ジョブ全体：会員ポイント更新ジョブ
    @Bean
    public Job memberPointUpdateJob(JobRepository jobRepository,
                                    Step memberCountStep,
                                    Step memberSearchStep,
                                    Step pointUpdateStep,
                                    JobExitCodeChangeListener listener) {
        return new JobBuilder("memberPointUpdateJob", jobRepository)
                .start(memberCountStep)
                .next(memberSearchStep)
                .next(pointUpdateStep)
                .listener(listener)
                .build();
    }

    // 工程1：全件数出力
    @Bean
    public Step memberCountStep(JobRepository jobRepository,
                                @Qualifier("jobTransactionManager") PlatformTransactionManager tm,
                                MemberCountTasklet tasklet) {
        return new StepBuilder("memberCountStep", jobRepository)
                .tasklet(tasklet, tm)
                .build();
    }

    // 工程2：指定ステータスの会員検索出力
    @Bean
    public Step memberSearchStep(JobRepository jobRepository,
                                 @Qualifier("jobTransactionManager") PlatformTransactionManager tm,
                                 MemberSearchTasklet tasklet) {
        return new StepBuilder("memberSearchStep", jobRepository)
                .tasklet(tasklet, tm)
                .build();
    }

    // 工程3：ポイント一括更新
    @Bean
    public Step pointUpdateStep(JobRepository jobRepository,
                                @Qualifier("jobTransactionManager") PlatformTransactionManager tm,
                                PointAddTasklet tasklet) {
        return new StepBuilder("pointUpdateStep", jobRepository)
                .tasklet(tasklet, tm)
                .build();
    }
}
