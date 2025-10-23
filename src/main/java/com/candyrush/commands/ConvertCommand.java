package com.candyrush.commands;

import com.candyrush.CandyRushPlugin;
import com.candyrush.utils.LanguageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /convert コマンド - インベントリ内の全食料をポイントに一括変換
 */
public class ConvertCommand implements CommandExecutor {

    private final CandyRushPlugin plugin;
    private final LanguageManager lang;

    public ConvertCommand(CandyRushPlugin plugin) {
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

        // 食料を一括変換
        plugin.getPointConversionManager().convertAllFood(player);

        return true;
    }
}
