package com.example.batch.my_batch_job.date.tasklet;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.batch.my_batch_job.common.entity.CalendarEntity;
import com.example.batch.my_batch_job.common.parts.HolidayManager;
import com.example.batch.my_batch_job.common.repository.CalendarMapper;

@Component
public class CalendarRegistrationTasklet implements Tasklet {

    @Autowired
    private HolidayManager holidayManager; // 祝日CSVロード済み

    @Autowired
    private CalendarMapper calendarMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        List<CalendarEntity> list = new ArrayList<>();
        
        // 登録対象期間（例：2023年〜2026年）
        LocalDate start = LocalDate.of(2023, 1, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            String dateStr = date.format(DATE_FORMATTER);
            String holidayName = holidayManager.getHolidayName(date);

        	// ① 曜日 (月0〜日6)
            int dow = date.getDayOfWeek().getValue() - 1;

            // ② 第何週か
            int weekNum = (date.getDayOfMonth() - 1) / 7 + 1;

            // ③ 営業日判定
            int isBizDay = 0;
            if (date.getDayOfWeek() == DayOfWeek.SATURDAY) {
                // 第2・4土曜 かつ 非祝日 なら 1
                if ((weekNum == 2 || weekNum == 4) && holidayName == null) {
                    isBizDay = 1;
                }
            } else if (date.getDayOfWeek() != DayOfWeek.SUNDAY && holidayName == null) {
                // 平日(非祝日)も営業日とするならここに追加
                isBizDay = 1; 
            }

            list.add(new CalendarEntity(dateStr, holidayName, dow, weekNum, isBizDay));

            // 500件ごとに一括登録（大量データ時のヒープ対策）
            if (list.size() >= 500) {
                calendarMapper.insertBatch(list);
                list.clear();
            }

            if (!list.isEmpty()) {
                calendarMapper.insertBatch(list);
            }
        }
        return RepeatStatus.FINISHED;
    }
}
