package com.example.batch.my_batch_job;

import org.springframework.batch.core.launch.support.CommandLineJobRunner;

import com.example.batch.my_batch_job.config.date.CalendarJobConfig;

public class JobLauncherMain {
    public static void main(String[] args) throws Exception {
        // 1. 外部へのDTDアクセスを許可する（念のため）
        System.setProperty("javax.xml.accessExternalDTD", "all");
        
        // 2. ★追加：XMLの検証自体をスキップする設定（環境により有効な方を試してください）
        // これにより DOCTYPE がなくてもエラーにならなくなります
        System.setProperty("jdk.xml.overrideDefaultParser", "true");

        // CommandLineJobRunnerの仕組みを使いつつ、Java構成クラスを直接指定して実行
        // 引数: [ジョブ名, 起動パラメータ...]
        // 注: Configクラス自体はプログラム内で直接指定
        CommandLineJobRunner.main(new String[] {
            CalendarJobConfig.class.getName(), 
            "calendarRegistrationJob"
        });
    }
}
