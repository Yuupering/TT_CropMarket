package tt.cropmarket.model;

public enum ItemGrade {
    NORMAL("§f일반", "§f"),
    SILVER("§b은",   "§b"),
    GOLD(  "§e금",   "§e");

    private final String displayName;
    private final String colorCode;

    ItemGrade(String displayName, String colorCode) {
        this.displayName = displayName;
        this.colorCode = colorCode;
    }

    public String getDisplayName() { return displayName; }
    public String getColorCode() { return colorCode; }
}
