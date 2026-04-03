package tt.cropmarket.gui;

import tt.cropmarket.CropMarketPlugin;
import tt.cropmarket.model.GeneralShopEntry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 일반 농작물 판매 세부 GUI (5행 = 45슬롯)
 *
 * 레이아웃:
 *   행0 [border × 9]
 *   행1 [b][b][b][b][icon:13][b][b][b][b]
 *   행2 [b][b][b][b][sell:22][b][b][b][b]
 *   행3 [border × 9]
 *   행4 [b][b][b][b][back:40][b][b][b][b]
 */
public class GeneralSellGUI {

    private final CropMarketPlugin plugin;

    public static final int    SLOT_ICON = 13;
    public static final int    SLOT_SELL = 22;
    public static final int    SLOT_BACK = 40;
    public static final String TITLE_PREFIX = "§5";
    public static final String TITLE_SUFFIX = " §5| 일반 판매";

    public GeneralSellGUI(CropMarketPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, GeneralShopEntry entry) {
        String title = TITLE_PREFIX + strip(entry.getDisplayName()) + TITLE_SUFFIX;
        Inventory inv = Bukkit.createInventory(null, 45, title);

        // 테두리
        ItemStack border = border();
        for (int i = 0; i < 45; i++) {
            if (isBorder(i)) inv.setItem(i, border);
        }

        var cfg       = plugin.getConfigManager();
        int available = plugin.getMarketManager().countItems(player, entry);
        int canSell   = available / entry.getSellAmount();

        double taxRate = (!cfg.getTaxReducedPermission().isEmpty()
                && player.hasPermission(cfg.getTaxReducedPermission()))
                ? cfg.getTaxReducedRate() / 100.0
                : cfg.getTaxDefaultRate() / 100.0;

        double gross  = entry.getPrice() * entry.getSellAmount();
        double tax    = Math.round(gross * taxRate * 100.0) / 100.0;
        double net    = gross - tax;
        int    taxPct = (int) Math.round(taxRate * 100);

        // 아이템 아이콘
        inv.setItem(SLOT_ICON, buildIcon(entry, available, canSell, gross, net, taxRate));

        // 판매 버튼
        boolean enough = canSell > 0;
        Material sellMat = enough ? Material.EMERALD : Material.COAL;
        String   sellName = enough ? "§a판매하기" : "§c아이템 부족";

        List<String> sellLore = new ArrayList<>();
        sellLore.add("§7판매 수량: §f" + entry.getSellAmount() + "개");
        sellLore.add("§7고정 가격: §a" + String.format("%,.0f", entry.getPrice()) + "§7원");
        sellLore.add("§7판매가:   §f" + String.format("%,.0f", gross) + "§7원");
        sellLore.add("§7세율:     §c" + taxPct + "%  §8(-" + String.format("%,.0f", tax) + "원)");
        sellLore.add("§7실수령:   §a+" + String.format("%,.0f", net) + "§7원");
        if (canSell > 0) {
            sellLore.add("§8──────────────");
            sellLore.add("§7가능 횟수: §f" + canSell + "번");
        }
        inv.setItem(SLOT_SELL, buildItemWithLore(sellMat, sellName, sellLore));

        // 뒤로가기
        inv.setItem(SLOT_BACK, makeItem(Material.ARROW, "§f» 목록으로 돌아가기"));

        player.openInventory(inv);
    }

    private ItemStack buildIcon(GeneralShopEntry entry, int available, int canSell,
                                double gross, double net, double taxRate) {
        ItemStack base = GeneralShopGUI.getEntryItem(entry);
        if (base == null) base = new ItemStack(entry.getIcon());
        base = base.clone();

        List<String> lore = new ArrayList<>();
        lore.add("§8──────────────");
        lore.add("§7판매 단위: §f" + entry.getSellAmount() + "개");
        lore.add("§7고정 가격: §a" + String.format("%,.0f", entry.getPrice()) + "§7원/묶음");
        lore.add("§7세금 적용: §f" + String.format("%,.0f", gross)
                + " §8→ §a" + String.format("%,.0f", net) + "§7원");
        lore.add("§8──────────────");
        lore.add("§7보유 수량: §f" + available + "개");
        lore.add("§7판매 가능: §f" + canSell + "묶음");

        ItemMeta meta = base.getItemMeta();
        meta.setDisplayName(entry.getDisplayName());
        meta.setLore(lore);
        base.setItemMeta(meta);
        return base;
    }

    // ──────────────────────────────────────────
    //  타이틀 파싱
    // ──────────────────────────────────────────

    public static String extractEntryName(String title) {
        String stripped = title.replaceAll("§[0-9a-fk-or]", "");
        return stripped.replace("| 일반 판매", "").trim();
    }

    // ──────────────────────────────────────────
    //  유틸
    // ──────────────────────────────────────────

    private boolean isBorder(int slot) {
        int row = slot / 9, col = slot % 9;
        return row == 0 || row == 3 || row == 4 || col == 0 || col == 8;
    }

    private ItemStack border() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta  meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildItemWithLore(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String strip(String s) {
        return s.replaceAll("§[0-9a-fk-or]", "");
    }
}
