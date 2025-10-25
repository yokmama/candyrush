package com.candyrush.commands;

import com.candyrush.CandyRushPlugin;
import com.candyrush.models.PlayerData;
import com.candyrush.models.Team;
import com.candyrush.models.TeamColor;
import com.candyrush.utils.LanguageManager;
import com.candyrush.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * /stats コマンド - ゲーム統計とランキングを表示
 */
public class StatsCommand implements CommandExecutor {

    private final CandyRushPlugin plugin;
    private final LanguageManager lang;

    public StatsCommand(CandyRushPlugin plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(lang.getMessage("player_only"));
            return true;
        }

        Player player = (Player) sender;

        // 引数がない場合、自分の統計を表示
        if (args.length == 0) {
            showPlayerStats(player, player);
            return true;
        }

        // サブコマンド処理
        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "top":
                showTopPlayers(player);
                break;

            case "teams":
                showTeamStats(player);
                break;

            case "help":
                showHelp(player);
                break;

            default:
                showPlayerStats(player, player);
                break;
        }

        return true;
    }

    /**
     * プレイヤー個人の統計を表示
     */
    private void showPlayerStats(Player viewer, Player target) {
        PlayerData data = plugin.getPlayerManager().getOrCreatePlayerData(target);

        viewer.sendMessage(MessageUtils.colorize("&a&l━━━━━━━━━━━━━━━━━━━━━━"));
        viewer.sendMessage(MessageUtils.colorize("&e&l  " + target.getName() + " の統計"));
        viewer.sendMessage(MessageUtils.colorize("&a&l━━━━━━━━━━━━━━━━━━━━━━"));

        if (data.getTeamColor() != null) {
            viewer.sendMessage(MessageUtils.colorize("&eチーム: " + data.getTeamColor().getFormattedName()));
        } else {
            viewer.sendMessage(MessageUtils.colorize("&eチーム: &7未割り当て"));
        }

        viewer.sendMessage(MessageUtils.colorize("&eポイント: &6" + MessageUtils.formatPoints(data.getPoints())));
        viewer.sendMessage(MessageUtils.colorize("&ePK: &c" + data.getPk()));
        viewer.sendMessage(MessageUtils.colorize("&ePKK: &6" + data.getPkk()));
        viewer.sendMessage(MessageUtils.colorize("&ePKK/PK: &b" + String.format("%.2f", data.getKDRatio())));

        if (data.isMurdererActive()) {
            viewer.sendMessage(MessageUtils.colorize("&c&l状態: Murderer"));
        }

        viewer.sendMessage(MessageUtils.colorize("&a&l━━━━━━━━━━━━━━━━━━━━━━"));
    }

    /**
     * トッププレイヤーランキングを表示
     */
    private void showTopPlayers(Player player) {
        List<PlayerData> topPlayers = plugin.getPlayerManager().getTopPlayers(10);

        player.sendMessage(MessageUtils.colorize("&6&l━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(MessageUtils.colorize("&e&l  トッププレイヤー TOP 10"));
        player.sendMessage(MessageUtils.colorize("&6&l━━━━━━━━━━━━━━━━━━━━━━"));

        if (topPlayers.isEmpty()) {
            player.sendMessage(MessageUtils.colorize("&7データがありません"));
        } else {
            int rank = 1;
            for (PlayerData data : topPlayers) {
                String rankColor = getRankColor(rank);
                String teamName = data.getTeamColor() != null ?
                        data.getTeamColor().getFormattedName() : "&7未所属";

                player.sendMessage(MessageUtils.colorize(
                        rankColor + rank + ". &f" + data.getName() +
                        " &7[" + teamName + "&7] &6" + MessageUtils.formatPoints(data.getPoints()) + "pt"));
                rank++;
            }
        }

        player.sendMessage(MessageUtils.colorize("&6&l━━━━━━━━━━━━━━━━━━━━━━"));
    }

    /**
     * チーム統計を表示
     */
    private void showTeamStats(Player player) {
        List<Team> ranking = plugin.getTeamManager().getTeamRanking();

        player.sendMessage(MessageUtils.colorize("&b&l━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(MessageUtils.colorize("&e&l  チームランキング"));
        player.sendMessage(MessageUtils.colorize("&b&l━━━━━━━━━━━━━━━━━━━━━━"));

        if (!plugin.getGameManager().isGameRunning()) {
            player.sendMessage(MessageUtils.colorize("&7ゲームが進行していません"));
        } else {
            int rank = 1;
            for (Team team : ranking) {
                String rankColor = getRankColor(rank);

                player.sendMessage(MessageUtils.colorize(
                        rankColor + rank + ". " + team.getFormattedName() +
                        " &7(" + team.getPlayerCount() + "人)"));
                player.sendMessage(MessageUtils.colorize(
                        "   &eポイント: &6" + MessageUtils.formatPoints(team.getPoints()) +
                        " &7| &eキル: &c" + team.getKills() +
                        " &7| &eデス: &8" + team.getDeaths()));
                rank++;
            }
        }

        player.sendMessage(MessageUtils.colorize("&b&l━━━━━━━━━━━━━━━━━━━━━━"));
    }

    /**
     * ヘルプメッセージを表示
     */
    private void showHelp(Player player) {
        player.sendMessage(lang.getMessage("stats.help"));
        player.sendMessage(lang.getMessage("stats.help_usage"));
        player.sendMessage(lang.getMessage("stats.help_top"));
        player.sendMessage(lang.getMessage("stats.help_teams"));
    }

    /**
     * ランクに応じた色を取得
     */
    private String getRankColor(int rank) {
        switch (rank) {
            case 1:
                return "&6&l"; // 金色・太字
            case 2:
                return "&7&l"; // 灰色・太字
            case 3:
                return "&c&l"; // 赤色・太字
            default:
                return "&e"; // 黄色
        }
    }
}
