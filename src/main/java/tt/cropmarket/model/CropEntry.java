package tt.cropmarket.model;

import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

public class CropEntry {

    private final String id;
    private final String displayName;
    private final Material icon;

    private String seedItemId = null;
    private double seedPrice  = 100.0;
    private int    maxHarvest = 0;   // 0 = 미설정 (수확량 보정 비적용)

    private final Map<ItemGrade, GradeConfig> gradeConfigs = new EnumMap<>(ItemGrade.class);
    private final Map<ItemGrade, GradeData>   gradeData    = new EnumMap<>(ItemGrade.class);

    public CropEntry(String id, String displayName, Material icon) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
    }

    public void addGrade(ItemGrade grade, GradeConfig config) {
        gradeConfigs.put(grade, config);
        gradeData.put(grade, new GradeData(config.getBasePrice()));
    }

    public String      getId()          { return id; }
    public String      getDisplayName() { return displayName; }
    public Material    getIcon()        { return icon; }
    public String      getSeedItemId()  { return seedItemId; }
    public double      getSeedPrice()   { return seedPrice; }
    public void        setSeedItemId(String id)      { this.seedItemId = id; }
    public void        setSeedPrice(double price)   { this.seedPrice = price; }
    public void        setMaxHarvest(int max)       { this.maxHarvest = max; }
    public int         getMaxHarvest()              { return maxHarvest; }
    public boolean     hasSeed()        { return seedItemId != null && !seedItemId.isEmpty(); }

    public GradeConfig getGradeConfig(ItemGrade grade) { return gradeConfigs.get(grade); }
    public GradeData   getGradeData(ItemGrade grade)   { return gradeData.get(grade); }

    public Map<ItemGrade, GradeConfig> getGradeConfigs() { return gradeConfigs; }
    public Map<ItemGrade, GradeData>   getAllGradeData()  { return gradeData; }
}
