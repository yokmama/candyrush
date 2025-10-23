package com.candyrush.utils;

import com.candyrush.CandyRushPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * 多言語対応を管理するクラス
 * messages_en.yml と messages_ja.yml から文言を読み込む
 */
public class LanguageManager {

    private final CandyRushPlugin plugin;
    private FileConfiguration messages;
    private String currentLanguage;

    public LanguageManager(CandyRushPlugin plugin) {
        this.plugin = plugin;
        this.currentLanguage = plugin.getConfigManager().getLanguage();
        loadMessages();
    }

    /**
     * 言語ファイルを読み込む
     */
    private void loadMessages() {
        String languageFile = "messages_" + currentLanguage + ".yml";
        File customFile = new File(plugin.getDataFolder(), languageFile);

        // カスタムファイルが存在すれば使用
        if (customFile.exists()) {
            messages = YamlConfiguration.loadConfiguration(customFile);
            plugin.getLogger().info("Loaded custom language file: " + languageFile);
        } else {
            // jarから読み込む
            InputStream defaultStream = plugin.getResource(languageFile);
            if (defaultStream != null) {
                messages = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
                plugin.getLogger().info("Loaded language file from jar: " + languageFile);
            } else {
                // フォールバック: 英語
                plugin.getLogger().warning("Language file not found: " + languageFile + ", falling back to English");
                InputStream fallbackStream = plugin.getResource("messages_en.yml");
                if (fallbackStream != null) {
                    messages = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(fallbackStream, StandardCharsets.UTF_8));
                } else {
                    plugin.getLogger().severe("Could not load any language file!");
                    messages = new YamlConfiguration(); // 空の設定
                }
            }
        }
    }

    /**
     * メッセージを取得（プレースホルダーなし）
     */
    public String getMessage(String key) {
        String message = messages.getString(key);
        if (message == null) {
            plugin.getLogger().warning("Missing translation key: " + key);
            return "&c[Missing: " + key + "]";
        }
        return MessageUtils.colorize(message);
    }

    /**
     * メッセージを取得（プレースホルダーあり）
     */
    public String getMessage(String key, Map<String, String> placeholders) {
        String message = getMessage(key);

        // プレースホルダーを置換
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return message;
    }

    /**
     * メッセージを取得（単一プレースホルダー）
     */
    public String getMessage(String key, String placeholder, String value) {
        Map<String, String> map = new HashMap<>();
        map.put(placeholder, value);
        return getMessage(key, map);
    }

    /**
     * プレフィックス付きメッセージを取得
     */
    public String getMessageWithPrefix(String key) {
        return getMessage("prefix") + getMessage(key);
    }

    /**
     * プレフィックス付きメッセージを取得（プレースホルダーあり）
     */
    public String getMessageWithPrefix(String key, Map<String, String> placeholders) {
        return getMessage("prefix") + getMessage(key, placeholders);
    }

    /**
     * 言語を変更
     */
    public void setLanguage(String language) {
        this.currentLanguage = language;
        loadMessages();
        plugin.getLogger().info("Language changed to: " + language);
    }

    /**
     * 現在の言語を取得
     */
    public String getCurrentLanguage() {
        return currentLanguage;
    }

    /**
     * 言語ファイルを再読み込み
     */
    public void reload() {
        this.currentLanguage = plugin.getConfigManager().getLanguage();
        loadMessages();
    }
}
