package io.github.mortuusars.envelope.event;

import io.github.mortuusars.envelope.world.level.saveddata.SealLocks;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameRules;

public class ServerEvents {
    public static void serverStarted(MinecraftServer server) {
    }

    public static void serverTick(MinecraftServer server) {
    }

    public static void playerLogin(ServerPlayer player) {
        player.serverLevel().getEnvelopeMailService().getKnownPlayers().add(player);
        SealLocks.get(player.level()).syncToClient(player);
    }

    public static void playerCopy(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
        if (!alive && !oldPlayer.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            transferKeptItems(oldPlayer, newPlayer);
        }
    }

    private static void transferKeptItems(ServerPlayer oldPlayer, ServerPlayer newPlayer) {
        for (int i = 0; i < oldPlayer.getInventory().getContainerSize(); i++) {
            ItemStack item = oldPlayer.getInventory().getItem(i);

            if (item.isEmpty()) {
                continue;
            }

            CustomData data = item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            if (data.contains("envelope_keep_in_inventory_on_transfer")) {
                data = data.update(tag -> tag.remove("envelope_keep_in_inventory_on_transfer"));
                if (data.isEmpty()) {
                    item.remove(DataComponents.CUSTOM_DATA);
                } else {
                    item.set(DataComponents.CUSTOM_DATA, data);
                }
                newPlayer.getInventory().setItem(i, item);
            }
        }
    }
}
