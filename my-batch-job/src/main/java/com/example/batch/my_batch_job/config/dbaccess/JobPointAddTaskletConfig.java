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

import com.example.batch.my_batch_job.config.JobBaseContextConfig;
import com.example.batch.my_batch_job.dbaccess.tasklet.MemberCountTasklet;
import com.example.batch.my_batch_job.dbaccess.tasklet.MemberSearchTasklet;
import com.example.batch.my_batch_job.dbaccess.tasklet.PointAddTasklet;

@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan({"com.example.batch.my_batch_job.dbaccess.tasklet"
	,"com.example.batch.my_batch_job.common.listener"})
@MapperScan(basePackages = "com.example.batch.my_batch_job.common.repository", sqlSessionFactoryRef = "jobSqlSessionFactory")
public class JobPointAddTaskletConfig {

//    // (1) ジョブの定義: 3つのステップを順次実行するように構成
//    @Bean
//    public Job jobPointAddTasklet(JobRepository jobRepository,
//                                  Step step01,
//                                  Step step02,
//                                  Step step03,
//                                  JobExitCodeChangeListener listener) {
//        return new JobBuilder("jobPointAddTasklet", jobRepository)
//                .start(step01)      // 最初に実行
//                .next(step02)       // 成功したら次に実行
//                .next(step03)       // 成功したら最後に実行
//                .listener(listener)
//                .build();
//    }
//
//    // (2) ステップ1: 件数出力
//    @Bean
//    public Step step01(JobRepository jobRepository,
//                       @Qualifier("jobTransactionManager") PlatformTransactionManager tm,
//                       MemberCountTasklet tasklet) {
//        return new StepBuilder("jobPointAddTasklet.step01", jobRepository)
//                .tasklet(tasklet, tm)
//                .build();
//    }
//
//    // (3) ステップ2: 検索結果出力
//    @Bean
//    public Step step02(JobRepository jobRepository,
//                       @Qualifier("jobTransactionManager") PlatformTransactionManager tm,
//                       MemberSearchTasklet tasklet) {
//        return new StepBuilder("jobPointAddTasklet.step02", jobRepository)
//                .tasklet(tasklet, tm)
//                .build();
//    }
//
//    // (4) ステップ3: ポイント更新
//    @Bean
//    public Step step03(JobRepository jobRepository,
//                       @Qualifier("jobTransactionManager") PlatformTransactionManager tm,
//                       PointAddTasklet tasklet) {
//        return new StepBuilder("jobPointAddTasklet.step03", jobRepository)
//                .tasklet(tasklet, tm)
//                .build();
//    }

    // --- ジョブ1: 件数出力 ---
    @Bean
    public Job jobMemberCount(JobRepository jobRepository, Step stepCount) {
        return new JobBuilder("jobMemberCount", jobRepository).start(stepCount).build();
    }
    @Bean
    public Step stepCount(JobRepository jobRepository, @Qualifier("jobTransactionManager") PlatformTransactionManager tm, 
    		MemberCountTasklet tasklet) {
        return new StepBuilder("stepCount", jobRepository).tasklet(tasklet, tm).build();
    }

    // --- ジョブ2: 検索出力 ---
    @Bean
    public Job jobMemberSearch(JobRepository jobRepository, Step stepSearch) {
        return new JobBuilder("jobMemberSearch", jobRepository).start(stepSearch).build();
    }
    @Bean
    public Step stepSearch(JobRepository jobRepository, @Qualifier("jobTransactionManager") PlatformTransactionManager tm, 
    		MemberSearchTasklet tasklet) {
        return new StepBuilder("stepSearch", jobRepository).tasklet(tasklet, tm).build();
    }
    // --- ジョブ3: ポイント更新 ---
    @Bean
    public Job jobPointAdd(JobRepository jobRepository, Step stepPointAdd) {
        return new JobBuilder("jobPointAdd", jobRepository).start(stepPointAdd).build();
    }

    @Bean
    public Step stepPointAdd(JobRepository jobRepository, @Qualifier("jobTransactionManager") PlatformTransactionManager tm, 
    		PointAddTasklet tasklet) {
        return new StepBuilder("stepPointAdd", jobRepository).tasklet(tasklet, tm).build();
    }

//    // Step定義：引数の tasklet は @Component により自動注入される
//    @Bean
//    public Step step01(JobRepository jobRepository,
//                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager,
//                       PointAddTasklet tasklet/*,
//                       ChunkErrorLoggingListener listener*/) {
//        return new StepBuilder("jobPointAddTasklet.step01",
//                jobRepository)
//                .tasklet(tasklet, transactionManager)
//                //.listener(listener)
//                .build();
//    }
//    // Job定義
//    @Bean
//    public Job jobPointAddTasklet(JobRepository jobRepository,
//                                             Step step01,
//                                             JobExitCodeChangeListener listener) {
//        return new JobBuilder("jobPointAddTasklet", jobRepository)
//                .start(step01)
//                .listener(listener)
//                .build();
//    }
}
