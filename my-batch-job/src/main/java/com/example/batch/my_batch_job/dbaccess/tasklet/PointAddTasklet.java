package com.example.batch.my_batch_job.dbaccess.tasklet;

import java.util.Locale;

import org.apache.ibatis.cursor.Cursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.validator.ValidationException;
import org.springframework.batch.item.validator.Validator;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import com.example.batch.my_batch_job.common.dto.MemberInfoDto;
import com.example.batch.my_batch_job.common.repository.MemberInfoRepository;

import jakarta.inject.Inject;

@Component
public class PointAddTasklet implements Tasklet {

    private static final String TARGET_STATUS = "1"; // (2)

    private static final String INITIAL_STATUS = "0"; // (3)

    private static final String GOLD_MEMBER = "G"; // (4)

    private static final String NORMAL_MEMBER = "N"; // (5)

    private static final int MAX_POINT = 1000000; // (6)

//    private static final int CHUNK_SIZE = 10;

    // ItemStreamReader / ItemWriter をやめ、Mapperを直接Injectする
    @Inject
    MemberInfoRepository memberInfoRepository;

    @Inject
    Validator<MemberInfoDto> validator;

    @Inject
    MessageSource messageSource;

    private static final Logger logger = LoggerFactory.getLogger(PointAddTasklet.class);

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        int errorCount = 0;
        
        //member_info更新
        // 1. データの全件取得 (リスト取得)
        // ※戻り値が Cursor<MemberInfoDto> の場合は for-each で回せます
        try (Cursor<MemberInfoDto> cursor = memberInfoRepository.cursor()) {

	        for (MemberInfoDto item : cursor) {
	            // 2. バリデーション
	            try {
	                validator.validate(item);
	            } catch (ValidationException e) {
	                logger.warn(messageSource.getMessage("errors.maxInteger", 
	                        new String[] { "point", "1000000" }, Locale.getDefault()));
	                errorCount++;
	                continue;
	            }
	
	            // 3. 業務ロジック（ポイント加算）
	            if (TARGET_STATUS.equals(item.getStatus())) {
	                if (GOLD_MEMBER.equals(item.getType())) {
	                    item.setPoint(item.getPoint() + 100);
	                } else if (NORMAL_MEMBER.equals(item.getType())) {
	                    item.setPoint(item.getPoint() + 10);
	                }
	
	                if (item.getPoint() > MAX_POINT) {
	                    item.setPoint(MAX_POINT);
	                }
	                item.setStatus(INITIAL_STATUS);
	            }
	
	            // 4. 更新の実行
	            memberInfoRepository.updatePointAndStatus(item);
	        }
        }
        
        // スキップ発生時のステータス設定
        if (errorCount > 0) {
            contribution.setExitStatus(new ExitStatus("SKIPPED"));
        }
        // 終了を通知（nullを返すとリピートを意味するため、通常はFINISHEDを返す）
        return RepeatStatus.FINISHED;
    }
}
