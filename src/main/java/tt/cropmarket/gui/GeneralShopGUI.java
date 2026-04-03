package tt.cropmarket.gui;

import tt.cropmarket.CropMarketPlugin;
import tt.cropmarket.model.GeneralShopEntry;
import tt.cropmarket.model.ItemType;
import dev.lone.itemsadder.api.CustomStack;
import net.Indyuce.mmoitems.MMOItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 일반 농작물 판매 목록 GUI (6행, 페이지당 28개)
 *
 * 레이아웃:
 *   행0 / 행5 : 테두리
 *   행1~4, 열1~7 : 아이템 슬롯 (28개)
 *   행5: [back:45][prev:46]...[info:49]...[next:52]
 */
public class GeneralShopGUI {

    private final CropMarketPlugin plugin;

    public static final String TITLE              = "§a일반 농작물 판매";
    public static final int    MAX_ITEMS_PER_PAGE = 28;
    public static final int    SLOT_BACK          = 45;
    public static final int    SLOT_PREV          = 46;
    public static final int    SLOT_NEXT          = 52;

    private static final List<Integer> ITEM_SLOTS = new ArrayList<>();
    static {
        for (int row = 1; row <= 4; row++)
            for (int col = 1; col <= 7; col++)
                ITEM_SLOTS.add(row * 9 + col);
    }

    public GeneralShopGUI(CropMarketPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        // 테두리
        ItemStack border = border();
        for (int i = 0; i < 54; i++) {
            int row = i / 9, col = i % 9;
            if (row == 0 || row == 5 || col == 0 || col == 8) inv.setItem(i, border);
        }

        List<GeneralShopEntry> allItems = plugin.getConfigManager().getGeneralShopItems();
        int totalPages = Math.max(1, (int) Math.ceil((double) allItems.size() / MAX_ITEMS_PER_PAGE));
        int from = page * MAX_ITEMS_PER_PAGE;
        int to   = Math.min(from + MAX_ITEMS_PER_PAGE, allItems.size());

        for (int i = from; i < to; i++) {
            inv.setItem(ITEM_SLOTS.get(i - from), buildItemIcon(allItems.get(i), player));
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

    private ItemStack buildItemIcon(GeneralShopEntry entry, Player player) {
        ItemStack base = getEntryItem(entry);
        if (base == null) base = new ItemStack(entry.getIcon());
        base = base.clone();

        var cfg       = plugin.getConfigManager();
        int available = plugin.getMarketManager().countItems(player, entry);
        int canSell   = available / entry.getSellAmount();

        double taxRate = (!cfg.getTaxReducedPermission().isEmpty()
                && player.hasPermission(cfg.getTaxReducedPermission()))
                ? cfg.getTaxReducedRate() / 100.0
                : cfg.getTaxDefaultRate() / 100.0;

        double gross = entry.getPrice() * entry.getSellAmount();
        double net   = gross * (1.0 - taxRate);

        List<String> lore = new ArrayList<>();
        lore.add("§8──────────────");
        lore.add("§7판매 단위: §f" + entry.getSellAmount() + "개");
        lore.add("§7고정 가격: §a" + String.format("%,.0f", entry.getPrice()) + "§7원");
        lore.add("§7판매가:   §f" + String.format("%,.0f", gross) + "§7원");
        lore.add("§7세금 후:  §a+" + String.format("%,.0f", net) + "§7원");
        lore.add("§8──────────────");
        lore.add("§7보유: §f" + available + "개  §7가능: §f" + canSell + "묶음");
        lore.add("§8──────────────");
        if (canSell > 0) {
            lore.add("§a» 클릭하여 판매");
        } else {
            lore.add("§c» 아이템이 부족합니다");
        }

        ItemMeta meta = base.getItemMeta();
        meta.setDisplayName(entry.getDisplayName());
        meta.setLore(lore);
        base.setItemMeta(meta);
        return base;
    }

    /** 슬롯 번호 → 아이템 전체 인덱스 (page 반영) */
    public int slotToEntryIndex(int slot, int page) {
        int local = ITEM_SLOTS.indexOf(slot);
        if (local < 0) return -1;
        return page * MAX_ITEMS_PER_PAGE + local;
    }

    // ──────────────────────────────────────────
    //  아이템 가져오기
    // ──────────────────────────────────────────

    public static ItemStack getEntryItem(GeneralShopEntry entry) {
        if (entry == null) return null;
        try {
            return switch (entry.getItemType()) {
                case VANILLA -> {
                    Material mat = Material.matchMaterial(entry.getItemId());
                    yield mat != null ? new ItemStack(mat) : null;
                }
                case ITEMSADDER -> {
                    CustomStack cs = CustomStack.getInstance(entry.getItemId());
                    if (cs == null) yield null;
                    ItemStack item = cs.getItemStack().clone();
                    item.setAmount(1);
                    yield item;
                }
                case MMOITEMS -> {
                    ItemStack item = MMOItems.plugin.getItem(entry.getMmoitemsType(), entry.getItemId());
                    if (item == null) yield null;
                    item = item.clone();
                    item.setAmount(1);
                    yield item;
                }
            };
        } catch (Throwable e) {
            return null;
        }
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

    private ItemStack makeItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
}
