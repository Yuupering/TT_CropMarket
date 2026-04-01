package tt.cropmarket.manager;

import tt.cropmarket.CropMarketPlugin;
import tt.cropmarket.model.*;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class ConfigManager {

    private final CropMarketPlugin plugin;

    private final List<CropEntry> crops = new ArrayList<>();

    // 회복 인터벌 (분) - 등급별 독립 운영
    private int normalRecoveryMinMinutes,   normalRecoveryMaxMinutes;
    private int silverRecoveryMinMinutes,   silverRecoveryMaxMinutes;
    private int goldRecoveryMinMinutes,     goldRecoveryMaxMinutes;

    // 등급별 하락/회복률 범위 (동일 사용)
    private double normalMin, normalMax;
    private double silverMin, silverMax;
    private double goldMin,   goldMax;

    // 크래시 설정 (threshold, min-chance, max-chance)
    private double normalCrashThresholdPct, normalCrashChanceMinPct, normalCrashChanceMaxPct;
    private double silverCrashThresholdPct, silverCrashChanceMinPct, silverCrashChanceMaxPct;
    private double goldCrashThresholdPct,   goldCrashChanceMinPct,   goldCrashChanceMaxPct;

    // 크래시 후 복구
    private int    crashRecoveryDelayHours;
    private double crashRecoveryNormalMult;
    private double crashRecoverySilverMult;
    private double crashRecoveryGoldMult;

    // 묶음 판매 수량
    private int normalSellAmount;
    private int silverSellAmount;
    private int goldSellAmount;

    // 씨앗
    private double seedDefaultPrice;
    private int[]  seedBuyAmounts;

    // 세금
    private double taxDefaultRate;
    private double taxReducedRate;
    private String taxReducedPermission;

    public ConfigManager(CropMarketPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        crops.clear();

        var cfg = plugin.getConfig();

        normalRecoveryMinMinutes = cfg.getInt("recovery.normal.min-minutes", 10);
        normalRecoveryMaxMinutes = cfg.getInt("recovery.normal.max-minutes", 180);
        silverRecoveryMinMinutes = cfg.getInt("recovery.silver.min-minutes", 15);
        silverRecoveryMaxMinutes = cfg.getInt("recovery.silver.max-minutes", 240);
        goldRecoveryMinMinutes   = cfg.getInt("recovery.gold.min-minutes", 20);
        goldRecoveryMaxMinutes   = cfg.getInt("recovery.gold.max-minutes", 300);

        normalMin = cfg.getDouble("price-adjustment.normal.min-percent", 1.0);
        normalMax = cfg.getDouble("price-adjustment.normal.max-percent", 3.0);
        silverMin = cfg.getDouble("price-adjustment.silver.min-percent", 7.0);
        silverMax = cfg.getDouble("price-adjustment.silver.max-percent", 15.0);
        goldMin   = cfg.getDouble("price-adjustment.gold.min-percent", 25.0);
        goldMax   = cfg.getDouble("price-adjustment.gold.max-percent", 50.0);

        normalCrashThresholdPct = cfg.getDouble("grade-settings.normal.crash-threshold-pct", 110.0);
        normalCrashChanceMinPct = cfg.getDouble("grade-settings.normal.crash-chance-min-pct", 1.0);
        normalCrashChanceMaxPct = cfg.getDouble("grade-settings.normal.crash-chance-max-pct", 5.0);
        silverCrashThresholdPct = cfg.getDouble("grade-settings.silver.crash-threshold-pct", 130.0);
        silverCrashChanceMinPct = cfg.getDouble("grade-settings.silver.crash-chance-min-pct", 3.0);
        silverCrashChanceMaxPct = cfg.getDouble("grade-settings.silver.crash-chance-max-pct", 9.0);
        goldCrashThresholdPct   = cfg.getDouble("grade-settings.gold.crash-threshold-pct", 150.0);
        goldCrashChanceMinPct   = cfg.getDouble("grade-settings.gold.crash-chance-min-pct", 5.0);
        goldCrashChanceMaxPct   = cfg.getDouble("grade-settings.gold.crash-chance-max-pct", 20.0);

        crashRecoveryDelayHours = cfg.getInt("crash-recovery.delay-hours", 6);
        crashRecoveryNormalMult = cfg.getDouble("crash-recovery.normal-multiplier", 1.25);
        crashRecoverySilverMult = cfg.getDouble("crash-recovery.silver-multiplier", 2.0);
        crashRecoveryGoldMult   = cfg.getDouble("crash-recovery.gold-multiplier", 1.5);

        normalSellAmount = cfg.getInt("sell-amount.normal", 64);
        silverSellAmount = cfg.getInt("sell-amount.silver", 8);
        goldSellAmount   = cfg.getInt("sell-amount.gold",   4);

        seedDefaultPrice = cfg.getDouble("seeds.default-price", 100.0);
        var rawAmounts   = cfg.getIntegerList("seeds.buy-amounts");
        seedBuyAmounts   = rawAmounts.isEmpty()
            ? new int[]{4, 8, 64}
            : rawAmounts.stream().mapToInt(Integer::intValue).limit(3).toArray();

        taxDefaultRate       = cfg.getDouble("tax.default-rate", 10.0);
        taxReducedRate       = cfg.getDouble("tax.reduced-rate", 5.0);
        taxReducedPermission = cfg.getString("tax.reduced-permission", "");

        ConfigurationSection cropsSection = cfg.getConfigurationSection("crops");
        if (cropsSection == null) return;

        for (String cropId : cropsSection.getKeys(false)) {
            ConfigurationSection cs = cropsSection.getConfigurationSection(cropId);
            if (cs == null) continue;

            String displayName = cs.getString("display-name", cropId);
            Material icon = Material.matchMaterial(cs.getString("icon", "WHEAT"));
            if (icon == null) icon = Material.WHEAT;

            CropEntry entry = new CropEntry(cropId, displayName, icon);
            loadGrade(entry, cs, ItemGrade.NORMAL);
            loadGrade(entry, cs, ItemGrade.SILVER);
            loadGrade(entry, cs, ItemGrade.GOLD);

            String seedId = cs.getString("seed-id");
            if (seedId != null) {
                entry.setSeedItemId(seedId);
                entry.setSeedPrice(cs.getDouble("seed-price", seedDefaultPrice));
            }

            crops.add(entry);
        }
    }

    private void loadGrade(CropEntry entry, ConfigurationSection cropSection, ItemGrade grade) {
        ConfigurationSection gs = cropSection.getConfigurationSection(grade.name().toLowerCase());
        if (gs == null) return;
        String itemId = gs.getString("item-id");
        if (itemId == null) return;

        // item-type 명시 → 우선 적용, 없으면 자동 감지
        ItemType itemType;
        String typeStr = gs.getString("item-type", "").toUpperCase();
        if (!typeStr.isEmpty()) {
            itemType = switch (typeStr) {
                case "ITEMSADDER" -> ItemType.ITEMSADDER;
                case "MMOITEMS"   -> ItemType.MMOITEMS;
                default           -> ItemType.VANILLA;
            };
        } else if (gs.contains("mmoitems-type")) {
            itemType = ItemType.MMOITEMS;
        } else if (itemId.contains(":")) {
            itemType = ItemType.ITEMSADDER;
        } else {
            itemType = ItemType.VANILLA;
        }

        entry.addGrade(grade, new GradeConfig(
            itemType, itemId,
            gs.getString("mmoitems-type", "MATERIAL"),
            gs.getDouble("base-price", 100.0),
            gs.getDouble("min-price",  10.0),
            gs.getDouble("max-price",  100000.0)
        ));
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public List<CropEntry> getCrops()  { return crops; }
    public CropEntry getCrop(String id) {
        return crops.stream().filter(c -> c.getId().equals(id)).findFirst().orElse(null);
    }

    /** 등급별 회복 주기 [min분, max분] */
    public int[] getRecoveryRange(ItemGrade grade) {
        return switch (grade) {
            case NORMAL -> new int[]{normalRecoveryMinMinutes, normalRecoveryMaxMinutes};
            case SILVER -> new int[]{silverRecoveryMinMinutes, silverRecoveryMaxMinutes};
            case GOLD   -> new int[]{goldRecoveryMinMinutes,   goldRecoveryMaxMinutes};
        };
    }

    /** 등급별 하락/회복 범위 [min%, max%] */
    public double[] getAdjustmentRange(ItemGrade grade) {
        return switch (grade) {
            case NORMAL -> new double[]{normalMin, normalMax};
            case SILVER -> new double[]{silverMin, silverMax};
            case GOLD   -> new double[]{goldMin,   goldMax};
        };
    }

    /** 등급별 크래시 설정 [thresholdPct, minChancePct, maxChancePct] */
    public double[] getCrashSettings(ItemGrade grade) {
        return switch (grade) {
            case NORMAL -> new double[]{normalCrashThresholdPct, normalCrashChanceMinPct, normalCrashChanceMaxPct};
            case SILVER -> new double[]{silverCrashThresholdPct, silverCrashChanceMinPct, silverCrashChanceMaxPct};
            case GOLD   -> new double[]{goldCrashThresholdPct,   goldCrashChanceMinPct,   goldCrashChanceMaxPct};
        };
    }

    public int    getCrashRecoveryDelayHours()       { return crashRecoveryDelayHours; }
    public double getCrashRecoveryMultiplier(ItemGrade grade) {
        return switch (grade) {
            case NORMAL -> crashRecoveryNormalMult;
            case SILVER -> crashRecoverySilverMult;
            case GOLD   -> crashRecoveryGoldMult;
        };
    }

    /** 등급별 묶음 판매 수량 */
    public int getSellAmount(ItemGrade grade) {
        return switch (grade) {
            case NORMAL -> normalSellAmount;
            case SILVER -> silverSellAmount;
            case GOLD   -> goldSellAmount;
        };
    }

    public double getSeedDefaultPrice()  { return seedDefaultPrice; }
    public int[]  getSeedBuyAmounts()   { return seedBuyAmounts; }

    public double getTaxDefaultRate()       { return taxDefaultRate; }
    public double getTaxReducedRate()       { return taxReducedRate; }
    public String getTaxReducedPermission() { return taxReducedPermission; }
}
