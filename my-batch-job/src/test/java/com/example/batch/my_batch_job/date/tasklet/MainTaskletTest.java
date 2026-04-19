package com.example.batch.my_batch_job.date.tasklet;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.batch.my_batch_job.common.parts.BusinessDayService;
import com.example.batch.my_batch_job.common.parts.HolidayManager;

@ExtendWith(MockitoExtension.class)
class MainTaskletTest {

//    @InjectMocks
    private MainTasklet target = new MainTasklet();

    // @Mock をやめて @Spy にするか、手動注入にする
    private BusinessDayService businessDayService = new BusinessDayService();

    private StepContribution contribution;
    private ChunkContext chunkContext;
    private StepExecution stepExecution;

    @BeforeEach
    void setUp() {
        // @Mock を削除し、実インスタンスを生成する
        MockitoAnnotations.openMocks(this);
        
        // もし自動注入がうまくいかない場合は手動でセット（ReflectionTestUtilsを使っても良い）
        // ★手動でサービスを注入する（これをしないと target 内のフィールドが null になる）
        ReflectionTestUtils.setField(target, "businessDayService", businessDayService);

        // 1. HolidayManagerの実体を生成し、祝日データをロードする
        HolidayManager holidayManager = new HolidayManager();
        holidayManager.loadHolidays(); // ここでCSVが読み込まれる

        // 2. BusinessDayServiceにHolidayManagerを手動注入する
        ReflectionTestUtils.setField(businessDayService, "holidayManager", holidayManager);

        // 3. MainTaskletにBusinessDayServiceを手動注入する
        ReflectionTestUtils.setField(target, "businessDayService", businessDayService);

        stepExecution = MetaDataInstanceFactory.createStepExecution();
    }

    @Test
    @DisplayName("正常系：実際の計算ロジックを通して金曜日が算出されること")
    void testExecute_Success_20231023() throws Exception {
        // 2023/10/23(月) の 1営業日前は 2023/10/20(金)
        JobParameters params = new JobParametersBuilder()
            .addString("targetDate", "20231023")
            .toJobParameters();

        stepExecution = MetaDataInstanceFactory.createStepExecution(params);
        chunkContext = new ChunkContext(new StepContext(stepExecution));
        contribution = new StepContribution(stepExecution);

        // ★ when(...).thenReturn(...) を削除（実ロジックを動かすため）

        // 実行
        RepeatStatus status = target.execute(contribution, chunkContext);

        // 検証
        assertEquals(RepeatStatus.FINISHED, status);

        // 4. 検証：ExecutionContextから算出結果を取り出して確認
        // Tasklet内で jobExecution の ExecutionContext に保存しているため、そこから取得する
        String actualDate = (String) stepExecution.getJobExecution()
                                                 .getExecutionContext()
                                                 .get("previousBusinessDay");

        assertEquals("20231020", actualDate, "2023/10/23の1営業日前は2023/10/20(金)であること");
    }

    @Test
    @DisplayName("正常系：実際の計算ロジックを通して祝日の前の営業日が算出されること")
    void testExecute_Success_20260430() throws Exception {
        // 2026/04/30(木) の 1営業日前は 2026/04/28(火)
        JobParameters params = new JobParametersBuilder()
            .addString("targetDate", "20260430")
            .toJobParameters();

        stepExecution = MetaDataInstanceFactory.createStepExecution(params);
        chunkContext = new ChunkContext(new StepContext(stepExecution));
        contribution = new StepContribution(stepExecution);

        // ★ when(...).thenReturn(...) を削除（実ロジックを動かすため）

        // 実行
        RepeatStatus status = target.execute(contribution, chunkContext);

        // 検証
        assertEquals(RepeatStatus.FINISHED, status);

        // 4. 検証：ExecutionContextから算出結果を取り出して確認
        // Tasklet内で jobExecution の ExecutionContext に保存しているため、そこから取得する
        String actualDate = (String) stepExecution.getJobExecution()
                                                 .getExecutionContext()
                                                 .get("previousBusinessDay");

        assertEquals("20260428", actualDate, "2026/04/30(木)の1営業日前は2026/04/28(火)であること");
    }

    @Test
    @DisplayName("正常系：実際の計算ロジックを通して祝日の前の営業日が算出されること")
    void testExecute_Success_20260507() throws Exception {
        // 2026/05/07(木) の 1営業日前は 2026/05/01(金)
        JobParameters params = new JobParametersBuilder()
            .addString("targetDate", "20260507")
            .toJobParameters();

        stepExecution = MetaDataInstanceFactory.createStepExecution(params);
        chunkContext = new ChunkContext(new StepContext(stepExecution));
        contribution = new StepContribution(stepExecution);

        // ★ when(...).thenReturn(...) を削除（実ロジックを動かすため）

        // 実行
        RepeatStatus status = target.execute(contribution, chunkContext);

        // 検証
        assertEquals(RepeatStatus.FINISHED, status);

        // 4. 検証：ExecutionContextから算出結果を取り出して確認
        // Tasklet内で jobExecution の ExecutionContext に保存しているため、そこから取得する
        String actualDate = (String) stepExecution.getJobExecution()
                                                 .getExecutionContext()
                                                 .get("previousBusinessDay");

        assertEquals("20260501", actualDate, "2026/05/07(木)の1営業日前は2026/05/01(金)であること");
    }

    @Test
    @DisplayName("異常系：パラメータが空文字の場合、リターンコード91で例外が投げられること")
    void testExecute_EmptyDate() {
        // 1. パラメータを空文字で作成
        JobParameters params = new JobParametersBuilder()
            .addString("targetDate", "")
            .toJobParameters();

        stepExecution = MetaDataInstanceFactory.createStepExecution(params);
        chunkContext = new ChunkContext(new StepContext(stepExecution));
        contribution = new StepContribution(stepExecution);

        // 2. 実行 & 検証
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            target.execute(contribution, chunkContext);
        });

        // BusinessDayServiceが返す "91" を MainTasklet がログ出力・例外スローすることを確認
        assertTrue(ex.getMessage().contains("コード: 91"));
    }

    @Test
    @DisplayName("異常系：パラメータが8桁数字でない（形式エラー）場合、リターンコード92で例外が投げられること")
    void testExecute_InvalidFormat() {
        // 1. 形式不備のパラメータ（例：ハイフンあり、または桁不足）
        JobParameters params = new JobParametersBuilder()
            .addString("targetDate", "2023-102") 
            .toJobParameters();

        stepExecution = MetaDataInstanceFactory.createStepExecution(params);
        chunkContext = new ChunkContext(new StepContext(stepExecution));
        contribution = new StepContribution(stepExecution);

        // 2. 実行 & 検証
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            target.execute(contribution, chunkContext);
        });

        // BusinessDayServiceが返す "92" を反映しているか確認
        assertTrue(ex.getMessage().contains("コード: 92"));
    }

    @Test
    @DisplayName("異常系：不正な日付形式で実ロジックが例外（またはコード93）を返すこと")
    void testExecute_BusinessError() {
        JobParameters params = new JobParametersBuilder()
            .addString("targetDate", "99999999") // 不正な日付
            .toJobParameters();

        stepExecution = MetaDataInstanceFactory.createStepExecution(params);
        chunkContext = new ChunkContext(new StepContext(stepExecution));
        contribution = new StepContribution(stepExecution);

        // 2. 実行 & 検証
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            target.execute(contribution, chunkContext);
        });

        // BusinessDayServiceが返す "93" を反映しているか確認
        assertTrue(ex.getMessage().contains("コード: 93"));
    }

//    @Test
//    @DisplayName("異常系：祝日ファイルが存在しない場合、リターンコード94が返ること")
//    void testExecute_FileNotFound() {
//        // HolidayManagerを「ファイルなし」の状態で作成
//        HolidayManager holidayManager = new HolidayManager();
//        // 実際に存在しないファイルを読み込ませるか、Reflectionで直接コードを書き換える
//        ReflectionTestUtils.setField(holidayManager, "loadErrorCode", "94");
//        ReflectionTestUtils.setField(businessDayService, "holidayManager", holidayManager);
//
//        // 実行 & 検証
//        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
//            target.execute(contribution, chunkContext);
//        });
//        assertTrue(ex.getMessage().contains("コード: 94"));
//    }

    @Test
    @DisplayName("異常系：祝日ファイルが存在しない場合、リターンコード94が返ること")
    void testExecute_FileNotFound() {
        // 1. パラメータ準備
        JobParameters params = new JobParametersBuilder()
            .addString("targetDate", "20231023")
            .toJobParameters();
        stepExecution = MetaDataInstanceFactory.createStepExecution(params);
        chunkContext = new ChunkContext(new StepContext(stepExecution));
        contribution = new StepContribution(stepExecution);

        // 2. 意図的に「ファイルなし(94)」状態のマネージャーを作成
        HolidayManager badManager = new HolidayManager();
        ReflectionTestUtils.setField(badManager, "loadErrorCode", "94");

        // 3. サービスにこの「悪いマネージャー」をセット
        ReflectionTestUtils.setField(businessDayService, "holidayManager", badManager);
        
        // 4. タスクレットにこの「悪いサービス」を再セット（念のため）
        ReflectionTestUtils.setField(target, "businessDayService", businessDayService);

        // 5. 実行 & 検証
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            target.execute(contribution, chunkContext);
        });

        // ログ出力して実際のメッセージを確認（デバッグ用）
        System.out.println("Actual Exception Message: " + ex.getMessage());

        assertTrue(ex.getMessage().contains("コード: 94"), 
            "例外メッセージにエラーコード94が含まれていること。実際のメッセージ: " + ex.getMessage());
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
