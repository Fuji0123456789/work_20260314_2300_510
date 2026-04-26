package com.example.batch.my_batch_job.common.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.batch.my_batch_job.common.entity.CalendarEntity;

@Mapper
public interface CalendarMapper {
    // リストを受け取って一括登録（UPSERT形式）
    int insertBatch(List<CalendarEntity> calendarList);

    // 指定した日付より過去で、最新の営業日(IS_BUSINESS_DAY=1)を1件取得
    String findPreviousBusinessDay(@Param("targetDate") String targetDate);
}
