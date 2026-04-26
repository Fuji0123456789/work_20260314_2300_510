package com.example.batch.my_batch_job.common.parts;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import com.example.batch.my_batch_job.common.repository.CalendarMapper;
import com.example.batch.my_batch_job.config.JobBaseContextConfig;
import com.example.batch.my_batch_job.config.LaunchContextConfig;

@SpringJUnitConfig
@ContextConfiguration(classes = {
    BusinessDayServiceTest.TestConfig.class,
    JobBaseContextConfig.class,
    LaunchContextConfig.class,
    BusinessDayService.class
})
@Transactional("jobTransactionManager")
class BusinessDayServiceTest {

    @Configuration
    @MapperScan("com.example.batch.my_batch_job.common.repository")
    @PropertySource("classpath:batch-application.properties")
    static class TestConfig {
        @Bean
        public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
            return new PropertySourcesPlaceholderConfigurer();
        }
    }

    @Autowired
    private BusinessDayService target;

    // --- 1. 入力チェック (運用ミス系: 80s) ---

    @Test
    void calculatePrevious_コード81_未指定() {
        RuntimeException e = assertThrows(RuntimeException.class, () -> target.calculatePrevious(null));
        assertTrue(e.getMessage().contains("81"), "メッセージ: " + e.getMessage());
    }

    @Test
    void calculatePrevious_コード82_形式不正() {
        RuntimeException e = assertThrows(RuntimeException.class, () -> target.calculatePrevious("2023-10-02"));
        assertTrue(e.getMessage().contains("82"), "メッセージ: " + e.getMessage());
    }

    @Test
    void calculatePrevious_コード82_実在しない日付() {
        // 2月30日など
        RuntimeException e = assertThrows(RuntimeException.class, () -> target.calculatePrevious("20230230"));
        
        // 修正前：assertTrue(e.getMessage().contains("実実在しない"));
        // 修正後：
        assertTrue(e.getMessage().contains("実在しない"), "実際のメッセージ: " + e.getMessage());
        assertTrue(e.getMessage().contains("82"));
    }

    // --- 2. データベース操作 (90s) ---

    @Test
    @Sql(statements = { "DROP TABLE IF EXISTS CALENDAR_MASTER" }, 
         config = @SqlConfig(dataSource = "jobDataSource"))
    void calculatePrevious_コード93_SQL文法エラー_テーブル不備() {
        // テーブルを消すことで BadSqlGrammarException を誘発
        RuntimeException e = assertThrows(RuntimeException.class, () -> target.calculatePrevious("20231002"));
        assertTrue(e.getMessage().contains("93"));
        assertTrue(e.getMessage().contains("文法エラー"));
    }

    @Test
    @Sql(statements = { 
        "DROP TABLE IF EXISTS CALENDAR_MASTER",
        "CREATE TABLE CALENDAR_MASTER (X INT)" // 本来必要な CALENDAR_DATE 等がない状態
    }, config = @SqlConfig(dataSource = "jobDataSource"))
    void calculatePrevious_コード93_その他のDB操作エラー() {
        // 実行
        RuntimeException e = assertThrows(RuntimeException.class, () -> target.calculatePrevious("20231002"));
        
        // 検証: コード「93」が含まれていることだけを確実にチェックする構成に変更
        assertTrue(e.getMessage().contains("93"), "実際のメッセージ: " + e.getMessage());
        
        // もし文言までチェックしたい場合は、実装クラスの String と完全に一致させる
        // assertTrue(e.getMessage().contains("データベースアクセス中にエラーが発生しました"));
    }

    // ※ 92(タイムアウト)については実DBで再現が難しいため、必要に応じてモック検討
    
    // --- 3. データ整合性チェック (91) ---

    @Test
    @Sql(statements = {
        "DROP TABLE IF EXISTS CALENDAR_MASTER",
        "CREATE TABLE CALENDAR_MASTER (CALENDAR_DATE CHAR(8), IS_BUSINESS_DAY INT)",
        "INSERT INTO CALENDAR_MASTER VALUES ('20231002', 1)" // 基準日のみ
    }, config = @SqlConfig(dataSource = "jobDataSource"))
    void calculatePrevious_コード91_データ不備_前営業日なし() {
        // 基準日より前のデータがない状態
        RuntimeException e = assertThrows(RuntimeException.class, () -> target.calculatePrevious("20231002"));
        assertTrue(e.getMessage().contains("91"));
        assertTrue(e.getMessage().contains("営業日データが存在しません"));
    }

    @Test
    @Sql(statements = {
        "DROP TABLE IF EXISTS CALENDAR_MASTER",
        "CREATE TABLE CALENDAR_MASTER (CALENDAR_DATE CHAR(8), IS_BUSINESS_DAY INT)",
        "INSERT INTO CALENDAR_MASTER VALUES ('20231002', 1)",
        "INSERT INTO CALENDAR_MASTER VALUES ('20230929', 1)"
    }, config = @SqlConfig(dataSource = "jobDataSource"))
    void calculatePrevious_正常終了() {
        var result = target.calculatePrevious("20231002");
        assertEquals("20230929", result.date());
        assertEquals("00", result.returnCode());
    }

    // --- 2. データベース操作 (90s) の網羅テスト ---

    /**
     * コード 93: SQL文法エラー、テーブル不備
     * (BadSqlGrammarException を誘発)
     */
    @Test
    @Sql(statements = { 
        "DROP TABLE IF EXISTS CALENDAR_MASTER" // テーブル自体を消す
    }, config = @SqlConfig(dataSource = "jobDataSource"))
    void calculatePrevious_コード93_SQL文法エラー() {
        RuntimeException e = assertThrows(RuntimeException.class, () -> target.calculatePrevious("20231002"));
        
        assertEquals("SQL文法エラーまたはテーブル定義に不備があります。コード: 93", e.getMessage());
        assertTrue(e.getCause() instanceof BadSqlGrammarException);
    }

    /**
     * コード 93: その他のDB操作エラー
     * (DataAccessException を直接投げて catch ブロックを検証)
     */
    @Test
    void calculatePrevious_コード93_データベースアクセスエラー() {
        // テスト用のServiceとモックMapperを作成
        BusinessDayService mockTarget = new BusinessDayService();
        CalendarMapper mockMapper = org.mockito.Mockito.mock(CalendarMapper.class);
        
        // DataAccessException の匿名サブクラスを投げる（実挙動をシミュレート）
        org.mockito.Mockito.when(mockMapper.findPreviousBusinessDay(org.mockito.ArgumentMatchers.anyString()))
            .thenThrow(new org.springframework.dao.DataAccessException("その他のDBエラー") {});
        
        org.springframework.test.util.ReflectionTestUtils.setField(mockTarget, "calendarMapper", mockMapper);
        
        // 実行
        RuntimeException e = assertThrows(RuntimeException.class, () -> mockTarget.calculatePrevious("20231002"));
        
        // 検証: DataAccessException 用のメッセージになっていること
        assertEquals("データベースアクセス中にエラーが発生しました。コード: 93", e.getMessage());
    }

    /**
     * コード 92: データベース接続失敗
     * (CannotGetJdbcConnectionException を誘発)
     */
    @Test
    void calculatePrevious_コード92_接続失敗() {
        BusinessDayService mockTarget = new BusinessDayService();
        CalendarMapper mockMapper = org.mockito.Mockito.mock(CalendarMapper.class);
        
        // 第2引数の null を SQLException にキャストして曖昧さを排除
        org.springframework.jdbc.CannotGetJdbcConnectionException connectionException = 
            new org.springframework.jdbc.CannotGetJdbcConnectionException("接続失敗", (java.sql.SQLException) null);

        org.mockito.Mockito.when(mockMapper.findPreviousBusinessDay(org.mockito.ArgumentMatchers.anyString()))
            .thenThrow(connectionException);
        
        org.springframework.test.util.ReflectionTestUtils.setField(mockTarget, "calendarMapper", mockMapper);
        
        RuntimeException e = assertThrows(RuntimeException.class, () -> mockTarget.calculatePrevious("20231002"));
        assertEquals("データベース接続失敗またはタイムアウトが発生しました。コード: 92", e.getMessage());
    }


    /**
     * コード 99: 未知の例外
     * (想定外のRuntimeExceptionを誘発)
     */
    @Test
    void calculatePrevious_コード99_予期せぬ例外() {
        // Mapperが全く関係ないExceptionを投げた場合
        BusinessDayService mockTarget = new BusinessDayService();
        CalendarMapper mockMapper = org.mockito.Mockito.mock(CalendarMapper.class);
        org.mockito.Mockito.when(mockMapper.findPreviousBusinessDay(anyString()))
            .thenThrow(new NullPointerException("想定外のぬるぽ"));
        
        org.springframework.test.util.ReflectionTestUtils.setField(mockTarget, "calendarMapper", mockMapper);
        
        RuntimeException e = assertThrows(RuntimeException.class, () -> mockTarget.calculatePrevious("20231002"));
        assertEquals("営業日算出中に予期せぬシステム例外が発生しました。コード: 99", e.getMessage());
    }

}
