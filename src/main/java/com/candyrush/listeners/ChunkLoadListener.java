package com.candyrush.listeners;

import com.candyrush.CandyRushPlugin;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

/**
 * チャンクロード時に古い宝箱をクリーンアップするリスナー
 * サーバー再起動後にチャンクがロードされた際、前回のゲームの宝箱を削除
 */
public class ChunkLoadListener implements Listener {

    private final CandyRushPlugin plugin;

    public ChunkLoadListener(CandyRushPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * チャンクロード時の処理
     */
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        World world = event.getWorld();
        int chunkX = event.getChunk().getX();
        int chunkZ = event.getChunk().getZ();

        // TreasureChestManagerに古い宝箱のクリーンアップを依頼
        plugin.getTreasureChestManager().cleanupChestOnChunkLoad(world, chunkX, chunkZ);

        // EventNpcのクリーンアップはゲーム開始時にdeleteOldNpcsFromDatabaseで一括実行
    }
}
