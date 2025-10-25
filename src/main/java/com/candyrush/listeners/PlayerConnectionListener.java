package com.candyrush.listeners;

import com.candyrush.CandyRushPlugin;
import com.candyrush.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * プレイヤーのログイン/ログアウトイベントを処理
 */
public class PlayerConnectionListener implements Listener {

    private final CandyRushPlugin plugin;

    public PlayerConnectionListener(CandyRushPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // プレイヤーデータの読み込みまたは作成
        plugin.getPlayerManager().handlePlayerJoin(event.getPlayer());

        var player = event.getPlayer();

        // スコアボードを設定
        plugin.getScoreboardManager().setupScoreboard(player);

        // ゲーム状態を表示
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String state = plugin.getGameManager().getCurrentState().name();
            int onlinePlayers = Bukkit.getOnlinePlayers().size();
            int minPlayers = plugin.getConfigManager().getMinPlayers();

            MessageUtils.sendMessage(player, "&a&l━━━━━━ CandyRush ━━━━━━");
            MessageUtils.sendMessage(player, "&eゲーム状態: &6" + getStateMessage(state));
            MessageUtils.sendMessage(player, "&eオンライン: &6" + onlinePlayers + "&7/&e最低" + minPlayers + "人");

            switch (state) {
                case "WAITING":
                    if (onlinePlayers >= minPlayers) {
                        MessageUtils.sendMessage(player, "&a最低人数に達しました！カウントダウンを開始します...");
                    } else {
                        MessageUtils.sendMessage(player, "&eあと&c" + (minPlayers - onlinePlayers) + "人&eでゲーム開始");
                    }
                    break;
                case "COUNTDOWN":
                    MessageUtils.sendMessage(player, "&eまもなくゲーム開始！");
                    break;
                case "RUNNING":
                    MessageUtils.sendMessage(player, "&aゲーム進行中！参加してください");
                    // ゲーム中に参加したプレイヤーにもショップアイテムを付与
                    plugin.getShopManager().giveShopItem(player);
                    break;
                case "COOLDOWN":
                    MessageUtils.sendMessage(player, "&cクールダウン中... しばらくお待ちください");
                    break;
            }
            MessageUtils.sendMessage(player, "&a&l━━━━━━━━━━━━━━━━━━━━");

            // カウントダウン開始チェック（最低人数に達したか）
            plugin.getGameManager().tryStartCountdown();
        }, 20L); // 1秒後にチェック
    }

    private String getStateMessage(String state) {
        switch (state) {
            case "WAITING": return "待機中";
            case "COUNTDOWN": return "カウントダウン中";
            case "RUNNING": return "ゲーム進行中";
            case "COOLDOWN": return "クールダウン中";
            default: return state;
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // プレイヤーデータの保存
        plugin.getPlayerManager().handlePlayerQuit(event.getPlayer());

        // カウントダウン中に人数不足になった場合
        if (plugin.getGameManager().getCurrentState().name().equals("COUNTDOWN")) {
            int onlinePlayers = Bukkit.getOnlinePlayers().size() - 1; // このプレイヤーを除く
            int minPlayers = plugin.getConfigManager().getMinPlayers();

            if (onlinePlayers < minPlayers) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plugin.getGameManager().cancelCountdown();
                }, 5L); // 少し遅延させてキャンセル
            }
        }
    }
}
