package com.candyrush.models;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;

/**
 * ショップで販売されるアイテムを表すモデル
 */
public class ShopItem {

    private final String id;
    private final String name;
    private final Material material;
    private final int amount;
    private final int price;
    private final PotionType potionType;
    private final String category;

    public ShopItem(String id, String name, Material material, int amount, int price, PotionType potionType, String category) {
        this.id = id;
        this.name = name;
        this.material = material;
        this.amount = amount;
        this.price = price;
        this.potionType = potionType;
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Material getMaterial() {
        return material;
    }

    public int getAmount() {
        return amount;
    }

    public int getPrice() {
        return price;
    }

    public PotionType getPotionType() {
        return potionType;
    }

    public String getCategory() {
        return category;
    }

    /**
     * アイテムスタックを作成
     */
    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§r" + name);

            List<String> lore = new ArrayList<>();
            lore.add("§6価格: " + price + "pt");
            lore.add("");
            lore.add("§7クリックで購入");
            meta.setLore(lore);

            // ポーション用の特殊処理
            if (meta instanceof PotionMeta && potionType != null) {
                PotionMeta potionMeta = (PotionMeta) meta;
                potionMeta.setBasePotionType(potionType);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * 購入用のアイテムを作成（実際に渡すアイテム、説明なし）
     */
    public ItemStack createPurchasedItem() {
        ItemStack item = new ItemStack(material, amount);

        // ポーション用の特殊処理
        if (potionType != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof PotionMeta) {
                PotionMeta potionMeta = (PotionMeta) meta;
                potionMeta.setBasePotionType(potionType);
                item.setItemMeta(potionMeta);
            }
        }

        return item;
    }
}
