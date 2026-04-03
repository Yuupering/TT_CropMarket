package tt.cropmarket.model;

import java.util.LinkedList;

public class GradeData {

    private double currentPrice;
    private int salesCount;
    private final LinkedList<Double> priceHistory = new LinkedList<>();
    private static final int MAX_HISTORY = 5;

    /** 붕괴 복구 예정 시각 (epoch ms). 0 = 붕괴 상태 아님 */
    private long crashRecoveryAt = 0L;

    public GradeData(double initialPrice) {
        this.currentPrice = initialPrice;
        this.salesCount = 0;
    }

    /** 데이터 로드 시 사용 – 히스토리에 영향 없음 */
    public void loadPrice(double price) {
        this.currentPrice = price;
        this.priceHistory.clear();
    }

    /** 가격 변동 기록 포함 업데이트 */
    public void updatePrice(double newPrice) {
        priceHistory.addLast(currentPrice);
        if (priceHistory.size() > MAX_HISTORY) priceHistory.removeFirst();
        this.currentPrice = newPrice;
    }

    /** 최근 N개 가격 기준 추세 (양수 = 상승, 음수 = 하락) */
    public double getPriceTrend() {
        if (priceHistory.isEmpty()) return 0.0;
        return currentPrice - priceHistory.peekFirst();
    }

    public double getCurrentPrice()      { return currentPrice; }
    public int getSalesCount()           { return salesCount; }
    public void incrementSales()         { salesCount++; }
    public void resetSales()             { salesCount = 0; }

    public long getCrashRecoveryAt()           { return crashRecoveryAt; }
    public void setCrashRecoveryAt(long time)  { this.crashRecoveryAt = time; }

    /**
     * 붕괴 상태일 때 남은 복구 시간에 따른 상태 문구를 반환합니다.
     * 붕괴 상태가 아니거나 복구 예정 시각이 없으면 null 반환.
     */
    public String getCrashStatusLine() {
        if (currentPrice > 0 || crashRecoveryAt <= 0) return null;
        long remaining = Math.max(0L, crashRecoveryAt - System.currentTimeMillis());
        long minutes   = remaining / 60000L;
        if (minutes >= 180) return "§c⚠ 시장이 붕괴 되었습니다!";
        if (minutes >= 120) return "§e⚠ 시장이 회복중입니다..";
        if (minutes >= 60)  return "§6⚠ 시장의 공기가 달라지고 있습니다..";
        if (minutes >= 30)  return "§a⚠ 시장 복구가 감지되고 있습니다!";
        return "§a⚡ 거래 재개가 임박했습니다!";
    }
}
