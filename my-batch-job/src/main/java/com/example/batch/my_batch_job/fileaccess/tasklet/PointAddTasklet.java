package com.example.batch.my_batch_job.fileaccess.tasklet;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.batch.item.validator.ValidationException;
import org.springframework.batch.item.validator.Validator;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.example.batch.my_batch_job.common.dto.MemberInfoDto;

import jakarta.inject.Inject;

@Component
@Scope("step")
public class PointAddTasklet implements Tasklet {

    private static final String TARGET_STATUS = "1"; // (3)

    private static final String INITIAL_STATUS = "0"; // (4)

    private static final String GOLD_MEMBER = "G"; // (5)

    private static final String NORMAL_MEMBER = "N"; // (6)

    private static final int MAX_POINT = 1000000; // (7)

    private static final int CHUNK_SIZE = 10; // (8)

    @Inject // (9)
    ItemStreamReader<MemberInfoDto> reader; // (10)

    @Inject // (9)
    ItemStreamWriter<MemberInfoDto> writer; // (11)

    private static final Logger logger = LoggerFactory.getLogger(PointAddTasklet.class);

    @Inject
    Validator<MemberInfoDto> validator;

    @Inject
    MessageSource messageSource;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception { // (12)
        MemberInfoDto item = null;

        List<MemberInfoDto> items = new ArrayList<>(CHUNK_SIZE); // (13)
        int errorCount = 0;

        try {

            reader.open(chunkContext.getStepContext().getStepExecution().getExecutionContext()); // (14)
            writer.open(chunkContext.getStepContext().getStepExecution().getExecutionContext()); // (14)

            while ((item = reader.read()) != null) { // (15)

                try {
                    validator.validate(item);
                } catch (ValidationException e) {
                    logger.warn(messageSource
                            .getMessage("errors.maxInteger", new String[] { "point", "1000000" }, Locale.getDefault()));
                    errorCount++;
                    continue;
                }

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

                items.add(item);

                if (items.size() == CHUNK_SIZE) { // (16)
                    writer.write(new Chunk(items)); // (17)
                    items.clear();
                }
            }

            writer.write(new Chunk(items)); // (18)
        } finally {
            try {
                reader.close(); // (19)
            } catch (ItemStreamException e) {
                // do nothing.
            }
            try {
                writer.close(); // (19)
            } catch (ItemStreamException e) {
                // do nothing.
            }
        }

        if (errorCount > 0) {
            contribution.setExitStatus(new ExitStatus("SKIPPED"));
        }
        return RepeatStatus.FINISHED; // (20)
    }
}
