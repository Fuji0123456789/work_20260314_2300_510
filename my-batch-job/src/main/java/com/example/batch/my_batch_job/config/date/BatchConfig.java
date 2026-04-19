package com.example.batch.my_batch_job.config.date;

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
import com.example.batch.my_batch_job.date.tasklet.MainTasklet;

/**
 * com.example.batch.my_batch_job.config.date.BatchConfig businessDayJob targetDate=20231023
 * 
 * @author user
 *
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan({"com.example.batch.my_batch_job.date.tasklet","com.example.batch.my_batch_job.common.parts"})
public class BatchConfig {

    /**
     * ステップの定義
     * @param jobRepository Spring Batchの実行状態を管理するリポジトリ
     * @param transactionManager トランザクションマネージャー
     * @param mainTasklet 先ほど作成したTasklet（DIされる）
     */
    @Bean
    public Step calculationStep(JobRepository jobRepository, 
    		@Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager, 
                               MainTasklet mainTasklet) {
        return new StepBuilder("calculationStep", jobRepository)
                .tasklet(mainTasklet, transactionManager) // Taskletを登録
                .build();
    }

    /**
     * ジョブの定義
     */
    @Bean
    public Job businessDayJob(JobRepository jobRepository, Step calculationStep) {
        return new JobBuilder("businessDayJob", jobRepository)
                .start(calculationStep) // 最初に実行するステップを指定
                .build();
    }
}
