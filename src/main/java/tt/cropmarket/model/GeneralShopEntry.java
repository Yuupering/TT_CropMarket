package tt.cropmarket.model;

import org.bukkit.Material;

/**
 * 일반 농작물 판매 상점의 단일 아이템 항목
 */
public class GeneralShopEntry {

    private final String   id;
    private final String   displayName;
    private final Material icon;
    private final ItemType itemType;
    private final String   itemId;
    private final String   mmoitemsType;
    private final int      sellAmount;
    private final double   price;   // 확정 고정가

    public GeneralShopEntry(String id, String displayName, Material icon,
                             ItemType itemType, String itemId, String mmoitemsType,
                             int sellAmount, double price) {
        this.id          = id;
        this.displayName = displayName;
        this.icon        = icon;
        this.itemType    = itemType;
        this.itemId      = itemId;
        this.mmoitemsType = mmoitemsType;
        this.sellAmount  = sellAmount;
        this.price       = price;
    }

    public String   getId()           { return id; }
    public String   getDisplayName()  { return displayName; }
    public Material getIcon()         { return icon; }
    public ItemType getItemType()     { return itemType; }
    public String   getItemId()       { return itemId; }
    public String   getMmoitemsType() { return mmoitemsType; }
    public int      getSellAmount()   { return sellAmount; }
    public double   getPrice()        { return price; }
}
