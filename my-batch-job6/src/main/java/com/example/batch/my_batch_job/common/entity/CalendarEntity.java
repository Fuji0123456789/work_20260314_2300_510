package com.example.batch.my_batch_job.common.entity;

public record CalendarEntity(
	    String calendarDate,
	    String holidayName,
	    int dayOfWeek,
	    int weekNumber,
	    int isBusinessDay
	) {}
