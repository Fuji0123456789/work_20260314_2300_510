package com.example.batch.my_batch_job.date.tasklet;

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

@Component
public class MainTasklet implements Tasklet {

    @Autowired
    private BusinessDayService businessDayService;

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		// TODO 自動生成されたメソッド・スタブ
	    final Logger logger = LoggerFactory.getLogger(BusinessDayService.class);

	    // ① 起動パラメータ（JobParameters）から日付取得
        String inputDate = (String) chunkContext.getStepContext()
                                               .getJobParameters()
                                               .get("targetDate");

        if (inputDate == null) {
            throw new IllegalArgumentException("パラメータ 'targetDate' が指定されていません。");
        }

        // ② 1営業日前算出クラス（サービス）の呼び出し
        BusinessDayResult result = businessDayService.calculatePrevious(inputDate);

        // ③ 算出結果およびリターンコードのログ出力
        logger.info("算出結果（日付）: {}", result.date());
        logger.info("リターンコード: {}", result.returnCode());

        // ④ 判定処理
        if ("00".equals(result.returnCode())) {
        	logger.info("1営業日前算出成功");
            // ★ ここを追加：算出結果を保存する
            chunkContext.getStepContext()
                        .getStepExecution()
                        .getJobExecution()
                        .getExecutionContext()
                        .put("previousBusinessDay", result.date());
                        
        } else {
            // 異常値（"91"など）であれば業務例外を投げる
            throw new RuntimeException("1営業日前算出で異常値が発生しました。コード: " + result.returnCode());
        }

        return RepeatStatus.FINISHED;
	}

}
