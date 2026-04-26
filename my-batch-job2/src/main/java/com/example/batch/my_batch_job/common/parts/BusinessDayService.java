package com.example.batch.my_batch_job.common.parts;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BusinessDayService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired
    private HolidayManager holidayManager;

    public BusinessDayResult calculatePrevious(String dateStr) {
        // 1. null または 空文字チェック
        if (dateStr == null || dateStr.isEmpty()) {
            return new BusinessDayResult(null, "91"); // 未設定エラー
        }

        // 2. 形式チェック（8桁数字であるか）
        if (!dateStr.matches("^[0-9]{8}$")) {
            return new BusinessDayResult(null, "92"); // 形式エラー（数字以外や桁数違い）
        }

        // 3. 祝日マネージャーのロード状態チェック
        String holidayStatus = holidayManager.getLoadErrorCode();
        if (!"00".equals(holidayStatus)) {
            return new BusinessDayResult(null, holidayStatus); // 94 または 95 を返す
        }

        try {
            // 3. 妥当な日付かチェック（LocalDate.parseで存在しない日付、例: 20230230 を判定）
            LocalDate date = LocalDate.parse(dateStr, FORMATTER);
            
            LocalDate prev = date.minusDays(1);
            
            // 土日、または祝日の間はさらに1日前へ
            while (isNonBusinessDay(prev)) {
                prev = prev.minusDays(1);
            }
            
            // 正常終了: リターンコード "00"
            return new BusinessDayResult(prev.format(FORMATTER), "00");

        } catch (DateTimeParseException e) {
            // 4. 日付として不適当（例：20231340など）
            return new BusinessDayResult(null, "93"); // 日付妥当性エラー
        }
    }

    // 祝日・土日判定メソッド
    private boolean isNonBusinessDay(LocalDate date) {
        // 土日判定
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return true;
        }
        // 祝日判定
        return holidayManager.isHoliday(date);
    }

    public record BusinessDayResult(String date, String returnCode) {}
}
