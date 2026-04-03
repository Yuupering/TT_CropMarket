package tt.cropmarket.listener;

import tt.cropmarket.CropMarketPlugin;
import tt.cropmarket.gui.CropSellGUI;
import tt.cropmarket.gui.GeneralSellGUI;
import tt.cropmarket.gui.GeneralShopGUI;
import tt.cropmarket.gui.MainMenuGUI;
import tt.cropmarket.gui.MarketGUI;
import tt.cropmarket.gui.SeedBuyGUI;
import tt.cropmarket.gui.SeedShopGUI;
import tt.cropmarket.manager.MarketManager.SellResult;
import tt.cropmarket.model.CropEntry;
import tt.cropmarket.model.GeneralShopEntry;
import tt.cropmarket.model.ItemGrade;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GUIListener implements Listener {

    private final CropMarketPlugin plugin;

    private final Map<UUID, CropEntry>        activeSellCrop   = new HashMap<>();
    private final Map<UUID, CropEntry>        activeBuyCrop    = new HashMap<>();
    private final Map<UUID, GeneralShopEntry> activeGeneralEntry = new HashMap<>();
    private final Map<UUID, Integer>          compassPage      = new HashMap<>();
    private final Map<UUID, Integer>          marketItemPage   = new HashMap<>();
    private final Map<UUID, Integer>          seedItemPage     = new HashMap<>();
    private final Map<UUID, Integer>          generalItemPage  = new HashMap<>();

    public GUIListener(CropMarketPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        String title = event.getView().getTitle();

        if (title.equals(MainMenuGUI.TITLE)) {
            event.setCancelled(true);
            handleMainMenu(player, event.getSlot());

        } else if (title.equals(SeedShopGUI.TITLE)) {
            event.setCancelled(true);
            handleSeedShopClick(player, event.getSlot());

        } else if (title.startsWith(SeedBuyGUI.TITLE_PREFIX) && title.endsWith(SeedBuyGUI.TITLE_SUFFIX)) {
            event.setCancelled(true);
            handleSeedBuyClick(player, event.getSlot(), title);

        } else if (title.equals(MarketGUI.TITLE)) {
            event.setCancelled(true);
            handleMarketClick(player, event.getSlot());

        } else if (title.startsWith(CropSellGUI.TITLE_PREFIX) && title.endsWith(CropSellGUI.TITLE_SUFFIX)) {
            event.setCancelled(true);
            handleSellClick(player, event.getSlot(), title);

        } else if (title.equals(GeneralShopGUI.TITLE)) {
            event.setCancelled(true);
            handleGeneralShopClick(player, event.getSlot());

        } else if (title.startsWith(GeneralSellGUI.TITLE_PREFIX) && title.endsWith(GeneralSellGUI.TITLE_SUFFIX)) {
            event.setCancelled(true);
            handleGeneralSellClick(player, event.getSlot(), title);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = event.getView().getTitle();
        UUID uuid = event.getPlayer().getUniqueId();

        if (title.equals(MarketGUI.TITLE) || (title.startsWith(CropSellGUI.TITLE_PREFIX) && title.endsWith(CropSellGUI.TITLE_SUFFIX))) {
            activeSellCrop.remove(uuid);
        }
        if (title.equals(SeedShopGUI.TITLE) || (title.startsWith(SeedBuyGUI.TITLE_PREFIX) && title.endsWith(SeedBuyGUI.TITLE_SUFFIX))) {
            activeBuyCrop.remove(uuid);
        }
        if (title.equals(MainMenuGUI.TITLE)) {
            compassPage.remove(uuid);
            marketItemPage.remove(uuid);
            seedItemPage.remove(uuid);
            generalItemPage.remove(uuid);
        }
        if (title.equals(GeneralShopGUI.TITLE)
                || (title.startsWith(GeneralSellGUI.TITLE_PREFIX) && title.endsWith(GeneralSellGUI.TITLE_SUFFIX))) {
            activeGeneralEntry.remove(uuid);
        }
    }

    // ──────────────────────────────────────────
    //  메인 메뉴
    // ──────────────────────────────────────────

    private void handleMainMenu(Player player, int slot) {
        if (slot == MainMenuGUI.SLOT_SEEDS) {
            seedItemPage.put(player.getUniqueId(), 0);
            plugin.getSeedShopGUI().open(player, 0);
        } else if (slot == MainMenuGUI.SLOT_MARKET) {
            marketItemPage.put(player.getUniqueId(), 0);
            plugin.getMarketGUI().open(player, 0, 0);
        } else if (slot == MainMenuGUI.SLOT_GENERAL) {
            var cfg = plugin.getConfigManager();
            if (!cfg.isGeneralShopEnabled()) return;
            if (cfg.isGeneralShopPermEnabled()
                    && !player.hasPermission(cfg.getGeneralShopPermNode())) return;
            generalItemPage.put(player.getUniqueId(), 0);
            plugin.getGeneralShopGUI().open(player, 0);
        }
    }

    // ──────────────────────────────────────────
    //  씨앗 상점 목록
    // ──────────────────────────────────────────

    private void handleSeedShopClick(Player player, int slot) {
        int page = seedItemPage.getOrDefault(player.getUniqueId(), 0);

        if (slot == SeedShopGUI.SLOT_BACK) {
            seedItemPage.remove(player.getUniqueId());
            plugin.getMainMenuGUI().open(player);
            return;
        }
        if (slot == SeedShopGUI.SLOT_PREV) {
            int newPage = Math.max(0, page - 1);
            seedItemPage.put(player.getUniqueId(), newPage);
            plugin.getSeedShopGUI().open(player, newPage);
            return;
        }
        if (slot == SeedShopGUI.SLOT_NEXT) {
            int newPage = page + 1;
            seedItemPage.put(player.getUniqueId(), newPage);
            plugin.getSeedShopGUI().open(player, newPage);
            return;
        }

        plugin.getSeedShopGUI().handleSeedClick(player, slot, page);
    }

    // ──────────────────────────────────────────
    //  씨앗 세부 구매 GUI
    // ──────────────────────────────────────────

    private void handleSeedBuyClick(Player player, int slot, String title) {
        // 뒤로가기 → 씨앗 상점의 현재 페이지로 복귀
        if (slot == SeedBuyGUI.SLOT_BACK) {
            int page = seedItemPage.getOrDefault(player.getUniqueId(), 0);
            plugin.getSeedShopGUI().open(player, page);
            return;
        }

        CropEntry crop = activeBuyCrop.get(player.getUniqueId());
        if (crop == null) {
            String rawName = SeedBuyGUI.extractCropName(title);
            for (CropEntry c : plugin.getConfigManager().getCrops()) {
                if (c.getDisplayName().replaceAll("§[0-9a-fk-or]", "").equals(rawName)) {
                    crop = c;
                    break;
                }
            }
            if (crop == null) return;
            activeBuyCrop.put(player.getUniqueId(), crop);
        }
        plugin.getSeedBuyGUI().handleClick(player, slot, crop);
    }

    // ──────────────────────────────────────────
    //  농작물 시장 목록
    // ──────────────────────────────────────────

    private void handleMarketClick(Player player, int slot) {
        int itemPage    = marketItemPage.getOrDefault(player.getUniqueId(), 0);
        int cPage       = compassPage.getOrDefault(player.getUniqueId(), 0);

        if (slot == MarketGUI.SLOT_BACK) {
            marketItemPage.remove(player.getUniqueId());
            plugin.getMainMenuGUI().open(player);
            return;
        }
        if (slot == MarketGUI.SLOT_PREV) {
            int newPage = Math.max(0, itemPage - 1);
            marketItemPage.put(player.getUniqueId(), newPage);
            plugin.getMarketGUI().open(player, cPage, newPage);
            return;
        }
        if (slot == MarketGUI.SLOT_NEXT) {
            int newPage = itemPage + 1;
            marketItemPage.put(player.getUniqueId(), newPage);
            plugin.getMarketGUI().open(player, cPage, newPage);
            return;
        }
        if (slot == MarketGUI.SLOT_INFO) {
            int next = (cPage + 1) % MarketGUI.MAX_PAGES;
            compassPage.put(player.getUniqueId(), next);
            plugin.getMarketGUI().open(player, next, itemPage);
            return;
        }

        int index = plugin.getMarketGUI().slotToCropIndex(slot, itemPage);
        if (index < 0) return;

        var crops = plugin.getConfigManager().getCrops();
        if (index >= crops.size()) return;

        CropEntry crop = crops.get(index);
        activeSellCrop.put(player.getUniqueId(), crop);
        plugin.getCropSellGUI().open(player, crop);
    }

    // ──────────────────────────────────────────
    //  작물별 판매 GUI
    // ──────────────────────────────────────────

    private void handleSellClick(Player player, int slot, String title) {
        if (slot == CropSellGUI.SLOT_BACK) {
            activeSellCrop.remove(player.getUniqueId());
            int itemPage = marketItemPage.getOrDefault(player.getUniqueId(), 0);
            int cPage    = compassPage.getOrDefault(player.getUniqueId(), 0);
            plugin.getMarketGUI().open(player, cPage, itemPage);
            return;
        }

        ItemGrade grade = switch (slot) {
            case CropSellGUI.SLOT_NORMAL_SELL -> ItemGrade.NORMAL;
            case CropSellGUI.SLOT_SILVER_SELL -> ItemGrade.SILVER;
            case CropSellGUI.SLOT_GOLD_SELL   -> ItemGrade.GOLD;
            default -> null;
        };
        if (grade == null) return;

        CropEntry crop = activeSellCrop.get(player.getUniqueId());
        if (crop == null) {
            String rawName = CropSellGUI.extractCropName(title);
            for (CropEntry c : plugin.getConfigManager().getCrops()) {
                if (c.getDisplayName().replaceAll("§[0-9a-fk-or]", "").equals(rawName)) {
                    crop = c;
                    break;
                }
            }
            if (crop == null) return;
            activeSellCrop.put(player.getUniqueId(), crop);
        }

        SellResult result = plugin.getMarketManager().sellCrop(player, crop, grade);

        if (result.success()) {
            double currentPrice = crop.getGradeData(grade).getCurrentPrice();

            player.sendMessage(String.format(
                "§a[농작물판매] §f%s %s§f등급 %d개 판매 완료",
                crop.getDisplayName(), grade.getDisplayName(), result.amount()
            ));
            player.sendMessage(String.format(
                "§7  판매가 §f%,.0f§7원  §8│  §7세금 §c-%,.0f§7원  §8│  §7실수령 §a+%,.0f§7원",
                result.grossPayment(), result.taxAmount(), result.netPayment()
            ));

            if (result.crashed()) {
                player.sendMessage("§c  ※ 시장 붕괴! 가격이 0원§c으로 폭락했습니다. 6시간 후 복구됩니다.");
                Bukkit.broadcastMessage(String.format(
                    "§c[시장 붕괴] §f%s %s§f등급 가격이 §c0원§f으로 폭락했습니다! §76시간 후 복구",
                    crop.getDisplayName(), grade.getDisplayName()
                ));
            } else {
                player.sendMessage(String.format(
                    "§7  가격 §c▼ %.1f%% §7하락 → 현재 §f%,.0f§7원",
                    result.decreasePercent(), currentPrice
                ));
            }
        } else {
            player.sendMessage("§c[농작물판매] " + result.message());
        }

        final CropEntry finalCrop = crop;
        Bukkit.getScheduler().runTask(plugin, () ->
            plugin.getCropSellGUI().open(player, finalCrop));
    }

    // ──────────────────────────────────────────
    //  일반 농작물 판매 목록 GUI
    // ──────────────────────────────────────────

    private void handleGeneralShopClick(Player player, int slot) {
        int page = generalItemPage.getOrDefault(player.getUniqueId(), 0);

        if (slot == GeneralShopGUI.SLOT_BACK) {
            generalItemPage.remove(player.getUniqueId());
            plugin.getMainMenuGUI().open(player);
            return;
        }
        if (slot == GeneralShopGUI.SLOT_PREV) {
            int newPage = Math.max(0, page - 1);
            generalItemPage.put(player.getUniqueId(), newPage);
            plugin.getGeneralShopGUI().open(player, newPage);
            return;
        }
        if (slot == GeneralShopGUI.SLOT_NEXT) {
            int newPage = page + 1;
            generalItemPage.put(player.getUniqueId(), newPage);
            plugin.getGeneralShopGUI().open(player, newPage);
            return;
        }

        int index = plugin.getGeneralShopGUI().slotToEntryIndex(slot, page);
        if (index < 0) return;

        var items = plugin.getConfigManager().getGeneralShopItems();
        if (index >= items.size()) return;

        GeneralShopEntry entry = items.get(index);
        activeGeneralEntry.put(player.getUniqueId(), entry);
        plugin.getGeneralSellGUI().open(player, entry);
    }

    // ──────────────────────────────────────────
    //  일반 농작물 판매 세부 GUI
    // ──────────────────────────────────────────

    private void handleGeneralSellClick(Player player, int slot, String title) {
        if (slot == GeneralSellGUI.SLOT_BACK) {
            activeGeneralEntry.remove(player.getUniqueId());
            int page = generalItemPage.getOrDefault(player.getUniqueId(), 0);
            plugin.getGeneralShopGUI().open(player, page);
            return;
        }
        if (slot != GeneralSellGUI.SLOT_SELL) return;

        GeneralShopEntry entry = activeGeneralEntry.get(player.getUniqueId());
        if (entry == null) {
            // 타이틀에서 복원
            String rawName = GeneralSellGUI.extractEntryName(title);
            for (GeneralShopEntry e : plugin.getConfigManager().getGeneralShopItems()) {
                if (e.getDisplayName().replaceAll("§[0-9a-fk-or]", "").equals(rawName)) {
                    entry = e;
                    break;
                }
            }
            if (entry == null) return;
            activeGeneralEntry.put(player.getUniqueId(), entry);
        }

        SellResult result = plugin.getMarketManager().sellGeneral(player, entry);

        if (result.success()) {
            player.sendMessage(String.format(
                "§a[일반판매] §f%s %d개 판매 완료",
                entry.getDisplayName(), result.amount()
            ));
            player.sendMessage(String.format(
                "§7  판매가 §f%,.0f§7원  §8│  §7세금 §c-%,.0f§7원  §8│  §7실수령 §a+%,.0f§7원",
                result.grossPayment(), result.taxAmount(), result.netPayment()
            ));
        } else {
            player.sendMessage("§c[일반판매] " + result.message());
        }

        final GeneralShopEntry finalEntry = entry;
        Bukkit.getScheduler().runTask(plugin, () ->
            plugin.getGeneralSellGUI().open(player, finalEntry));
    }
}
