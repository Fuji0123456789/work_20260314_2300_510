package com.example.batch.my_batch_job.date.tasklet;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.transaction.annotation.Transactional;

import com.example.batch.my_batch_job.common.parts.BusinessDayService;
import com.example.batch.my_batch_job.config.JobBaseContextConfig;
import com.example.batch.my_batch_job.config.LaunchContextConfig;
import com.example.batch.my_batch_job.config.date.BatchConfig;

@SpringBatchTest
@ContextConfiguration(classes = {
    MainTaskletTest.TestConfig.class, // 下記の設定を読み込む
    JobBaseContextConfig.class,
    LaunchContextConfig.class,
    BatchConfig.class,
    MainTasklet.class,
    BusinessDayService.class
})
//トランザクションマネージャーの名前を指定（TERASOLUNAや標準的なBatch構成では "jobTransactionManager"）
@Transactional("jobTransactionManager") 
class MainTaskletTest {

    @Configuration
    @MapperScan("com.example.batch.my_batch_job.common.repository")
    // resources直下のファイル名を正確に指定
    @PropertySource("classpath:batch-application.properties") 
    static class TestConfig {
        @Bean
        public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
            return new PropertySourcesPlaceholderConfigurer();
        }
    }

    @Autowired
    private MainTasklet mainTasklet;

    @Test
    @Sql(
        statements = {
            "DROP TABLE IF EXISTS CALENDAR_MASTER",
            // カラム名をSQLの期待値に合わせて修正
            "CREATE TABLE CALENDAR_MASTER (CALENDAR_DATE CHAR(8), IS_BUSINESS_DAY INT)",
            "INSERT INTO CALENDAR_MASTER (CALENDAR_DATE, IS_BUSINESS_DAY) VALUES ('20231002', 1)",
            "INSERT INTO CALENDAR_MASTER (CALENDAR_DATE, IS_BUSINESS_DAY) VALUES ('20230929', 1)"
        },
        config = @SqlConfig(dataSource = "jobDataSource", transactionManager = "jobTransactionManager")
    )
    void execute_正常系() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetDate", "20231002")
                .toJobParameters();
        
        StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution(jobParameters);
        StepContribution contribution = new StepContribution(stepExecution);
        ChunkContext chunkContext = new ChunkContext(new StepContext(stepExecution));

        RepeatStatus status = mainTasklet.execute(contribution, chunkContext);

        assertEquals(RepeatStatus.FINISHED, status);
        assertNotNull(stepExecution.getJobExecution().getExecutionContext().get("previousBusinessDay"));
    }

    @Test
    @Sql(statements = {
        "DROP TABLE IF EXISTS CALENDAR_MASTER",
        "CREATE TABLE CALENDAR_MASTER (CALENDAR_DATE CHAR(8), IS_BUSINESS_DAY INT)",
        "INSERT INTO CALENDAR_MASTER VALUES ('20231009', 0)", // 月（祝日）
        "INSERT INTO CALENDAR_MASTER VALUES ('20231008', 0)", // 日
        "INSERT INTO CALENDAR_MASTER VALUES ('20231007', 0)", // 土
        "INSERT INTO CALENDAR_MASTER VALUES ('20231006', 1)"  // 金（これがヒットすべき前営業日）
    }, config = @SqlConfig(dataSource = "jobDataSource", transactionManager = "jobTransactionManager"))
    void execute_正常系_基準日が祝日の場合に金曜日まで遡ること() throws Exception {
        // 基準日を祝日の月曜日に設定
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetDate", "20231009")
                .toJobParameters();
        
        StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution(jobParameters);
        ChunkContext chunkContext = new ChunkContext(new StepContext(stepExecution));

        // 実行
        mainTasklet.execute(new StepContribution(stepExecution), chunkContext);

        // 検証：3日前の金曜日が取得できていること
        String actual = (String) stepExecution.getJobExecution().getExecutionContext().get("previousBusinessDay");
        assertEquals("20231006", actual);
    }

    @Test
    void execute_異常系_パラメータtargetDateが指定されていない場合に例外が発生すること() {
        // ① パラメータを空（またはtargetDateを含まない）にして作成
        JobParameters emptyParams = new JobParametersBuilder()
                .toJobParameters();
        
        StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution(emptyParams);
        StepContribution contribution = new StepContribution(stepExecution);
        ChunkContext chunkContext = new ChunkContext(new StepContext(stepExecution));

        // ② 実行して IllegalArgumentException が発生することを確認
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            mainTasklet.execute(contribution, chunkContext);
        });

        // ③ エラーメッセージの検証
        assertTrue(exception.getMessage().contains("targetDate"));
    }

    @Test
    void execute_異常系_基準日が空文字の場合にコード81が発生すること() {
        // パラメータを空文字にする
        JobParameters params = new JobParametersBuilder()
                .addString("targetDate", "")
                .toJobParameters();
        
        StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution(params);
        ChunkContext chunkContext = new ChunkContext(new StepContext(stepExecution));

        // 実行・検証
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            mainTasklet.execute(new StepContribution(stepExecution), chunkContext);
        });

        assertTrue(exception.getMessage().contains("81"), "コード81が含まれること");
    }

    @Test
    void execute_異常系_基準日が数字8桁ではない場合にコード82が発生すること() {
        // 桁数が違う（または数字以外）
        JobParameters params = new JobParametersBuilder()
                .addString("targetDate", "2023101")
                .toJobParameters();
        
        StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution(params);
        ChunkContext chunkContext = new ChunkContext(new StepContext(stepExecution));

        // 実行・検証
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            mainTasklet.execute(new StepContribution(stepExecution), chunkContext);
        });

        assertTrue(exception.getMessage().contains("82"), "コード82が含まれること");
        assertTrue(exception.getMessage().contains("形式"), "形式エラーの文言が含まれること");
    }

    @Test
    void execute_異常系_基準日が実在しない日付の場合にコード82が発生すること() {
        // 2月30日など
        JobParameters params = new JobParametersBuilder()
                .addString("targetDate", "20230230")
                .toJobParameters();
        
        StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution(params);
        ChunkContext chunkContext = new ChunkContext(new StepContext(stepExecution));

        // 実行・検証
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            mainTasklet.execute(new StepContribution(stepExecution), chunkContext);
        });

        assertTrue(exception.getMessage().contains("82"), "コード82が含まれること");
        assertTrue(exception.getMessage().contains("実在しない"), "実在しない日付の文言が含まれること");
    }

    @Test
    @Sql(statements = {
        "DELETE FROM CALENDAR_MASTER" // データを空にする
    }, config = @SqlConfig(dataSource = "jobDataSource", transactionManager = "jobTransactionManager"))
    void execute_異常系_DBにデータがない場合にコード91の例外が発生すること() {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetDate", "20231002")
                .toJobParameters();
        
        StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution(jobParameters);
        ChunkContext chunkContext = new ChunkContext(new StepContext(stepExecution));

        // 実行・検証：データ未存在により RuntimeException が投げられること
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            mainTasklet.execute(new StepContribution(stepExecution), chunkContext);
        });

        // 実際のサービスが出力したコード「91」が含まれているか検証
        assertTrue(exception.getMessage().contains("91"), 
            "実際のメッセージ: " + exception.getMessage());
    }
    
}
