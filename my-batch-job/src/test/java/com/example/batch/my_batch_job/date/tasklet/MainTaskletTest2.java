package com.example.batch.my_batch_job.date.tasklet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.test.MetaDataInstanceFactory;

import com.example.batch.my_batch_job.common.parts.BusinessDayService;
import com.example.batch.my_batch_job.common.parts.BusinessDayService.BusinessDayResult;

@ExtendWith(MockitoExtension.class)
class MainTaskletTest2 {

    @InjectMocks
    private MainTasklet target;

    @Mock
    private BusinessDayService businessDayService;

    private StepContribution contribution;
    private ChunkContext chunkContext;
    private StepExecution stepExecution; // パラメータセット用にメンバ変数化

    @BeforeEach
    void setUp() {
        // Spring Batchのテスト用ユーティリティを使用してContextを生成
        // StepExecutionを生成
        stepExecution = MetaDataInstanceFactory.createStepExecution();
    }

    @Test
    @DisplayName("正常系：サービスがリターンコード00を返した場合、FINISHEDが返ること")
    void testExecute_Success() throws Exception {
        // 1. パラメータを先に作る
        JobParameters params = new JobParametersBuilder()
            .addString("targetDate", "20231023")
            .toJobParameters();

        // 2. パラメータを指定してStepExecutionを再生成する
        stepExecution = MetaDataInstanceFactory.createStepExecution(params);
        
        // 3. Contextを組み立て直す
        chunkContext = new ChunkContext(new StepContext(stepExecution));
        contribution = new StepContribution(stepExecution);

        // 準備：サービスの振る舞いを定義
        when(businessDayService.calculatePrevious("20231023"))
            .thenReturn(new BusinessDayResult("20231020", "00"));

        // 実行
        RepeatStatus status = target.execute(contribution, chunkContext);

        // 検証
        assertEquals(RepeatStatus.FINISHED, status);
        verify(businessDayService, times(1)).calculatePrevious("20231023");

        // 4. 検証：ExecutionContextから算出結果を取り出して確認
        // Tasklet内で jobExecution の ExecutionContext に保存しているため、そこから取得する
        String actualDate = (String) stepExecution.getJobExecution()
                                                 .getExecutionContext()
                                                 .get("previousBusinessDay");

        assertEquals("20231020", actualDate, "2023/10/23の1営業日前は2023/10/20(金)であること");
    }

    @Test
    @DisplayName("異常系：サービスがリターンコード93を返した場合、RuntimeExceptionが投げられること")
    void testExecute_BusinessError() {
        // 1. パラメータを先に作る
        JobParameters params = new JobParametersBuilder()
            .addString("targetDate", "invalid")
            .toJobParameters();

        // 2. パラメータを指定してStepExecutionを再生成する
        stepExecution = MetaDataInstanceFactory.createStepExecution(params);
        
        // 3. Contextを組み立て直す
        chunkContext = new ChunkContext(new StepContext(stepExecution));
        contribution = new StepContribution(stepExecution);

        // 準備：サービスの振る舞いを定義
        when(businessDayService.calculatePrevious("invalid"))
            .thenReturn(new BusinessDayResult(null, "93"));

        // 実行 & 検証
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            target.execute(contribution, chunkContext);
        });

        assertTrue(ex.getMessage().contains("異常値が発生しました。コード: 9"));
    }

    @Test
    @DisplayName("異常系：パラメータ targetDate が存在しない場合、IllegalArgumentExceptionが投げられること")
    void testExecute_NoParameter() {
        // 準備：パラメータをセットしない
        JobParameters params = new JobParametersBuilder()
                .toJobParameters();

        // 2. このテスト専用の StepExecution を生成
        StepExecution testStepExecution = MetaDataInstanceFactory.createStepExecution(params);
        
        // 3. 重要：chunkContext と contribution を最新の execution で作り直す
        ChunkContext chunkContext = new ChunkContext(new StepContext(testStepExecution));
        StepContribution contribution = new StepContribution(testStepExecution);

        // 実行 & 検証
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            target.execute(contribution, chunkContext);
        });

        assertEquals("パラメータ 'targetDate' が指定されていません。", ex.getMessage());
    }
}
