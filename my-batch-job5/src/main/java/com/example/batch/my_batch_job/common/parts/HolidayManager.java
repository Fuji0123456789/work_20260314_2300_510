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

/**
 * 祝日データを管理するコンポーネント。
 * クラスパス上の祝日CSVファイルを読み込み、メモリ上に保持して祝日判定および祝日名の取得機能を提供します。
 */
@Component
public class HolidayManager {

    /** 祝日データを保持するマップ（キー：日付、値：祝日名） */
    private final Map<LocalDate, String> holidays = new HashMap<>();

    /** CSVの日付フォーマット（yyyy/M/d） */
    private static final DateTimeFormatter CSV_FORMATTER = DateTimeFormatter.ofPattern("yyyy/M/d");

    /**
     * 祝日データのロード結果コード。
     * 00:正常、94:ファイルなし、95:その他エラー
     */
    private String loadErrorCode = "00";

    /**
     * インスタンス初期化時に祝日データをCSVからロードします。
     * 内閣府公表の「国民の祝日」CSV（MS932形式）を想定しています。
     * ロードに失敗した場合は例外をスローします。
     * 
     * @throws RuntimeException 祝日CSVの読み込みに失敗した場合
     */
    @PostConstruct
    public void loadHolidays() {
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
                    // 2列目の祝日名から引用符を除去して格納
                    String name = values[1].replace("\"", "");
                    holidays.put(date, name);
                }
            }
            this.loadErrorCode = "00";
        } catch (FileNotFoundException e) {
            this.loadErrorCode = "94";
            throw new RuntimeException("祝日データファイル [syukujitsu.csv] がクラスパス上に見つかりません。", e);
        } catch (Exception e) {
            this.loadErrorCode = "95";
            throw new RuntimeException("祝日データの読み込みまたはパース中に予期しないエラーが発生しました。", e);
        }
    }

    /**
     * 祝日データのロード結果コードを取得します。
     * @return ロード結果コード
     */
    public String getLoadErrorCode() {
        return loadErrorCode;
    }

    /**
     * 祝日データのロード結果コードを設定します（主にテスト用）。
     * @param loadErrorCode 設定するロード結果コード
     */
    public void setLoadErrorCode(String loadErrorCode) {
        this.loadErrorCode = loadErrorCode;
    }

    /**
     * 指定された日付の祝日名を取得します。
     * @param date 判定対象の日付
     * @return 祝日名（祝日でない場合は null）
     */
    public String getHolidayName(LocalDate date) {
        return holidays.get(date);
    }

    /**
     * 指定された日付が祝日かどうかを判定します。
     * @param date 判定対象の日付
     * @return 祝日の場合は true、そうでない場合は false
     */
    public boolean isHoliday(LocalDate date) {
        return holidays.containsKey(date);
    }
}
