package tt.cropmarket.command;

import tt.cropmarket.CropMarketPlugin;
import tt.cropmarket.model.CropEntry;
import tt.cropmarket.model.GradeConfig;
import tt.cropmarket.model.GradeData;
import tt.cropmarket.model.ItemGrade;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class MarketCommand implements CommandExecutor, TabCompleter {

    private final CropMarketPlugin plugin;
    private final Random random = new Random();

    public MarketCommand(CropMarketPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmdName = command.getName();

        // /농작물 → 작물 상점 열기
        if (cmdName.equals("농작물")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§c플레이어만 사용 가능합니다.");
                return true;
            }
            if (!player.hasPermission("cropmarket.use")) {
                player.sendMessage("§c권한이 없습니다.");
                return true;
            }
            plugin.getMainMenuGUI().open(player);
            return true;
        }

        // /농작물관리 ... → 관리자 기능
        if (!cmdName.equals("농작물관리")) return true;

        if (!sender.hasPermission("cropmarket.admin")) {
            sender.sendMessage("§c관리자 권한이 필요합니다.");
            return true;
        }

        if (args.length < 1) { sendAdminHelp(sender); return true; }

        switch (args[0].toLowerCase()) {

            // ── reload ──────────────────────────────
            case "reload" -> {
                plugin.getConfigManager().reload();
                plugin.getDataManager().load();
                sender.sendMessage("§a[농작물관리] 설정 및 데이터를 다시 불러왔습니다.");
            }

            // ── info ────────────────────────────────
            case "info" -> {
                sender.sendMessage("§6[CropMarket] §e현재 가격 정보:");
                sender.sendMessage("§7다음 회복: §f" + plugin.getRecoveryScheduler().getTimeRemaining());
                for (CropEntry crop : plugin.getConfigManager().getCrops()) {
                    sender.sendMessage("§e" + crop.getDisplayName() + " §8(" + crop.getId() + ")");
                    for (ItemGrade grade : ItemGrade.values()) {
                        GradeConfig config = crop.getGradeConfig(grade);
                        GradeData   data   = crop.getGradeData(grade);
                        if (config == null || data == null) continue;
                        sender.sendMessage(String.format("  §7%s§7: §f%,.0f§7원  §8(기준: %,.0f / 판매: %d회)",
                                grade.getDisplayName(), data.getCurrentPrice(),
                                config.getBasePrice(), data.getSalesCount()));
                    }
                }
            }

            // ── reset [cropId] [grade] ───────────────
            case "reset" -> {
                if (args.length < 2) {
                    // 전체 리셋
                    resetAll();
                    sender.sendMessage("§a[농작물관리] 모든 작물 가격이 기준가로 초기화되었습니다.");
                } else {
                    CropEntry crop = plugin.getConfigManager().getCrop(args[1]);
                    if (crop == null) { sender.sendMessage("§c알 수 없는 작물: " + args[1]); return true; }

                    if (args.length >= 3) {
                        ItemGrade grade = parseGrade(args[2]);
                        if (grade == null) { sender.sendMessage("§c잘못된 등급: " + args[2]); return true; }
                        resetGrade(crop, grade);
                        sender.sendMessage("§a[농작물관리] " + crop.getDisplayName()
                                + " " + grade.getDisplayName() + " §a등급이 초기화되었습니다.");
                    } else {
                        for (ItemGrade g : ItemGrade.values()) resetGrade(crop, g);
                        sender.sendMessage("§a[농작물관리] " + crop.getDisplayName() + " §a전 등급이 초기화되었습니다.");
                    }
                    plugin.getDataManager().save();
                }
            }

            // ── set <cropId> <grade> <price> ─────────
            case "set" -> {
                if (args.length < 4) {
                    sender.sendMessage("§c사용법: /농작물관리 set <작물ID> <등급> <가격>");
                    return true;
                }
                CropEntry crop = plugin.getConfigManager().getCrop(args[1]);
                if (crop == null) { sender.sendMessage("§c알 수 없는 작물: " + args[1]); return true; }

                ItemGrade grade = parseGrade(args[2]);
                if (grade == null) { sender.sendMessage("§c잘못된 등급: " + args[2]); return true; }

                double price;
                try { price = Double.parseDouble(args[3]); }
                catch (NumberFormatException e) { sender.sendMessage("§c올바른 숫자를 입력하세요."); return true; }

                GradeConfig config = crop.getGradeConfig(grade);
                GradeData   data   = crop.getGradeData(grade);
                if (config == null || data == null) { sender.sendMessage("§c해당 등급이 설정되어 있지 않습니다."); return true; }

                double clamped = Math.max(config.getMinPrice(), Math.min(config.getMaxPrice(), price));
                data.loadPrice(clamped);
                plugin.getDataManager().save();
                sender.sendMessage(String.format("§a[농작물관리] %s %s등급 가격을 %,.0f원으로 설정했습니다.",
                        crop.getDisplayName(), grade.getDisplayName(), clamped));
            }

            default -> sendAdminHelp(sender);
        }
        return true;
    }

    private void resetAll() {
        for (CropEntry crop : plugin.getConfigManager().getCrops())
            for (ItemGrade g : ItemGrade.values())
                resetGrade(crop, g);
        plugin.getDataManager().save();
    }

    private void resetGrade(CropEntry crop, ItemGrade grade) {
        GradeConfig config = crop.getGradeConfig(grade);
        GradeData   data   = crop.getGradeData(grade);
        if (config == null || data == null) return;

        // 기준가(basePrice) ~ 최대가(maxPrice) 사이에서 랜덤 가격 설정
        double basePrice = config.getBasePrice();
        double maxPrice = config.getMaxPrice();
        double randomPrice = basePrice + random.nextDouble() * (maxPrice - basePrice);
        data.loadPrice(randomPrice);
        data.resetSales();
    }

    private ItemGrade parseGrade(String s) {
        try { return ItemGrade.valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage("§6[농작물관리] §e관리 명령어:");
        sender.sendMessage("§7/농작물관리 reload §8- 설정 재로드");
        sender.sendMessage("§7/농작물관리 info §8- 현재 가격 현황");
        sender.sendMessage("§7/농작물관리 reset §8- 전체 가격 초기화");
        sender.sendMessage("§7/농작물관리 reset <작물ID> [등급] §8- 특정 가격 초기화");
        sender.sendMessage("§7/농작물관리 set <작물ID> <등급> <가격> §8- 가격 직접 설정");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        String cmdName = cmd.getName();

        // /농작물관리 명령어 자동완성
        if (cmdName.equals("농작물관리")) {
            if (args.length == 1) {
                return List.of("reload", "info", "reset", "set");
            }

            if (args.length >= 2) {
                String sub = args[0].toLowerCase();
                if (args.length == 2 && (sub.equals("reset") || sub.equals("set")))
                    return plugin.getConfigManager().getCrops().stream()
                            .map(CropEntry::getId).collect(Collectors.toList());
                if (args.length == 3 && (sub.equals("reset") || sub.equals("set")))
                    return Arrays.stream(ItemGrade.values())
                            .map(g -> g.name().toLowerCase()).collect(Collectors.toList());
            }
        }

        return List.of();
    }
}
