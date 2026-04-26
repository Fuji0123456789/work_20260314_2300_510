rem 使い方
rem コマンドプロンプトから以下のように実行します。
rem run-job.bat job01
rem run-job.bat job01 targetDate=20260411

@echo off
setlocal

rem --- 設定項目 ---
set "MAIN_CLASS=org.springframework.batch.core.launch.support.CommandLineJobRunner"
set "CONFIG_CLASS=com.example.batch.my_batch_job.config.LaunchContextConfig"
set "JOB_CONFIG_CLASS=com.example.batch.my_batch_job.jobs.Job01Config"
set "LIB_DIR=lib"
set "TARGET_DIR=target\classes"

rem --- クラスパスの組み立て ---
rem lib配下の全JARと、自分のプロジェクトのクラスファイルを指定
set "CLASSPATH=%TARGET_DIR%;%LIB_DIR%\*"

rem --- ジョブの実行 ---
rem 引数1：ジョブ名 (例: job01)
rem 引数2以降：ジョブパラメータ (例: date=20260411)
set "JOB_NAME=%1"
shift
set "PARAMS=%*"

echo [INFO] Starting Job: %JOB_NAME%...

java -cp "%CLASSPATH%" ^
    %MAIN_CLASS% ^
    %CONFIG_CLASS%,%JOB_CONFIG_CLASS% ^
    %JOB_NAME% ^
    %PARAMS%

rem --- 終了コードの返却 ---
set "EXIT_CODE=%ERRORLEVEL%"
echo [INFO] Job Finished with Exit Code: %EXIT_CODE%

exit /b %EXIT_CODE%
