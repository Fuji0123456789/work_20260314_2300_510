package com.example.batch.my_batch_job.dbaccess.tasklet;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.validator.Validator;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import com.example.batch.my_batch_job.common.dto.MemberInfoDto;
import com.example.batch.my_batch_job.common.repository.MemberInfoRepository;

import jakarta.inject.Inject;

@Component
public class MemberSearchTasklet implements Tasklet {

    // ItemStreamReader / ItemWriter をやめ、Mapperを直接Injectする
    @Inject
    MemberInfoRepository memberInfoRepository;

    @Inject
    Validator<MemberInfoDto> validator;

    @Inject
    MessageSource messageSource;

    private static final Logger logger = LoggerFactory.getLogger(MemberSearchTasklet.class);

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        //inputStatusでmember_infoを検索した1件目の各項目出力
        // パラメータから処理対象のステータスを受け取る例
        Map<String, Object> params = chunkContext.getStepContext().getJobParameters();
        logger.info(params.toString());
        String status = (String) params.get("inputStatus"); 
        logger.info(status);
        // その値を使ってリポジトリを呼ぶ
        List<MemberInfoDto> items = memberInfoRepository.findByStatus(status);
        logger.info("getId："+items.get(0).getId());
        logger.info("getType："+items.get(0).getType());
        logger.info("getStatus："+items.get(0).getStatus());
        logger.info("getPoint："+items.get(0).getPoint());

        // 終了を通知（nullを返すとリピートを意味するため、通常はFINISHEDを返す）
        return RepeatStatus.FINISHED;
    }
}
