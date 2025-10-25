package com.candyrush.models;

/**
 * チェストの戦利品カテゴリー
 * チェストの種類によって異なるアイテムカテゴリーが入る
 */
public enum ChestLootCategory {
    /** 食料・お菓子系 */
    FOOD,

    /** ポーション・薬系 */
    POTION,

    /** 武器・防具系 */
    EQUIPMENT,

    /** 燃料・素材系 */
    MATERIAL,

    /** ユーティリティ系（矢、エンダーパール等） */
    UTILITY,

    /** トラップチェスト用（高性能装備） */
    TRAP_REWARD
}
