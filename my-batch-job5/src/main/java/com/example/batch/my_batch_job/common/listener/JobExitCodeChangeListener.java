package com.example.batch.my_batch_job.common.listener;

import java.util.Collection;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

/**
 * JobExecutionListenerの実装
JobExecutionListenerインタフェースを利用してジョブの終了コードを条件により変更する。
ここでは、JobExecutionListenerインタフェースの実装クラスとして、 最終的なジョブの終了コードを各ステップの終了コードに合わせて変更する処理を実装する。
 * @author user
 *
 */
@Component
public class JobExitCodeChangeListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        // do nothing.
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        Collection<StepExecution> stepExecutions = jobExecution.getStepExecutions();
        for (StepExecution stepExecution : stepExecutions) { // (1)
            if ("SKIPPED".equals(stepExecution.getExitStatus().getExitCode())) {
                jobExecution.setExitStatus(new ExitStatus("SKIPPED"));
                break;
            }
        }
    }
}
