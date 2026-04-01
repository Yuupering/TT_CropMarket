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
import java.util.HashMap;
import java.util.List;

/**
 * 씨앗 세부 구매 GUI (5행 = 45슬롯)
 *
 * 레이아웃:
 *   행0 [border × 9]
 *   행1 [b][b][b][b][seed icon][b][b][b][b]
 *   행2 [b][b][b][b][buy button][b][b][b][b]
 *   행3 [border × 9]
 *   행4 [b][b][b][b][back][b][b][b][b]
 */
public class SeedBuyGUI {

    public static final String TITLE_PREFIX = "§2";
    public static final String TITLE_SUFFIX = " §2| 씨앗 구매";

    public static final int SLOT_ICON = 13;
    public static final int SLOT_BUY  = 22;
    public static final int SLOT_BACK = 40;

    private final CropMarketPlugin plugin;

    public SeedBuyGUI(CropMarketPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, CropEntry crop) {
        String title = TITLE_PREFIX + strip(crop.getDisplayName()) + TITLE_SUFFIX;
        Inventory inv = Bukkit.createInventory(null, 45, title);

        ItemStack border = border();
        for (int i = 0; i < 45; i++) {
            if (isBorder(i)) inv.setItem(i, border);
        }

        int buyAmount = plugin.getConfigManager().getSeedBuyAmount();

        inv.setItem(SLOT_ICON, buildSeedIcon(player, crop, buyAmount));
        inv.setItem(SLOT_BUY,  buildBuyButton(player, crop, buyAmount));
        inv.setItem(SLOT_BACK, makeItem(Material.ARROW, "§f» 목록으로 돌아가기"));

        player.openInventory(inv);
    }

    // ──────────────────────────────────────────
    //  구매 처리 (GUIListener 에서 호출)
    // ──────────────────────────────────────────

    public void handleClick(Player player, int slot, CropEntry crop) {
        if (slot == SLOT_BACK) {
            plugin.getSeedShopGUI().open(player);
            return;
        }

        if (slot != SLOT_BUY) return;

        int    amount  = plugin.getConfigManager().getSeedBuyAmount();
        double cost    = crop.getSeedPrice() * amount;
        double balance = plugin.getEconomy().getBalance(player);

        if (balance < cost) {
            player.sendMessage(String.format(
                "§c[씨앗구매] 잔액이 부족합니다. 필요: §f%,.0f§c원  보유: §f%,.0f§c원",
                cost, balance));
            Bukkit.getScheduler().runTask(plugin, () -> open(player, crop));
            return;
        }

        ItemStack seedItem;
        try {
            CustomStack cs = CustomStack.getInstance(crop.getSeedItemId());
            if (cs == null) {
                player.sendMessage("§c[씨앗구매] 씨앗 아이템을 찾을 수 없습니다: " + crop.getSeedItemId());
                return;
            }
            seedItem = cs.getItemStack().clone();
            seedItem.setAmount(amount);
        } catch (Exception e) {
            player.sendMessage("§c[씨앗구매] 씨앗 지급 중 오류가 발생했습니다.");
            return;
        }

        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(seedItem);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(i -> player.getInventory().removeItem(i));
            player.sendMessage("§c[씨앗구매] 인벤토리가 가득 찼습니다.");
            return;
        }

        plugin.getEconomy().withdrawPlayer(player, cost);
        player.sendMessage(String.format(
            "§a[씨앗구매] §f%s §7씨앗 %d개를 §c-%,.0f§7원에 구매했습니다.",
            strip(crop.getDisplayName()), amount, cost));

        Bukkit.getScheduler().runTask(plugin, () -> open(player, crop));
    }

    // ──────────────────────────────────────────
    //  아이템 빌드
    // ──────────────────────────────────────────

    private ItemStack buildSeedIcon(Player player, CropEntry crop, int buyAmount) {
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
        lore.add("§8────────────────────");
        lore.add("§7개당 가격: §f" + String.format("%,.0f", pricePerSeed) + "§7원");
        lore.add("§7구매 수량: §f" + buyAmount + "개");
        lore.add("§7총 비용:   " + (canAfford ? "§a" : "§c") + String.format("%,.0f", totalCost) + "§7원");
        lore.add("§8────────────────────");
        lore.add("§7보유 잔액: §f" + String.format("%,.0f", balance) + "§7원");
        meta.setLore(lore);
        display.setItemMeta(meta);
        return display;
    }

    private ItemStack buildBuyButton(Player player, CropEntry crop, int buyAmount) {
        double totalCost  = crop.getSeedPrice() * buyAmount;
        double balance    = plugin.getEconomy().getBalance(player);
        boolean canAfford = balance >= totalCost;

        Material mat  = canAfford ? Material.EMERALD : Material.COAL;
        String   name = canAfford ? "§a구매하기" : "§c잔액 부족";

        List<String> lore = new ArrayList<>();
        lore.add("§7구매 수량: §f" + buyAmount + "개");
        lore.add("§7총 비용:   " + (canAfford ? "§a" : "§c") + String.format("%,.0f", totalCost) + "§7원");

        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ──────────────────────────────────────────
    //  유틸
    // ──────────────────────────────────────────

    private boolean isBorder(int slot) {
        int row = slot / 9, col = slot % 9;
        return row == 0 || row == 3 || row == 4 || col == 0 || col == 8;
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

    private String strip(String s) {
        return s.replaceAll("§[0-9a-fk-or]", "");
    }

    public static String extractCropName(String title) {
        String stripped = title.replaceAll("§[0-9a-fk-or]", "");
        return stripped.replace(" | 씨앗 구매", "").trim();
    }
}
