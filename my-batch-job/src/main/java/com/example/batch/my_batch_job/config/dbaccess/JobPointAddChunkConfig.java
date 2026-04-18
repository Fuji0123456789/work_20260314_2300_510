package com.example.batch.my_batch_job.config.dbaccess;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.batch.MyBatisBatchItemWriter;
import org.mybatis.spring.batch.MyBatisCursorItemReader;
import org.mybatis.spring.batch.builder.MyBatisBatchItemWriterBuilder;
import org.mybatis.spring.batch.builder.MyBatisCursorItemReaderBuilder;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;

import com.example.batch.my_batch_job.common.dto.MemberInfoDto;
import com.example.batch.my_batch_job.common.listener.JobExitCodeChangeListener;
import com.example.batch.my_batch_job.common.listener.StepExitStatusChangeListener;
import com.example.batch.my_batch_job.config.JobBaseContextConfig;
import com.example.batch.my_batch_job.dbaccess.chunk.PointAddItemProcessor;

@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan({"com.example.batch.my_batch_job.dbaccess.chunk"
	,"com.example.batch.my_batch_job.common.listener"})
@MapperScan(basePackages = "com.example.batch.my_batch_job.common.repository", sqlSessionFactoryRef = "jobSqlSessionFactory")
public class JobPointAddChunkConfig {

    @Bean
    public MyBatisCursorItemReader<MemberInfoDto> reader(
            @Qualifier("jobSqlSessionFactory") SqlSessionFactory jobSqlSessionFactory) {
        return new MyBatisCursorItemReaderBuilder<MemberInfoDto>()
                .sqlSessionFactory(jobSqlSessionFactory)
                .queryId(
                        "com.example.batch.my_batch_job.common.repository.MemberInfoRepository.cursor")
                .build();
    }

    @Bean
    public MyBatisBatchItemWriter<MemberInfoDto> writer(
            @Qualifier("jobSqlSessionFactory") SqlSessionFactory jobSqlSessionFactory,
            SqlSessionTemplate batchModeSqlSessionTemplate) {
        return new MyBatisBatchItemWriterBuilder<MemberInfoDto>()
                .sqlSessionFactory(jobSqlSessionFactory)
                .statementId(
                        "com.example.batch.my_batch_job.common.repository.MemberInfoRepository.updatePointAndStatus")
                .sqlSessionTemplate(batchModeSqlSessionTemplate)
                .build();
    }

    @Bean
    public Step step01(JobRepository jobRepository,
                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager,
                       ItemReader<MemberInfoDto> reader,
                       PointAddItemProcessor processor,
                       ItemWriter<MemberInfoDto> writer,
                       //ChunkErrorLoggingListener listener,
                       StepExitStatusChangeListener listener) {
        return new StepBuilder("jobPointAddChunk.step01",
                jobRepository)
                .<MemberInfoDto, MemberInfoDto>chunk(10,
                        transactionManager)
                .listener(listener)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public Job jobPointAddChunk(JobRepository jobRepository,
                                             Step step01,
                                             JobExitCodeChangeListener listener) {
        return new JobBuilder("jobPointAddChunk", jobRepository)
                .start(step01)
                .listener(listener)
                .build();
    }
}
