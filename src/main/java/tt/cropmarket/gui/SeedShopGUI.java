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
 * 씨앗 구매 GUI (6행 = 54슬롯)
 *
 * 레이아웃:
 *   행0        [border × 9]
 *   행1        [b][씨앗0][씨앗1][씨앗2][씨앗3][씨앗4][씨앗5][씨앗6][b]
 *   행2        [b][씨앗7][씨앗8][씨앗9][씨앗10][b][b][b][b]
 *   행3~4      [border × 18]
 *   행5        [b][b][b][b][BACK][b][b][b][b]
 */
public class SeedShopGUI {

    public static final String TITLE  = "§2씨앗 구매";
    public static final int    SLOT_BACK = 49;

    // 씨앗 표시 슬롯 (행1 col1~7, 행2 col1~7)
    private static final List<Integer> SEED_SLOTS = new ArrayList<>();
    static {
        for (int row = 1; row <= 2; row++)
            for (int col = 1; col <= 7; col++)
                SEED_SLOTS.add(row * 9 + col);
    }

    private final CropMarketPlugin plugin;

    public SeedShopGUI(CropMarketPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        ItemStack border = border();
        for (int i = 0; i < 54; i++) {
            int row = i / 9, col = i % 9;
            if (row == 0 || row >= 3 || col == 0 || col == 8) inv.setItem(i, border);
        }

        // 씨앗 배치
        List<CropEntry> crops = plugin.getConfigManager().getCrops().stream()
                .filter(CropEntry::hasSeed).toList();

        int buyAmount = plugin.getConfigManager().getSeedBuyAmount();

        for (int i = 0; i < Math.min(crops.size(), SEED_SLOTS.size()); i++) {
            CropEntry crop = crops.get(i);
            inv.setItem(SEED_SLOTS.get(i), buildSeedItem(player, crop, buyAmount));
        }

        // 뒤로가기 버튼
        inv.setItem(SLOT_BACK, makeItem(Material.BARRIER, "§c» 뒤로가기"));

        player.openInventory(inv);
    }

    private ItemStack buildSeedItem(Player player, CropEntry crop, int buyAmount) {
        // ItemsAdder 아이템 표시 시도, 실패 시 WHEAT_SEEDS
        ItemStack display;
        try {
            CustomStack cs = CustomStack.getInstance(crop.getSeedItemId());
            display = (cs != null) ? cs.getItemStack().clone() : new ItemStack(Material.WHEAT_SEEDS);
        } catch (Exception e) {
            display = new ItemStack(Material.WHEAT_SEEDS);
        }
        display.setAmount(1);

        double pricePerSeed = crop.getSeedPrice();
        double totalCost    = pricePerSeed * buyAmount;
        double balance      = plugin.getEconomy().getBalance(player);
        boolean canAfford   = balance >= totalCost;

        ItemMeta meta = display.getItemMeta();
        meta.setDisplayName("§a" + strip(crop.getDisplayName()) + " §f씨앗");

        List<String> lore = new ArrayList<>();
        lore.add("§8──────────────");
        lore.add("§7개당 가격: §f" + String.format("%,.0f", pricePerSeed) + "§7원");
        lore.add("§7구매 수량: §f" + buyAmount + "개");
        lore.add("§7총 비용:   " + (canAfford ? "§a" : "§c") + String.format("%,.0f", totalCost) + "§7원");
        lore.add("§8──────────────");
        lore.add("§7보유 잔액: §f" + String.format("%,.0f", balance) + "§7원");
        lore.add("");
        lore.add(canAfford ? "§a» 클릭하여 구매" : "§c» 잔액 부족");
        meta.setLore(lore);
        display.setItemMeta(meta);
        return display;
    }

    // ──────────────────────────────────────────
    //  구매 처리 (GUIListener 에서 호출)
    // ──────────────────────────────────────────

    public void handleBuy(Player player, int slot) {
        if (slot == SLOT_BACK) {
            plugin.getMainMenuGUI().open(player);
            return;
        }

        int index = SEED_SLOTS.indexOf(slot);
        if (index < 0) return;

        List<CropEntry> crops = plugin.getConfigManager().getCrops().stream()
                .filter(CropEntry::hasSeed).toList();
        if (index >= crops.size()) return;

        CropEntry crop = crops.get(index);
        plugin.getSeedBuyGUI().open(player, crop);
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
