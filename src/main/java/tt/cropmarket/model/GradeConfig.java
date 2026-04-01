package tt.cropmarket.model;

public class GradeConfig {

    private final ItemType itemType;
    private final String itemId;
    private final String mmoitemsType; // MMOItems 전용
    private final double basePrice;
    private final double minPrice;
    private final double maxPrice;

    public GradeConfig(ItemType itemType, String itemId, String mmoitemsType,
                       double basePrice, double minPrice, double maxPrice) {
        this.itemType = itemType;
        this.itemId = itemId;
        this.mmoitemsType = mmoitemsType;
        this.basePrice = basePrice;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
    }

    public ItemType getItemType()    { return itemType; }
    public String getItemId()        { return itemId; }
    public String getMmoitemsType()  { return mmoitemsType; }
    public double getBasePrice()     { return basePrice; }
    public double getMinPrice()      { return minPrice; }
    public double getMaxPrice()      { return maxPrice; }
}
