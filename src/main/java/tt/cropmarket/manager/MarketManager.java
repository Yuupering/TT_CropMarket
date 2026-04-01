package tt.cropmarket.manager;

import tt.cropmarket.CropMarketPlugin;
import tt.cropmarket.model.*;
import dev.lone.itemsadder.api.CustomStack;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Random;
import java.util.logging.Logger;

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
            plugin.getLogger().severe("Config/Data null - 작물: " + crop.getId() + ", 등급: " + grade);
            return SellResult.fail("이 등급은 설정되지 않았습니다.");
        }

        int required  = grade.getSellAmount();
        int available = countItems(player, config);
        plugin.getLogger().info("아이템 확인 - 필요: " + required + ", 보유: " + available);

        if (available < required) {
            plugin.getLogger().info("아이템 부족!");
            return SellResult.fail("아이템이 부족합니다. 필요: " + required + "개, 보유: " + available + "개");
        }

        plugin.getLogger().info("판매 처리 중...");

        // 아이템 제거
        removeItems(player, config, required);

        // 세금 계산
        ConfigManager cfg = plugin.getConfigManager();
        double taxRate = (!cfg.getTaxReducedPermission().isEmpty()
                && player.hasPermission(cfg.getTaxReducedPermission()))
                ? cfg.getTaxReducedRate() / 100.0
                : cfg.getTaxDefaultRate() / 100.0;

        double grossPayment = data.getCurrentPrice() * grade.getSellAmount();
        double taxAmount    = Math.round(grossPayment * taxRate * 100.0) / 100.0;
        double netPayment   = grossPayment - taxAmount;

        plugin.getEconomy().depositPlayer(player, netPayment);

        // 가격 하락 적용
        double[] range = cfg.getAdjustmentRange(grade);
        double decreasePercent = Math.round((range[0] + random.nextDouble() * (range[1] - range[0])) * 10.0) / 10.0;
        double newPrice = data.getCurrentPrice() * (1.0 - decreasePercent / 100.0);
        newPrice = Math.max(newPrice, config.getMinPrice());

        // 크래시 체크: 임계값 이상일 때만 확률 발생 (가격이 높을수록 확률 증가)
        boolean crashed = false;
        double[] crashSettings  = cfg.getCrashSettings(grade);
        double crashThreshold   = config.getBasePrice() * (crashSettings[0] / 100.0);
        double minChance        = crashSettings[1] / 100.0;
        double maxChance        = crashSettings[2] / 100.0;

        double crashChance = 0.0;
        if (newPrice >= crashThreshold) {
            // 가격이 threshold에서 maxPrice로 갈수록 확률이 minChance에서 maxChance로 증가
            double maxPrice = config.getMaxPrice();
            double priceRange = maxPrice - crashThreshold;
            if (priceRange > 0) {
                double progress = (newPrice - crashThreshold) / priceRange;
                progress = Math.min(progress, 1.0);
                crashChance = minChance + (maxChance - minChance) * progress;
                crashChance = Math.min(crashChance, maxChance);
            } else {
                crashChance = maxChance;
            }
        }

        double priceBeforeCrash = newPrice;
        if (random.nextDouble() < crashChance) {
            newPrice = 0.0;
            crashed  = true;
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
                crashChance * 100.0
            );
            scheduleCrashRecovery(crop, grade, config, data);
        }

        plugin.getDataManager().save();

        return SellResult.success(required, grossPayment, taxAmount, netPayment, decreasePercent, crashed);
    }

    private void scheduleCrashRecovery(CropEntry crop, ItemGrade grade,
                                        GradeConfig config, GradeData data) {
        ConfigManager cfg        = plugin.getConfigManager();
        long delayHours          = cfg.getCrashRecoveryDelayHours();
        double targetMultiplier  = cfg.getCrashRecoveryMultiplier(grade);
        double targetPrice       = Math.min(config.getBasePrice() * targetMultiplier, config.getMaxPrice());
        long delayTicks          = delayHours * 3600L * 20L;

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
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

    public boolean matchesItem(ItemStack item, GradeConfig config) {
        if (item == null) return false;
        try {
            if (config.getItemType() == ItemType.MMOITEMS) {
                NBTItem nbt = NBTItem.get(item);
                if (!nbt.hasType()) return false;
                String type = nbt.getType();
                String id   = nbt.getString("MMOITEMS_ITEM_ID");
                return config.getMmoitemsType().equalsIgnoreCase(type)
                    && config.getItemId().equalsIgnoreCase(id);
            } else {
                CustomStack cs = CustomStack.byItemStack(item);
                return cs != null && cs.getNamespacedID().equals(config.getItemId());
            }
        } catch (Exception e) {
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
