package com.example.batch.my_batch_job.config.fileaccess;

import java.io.File;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ClassUtils;

import com.example.batch.my_batch_job.common.dto.MemberInfoDto;
import com.example.batch.my_batch_job.common.listener.JobExitCodeChangeListener;
import com.example.batch.my_batch_job.config.JobBaseContextConfig;
import com.example.batch.my_batch_job.fileaccess.tasklet.PointAddTasklet;

@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan(value = {"com.example.batch.my_batch_job.fileaccess.tasklet"
		,"com.example.batch.my_batch_job.common.listener"}, scopedProxy = ScopedProxyMode.TARGET_CLASS)
public class JobPointAddTaskletConfig {

    @Bean
    @StepScope
    public FlatFileItemReader<MemberInfoDto> reader(
            @Value("#{jobParameters['inputFile']}") File inputFile) {
        DefaultLineMapper<MemberInfoDto> lineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setNames("id", "type", "status", "point");
        lineTokenizer.setDelimiter(",");
        lineTokenizer.setQuoteCharacter('"');
        BeanWrapperFieldSetMapper<MemberInfoDto> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(MemberInfoDto.class);
        lineMapper.setLineTokenizer(lineTokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);
        return new FlatFileItemReaderBuilder<MemberInfoDto>()
                .name(ClassUtils.getShortName(FlatFileItemReader.class))
                .lineMapper(lineMapper)
                .resource(new FileSystemResource(inputFile))
                .encoding("MS932")
                .strict(true)
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemWriter<MemberInfoDto> writer(
            @Value("#{jobParameters['outputFile']}") File outputFile) {
        DelimitedLineAggregator<MemberInfoDto> lineAggregator = new DelimitedLineAggregator<>();
        lineAggregator.setDelimiter(",");
        BeanWrapperFieldExtractor<MemberInfoDto> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(new String[] {"id", "type", "status", "point"});
        lineAggregator.setFieldExtractor(fieldExtractor);
        return new FlatFileItemWriterBuilder<MemberInfoDto>()
                .name(ClassUtils.getShortName(FlatFileItemWriter.class))
                .resource(new FileSystemResource(outputFile))
                .lineAggregator(lineAggregator)
                .encoding("UTF-8")
                .lineSeparator("\n")
                .append(false)
                .shouldDeleteIfExists(true)
                .transactional(true)
                .build();
    }

    // (2)
    @Bean
    public Step step01(JobRepository jobRepository,
                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager,
                       PointAddTasklet tasklet/*,
                       ChunkErrorLoggingListener listener*/) {
        return new StepBuilder("jobPointAddTasklet.step01",
                jobRepository)
                .tasklet(tasklet, transactionManager)
                //.listener(listener)
                .build();
    }

    // (1)
    @Bean
    public Job jobPointAddTasklet(JobRepository jobRepository,
                                             Step step01,
                                             JobExitCodeChangeListener listener) {
        return new JobBuilder("jobPointAddTasklet", jobRepository)
                .start(step01)
                .listener(listener)
                .build();
    }}
