package tt.cropmarket.manager;

import tt.cropmarket.CropMarketPlugin;
import tt.cropmarket.model.*;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class ConfigManager {

    private final CropMarketPlugin plugin;

    private final List<CropEntry>        crops            = new ArrayList<>();
    private final List<GeneralShopEntry> generalShopItems = new ArrayList<>();

    // 일반 농작물 판매 상점
    private boolean generalShopEnabled;
    private boolean generalShopPermEnabled;
    private String  generalShopPermNode;

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

    // 수확량 기반 가격 보정
    private boolean yieldAdjustmentEnabled;
    private int     yieldBaseMaxHarvest;
    private double  yieldPerHarvestStep;

    // 보이지 않는 손
    private boolean invisibleHandEnabled;
    private double  invisibleHandChancePct;

    // 수확량 기반 붕괴 확률 보정 (등급별 독립)
    private int     crashChanceAdjBaseMaxHarvest;
    private boolean crashChanceAdjNormalEnabled;
    private double  crashChanceAdjNormalStep;
    private boolean crashChanceAdjSilverEnabled;
    private double  crashChanceAdjSilverStep;
    private boolean crashChanceAdjGoldEnabled;
    private double  crashChanceAdjGoldStep;

    public ConfigManager(CropMarketPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        crops.clear();
        generalShopItems.clear();

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

        yieldAdjustmentEnabled = cfg.getBoolean("yield-adjustment.enabled", false);
        yieldBaseMaxHarvest    = cfg.getInt("yield-adjustment.base-max-harvest", 4);
        yieldPerHarvestStep    = cfg.getDouble("yield-adjustment.per-harvest-step", 0.15);

        invisibleHandEnabled   = cfg.getBoolean("invisible-hand.enabled", false);
        invisibleHandChancePct = cfg.getDouble("invisible-hand.chance-pct", 10.0);

        crashChanceAdjBaseMaxHarvest  = cfg.getInt("crash-chance-adjustment.base-max-harvest", 4);
        crashChanceAdjNormalEnabled   = cfg.getBoolean("crash-chance-adjustment.normal.enabled", false);
        crashChanceAdjNormalStep      = cfg.getDouble("crash-chance-adjustment.normal.per-harvest-step", 0.1);
        crashChanceAdjSilverEnabled   = cfg.getBoolean("crash-chance-adjustment.silver.enabled", false);
        crashChanceAdjSilverStep      = cfg.getDouble("crash-chance-adjustment.silver.per-harvest-step", 0.1);
        crashChanceAdjGoldEnabled     = cfg.getBoolean("crash-chance-adjustment.gold.enabled", false);
        crashChanceAdjGoldStep        = cfg.getDouble("crash-chance-adjustment.gold.per-harvest-step", 0.1);

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

            if (cs.contains("max-harvest")) {
                entry.setMaxHarvest(cs.getInt("max-harvest", 0));
            }

            crops.add(entry);
        }

        // 일반 농작물 판매 상점 로드
        generalShopEnabled     = cfg.getBoolean("general-shop.enabled", false);
        generalShopPermEnabled = cfg.getBoolean("general-shop.permission.enabled", false);
        generalShopPermNode    = cfg.getString("general-shop.permission.node", "");

        ConfigurationSection gsItems = cfg.getConfigurationSection("general-shop.items");
        if (gsItems != null) {
            for (String key : gsItems.getKeys(false)) {
                ConfigurationSection is = gsItems.getConfigurationSection(key);
                if (is == null || !is.getBoolean("enabled", true)) continue;

                String   displayName = is.getString("display-name", key);
                Material icon        = Material.matchMaterial(is.getString("icon", "PAPER"));
                if (icon == null) icon = Material.PAPER;

                String itemId    = is.getString("item-id", "");
                String mmoType   = is.getString("mmoitems-type", "MATERIAL");
                int    sellAmt   = is.getInt("sell-amount", 1);

                // 아이템 타입 자동 감지
                ItemType itemType;
                String typeStr = is.getString("item-type", "").toUpperCase();
                if (!typeStr.isEmpty()) {
                    itemType = switch (typeStr) {
                        case "ITEMSADDER" -> ItemType.ITEMSADDER;
                        case "MMOITEMS"   -> ItemType.MMOITEMS;
                        default           -> ItemType.VANILLA;
                    };
                } else if (is.contains("mmoitems-type")) {
                    itemType = ItemType.MMOITEMS;
                } else if (itemId.contains(":")) {
                    itemType = ItemType.ITEMSADDER;
                } else {
                    itemType = ItemType.VANILLA;
                }

                // 가격 결정
                double price;
                if (is.getBoolean("use-base-price", false)) {
                    String cropRef  = is.getString("crop-ref", "");
                    String gradeRef = is.getString("grade-ref", "normal").toUpperCase();
                    CropEntry cropEntry = getCrop(cropRef);
                    if (cropEntry != null) {
                        try {
                            ItemGrade grade = ItemGrade.valueOf(gradeRef);
                            GradeConfig gc  = cropEntry.getGradeConfig(grade);
                            price = (gc != null) ? gc.getBasePrice() : is.getDouble("price", 0.0);
                        } catch (IllegalArgumentException e) {
                            price = is.getDouble("price", 0.0);
                        }
                    } else {
                        price = is.getDouble("price", 0.0);
                    }
                } else {
                    price = is.getDouble("price", 0.0);
                }

                generalShopItems.add(new GeneralShopEntry(
                    key, displayName, icon, itemType, itemId, mmoType, sellAmt, price
                ));
            }
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

    /**
     * 수확량 보정 적용 하락 범위 [min%, max%]
     * maxHarvest <= 0 이거나 기능 비활성화 시 기본 범위 반환
     */
    public double[] getAdjustmentRange(ItemGrade grade, int maxHarvest) {
        double[] base = getAdjustmentRange(grade);
        if (!yieldAdjustmentEnabled || maxHarvest <= 0) return base;

        int diff = maxHarvest - yieldBaseMaxHarvest;
        if (diff == 0) return base;

        double minAdj = Math.round((base[0] - diff * yieldPerHarvestStep) * 100.0) / 100.0;
        double maxAdj = Math.round((base[1] - diff * yieldPerHarvestStep * 2.0) * 100.0) / 100.0;
        minAdj = Math.max(0.1, minAdj);
        maxAdj = Math.max(minAdj + 0.1, maxAdj);
        return new double[]{minAdj, maxAdj};
    }

    /** 등급별 크래시 설정 [thresholdPct, minChancePct, maxChancePct] */
    public double[] getCrashSettings(ItemGrade grade) {
        return switch (grade) {
            case NORMAL -> new double[]{normalCrashThresholdPct, normalCrashChanceMinPct, normalCrashChanceMaxPct};
            case SILVER -> new double[]{silverCrashThresholdPct, silverCrashChanceMinPct, silverCrashChanceMaxPct};
            case GOLD   -> new double[]{goldCrashThresholdPct,   goldCrashChanceMinPct,   goldCrashChanceMaxPct};
        };
    }

    /**
     * 수확량 보정 적용 크래시 설정 [thresholdPct, minChancePct, maxChancePct]
     * 해당 등급의 보정이 비활성화되어 있거나 maxHarvest <= 0 이면 기본 설정 반환
     */
    public double[] getCrashSettings(ItemGrade grade, int maxHarvest) {
        double[] base = getCrashSettings(grade);

        boolean enabled = switch (grade) {
            case NORMAL -> crashChanceAdjNormalEnabled;
            case SILVER -> crashChanceAdjSilverEnabled;
            case GOLD   -> crashChanceAdjGoldEnabled;
        };
        if (!enabled || maxHarvest <= 0) return base;

        double step = switch (grade) {
            case NORMAL -> crashChanceAdjNormalStep;
            case SILVER -> crashChanceAdjSilverStep;
            case GOLD   -> crashChanceAdjGoldStep;
        };

        int diff = maxHarvest - crashChanceAdjBaseMaxHarvest;
        if (diff == 0) return base;

        double multiplier = Math.max(0.01, 1.0 - diff * step);
        double minAdj = Math.round(base[1] * multiplier * 100.0) / 100.0;
        double maxAdj = Math.round(base[2] * multiplier * 100.0) / 100.0;
        minAdj = Math.max(0.1, minAdj);
        maxAdj = Math.max(minAdj + 0.1, maxAdj);
        return new double[]{base[0], minAdj, maxAdj};
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

    public boolean isYieldAdjustmentEnabled() { return yieldAdjustmentEnabled; }
    public boolean isInvisibleHandEnabled()   { return invisibleHandEnabled; }
    public double  getInvisibleHandChancePct(){ return invisibleHandChancePct; }

    public List<GeneralShopEntry> getGeneralShopItems()    { return Collections.unmodifiableList(generalShopItems); }
    public boolean isGeneralShopEnabled()                  { return generalShopEnabled; }
    public boolean isGeneralShopPermEnabled()              { return generalShopPermEnabled; }
    public String  getGeneralShopPermNode()                { return generalShopPermNode; }
}
