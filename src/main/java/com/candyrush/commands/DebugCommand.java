package com.candyrush.commands;

import com.candyrush.CandyRushPlugin;
import com.candyrush.models.GameState;
import com.candyrush.utils.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * デバッグ用コマンド
 * ゲームの状態確認や強制開始など
 */
public class DebugCommand implements CommandExecutor {

    private final CandyRushPlugin plugin;
    private final LanguageManager lang;

    public DebugCommand(CandyRushPlugin plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("candyrush.admin")) {
            sender.sendMessage(lang.getMessage("no_permission"));
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "status":
                showStatus(sender);
                break;
            case "start":
                forceStart(sender);
                break;
            case "stop":
                forceStop(sender);
                break;
            case "reset":
                resetGame(sender);
                break;
            case "setcenter":
                setMapCenter(sender);
                break;
            case "clearcenter":
                clearMapCenter(sender);
                break;
            default:
                showHelp(sender);
                break;
        }

        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(lang.getMessage("admin.debug_help_title"));
        sender.sendMessage(lang.getMessage("admin.debug_status"));
        sender.sendMessage(lang.getMessage("admin.debug_start"));
        sender.sendMessage(lang.getMessage("admin.debug_stop"));
        sender.sendMessage(lang.getMessage("admin.debug_reset"));
        sender.sendMessage(lang.getMessage("admin.debug_setcenter"));
        sender.sendMessage(lang.getMessage("admin.debug_clearcenter"));
    }

    private void showStatus(CommandSender sender) {
        GameState state = plugin.getGameManager().getCurrentState();
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        int minPlayers = plugin.getConfigManager().getMinPlayers();
        boolean canStart = plugin.getGameManager().canStartGame();

        sender.sendMessage(lang.getMessage("admin.status_title"));

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("state", state.name());
        sender.sendMessage(lang.getMessage("admin.status_state", placeholders));

        placeholders.clear();
        placeholders.put("online", String.valueOf(onlinePlayers));
        sender.sendMessage(lang.getMessage("admin.status_online", placeholders));

        placeholders.clear();
        placeholders.put("min", String.valueOf(minPlayers));
        sender.sendMessage(lang.getMessage("admin.status_min", placeholders));

        placeholders.clear();
        placeholders.put("canStart", String.valueOf(canStart));
        sender.sendMessage(lang.getMessage("admin.status_can_start", placeholders));

        placeholders.clear();
        placeholders.put("canJoin", String.valueOf(plugin.getGameManager().canJoinGame()));
        sender.sendMessage(lang.getMessage("admin.status_can_join", placeholders));

        placeholders.clear();
        placeholders.put("running", String.valueOf(plugin.getGameManager().isGameRunning()));
        sender.sendMessage(lang.getMessage("admin.status_running", placeholders));

        if (plugin.getGameManager().getCurrentRound() != null) {
            Integer roundId = plugin.getGameManager().getCurrentRound().getId();
            placeholders.clear();
            placeholders.put("round", roundId != null ? String.valueOf(roundId) : "Not saved yet");
            sender.sendMessage(lang.getMessage("admin.status_round", placeholders));
        }

        // チーム情報
        sender.sendMessage(lang.getMessage("admin.status_teams_title"));
        for (Player player : Bukkit.getOnlinePlayers()) {
            var teamOpt = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
            String teamName = teamOpt.map(team -> team.getColor().name()).orElse("None");
            placeholders.clear();
            placeholders.put("player", player.getName());
            placeholders.put("team", teamName);
            sender.sendMessage(lang.getMessage("admin.status_team_player", placeholders));
        }
    }

    private void forceStart(CommandSender sender) {
        GameState state = plugin.getGameManager().getCurrentState();

        if (state != GameState.WAITING) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("state", state.name());
            sender.sendMessage(lang.getMessage("admin.start_already_started", placeholders));
            return;
        }

        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        if (onlinePlayers == 0) {
            sender.sendMessage(lang.getMessage("admin.start_no_players"));
            return;
        }

        sender.sendMessage(lang.getMessage("admin.start_success"));
        plugin.getGameManager().tryStartCountdown();

        // カウントダウンが始まらなかった場合の確認
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (plugin.getGameManager().getCurrentState() == GameState.WAITING) {
                sender.sendMessage(lang.getMessage("admin.start_failed"));
                sender.sendMessage(lang.getMessage("admin.start_check_status"));
            }
        }, 20L);
    }

    private void forceStop(CommandSender sender) {
        if (!plugin.getGameManager().isGameRunning()) {
            sender.sendMessage(lang.getMessage("admin.stop_not_running"));
            return;
        }

        sender.sendMessage(lang.getMessage("admin.stop_success"));
        // GameManagerのstopGameメソッドを呼び出す必要がある
        // TODO: GameManagerにpublic stopGame()メソッドを追加
        sender.sendMessage(lang.getMessage("admin.stop_not_implemented"));
    }

    private void resetGame(CommandSender sender) {
        sender.sendMessage(lang.getMessage("admin.reset_success"));
        plugin.getGameManager().initialize();
        sender.sendMessage(lang.getMessage("admin.reset_complete"));
    }

    private void setMapCenter(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(lang.getMessage("player_only"));
            return;
        }

        Player player = (Player) sender;
        int x = player.getLocation().getBlockX();
        int z = player.getLocation().getBlockZ();

        // 座標を設定
        plugin.getConfigManager().setMapCenter(x, z);

        sender.sendMessage(lang.getMessage("admin.setcenter_border"));
        sender.sendMessage(lang.getMessage("admin.setcenter_title"));

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("x", String.valueOf(x));
        sender.sendMessage(lang.getMessage("admin.setcenter_x", placeholders));

        placeholders.clear();
        placeholders.put("z", String.valueOf(z));
        sender.sendMessage(lang.getMessage("admin.setcenter_z", placeholders));

        sender.sendMessage(lang.getMessage("admin.setcenter_note"));
        sender.sendMessage(lang.getMessage("admin.setcenter_border"));

        plugin.getLogger().info("Map center set to X=" + x + ", Z=" + z + " by " + player.getName());
    }

    private void clearMapCenter(CommandSender sender) {
        // 現在の設定を確認
        Integer currentX = plugin.getConfigManager().getMapCenterX();
        Integer currentZ = plugin.getConfigManager().getMapCenterZ();

        if (currentX == null && currentZ == null) {
            sender.sendMessage(lang.getMessage("admin.clearcenter_already_cleared"));
            sender.sendMessage(lang.getMessage("admin.clearcenter_using_spawn"));
            return;
        }

        // 座標をクリア
        plugin.getConfigManager().clearMapCenter();

        sender.sendMessage(lang.getMessage("admin.clearcenter_border"));
        sender.sendMessage(lang.getMessage("admin.clearcenter_title"));
        if (currentX != null && currentZ != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("x", String.valueOf(currentX));
            placeholders.put("z", String.valueOf(currentZ));
            sender.sendMessage(lang.getMessage("admin.clearcenter_previous", placeholders));
        }
        sender.sendMessage(lang.getMessage("admin.clearcenter_note"));
        sender.sendMessage(lang.getMessage("admin.clearcenter_border"));

        plugin.getLogger().info("Map center cleared by " + sender.getName());
    }
}