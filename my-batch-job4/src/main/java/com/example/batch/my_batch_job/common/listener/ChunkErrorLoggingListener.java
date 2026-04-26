package com.example.batch.my_batch_job.common.listener;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.item.validator.ValidationException;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;

@Component
public class ChunkErrorLoggingListener implements ChunkListener {
    private static final Logger logger = LoggerFactory.getLogger(ChunkErrorLoggingListener.class);

    @Inject
    MessageSource messageSource; // (1)

    @Override
    public void beforeChunk(ChunkContext chunkContext) {
        // do nothing.
    }

    @Override
    public void afterChunk(ChunkContext chunkContext) {
        // do nothing.
    }

    @Override
    public void afterChunkError(ChunkContext chunkContext) {
        Exception e = (Exception) chunkContext.getAttribute(ChunkListener.ROLLBACK_EXCEPTION_KEY); // (2)
        if (e instanceof ValidationException) {
            logger.error(messageSource
                    .getMessage("errors.maxInteger", new String[] { "point", "1000000" }, Locale.getDefault())); // (3)
        }
    }
}
