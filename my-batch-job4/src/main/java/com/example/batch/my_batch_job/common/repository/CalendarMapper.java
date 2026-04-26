package com.example.batch.my_batch_job.common.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.batch.my_batch_job.common.entity.CalendarEntity;

@Mapper
public interface CalendarMapper {
    // リストを受け取って一括登録（UPSERT形式）
    int insertBatch(List<CalendarEntity> calendarList);
}
