package tt.cropmarket.manager;

import tt.cropmarket.CropMarketPlugin;
import tt.cropmarket.model.CropEntry;
import tt.cropmarket.model.GradeConfig;
import tt.cropmarket.model.GradeData;
import tt.cropmarket.model.ItemGrade;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class DataManager {

    private final CropMarketPlugin plugin;
    private final File dataFile;
    private YamlConfiguration dataConfig;

    private long nextResetTime = 0L;

    public DataManager(CropMarketPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load() {
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); }
            catch (IOException e) { e.printStackTrace(); }
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        nextResetTime = dataConfig.getLong("next-reset", 0L);

        for (CropEntry crop : plugin.getConfigManager().getCrops()) {
            for (ItemGrade grade : ItemGrade.values()) {
                GradeConfig config = crop.getGradeConfig(grade);
                GradeData   data   = crop.getGradeData(grade);
                if (config == null || data == null) continue;

                String path = "crops." + crop.getId() + "." + grade.name().toLowerCase();

                double savedPrice  = dataConfig.getDouble(path + ".current-price", config.getBasePrice());
                int    savedSales  = dataConfig.getInt(path + ".sales-count", 0);

                data.loadPrice(savedPrice);
                for (int i = 0; i < savedSales; i++) data.incrementSales();
            }
        }
    }

    public void save() {
        saveAsync();
    }

    public void saveAsync() {
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            dataConfig.set("next-reset", nextResetTime);

            for (CropEntry crop : plugin.getConfigManager().getCrops()) {
                for (ItemGrade grade : ItemGrade.values()) {
                    GradeData data = crop.getGradeData(grade);
                    if (data == null) continue;

                    String path = "crops." + crop.getId() + "." + grade.name().toLowerCase();
                    dataConfig.set(path + ".current-price", data.getCurrentPrice());
                    dataConfig.set(path + ".sales-count",   data.getSalesCount());
                }
            }

            try { dataConfig.save(dataFile); }
            catch (IOException e) { e.printStackTrace(); }
        });
    }

    public long getNextResetTime()           { return nextResetTime; }
    public void setNextResetTime(long time)  { this.nextResetTime = time; }
}
