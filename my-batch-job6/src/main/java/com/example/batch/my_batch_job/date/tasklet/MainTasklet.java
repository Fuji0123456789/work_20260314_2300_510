package com.example.batch.my_batch_job.date.tasklet;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.batch.my_batch_job.common.parts.BusinessDayService;
import com.example.batch.my_batch_job.common.parts.BusinessDayService.BusinessDayResult;

/**
 * 前営業日を算出し、ジョブ内で共有可能にするタスクレット。
 * 起動パラメータから取得した日付を基に計算を行い、結果を JobExecutionContext に保存します。
 */
@Component
public class MainTasklet implements Tasklet {

    /** ロガー（自クラス名を指定） */
    private static final Logger logger = LoggerFactory.getLogger(MainTasklet.class);

    @Autowired
    private BusinessDayService businessDayService;

    /**
     * 前営業日の算出処理を実行します。
     * 
     * @param contribution ステップの寄与
     * @param chunkContext チャンクのコンテキスト。ここからパラメータ取得および結果保存を行う
     * @return 処理終了ステータス
     * @throws Exception 算出サービス内で異常を検知した場合（例外がスローされる）
     */
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        // ① 起動パラメータ（JobParameters）から基準日を取得
        Map<String, Object> jobParameters = chunkContext.getStepContext().getJobParameters();
        String inputDate = (String) jobParameters.get("targetDate");

        if (inputDate == null) {
            // パラメータ自体がない場合はリターンコード81相当だが、早期にチェック
            throw new IllegalArgumentException("起動パラメータ 'targetDate' が指定されていません。コード: 81");
        }

        // ② 1営業日前算出サービスの呼び出し
        // サービス内で異常（入力不正、DBエラー、マスタ不足等）があれば RuntimeException がスローされるため、
        // ここに到達した時点で「正常終了(00)」であることが保証される。
        BusinessDayResult result = businessDayService.calculatePrevious(inputDate);

        // ③ 算出結果およびリターンコードのログ出力
        logger.info("基準日: {}", inputDate);
        logger.info("算出結果（前営業日）: {}", result.date());
        logger.info("リターンコード: {}", result.returnCode());
        logger.info("1営業日前算出成功");

        // ④ 算出結果を JobExecutionContext に保存（後続ステップで利用可能にする）
        chunkContext.getStepContext()
                    .getStepExecution()
                    .getJobExecution()
                    .getExecutionContext()
                    .put("previousBusinessDay", result.date());

        return RepeatStatus.FINISHED;
    }
}
