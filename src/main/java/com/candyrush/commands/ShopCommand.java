package com.candyrush.commands;

import com.candyrush.CandyRushPlugin;
import com.candyrush.models.GameState;
import com.candyrush.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /shop コマンド - ショップを開く
 */
public class ShopCommand implements CommandExecutor {

    private final CandyRushPlugin plugin;

    public ShopCommand(CandyRushPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("このコマンドはプレイヤーのみ実行できます。");
            return true;
        }

        Player player = (Player) sender;

        // ゲーム中のみショップを開ける
        if (plugin.getGameManager().getCurrentState() != GameState.RUNNING) {
            MessageUtils.sendMessage(player, "&cゲーム中のみショップを利用できます！");
            return true;
        }

        // ショップを開く
        plugin.getShopManager().openShop(player);

        return true;
    }
}
