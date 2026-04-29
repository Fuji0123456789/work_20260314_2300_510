package com.example.batch.my_batch_job.trial;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class Trial {

	public static void main(String[] args) {
	    CharSequence dateStr = "20230230";
	    
	    // ResolverStyle.STRICT を指定して厳格にチェックする
	    // STRICTモードでは 'y' ではなく 'u' を使う必要がある
	    DateTimeFormatter strictFormatter = DateTimeFormatter.ofPattern("uuuuMMdd")
	                                          .withResolverStyle(java.time.format.ResolverStyle.STRICT);
	    try {
	        LocalDate.parse(dateStr, strictFormatter);
	        System.out.println("パース成功: " + dateStr);
	    } catch (DateTimeParseException e) {
	        throw new RuntimeException("基準日が実在しない日付です(" + dateStr + ")。コード: 82", e);
	    }
	}

}
