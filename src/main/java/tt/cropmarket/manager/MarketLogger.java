package tt.cropmarket.manager;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * CropMarket 전용 파일 로거
 * plugins/CropMarket/logs/market-YYYY-MM-DD.log 형식으로 날짜별 기록
 */
public class MarketLogger {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final File logDir;

    public MarketLogger(File dataFolder) {
        this.logDir = new File(dataFolder, "logs");
        logDir.mkdirs();
    }

    // ──────────────────────────────────────────
    //  이벤트별 로그 메서드
    // ──────────────────────────────────────────

    /** 판매 이벤트 */
    public void logSell(String playerName, String cropName, String gradeName,
                        int amount, double gross, double tax, double net,
                        double priceAfter, double dropPct) {
        log(String.format(
            "[판매] 플레이어: %s | %s (%s등급) x%d개 | 판매가: %,.0f원 | 세금: %,.0f원 | 실수령: %,.0f원 | 하락: %.1f%% | 판매 후 가격: %,.0f원",
            playerName, cropName, gradeName, amount, gross, tax, net, dropPct, priceAfter
        ));
    }

    /** 시장 붕괴 이벤트 */
    public void logCrash(String playerName, String cropName, String gradeName,
                         double priceBeforeCrash, double crashChancePct) {
        log(String.format(
            "[붕괴] 플레이어: %s | %s (%s등급) | 붕괴 전 가격: %,.0f원 | 발동 확률: %.1f%%",
            playerName, cropName, gradeName, priceBeforeCrash, crashChancePct
        ));
    }

    /** 붕괴 후 가격 복구 이벤트 */
    public void logCrashRecovery(String cropName, String gradeName, double restoredPrice) {
        log(String.format(
            "[붕괴회복] %s (%s등급) | 복구 가격: %,.0f원",
            cropName, gradeName, restoredPrice
        ));
    }

    /** 자연 회복 이벤트 */
    public void logRecovery(String gradeName, String cropName,
                            double beforePrice, double afterPrice, double pct) {
        log(String.format(
            "[회복] %s등급 | %s | %,.0f원 → %,.0f원 (+%.1f%%)",
            gradeName, cropName, beforePrice, afterPrice, pct * 100.0
        ));
    }

    /** 일반 정보 */
    public void logInfo(String message) {
        log("[INFO] " + message);
    }

    // ──────────────────────────────────────────
    //  내부 기록 (synchronized → 스레드 안전)
    // ──────────────────────────────────────────

    public synchronized void log(String message) {
        LocalDateTime now  = LocalDateTime.now();
        String date        = now.format(DATE_FMT);
        String time        = now.format(TIME_FMT);
        String line        = "[" + time + "] " + message;

        File logFile = new File(logDir, "market-" + date + ".log");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
            // 로그 파일 쓰기 실패 시 무시 (서버 콘솔에는 이미 출력됨)
        }
    }
}
