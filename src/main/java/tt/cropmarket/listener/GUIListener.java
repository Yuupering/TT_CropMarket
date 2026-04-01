package tt.cropmarket.listener;

import tt.cropmarket.CropMarketPlugin;
import tt.cropmarket.gui.CropSellGUI;
import tt.cropmarket.gui.MainMenuGUI;
import tt.cropmarket.gui.MarketGUI;
import tt.cropmarket.gui.SeedShopGUI;
import tt.cropmarket.manager.MarketManager.SellResult;
import tt.cropmarket.model.CropEntry;
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
    private final Map<UUID, CropEntry> activeSellCrop = new HashMap<>();
    private final Map<UUID, Integer>   compassPage    = new HashMap<>();

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
            plugin.getSeedShopGUI().handleBuy(player, event.getSlot());

        } else if (title.equals(MarketGUI.TITLE)) {
            event.setCancelled(true);
            handleMarketClick(player, event.getSlot());

        } else if (title.startsWith(CropSellGUI.TITLE_PREFIX) && title.endsWith(CropSellGUI.TITLE_SUFFIX)) {
            event.setCancelled(true);
            handleSellClick(player, event.getSlot(), title);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = event.getView().getTitle();
        UUID uuid = event.getPlayer().getUniqueId();
        if (title.equals(MarketGUI.TITLE) || (title.startsWith(CropSellGUI.TITLE_PREFIX) && title.endsWith(CropSellGUI.TITLE_SUFFIX))) {
            activeSellCrop.remove(uuid);
        }
        // compassPage는 MainMenu로 완전히 나갔을 때만 초기화
        if (title.equals(MainMenuGUI.TITLE)) {
            compassPage.remove(uuid);
        }
    }

    // ──────────────────────────────────────────
    //  초기 메뉴
    // ──────────────────────────────────────────

    private void handleMainMenu(Player player, int slot) {
        if (slot == MainMenuGUI.SLOT_SEEDS) {
            plugin.getSeedShopGUI().open(player);
        } else if (slot == MainMenuGUI.SLOT_MARKET) {
            plugin.getMarketGUI().open(player);
        }
    }

    // ──────────────────────────────────────────
    //  농작물 시장 목록
    // ──────────────────────────────────────────

    private void handleMarketClick(Player player, int slot) {
        // 뒤로가기
        if (slot == MarketGUI.SLOT_BACK) {
            plugin.getMainMenuGUI().open(player);
            return;
        }

        // 나침반 클릭 → 다음 페이지
        if (slot == MarketGUI.SLOT_INFO) {
            int current = compassPage.getOrDefault(player.getUniqueId(), 0);
            int next = (current + 1) % MarketGUI.MAX_PAGES;
            compassPage.put(player.getUniqueId(), next);
            plugin.getMarketGUI().open(player, next);
            return;
        }

        int index = plugin.getMarketGUI().slotToCropIndex(slot);
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
        // 뒤로가기
        if (slot == CropSellGUI.SLOT_BACK) {
            activeSellCrop.remove(player.getUniqueId());
            plugin.getMarketGUI().open(player);
            return;
        }

        // 판매 슬롯 → 등급
        ItemGrade grade = switch (slot) {
            case CropSellGUI.SLOT_NORMAL_SELL -> ItemGrade.NORMAL;
            case CropSellGUI.SLOT_SILVER_SELL -> ItemGrade.SILVER;
            case CropSellGUI.SLOT_GOLD_SELL   -> ItemGrade.GOLD;
            default -> null;
        };
        if (grade == null) return;

        // 작물 찾기
        CropEntry crop = activeSellCrop.get(player.getUniqueId());
        if (crop == null) {
            String rawName = CropSellGUI.extractCropName(title);
            plugin.getLogger().info("추출된 작물 이름: '" + rawName + "'");
            for (CropEntry c : plugin.getConfigManager().getCrops()) {
                String cropDisplayName = c.getDisplayName().replaceAll("§[0-9a-fk-or]", "");
                plugin.getLogger().info("비교: '" + cropDisplayName + "' vs '" + rawName + "'");
                if (cropDisplayName.equals(rawName)) {
                    crop = c;
                    plugin.getLogger().info("작물 찾음: " + crop.getId());
                    break;
                }
            }
            if (crop == null) {
                plugin.getLogger().info("작물을 찾을 수 없음!");
                return;
            }
            activeSellCrop.put(player.getUniqueId(), crop);
        }

        plugin.getLogger().info("판매 시작: " + crop.getId() + ", 등급: " + grade);

        // 판매 처리
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

        // GUI 갱신
        final CropEntry finalCrop = crop;
        Bukkit.getScheduler().runTask(plugin, () ->
            plugin.getCropSellGUI().open(player, finalCrop));
    }
}
