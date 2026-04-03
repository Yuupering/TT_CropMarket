package tt.cropmarket.manager;

import tt.cropmarket.CropMarketPlugin;
import tt.cropmarket.model.*;
import tt.cropmarket.model.GeneralShopEntry;
import dev.lone.itemsadder.api.CustomStack;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class MarketManager {

    private final CropMarketPlugin plugin;
    private final Random random = new Random();

    public MarketManager(CropMarketPlugin plugin) {
        this.plugin = plugin;
    }

    // ──────────────────────────────────────────
    //  판매 처리
    // ──────────────────────────────────────────

    public SellResult sellCrop(Player player, CropEntry crop, ItemGrade grade) {
        GradeConfig config = crop.getGradeConfig(grade);
        GradeData   data   = crop.getGradeData(grade);

        if (config == null || data == null) {
            return SellResult.fail("이 등급은 설정되지 않았습니다.");
        }

        // 세금 계산
        ConfigManager cfg = plugin.getConfigManager();

        int required  = cfg.getSellAmount(grade);
        int available = countItems(player, config);

        if (available < required) {
            return SellResult.fail("아이템이 부족합니다. 필요: " + required + "개, 보유: " + available + "개");
        }

        // 아이템 제거
        removeItems(player, config, required);

        double taxRate = (!cfg.getTaxReducedPermission().isEmpty()
                && player.hasPermission(cfg.getTaxReducedPermission()))
                ? cfg.getTaxReducedRate() / 100.0
                : cfg.getTaxDefaultRate() / 100.0;

        double grossPayment = data.getCurrentPrice() * cfg.getSellAmount(grade);
        double taxAmount    = Math.round(grossPayment * taxRate * 100.0) / 100.0;
        double netPayment   = grossPayment - taxAmount;

        plugin.getEconomy().depositPlayer(player, netPayment);

        // 붕괴 상태(가격 0)면 가격 변동·크래시 체크 없이 0 유지
        boolean crashed         = false;
        double  newPrice;
        double  decreasePercent = 0.0;
        double  priceBeforeCrash = data.getCurrentPrice();
        double  crashChancePct   = 0.0;

        if (data.getCurrentPrice() <= 0) {
            newPrice = 0.0;
        } else {
            // 가격 하락 적용 (수확량 보정 포함)
            double[] range = cfg.getAdjustmentRange(grade, crop.getMaxHarvest());
            decreasePercent = Math.round((range[0] + random.nextDouble() * (range[1] - range[0])) * 10.0) / 10.0;
            newPrice = data.getCurrentPrice() * (1.0 - decreasePercent / 100.0);
            newPrice = Math.max(newPrice, config.getMinPrice());

            // 크래시 체크: 임계값 이상일 때만 확률 발생 (가격이 높을수록 확률 증가)
            double[] crashSettings = cfg.getCrashSettings(grade, crop.getMaxHarvest());
            double crashThreshold  = config.getBasePrice() * (crashSettings[0] / 100.0);
            double minChance       = crashSettings[1] / 100.0;
            double maxChance       = crashSettings[2] / 100.0;

            double crashChance = 0.0;
            if (newPrice >= crashThreshold) {
                double maxPrice   = config.getMaxPrice();
                double priceRange = maxPrice - crashThreshold;
                if (priceRange > 0) {
                    double progress = Math.min((newPrice - crashThreshold) / priceRange, 1.0);
                    crashChance = Math.min(minChance + (maxChance - minChance) * progress, maxChance);
                } else {
                    crashChance = maxChance;
                }
            }
            crashChancePct = crashChance * 100.0;

            if (random.nextDouble() < crashChance) {
                newPrice = 0.0;
                crashed  = true;
            }
        }

        data.updatePrice(newPrice);
        data.incrementSales();

        // 파일 로그: 판매
        MarketLogger logger = plugin.getMarketLogger();
        logger.logSell(
            player.getName(),
            crop.getDisplayName(),
            grade.getDisplayName(),
            required,
            grossPayment, taxAmount, netPayment,
            newPrice, decreasePercent
        );

        // 파일 로그: 붕괴
        if (crashed) {
            logger.logCrash(
                player.getName(),
                crop.getDisplayName(),
                grade.getDisplayName(),
                priceBeforeCrash,
                crashChancePct
            );
            scheduleCrashRecovery(crop, grade, config, data);

            // 보이지 않는 손
            if (cfg.isInvisibleHandEnabled()) {
                double chance = cfg.getInvisibleHandChancePct() / 100.0;
                if (random.nextDouble() < chance) {
                    triggerInvisibleHand();
                }
            }
        }

        plugin.getDataManager().save();

        return SellResult.success(required, grossPayment, taxAmount, netPayment, decreasePercent, crashed);
    }

    private void triggerInvisibleHand() {
        ConfigManager cfg = plugin.getConfigManager();
        for (CropEntry c : cfg.getCrops()) {
            for (ItemGrade g : ItemGrade.values()) {
                GradeConfig gc = c.getGradeConfig(g);
                GradeData   gd = c.getGradeData(g);
                if (gc == null || gd == null) continue;

                double minPrice = gc.getBasePrice() * 0.5;
                double maxPrice = gc.getMaxPrice();
                double newPrice = minPrice + random.nextDouble() * (maxPrice - minPrice);
                newPrice = Math.max(gc.getMinPrice(), Math.min(newPrice, maxPrice));
                gd.loadPrice(newPrice);
            }
        }
        plugin.getDataManager().save();
        plugin.getServer().broadcastMessage(
            "§6[보이지 않는 손] §r시장에 보이지 않는 손이 움직입니다! §e모든 작물 가격이 재조정되었습니다."
        );
        plugin.getMarketLogger().logInvisibleHand();
    }

    private void scheduleCrashRecovery(CropEntry crop, ItemGrade grade,
                                        GradeConfig config, GradeData data) {
        ConfigManager cfg        = plugin.getConfigManager();
        long delayHours          = cfg.getCrashRecoveryDelayHours();
        double targetMultiplier  = cfg.getCrashRecoveryMultiplier(grade);
        double targetPrice       = Math.min(config.getBasePrice() * targetMultiplier, config.getMaxPrice());
        long delayTicks          = delayHours * 3600L * 20L;

        // 복구 예정 시각 기록 (GUI 상태 문구 표시용)
        data.setCrashRecoveryAt(System.currentTimeMillis() + delayHours * 3600L * 1000L);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            data.setCrashRecoveryAt(0L);
            data.loadPrice(targetPrice);
            plugin.getDataManager().save();
            plugin.getMarketLogger().logCrashRecovery(
                crop.getDisplayName(), grade.getDisplayName(), targetPrice
            );
            plugin.getServer().broadcastMessage(String.format(
                "§a[시장 회복] §r%s %s§r등급 가격이 §a%,.0f§r원으로 회복되었습니다.",
                crop.getDisplayName(), grade.getDisplayName(), targetPrice
            ));
        }, delayTicks);
    }

    // ──────────────────────────────────────────
    //  서버 재시작 후 붕괴 복구 재등록
    // ──────────────────────────────────────────

    /**
     * 서버 재시작 시 호출 — 가격=0 이고 crashRecoveryAt > 0 인 작물의
     * 복구 타스크를 재등록합니다. 이미 지난 시각이면 즉시 복구합니다.
     */
    public void rescheduleCrashedCrops() {
        long now = System.currentTimeMillis();
        for (CropEntry crop : plugin.getConfigManager().getCrops()) {
            for (ItemGrade grade : ItemGrade.values()) {
                GradeConfig config = crop.getGradeConfig(grade);
                GradeData   data   = crop.getGradeData(grade);
                if (config == null || data == null) continue;
                if (data.getCurrentPrice() > 0) continue;

                long recoveryAt = data.getCrashRecoveryAt();

                if (recoveryAt <= 0) {
                    // crashRecoveryAt 미등록 → 지금부터 delay 만큼 후 복구
                    long newRecoveryAt = now + (long) plugin.getConfigManager().getCrashRecoveryDelayHours() * 3600L * 1000L;
                    data.setCrashRecoveryAt(newRecoveryAt);
                    recoveryAt = newRecoveryAt;
                }

                long remainingMs   = Math.max(0L, recoveryAt - now);
                long remainingTicks = remainingMs / 50L;

                final CropEntry  fc = crop;
                final ItemGrade  fg = grade;
                final GradeConfig fgc = config;
                final GradeData  fgd = data;

                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    double targetPrice = Math.min(
                        fgc.getBasePrice() * plugin.getConfigManager().getCrashRecoveryMultiplier(fg),
                        fgc.getMaxPrice()
                    );
                    fgd.setCrashRecoveryAt(0L);
                    fgd.loadPrice(targetPrice);
                    plugin.getDataManager().save();
                    plugin.getMarketLogger().logCrashRecovery(
                        fc.getDisplayName(), fg.getDisplayName(), targetPrice
                    );
                    plugin.getServer().broadcastMessage(String.format(
                        "§a[시장 회복] §r%s %s§r등급 가격이 §a%,.0f§r원으로 회복되었습니다.",
                        fc.getDisplayName(), fg.getDisplayName(), targetPrice
                    ));
                }, remainingTicks);
            }
        }
        plugin.getDataManager().save();
    }

    // ──────────────────────────────────────────
    //  일반 판매
    // ──────────────────────────────────────────

    public SellResult sellGeneral(Player player, GeneralShopEntry entry) {
        ConfigManager cfg = plugin.getConfigManager();
        int available = countItems(player, entry);
        int required  = entry.getSellAmount();

        if (available < required) {
            return SellResult.fail("아이템이 부족합니다. 필요: " + required + "개, 보유: " + available + "개");
        }

        removeItems(player, entry, required);

        double taxRate = (!cfg.getTaxReducedPermission().isEmpty()
                && player.hasPermission(cfg.getTaxReducedPermission()))
                ? cfg.getTaxReducedRate() / 100.0
                : cfg.getTaxDefaultRate() / 100.0;

        double grossPayment = entry.getPrice() * required;
        double taxAmount    = Math.round(grossPayment * taxRate * 100.0) / 100.0;
        double netPayment   = grossPayment - taxAmount;

        plugin.getEconomy().depositPlayer(player, netPayment);
        plugin.getMarketLogger().logGeneralSell(
            player.getName(), entry.getDisplayName(), required, grossPayment, taxAmount, netPayment
        );

        return SellResult.success(required, grossPayment, taxAmount, netPayment, 0, false);
    }

    // ──────────────────────────────────────────
    //  아이템 카운트 / 제거
    // ──────────────────────────────────────────

    public int countItems(Player player, GradeConfig config) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            if (matchesItem(item, config)) count += item.getAmount();
        }
        return count;
    }

    public int countItems(Player player, GeneralShopEntry entry) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            if (matchesItem(item, entry)) count += item.getAmount();
        }
        return count;
    }

    private void removeItems(Player player, GeneralShopEntry entry, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item == null || !matchesItem(item, entry)) continue;
            if (item.getAmount() <= remaining) {
                remaining -= item.getAmount();
                player.getInventory().setItem(i, null);
            } else {
                item.setAmount(item.getAmount() - remaining);
                remaining = 0;
            }
        }
        player.updateInventory();
    }

    private void removeItems(Player player, GradeConfig config, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item == null || !matchesItem(item, config)) continue;
            if (item.getAmount() <= remaining) {
                remaining -= item.getAmount();
                player.getInventory().setItem(i, null);
            } else {
                item.setAmount(item.getAmount() - remaining);
                remaining = 0;
            }
        }
        player.updateInventory();
    }

    // ──────────────────────────────────────────
    //  아이템 매칭
    // ──────────────────────────────────────────

    public boolean matchesItem(ItemStack item, GeneralShopEntry entry) {
        if (item == null) return false;
        try {
            return switch (entry.getItemType()) {
                case VANILLA -> {
                    Material mat = Material.matchMaterial(entry.getItemId());
                    yield mat != null && item.getType() == mat;
                }
                case MMOITEMS -> {
                    NBTItem nbt = NBTItem.get(item);
                    if (!nbt.hasType()) yield false;
                    yield entry.getMmoitemsType().equalsIgnoreCase(nbt.getType())
                        && entry.getItemId().equalsIgnoreCase(nbt.getString("MMOITEMS_ITEM_ID"));
                }
                case ITEMSADDER -> {
                    CustomStack cs = CustomStack.byItemStack(item);
                    yield cs != null && cs.getNamespacedID().equals(entry.getItemId());
                }
            };
        } catch (Throwable e) {
            return false;
        }
    }

    public boolean matchesItem(ItemStack item, GradeConfig config) {
        if (item == null) return false;
        try {
            return switch (config.getItemType()) {
                case VANILLA -> {
                    Material mat = Material.matchMaterial(config.getItemId());
                    yield mat != null && item.getType() == mat;
                }
                case MMOITEMS -> {
                    NBTItem nbt = NBTItem.get(item);
                    if (!nbt.hasType()) yield false;
                    yield config.getMmoitemsType().equalsIgnoreCase(nbt.getType())
                        && config.getItemId().equalsIgnoreCase(nbt.getString("MMOITEMS_ITEM_ID"));
                }
                case ITEMSADDER -> {
                    CustomStack cs = CustomStack.byItemStack(item);
                    yield cs != null && cs.getNamespacedID().equals(config.getItemId());
                }
            };
        } catch (Throwable e) {
            return false;
        }
    }

    // ──────────────────────────────────────────
    //  판매 결과 레코드
    // ──────────────────────────────────────────

    public record SellResult(boolean success, int amount,
                              double grossPayment, double taxAmount, double netPayment,
                              double decreasePercent, boolean crashed, String message) {

        public static SellResult success(int amount, double grossPayment,
                                         double taxAmount, double netPayment,
                                         double decreasePercent, boolean crashed) {
            return new SellResult(true, amount, grossPayment, taxAmount, netPayment,
                                  decreasePercent, crashed, null);
        }

        public static SellResult fail(String message) {
            return new SellResult(false, 0, 0, 0, 0, 0, false, message);
        }
    }
}
