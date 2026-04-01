package tt.cropmarket.gui;

import tt.cropmarket.CropMarketPlugin;
import tt.cropmarket.model.CropEntry;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 씨앗 구매 목록 GUI (6행 = 54슬롯)
 *
 * 레이아웃:
 *   행0        [border × 9]
 *   행1~4      [b][씨앗...][b]  (28슬롯)
 *   행5        [b][b][prev][b][back][b][next][b][b]
 */
public class SeedShopGUI {

    public static final String TITLE             = "§2씨앗 구매";
    public static final int    MAX_ITEMS_PER_PAGE = 28;
    public static final int    SLOT_PREV         = 47;
    public static final int    SLOT_BACK         = 49;
    public static final int    SLOT_NEXT         = 51;

    // 씨앗 표시 슬롯: 행1~4, 열1~7
    private static final List<Integer> SEED_SLOTS = new ArrayList<>();
    static {
        for (int row = 1; row <= 4; row++)
            for (int col = 1; col <= 7; col++)
                SEED_SLOTS.add(row * 9 + col);
    }

    private final CropMarketPlugin plugin;

    public SeedShopGUI(CropMarketPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        ItemStack border = border();
        for (int i = 0; i < 54; i++) {
            int row = i / 9, col = i % 9;
            if (row == 0 || row == 5 || col == 0 || col == 8) inv.setItem(i, border);
        }

        List<CropEntry> allSeeds = plugin.getConfigManager().getCrops().stream()
                .filter(CropEntry::hasSeed).toList();
        int totalPages = Math.max(1, (int) Math.ceil((double) allSeeds.size() / MAX_ITEMS_PER_PAGE));
        int from = page * MAX_ITEMS_PER_PAGE;
        int to   = Math.min(from + MAX_ITEMS_PER_PAGE, allSeeds.size());

        List<CropEntry> pageSeeds = allSeeds.subList(from, to);
        for (int i = 0; i < pageSeeds.size(); i++) {
            inv.setItem(SEED_SLOTS.get(i), buildSeedItem(player, pageSeeds.get(i)));
        }

        inv.setItem(SLOT_BACK, makeItem(Material.BARRIER, "§c» 뒤로가기"));

        if (page > 0) {
            inv.setItem(SLOT_PREV, makeItem(Material.ARROW,
                "§f« 이전 페이지  §8[" + page + "/" + totalPages + "]"));
        }
        if (page < totalPages - 1) {
            inv.setItem(SLOT_NEXT, makeItem(Material.ARROW,
                "§f다음 페이지 »  §8[" + (page + 2) + "/" + totalPages + "]"));
        }

        player.openInventory(inv);
    }

    /** 씨앗 슬롯 클릭 시 세부 구매창 오픈 (GUIListener 에서 호출) */
    public void handleSeedClick(Player player, int slot, int page) {
        int local = SEED_SLOTS.indexOf(slot);
        if (local < 0) return;

        List<CropEntry> allSeeds = plugin.getConfigManager().getCrops().stream()
                .filter(CropEntry::hasSeed).toList();
        int globalIndex = page * MAX_ITEMS_PER_PAGE + local;
        if (globalIndex >= allSeeds.size()) return;

        plugin.getSeedBuyGUI().open(player, allSeeds.get(globalIndex));
    }

    // ──────────────────────────────────────────
    //  아이템 빌드
    // ──────────────────────────────────────────

    private ItemStack buildSeedItem(Player player, CropEntry crop) {
        ItemStack display;
        try {
            CustomStack cs = CustomStack.getInstance(crop.getSeedItemId());
            display = (cs != null) ? cs.getItemStack().clone() : new ItemStack(Material.WHEAT_SEEDS);
        } catch (Exception e) {
            display = new ItemStack(Material.WHEAT_SEEDS);
        }
        display.setAmount(1);

        double pricePerSeed = crop.getSeedPrice();
        int[]  amounts      = plugin.getConfigManager().getSeedBuyAmounts();

        ItemMeta meta = display.getItemMeta();
        meta.setDisplayName("§a" + strip(crop.getDisplayName()) + " §f씨앗");

        List<String> lore = new ArrayList<>();
        lore.add("§8────────────────────");
        lore.add("§7개당 가격: §f" + String.format("%,.0f", pricePerSeed) + "§7원");
        StringBuilder amtLine = new StringBuilder("§7구매 수량: ");
        for (int i = 0; i < amounts.length; i++) {
            if (i > 0) amtLine.append("§8 / ");
            amtLine.append("§f").append(amounts[i]).append("§7개");
        }
        lore.add(amtLine.toString());
        lore.add("§8────────────────────");
        lore.add("§a» 클릭하여 수량 선택");
        meta.setLore(lore);
        display.setItemMeta(meta);
        return display;
    }

    // ──────────────────────────────────────────
    //  유틸
    // ──────────────────────────────────────────

    private String strip(String s) {
        return s.replaceAll("§[0-9a-fk-or]", "");
    }

    private ItemStack border() {
        ItemStack item = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
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
}
