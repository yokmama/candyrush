package com.candyrush.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /candyrush コマンドのタブ補完
 */
public class DebugCommandTabCompleter implements TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
        "status",
        "start",
        "stop",
        "reset",
        "setcenter",
        "clearcenter"
    );

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // 権限チェック
        if (!sender.hasPermission("candyrush.admin")) {
            return new ArrayList<>();
        }

        // 第1引数（サブコマンド）
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                .filter(sub -> sub.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
