package com.example.batch.my_batch_job.config.fileaccess;

import java.io.File;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
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
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ClassUtils;

import com.example.batch.my_batch_job.common.dto.MemberInfoDto;
import com.example.batch.my_batch_job.common.listener.JobExitCodeChangeListener;
import com.example.batch.my_batch_job.common.listener.StepExitStatusChangeListener;
import com.example.batch.my_batch_job.config.JobBaseContextConfig;
import com.example.batch.my_batch_job.fileaccess.chunk.PointAddItemProcessor;

@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan({"com.example.batch.my_batch_job.fileaccess.chunk"
	,"com.example.batch.my_batch_job.common.listener"}) // Processorがあるパッケージを正確に指定
public class JobPointAddChunkConfig {

    // (1)
    @Bean
    @StepScope
    public FlatFileItemReader<MemberInfoDto> reader(
            @Value("#{jobParameters['inputFile']}") File inputFile) {
        DefaultLineMapper<MemberInfoDto> lineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer(); // (3)
        lineTokenizer.setNames("id", "type", "status", "point"); // (4)
        lineTokenizer.setDelimiter(","); // (5)
        lineTokenizer.setQuoteCharacter('"');
        BeanWrapperFieldSetMapper<MemberInfoDto> fieldSetMapper = new BeanWrapperFieldSetMapper<>(); // (6)
        fieldSetMapper.setTargetType(MemberInfoDto.class);
        lineMapper.setLineTokenizer(lineTokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);
        return new FlatFileItemReaderBuilder<MemberInfoDto>()
                .name(ClassUtils.getShortName(FlatFileItemReader.class))
                .lineMapper(lineMapper)
                .resource(new FileSystemResource(inputFile)) // (2)
                .encoding("MS932")
                .strict(true)
                .build();
    }

    // (7)
    @Bean
    @StepScope
    public FlatFileItemWriter<MemberInfoDto> writer(
            @Value("#{jobParameters['outputFile']}") File outputFile) {
        BeanWrapperFieldExtractor<MemberInfoDto> fieldExtractor = new BeanWrapperFieldExtractor<>(); // (11)
        fieldExtractor.setNames(new String[] {"id", "type", "status", "point"}); // (12)
        DelimitedLineAggregator<MemberInfoDto> lineAggregator = new DelimitedLineAggregator<>(); // (9)
        lineAggregator.setDelimiter(","); // (10)
        lineAggregator.setFieldExtractor(fieldExtractor);
        return new FlatFileItemWriterBuilder<MemberInfoDto>()
                .name(ClassUtils.getShortName(FlatFileItemWriter.class))
                .resource(new FileSystemResource(outputFile)) // (8)
                .lineAggregator(lineAggregator)
                .encoding("UTF-8")
                .lineSeparator("\n")
                .append(false)
                .shouldDeleteIfExists(true)
                .transactional(true)
                .build();
    }

    // (1)
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
                .<MemberInfoDto, MemberInfoDto>chunk(10, // (3)
                        transactionManager)
                .listener(listener)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    // (1)
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
