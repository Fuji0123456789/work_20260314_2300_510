package com.example.batch.my_batch_job.common.listener;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

/**
 * StepExecutionListenerの実装
StepExecutionListenerインタフェースを利用してステップの終了コードを条件により変更する。
ここでは、StepExecutionListenerインタフェースの実装クラスとして、 入力データと出力データの件数が異なる場合に終了コードをSKIPPEDに変更する処理を実装する。

なお、このクラスはタスクレットモデルでは作成する必要がない。 タスクレットモデルではTaskletの実装クラス内でStepExecutionクラスに独自の終了コードを設定することができるためである。
 * @author user
 *
 */
@Component
public class StepExitStatusChangeListener implements StepExecutionListener {

    @Override
    public void beforeStep(StepExecution stepExecution) {
        // do nothing.
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        ExitStatus exitStatus = stepExecution.getExitStatus();
        if (conditionalCheck(stepExecution)) {
            exitStatus = new ExitStatus("SKIPPED"); // (1)
        }
        return exitStatus;
    }

    private boolean conditionalCheck(StepExecution stepExecution) {
        return (stepExecution.getWriteCount() != stepExecution.getReadCount()); // (2)
    }
}
