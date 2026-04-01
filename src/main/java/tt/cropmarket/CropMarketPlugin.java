package tt.cropmarket;

import tt.cropmarket.command.MarketCommand;
import tt.cropmarket.gui.CropSellGUI;
import tt.cropmarket.gui.MainMenuGUI;
import tt.cropmarket.gui.MarketGUI;
import tt.cropmarket.gui.SeedShopGUI;
import tt.cropmarket.listener.GUIListener;
import tt.cropmarket.manager.ConfigManager;
import tt.cropmarket.manager.DataManager;
import tt.cropmarket.manager.MarketLogger;
import tt.cropmarket.manager.MarketManager;
import tt.cropmarket.scheduler.RecoveryScheduler;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class CropMarketPlugin extends JavaPlugin {

    private static CropMarketPlugin instance;

    private Economy           economy;
    private ConfigManager     configManager;
    private DataManager       dataManager;
    private MarketManager     marketManager;
    private MarketLogger      marketLogger;
    private RecoveryScheduler recoveryScheduler;
    private MainMenuGUI       mainMenuGUI;
    private MarketGUI         marketGUI;
    private SeedShopGUI       seedShopGUI;
    private CropSellGUI       cropSellGUI;

    @Override
    public void onEnable() {
        instance = this;

        if (!setupEconomy()) {
            getLogger().severe("Vault Economy를 찾을 수 없습니다! 플러그인을 비활성화합니다.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();

        marketLogger      = new MarketLogger(getDataFolder());
        configManager     = new ConfigManager(this);
        dataManager       = new DataManager(this);
        marketManager     = new MarketManager(this);
        mainMenuGUI       = new MainMenuGUI(this);
        marketGUI         = new MarketGUI(this);
        seedShopGUI       = new SeedShopGUI(this);
        cropSellGUI       = new CropSellGUI(this);

        dataManager.load();

        recoveryScheduler = new RecoveryScheduler(this);
        recoveryScheduler.start();

        getServer().getPluginManager().registerEvents(new GUIListener(this), this);

        MarketCommand cmd = new MarketCommand(this);

        // /농작물 명령어
        var nongCmd = getCommand("농작물");
        if (nongCmd != null) {
            nongCmd.setExecutor(cmd);
            nongCmd.setTabCompleter(cmd);
        }

        // /농작물관리 명령어
        var manageCmd = getCommand("농작물관리");
        if (manageCmd != null) {
            manageCmd.setExecutor(cmd);
            manageCmd.setTabCompleter(cmd);
        }

        // 5분마다 자동 저장
        getServer().getScheduler().runTaskTimerAsynchronously(this,
            () -> getServer().getScheduler().runTask(this, () -> dataManager.save()),
            6000L, 6000L);

        getLogger().info("CropMarket이 활성화되었습니다. 작물 수: "
            + configManager.getCrops().size());
        marketLogger.logInfo("=== CropMarket 활성화 | 작물 수: " + configManager.getCrops().size() + " ===");
    }

    @Override
    public void onDisable() {
        if (recoveryScheduler != null) recoveryScheduler.stop();
        if (dataManager       != null) dataManager.save();
        if (marketLogger      != null) marketLogger.logInfo("=== CropMarket 비활성화 | 데이터 저장 완료 ===");
        getLogger().info("CropMarket이 비활성화되었습니다. 데이터 저장 완료.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp =
            getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    // ──────────────────────────────────────────
    //  Getters
    // ──────────────────────────────────────────

    public static CropMarketPlugin getInstance() { return instance; }

    public Economy            getEconomy()           { return economy; }
    public ConfigManager      getConfigManager()      { return configManager; }
    public DataManager        getDataManager()        { return dataManager; }
    public MarketManager      getMarketManager()      { return marketManager; }
    public MarketLogger       getMarketLogger()       { return marketLogger; }
    public RecoveryScheduler  getRecoveryScheduler()  { return recoveryScheduler; }
    public MainMenuGUI        getMainMenuGUI()        { return mainMenuGUI; }
    public MarketGUI          getMarketGUI()          { return marketGUI; }
    public SeedShopGUI        getSeedShopGUI()        { return seedShopGUI; }
    public CropSellGUI        getCropSellGUI()        { return cropSellGUI; }
}
