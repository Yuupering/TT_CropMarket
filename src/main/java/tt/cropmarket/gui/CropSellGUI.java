package tt.cropmarket.gui;

import tt.cropmarket.CropMarketPlugin;
import tt.cropmarket.model.*;
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
 * 작물별 판매 GUI (5행 = 45슬롯)
 *
 * 레이아웃:
 *   행0 [border × 9]
 *   행1 [b][normal icon][b][b][silver icon][b][b][gold icon][b]
 *   행2 [b][normal sell][b][b][silver sell][b][b][gold sell][b]
 *   행3 [border × 9]
 *   행4 [b][b][b][b][back][b][b][b][b]
 */
public class CropSellGUI {

    private final CropMarketPlugin plugin;

    // 등급 표시 슬롯
    public static final int SLOT_NORMAL_ICON  = 10;
    public static final int SLOT_SILVER_ICON  = 13;
    public static final int SLOT_GOLD_ICON    = 16;

    // 판매 버튼 슬롯
    public static final int SLOT_NORMAL_SELL  = 19;
    public static final int SLOT_SILVER_SELL  = 22;
    public static final int SLOT_GOLD_SELL    = 25;

    public static final int SLOT_BACK         = 40;

    // 타이틀 식별용 접두사
    public static final String TITLE_PREFIX = "§6";
    public static final String TITLE_SUFFIX = " §6| 판매";

    public CropSellGUI(CropMarketPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, CropEntry crop) {
        String title = TITLE_PREFIX + strip(crop.getDisplayName()) + TITLE_SUFFIX;
        Inventory inv = Bukkit.createInventory(null, 45, title);

        // 테두리
        ItemStack border = border();
        for (int i = 0; i < 45; i++) {
            if (isBorder(i)) inv.setItem(i, border);
        }

        // 등급 표시 & 판매 버튼
        placeGrade(inv, player, crop, ItemGrade.NORMAL, SLOT_NORMAL_ICON, SLOT_NORMAL_SELL);
        placeGrade(inv, player, crop, ItemGrade.SILVER, SLOT_SILVER_ICON, SLOT_SILVER_SELL);
        placeGrade(inv, player, crop, ItemGrade.GOLD,   SLOT_GOLD_ICON,   SLOT_GOLD_SELL);

        // 뒤로가기
        inv.setItem(SLOT_BACK, makeItem(Material.ARROW, "§f» 목록으로 돌아가기"));

        player.openInventory(inv);
    }

    private void placeGrade(Inventory inv, Player player, CropEntry crop,
                            ItemGrade grade, int iconSlot, int sellSlot) {
        GradeConfig config = crop.getGradeConfig(grade);
        GradeData   data   = crop.getGradeData(grade);

        if (config == null || data == null) {
            inv.setItem(iconSlot, makeItem(Material.BARRIER, "§c미설정"));
            return;
        }

        int sellAmount = plugin.getConfigManager().getSellAmount(grade);
        int available  = plugin.getMarketManager().countItems(player, config);
        int canSell    = available / sellAmount;

        inv.setItem(iconSlot, buildGradeIcon(grade, config, data, available, canSell, sellAmount));
        inv.setItem(sellSlot, buildSellButton(player, grade, data, canSell, sellAmount));
    }

    private ItemStack buildGradeIcon(ItemGrade grade, GradeConfig config,
                                     GradeData data, int available, int canSell, int sellAmount) {
        // 등급별 아이템 가져오기
        ItemStack display = getGradeItem(config);
        if (display == null) {
            // 폴백: 기본 재료
            Material mat = switch (grade) {
                case NORMAL -> Material.PAPER;
                case SILVER -> Material.IRON_NUGGET;
                case GOLD   -> Material.GOLD_NUGGET;
            };
            display = new ItemStack(mat);
        }
        display = display.clone();

        double current = data.getCurrentPrice();
        double base    = config.getBasePrice();
        double trend   = data.getPriceTrend();
        String arrow   = trend > 0.5 ? "§a▲ 상승 중" : trend < -0.5 ? "§c▼ 하락 중" : "§7─ 안정";
        double pct     = (current / base) * 100.0;

        List<String> lore = new ArrayList<>();
        lore.add("§8──────────────");
        lore.add("§7판매 단위: §f" + sellAmount + "개");
        lore.add("§7현재 가격: " + grade.getColorCode()
                + String.format("%,.0f", current) + "§7원");
        lore.add("§7기준가 대비: §f" + String.format("%.1f", pct) + "%");
        lore.add("§7가격 추이:  " + arrow);
        lore.add("§8──────────────");
        lore.add("§7최저가: §c" + String.format("%,.0f", config.getMinPrice()) + "§7원");
        lore.add("§7최고가: §a" + String.format("%,.0f", config.getMaxPrice()) + "§7원");
        lore.add("§8──────────────");
        lore.add("§7보유 수량: §f" + available + "개");
        lore.add("§7판매 가능: §f" + canSell + "묶음");

        // 아이템에 이름과 설명 적용
        ItemMeta meta = display.getItemMeta();
        meta.setDisplayName(grade.getColorCode() + stripColor(grade.getDisplayName()) + " 등급");
        meta.setLore(lore);
        display.setItemMeta(meta);
        return display;
    }

    private ItemStack buildSellButton(Player player, ItemGrade grade, GradeData data, int canSell, int sellAmount) {
        boolean enough = canSell > 0;
        Material mat   = enough ? Material.EMERALD : Material.COAL;
        String   name  = enough ? "§a판매하기" : "§c아이템 부족";

        // 세금 계산
        var cfg = plugin.getConfigManager();
        double taxRate = (!cfg.getTaxReducedPermission().isEmpty()
                && player.hasPermission(cfg.getTaxReducedPermission()))
                ? cfg.getTaxReducedRate() / 100.0
                : cfg.getTaxDefaultRate() / 100.0;

        double gross  = data.getCurrentPrice() * sellAmount;
        double tax    = Math.round(gross * taxRate * 100.0) / 100.0;
        double net    = gross - tax;
        int    taxPct = (int) Math.round(taxRate * 100);

        List<String> lore = new ArrayList<>();
        lore.add("§7판매 수량: §f" + sellAmount + "개");
        lore.add("§7판매가:   §f" + String.format("%,.0f", gross) + "§7원");
        lore.add("§7세율:     §c" + taxPct + "%  §8(-" + String.format("%,.0f", tax) + "원)");
        lore.add("§7실수령:   §a+" + String.format("%,.0f", net) + "§7원");
        if (canSell > 0) lore.add("§8──────────────");
        if (canSell > 0) lore.add("§7가능 횟수: §f" + canSell + "번");

        return buildItemWithLore(mat, name, lore);
    }

    // ──────────────────────────────────────────
    //  등급별 아이템 가져오기
    // ──────────────────────────────────────────

    private ItemStack getGradeItem(GradeConfig config) {
        if (config == null) return null;
        try {
            return switch (config.getItemType()) {
                case VANILLA -> {
                    Material mat = Material.matchMaterial(config.getItemId());
                    yield mat != null ? new ItemStack(mat) : null;
                }
                case ITEMSADDER -> {
                    CustomStack cs = CustomStack.getInstance(config.getItemId());
                    if (cs == null) yield null;
                    ItemStack item = cs.getItemStack().clone();
                    item.setAmount(1);
                    yield item;
                }
                case MMOITEMS -> {
                    ItemStack item = MMOItems.plugin.getItem(config.getMmoitemsType(), config.getItemId());
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

    /** §색상코드 제거 (타이틀 비교용) */
    private String strip(String s) {
        return s.replaceAll("§[0-9a-fk-or]", "");
    }

    private String stripColor(String s) {
        return s.replaceAll("§[0-9a-fk-or]", "");
    }

    /** 타이틀에서 작물 displayName 추출 */
    public static String extractCropName(String title) {
        // Remove all color codes first, then remove the " | 판매" suffix
        String stripped = title.replaceAll("§[0-9a-fk-or]", "");
        return stripped.replace(" | 판매", "").trim();
    }
}
