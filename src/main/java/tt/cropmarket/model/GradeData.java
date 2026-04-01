package tt.cropmarket.model;

import java.util.LinkedList;

public class GradeData {

    private double currentPrice;
    private int salesCount;
    private final LinkedList<Double> priceHistory = new LinkedList<>();
    private static final int MAX_HISTORY = 5;

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
}
