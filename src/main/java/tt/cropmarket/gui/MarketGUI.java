package tt.cropmarket.gui;

import tt.cropmarket.CropMarketPlugin;
import tt.cropmarket.model.*;
import net.Indyuce.mmoitems.MMOItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class MarketGUI {

    private final CropMarketPlugin plugin;

    // 작물 슬롯: 6행 인벤토리 기준 행 1-4, 열 1-7
    private static final List<Integer> CROP_SLOTS = new ArrayList<>();
    static {
        for (int row = 1; row <= 4; row++)
            for (int col = 1; col <= 7; col++)
                CROP_SLOTS.add(row * 9 + col);
    }

    public static final String TITLE     = "§6농산물 시장";
    public static final int    SLOT_BACK = 45;
    public static final int    SLOT_INFO = 49;
    public static final int    MAX_PAGES = 3;

    public MarketGUI(CropMarketPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        ItemStack border = border();
        // 테두리 채우기
        for (int i = 0; i < 54; i++) {
            int row = i / 9, col = i % 9;
            if (row == 0 || row == 5 || col == 0 || col == 8) inv.setItem(i, border);
        }

        // 작물 아이템 배치
        List<CropEntry> crops = plugin.getConfigManager().getCrops();
        for (int i = 0; i < Math.min(crops.size(), CROP_SLOTS.size()); i++) {
            inv.setItem(CROP_SLOTS.get(i), buildCropItem(crops.get(i)));
        }

        // 하단: 뒤로가기 + 나침반 (시스템 설명, 페이지별)
        inv.setItem(SLOT_BACK, makeItem(Material.BARRIER, "§c» 뒤로가기"));
        inv.setItem(SLOT_INFO, buildInfoItem(page));

        player.openInventory(inv);
    }

    private ItemStack buildInfoItem(int page) {
        String nextLabel = (page + 1 < MAX_PAGES) ? "§7클릭하여 다음 페이지 »" : "§7클릭하여 처음으로 »";
        String pageLabel = "§8[" + (page + 1) + "/" + MAX_PAGES + "]  " + nextLabel;

        return switch (page) {
            case 0 -> makeItem(Material.COMPASS,
                    "§e시장 시스템  §8[가격 하락]",
                    "§8────────────────────",
                    "§f판매 즉시 §c가격이 하락§f됩니다.",
                    "§7일반 §c▼ §f1~3%",
                    "§7은   §c▼ §f7~15%",
                    "§7금   §c▼ §f10~75%",
                    "§8────────────────────",
                    pageLabel);
            case 1 -> makeItem(Material.COMPASS,
                    "§e시장 시스템  §8[가격 회복]",
                    "§8────────────────────",
                    "§f일정 시간이 지나면 §a가격이 회복§f됩니다.",
                    "§7일반  §a⟳ §f1~15분",
                    "§7은    §a⟳ §f10~60분",
                    "§7금    §a⟳ §f10~90분",
                    "§8────────────────────",
                    pageLabel);
            default -> makeItem(Material.COMPASS,
                    "§e시장 시스템  §8[시장 붕괴]",
                    "§8────────────────────",
                    "§f기준가 이상에서 판매 시 §c시장 붕괴§f가",
                    "§f발생하며 가격이 높을수록 확률이 증가합니다.",
                    "§7일반  §8[기준가액 §f125%§8]",
                    "§7  최소 §f1%  §7최대 §f5%",
                    "§7은    §8[기준가액 §f170%§8]",
                    "§7  최소 §f3%  §7최대 §f9%",
                    "§7금    §8[기준가액 §f200%§8]",
                    "§7  최소 §f5%  §7최대 §f20%",
                    "",
                    "§f붕괴 후 §e6시간 §f뒤 가격이 복구됩니다.",
                    "§8────────────────────",
                    pageLabel);
        };
    }

    private ItemStack getMmoIcon(CropEntry crop) {
        GradeConfig config = crop.getGradeConfig(ItemGrade.NORMAL);
        if (config != null && config.getItemType() == ItemType.MMOITEMS) {
            try {
                ItemStack item = MMOItems.plugin.getItem(config.getMmoitemsType(), config.getItemId());
                if (item != null) return item.clone();
            } catch (Exception e) {
                // 폴백
            }
        }
        return new ItemStack(crop.getIcon());
    }

    private ItemStack buildCropItem(CropEntry crop) {
        ItemStack item = getMmoIcon(crop);
        ItemMeta  meta = item.getItemMeta();
        meta.setDisplayName(crop.getDisplayName());

        List<String> lore = new ArrayList<>();
        lore.add("§8──────────────");

        for (ItemGrade grade : ItemGrade.values()) {
            GradeConfig config = crop.getGradeConfig(grade);
            GradeData   data   = crop.getGradeData(grade);
            if (config == null || data == null) continue;

            double current = data.getCurrentPrice();
            double trend   = data.getPriceTrend();
            String arrow   = trend > 0.5 ? "§a▲" : trend < -0.5 ? "§c▼" : "§7─";

            lore.add(grade.getDisplayName() + " §7등급  [×" + grade.getSellAmount() + "]");
            lore.add("  §7현재가: " + grade.getColorCode()
                    + String.format("%,.0f", current) + "§7원  " + arrow);
        }

        lore.add("§8──────────────");
        lore.add("§e» 클릭하여 판매");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /** 슬롯 번호 → 작물 인덱스 */
    public int slotToCropIndex(int slot) {
        return CROP_SLOTS.indexOf(slot);
    }

    // ──────────────────────────────────────────
    //  유틸
    // ──────────────────────────────────────────

    private ItemStack border() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta  meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeItem(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of(loreLines));
        item.setItemMeta(meta);
        return item;
    }
}
