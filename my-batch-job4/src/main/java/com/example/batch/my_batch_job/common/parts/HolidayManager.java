package com.example.batch.my_batch_job.common.parts;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class HolidayManager {

    // ★ Set から Map に変更（日付, 祝日名）
    private final Map<LocalDate, String> holidays = new HashMap<>();
    private static final DateTimeFormatter CSV_FORMATTER = DateTimeFormatter.ofPattern("yyyy/M/d");
    // エラー状態を保持するフィールド
    // 00:正常, 94:ファイルなし, 95:その他エラー
    private String loadErrorCode = "00";

    @PostConstruct
    public void loadHolidays() {
        // Classpath上の syukujitsu.csv を読み込む
        ClassPathResource resource = new ClassPathResource("syukujitsu.csv");
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), Charset.forName("MS932")))) {
            
            // 1行目はヘッダーなのでスキップ
            br.readLine();
            
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length >= 2) {
                    // 1列目の日付（yyyy/M/d形式）をパース
                    LocalDate date = LocalDate.parse(values[0], CSV_FORMATTER);
                    // 2列目：祝日名
                    String name = values[1].replace("\"", ""); // 引用符除去
                    holidays.put(date, name);
                }
            }
            this.loadErrorCode = "00"; // 正常終了
        } catch (FileNotFoundException e) {
            this.loadErrorCode = "94"; // ファイルが存在しない
//            throw new RuntimeException("class path resource [syukujitsu.csv] cannot be opened because it does not exist", e);
        } catch (Exception e) {
            this.loadErrorCode = "95"; // パースエラーなどその他
//            throw new RuntimeException("祝日データの読み込みに失敗しました。", e);
        }
    }

	public String getLoadErrorCode() {
		return loadErrorCode;
	}

	public void setLoadErrorCode(String loadErrorCode) {
		this.loadErrorCode = loadErrorCode;
	}

    // ★ 日付を渡して祝日名を取得するメソッド（祝日でなければ null）
	public String getHolidayName(LocalDate date) {
		// TODO 自動生成されたメソッド・スタブ
        return holidays.get(date);
	}

    public boolean isHoliday(LocalDate date) {
        return holidays.containsKey(date);
    }
}
