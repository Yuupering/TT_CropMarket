package tt.cropmarket.model;

public enum ItemGrade {
    NORMAL(64, "§f일반", "§f"),
    SILVER(8,  "§b은",   "§b"),
    GOLD(4,    "§e금",   "§e");

    private final int sellAmount;
    private final String displayName;
    private final String colorCode;

    ItemGrade(int sellAmount, String displayName, String colorCode) {
        this.sellAmount = sellAmount;
        this.displayName = displayName;
        this.colorCode = colorCode;
    }

    public int getSellAmount() { return sellAmount; }
    public String getDisplayName() { return displayName; }
    public String getColorCode() { return colorCode; }
}
