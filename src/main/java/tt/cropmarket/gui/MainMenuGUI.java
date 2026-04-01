package tt.cropmarket.gui;

import tt.cropmarket.CropMarketPlugin;
import tt.cropmarket.model.CropEntry;
import tt.cropmarket.model.GradeConfig;
import tt.cropmarket.model.ItemGrade;
import tt.cropmarket.model.ItemType;
import net.Indyuce.mmoitems.MMOItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * 초기 선택 메뉴 (3행)
 *
 * 레이아웃:
 *   행0 [border × 9]
 *   행1 [border][border][border][border][SEEDS 13][border][border][border][border]  -- 씨앗
 *   행1 실제: 슬롯 11 = 씨앗구매 / 슬롯 15 = 농작물판매
 *   행2 [border × 9]
 */
public class MainMenuGUI {

    public static final String TITLE = "§6작물 상점";

    public static final int SLOT_SEEDS  = 11;
    public static final int SLOT_MARKET = 15;

    private final CropMarketPlugin plugin;

    public MainMenuGUI(CropMarketPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        // 전체 유리 테두리
        ItemStack border = border();
        for (int i = 0; i < 27; i++) inv.setItem(i, border);

        // 씨앗 구매 버튼
        inv.setItem(SLOT_SEEDS, makeItem(
            Material.WHEAT_SEEDS,
            "§a씨앗 구매",
            "§7작물 씨앗을 구매합니다.",
            "§8──────────────",
            "§7씨앗 종류: §f" + plugin.getConfigManager().getCrops().stream()
                .filter(c -> c.hasSeed()).count() + "종",
            "§7씨앗 1개당: §f" + String.format("%,.0f", plugin.getConfigManager().getSeedDefaultPrice()) + "§7원",
            "§7구매 단위: §f" + java.util.Arrays.stream(plugin.getConfigManager().getSeedBuyAmounts())
                .mapToObj(Integer::toString).collect(java.util.stream.Collectors.joining("§8 / §f")) + "§7개",
            "",
            "§a» 클릭하여 입장"
        ));

        // 농작물 판매 버튼
        inv.setItem(SLOT_MARKET, makeItem(
            Material.EMERALD,
            "§6농작물 판매",
            "§7수확한 농작물을 판매합니다.",
            "§8──────────────",
            "§7작물 종류: §f" + plugin.getConfigManager().getCrops().size() + "종",
            "",
            "§e» 클릭하여 입장"
        ));

        player.openInventory(inv);
    }

    private ItemStack border() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack getFirstCropIcon() {
        List<CropEntry> crops = plugin.getConfigManager().getCrops();
        if (crops.isEmpty()) return new ItemStack(Material.EMERALD);

        CropEntry firstCrop = crops.get(0);
        GradeConfig config = firstCrop.getGradeConfig(ItemGrade.NORMAL);
        if (config == null) return new ItemStack(Material.EMERALD);

        // MMOITEM 가져오기
        if (config.getItemType() == ItemType.MMOITEMS) {
            try {
                ItemStack item = MMOItems.plugin.getItem(config.getMmoitemsType(), config.getItemId());
                if (item != null) return item.clone();
            } catch (Exception e) {
                // 폴백
            }
        }

        return new ItemStack(Material.EMERALD);
    }

    private ItemStack getFirstCropSeedIcon() {
        List<CropEntry> crops = plugin.getConfigManager().getCrops();
        if (crops.isEmpty()) return new ItemStack(Material.WHEAT_SEEDS);

        // 씨앗이 있는 첫 번째 작물 찾기
        CropEntry seedCrop = crops.stream()
            .filter(c -> c.hasSeed())
            .findFirst()
            .orElse(crops.get(0));

        GradeConfig config = seedCrop.getGradeConfig(ItemGrade.NORMAL);
        if (config == null) return new ItemStack(Material.WHEAT_SEEDS);

        // MMOITEM 가져오기
        if (config.getItemType() == ItemType.MMOITEMS) {
            try {
                ItemStack item = MMOItems.plugin.getItem(config.getMmoitemsType(), config.getItemId());
                if (item != null) return item.clone();
            } catch (Exception e) {
                // 폴백
            }
        }

        return new ItemStack(Material.WHEAT_SEEDS);
    }

    private ItemStack makeItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of(lore));
        item.setItemMeta(meta);
        return item;
    }
}
