package tt.cropmarket.scheduler;

import tt.cropmarket.CropMarketPlugin;
import tt.cropmarket.model.CropEntry;
import tt.cropmarket.model.GradeConfig;
import tt.cropmarket.model.GradeData;
import tt.cropmarket.model.ItemGrade;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 가격 자연 회복 스케줄러
 * - 등급별 독립적인 회복 주기로 운영
 * - 회복 폭 = price-adjustment 범위와 동일 (등급별)
 * - 회복 상한 = 각 등급의 max-price
 */
public class RecoveryScheduler {

    private final CropMarketPlugin plugin;
    private final Random random = new Random();
    private final Map<ItemGrade, BukkitTask> tasks = new HashMap<>();
    private final Map<ItemGrade, Long> nextRecoveryTimes = new HashMap<>();

    public RecoveryScheduler(CropMarketPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        for (ItemGrade grade : ItemGrade.values()) {
            scheduleNext(grade);
        }
    }

    private void scheduleNext(ItemGrade grade) {
        int[] range = plugin.getConfigManager().getRecoveryRange(grade);
        int minMin = range[0];
        int maxMin = range[1];
        int delayMinutes = minMin + random.nextInt(maxMin - minMin + 1);
        long delayTicks = (long) delayMinutes * 60L * 20L;

        nextRecoveryTimes.put(grade, System.currentTimeMillis() + (long) delayMinutes * 60L * 1000L);

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            applyRecovery(grade);
            scheduleNext(grade);
        }, delayTicks);

        tasks.put(grade, task);
        plugin.getLogger().info("[" + grade.getDisplayName() + "] 다음 회복 이벤트: " + delayMinutes + "분 후");
    }

    private void applyRecovery(ItemGrade grade) {
        for (CropEntry crop : plugin.getConfigManager().getCrops()) {
            GradeConfig config = crop.getGradeConfig(grade);
            GradeData   data   = crop.getGradeData(grade);
            if (config == null || data == null) continue;

            double current  = data.getCurrentPrice();
            double maxPrice = config.getMaxPrice();
            if (current >= maxPrice) continue;

            double[] range = plugin.getConfigManager().getAdjustmentRange(grade);
            double pct = (range[0] + random.nextDouble() * (range[1] - range[0])) / 100.0;
            double newPrice = Math.min(current * (1.0 + pct), maxPrice);
            data.updatePrice(newPrice);

            plugin.getMarketLogger().logRecovery(
                grade.getDisplayName(),
                crop.getDisplayName(),
                current, newPrice, pct
            );
        }
        plugin.getDataManager().save();
    }

    public void stop() {
        for (BukkitTask task : tasks.values()) {
            if (task != null && !task.isCancelled()) task.cancel();
        }
        tasks.clear();
    }

    /** GUI / 커맨드용 남은 시간 문자열 (가장 가까운 회복까지의 시간) */
    public String getTimeRemaining() {
        long nextTime = Long.MAX_VALUE;
        for (Long time : nextRecoveryTimes.values()) {
            if (time != null && time < nextTime) {
                nextTime = time;
            }
        }

        if (nextTime == Long.MAX_VALUE) return "미설정";

        long ms = nextTime - System.currentTimeMillis();
        if (ms <= 0) return "곧";
        long totalSec = ms / 1000;
        long hours   = totalSec / 3600;
        long minutes = (totalSec % 3600) / 60;
        long seconds = totalSec % 60;
        if (hours > 0) return hours + "시간 " + minutes + "분";
        if (minutes > 0) return minutes + "분 " + seconds + "초";
        return seconds + "초";
    }

    /** 특정 등급의 남은 회복 시간 */
    public String getTimeRemaining(ItemGrade grade) {
        Long nextTime = nextRecoveryTimes.get(grade);
        if (nextTime == null) return "미설정";

        long ms = nextTime - System.currentTimeMillis();
        if (ms <= 0) return "곧";
        long totalSec = ms / 1000;
        long hours   = totalSec / 3600;
        long minutes = (totalSec % 3600) / 60;
        long seconds = totalSec % 60;
        if (hours > 0) return hours + "시간 " + minutes + "분";
        if (minutes > 0) return minutes + "분 " + seconds + "초";
        return seconds + "초";
    }
}
