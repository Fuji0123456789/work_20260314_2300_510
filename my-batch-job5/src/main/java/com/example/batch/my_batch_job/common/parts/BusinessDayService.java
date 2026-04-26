package com.example.batch.my_batch_job.common.parts;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.stereotype.Service;

import com.example.batch.my_batch_job.common.repository.CalendarMapper;

/**
 * 営業日計算に関する業務ロジックを提供するサービスクラス。
 * カレンダマスタテーブルを参照し、特定の基準日から数えた営業日の算出を行います。
 */
@Service
public class BusinessDayService {

    /** カレンダマスタへアクセスするためのマッパー */
    @Autowired
    private CalendarMapper calendarMapper;

    /** 厳格な日付チェック用のフォーマッタ（uuuuは西暦、STRICTは厳密判定） */
    private static final DateTimeFormatter STRICT_FORMATTER = 
            DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);

    /**
     * 指定された日付の「1営業日前」を算出します。
     * 判定にはデータベースの CALENDAR_MASTER テーブルを使用します。
     * 正常（00）以外は、具体的な原因をメッセージに含めて例外（RuntimeException）をスローします。
     *
     * <pre>
     * 【リターンコード体系】
     * 正常終了    00       算出に成功
     * 運用ミス    81       基準日が未設定（null/空文字）
     * 運用ミス    82       形式不正（桁数/実在しない日付）
     * データ不備  91       カレンダマスタに該当期間のデータがない
     * インフラ    92       DB接続失敗・タイムアウト
     * アプリ欠陥  93       SQL文法エラー・テーブル定義不正
     * 不明        99       予期せぬ実行時例外
     * </pre>
     *
     * @param dateStr 基準日（yyyyMMdd形式の8桁文字列）
     * @return 算出結果の日付と処理結果コードを含む BusinessDayResult オブジェクト
     * @throws RuntimeException 正常終了（00）以外の事象が発生した場合
     */
    public BusinessDayResult calculatePrevious(String dateStr) {
        
        // --- 1. 入力チェック (運用ミス系: 80s) ---
        if (dateStr == null || dateStr.isEmpty()) {
            throw new RuntimeException("基準日が指定されていません。コード: 81");
        }
        
        if (!dateStr.matches("^[0-9]{8}$")) {
            throw new RuntimeException("基準日の形式が数字8桁ではありません(" + dateStr + ")。コード: 82");
        }
        
        try {
            // ResolverStyle.STRICTにより、20230230などはここで例外が飛ぶ
            LocalDate.parse(dateStr, STRICT_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new RuntimeException("基準日が実在しない日付です(" + dateStr + ")。コード: 82", e);
        }

        // --- 2. データベース操作 (90s) ---
        String prevDate;
        try {
            prevDate = calendarMapper.findPreviousBusinessDay(dateStr);
            
        } catch (CannotGetJdbcConnectionException | QueryTimeoutException e) {
            // インフラ起因 (92)
            throw new RuntimeException("データベース接続失敗またはタイムアウトが発生しました。コード: 92", e);
            
        } catch (BadSqlGrammarException e) {
            // アプリ欠陥：SQL文法エラー、テーブル・カラム不備 (93)
            throw new RuntimeException("SQL文法エラーまたはテーブル定義に不備があります。コード: 93", e);
            
        } catch (DataAccessException e) {
            // アプリ欠陥：その他のDB操作エラー (93)
            throw new RuntimeException("データベースアクセス中にエラーが発生しました。コード: 93", e);
            
        } catch (Exception e) {
            // 未知の例外 (99)
            throw new RuntimeException("営業日算出中に予期せぬシステム例外が発生しました。コード: 99", e);
        }

        // --- 3. データ整合性チェック (91) ---
        if (prevDate == null) {
            // データ不備：基準日以前の営業日がマスタに存在しない (91)
            throw new RuntimeException("カレンダマスタに対象期間の営業日データが存在しません。コード: 91");
        }

        // 全ての関門を突破した場合のみ、正常コードでリターン
        return new BusinessDayResult(prevDate, "00");
    }

    /**
     * 営業日計算の結果を保持するレコード。
     * 
     * @param date 算出された日付（yyyyMMdd形式）。正常時以外は原則復帰しない。
     * @param returnCode 処理結果を識別するリターンコード（正常時は "00"）
     */
    public record BusinessDayResult(String date, String returnCode) {}
}
