package com.example.batch.my_batch_job.dbaccess.tasklet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import com.example.batch.my_batch_job.common.repository.MemberInfoRepository;

import jakarta.inject.Inject;

@Component
public class MemberCountTasklet implements Tasklet {
    @Inject
    MemberInfoRepository memberInfoRepository;
    private static final Logger logger = LoggerFactory.getLogger(MemberCountTasklet.class);

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        //member_infoのtotalCount出力
        logger.info("member_infoのtotalCount：{}", memberInfoRepository.count());

        // 終了を通知（nullを返すとリピートを意味するため、通常はFINISHEDを返す）
        return RepeatStatus.FINISHED;
    }
}
