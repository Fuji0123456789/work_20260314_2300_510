CREATE TABLE CALENDAR_MASTER (
    CALENDAR_DATE CHAR(8) PRIMARY KEY, -- yyyyMMdd
    HOLIDAY_NAME VARCHAR(100),         -- 祝日名（平日ならNULL）
    DAY_OF_WEEK INT,                   -- 0(月)〜6(日)
    WEEK_NUMBER INT,                   -- その月の第何週か
    IS_BUSINESS_DAY INT                -- 第2・4土曜(非祝日)なら1、他は0
);
