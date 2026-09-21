package io.github.mortuusars.envelope.network.packet.clientbound;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.SealLock;
import io.github.mortuusars.envelope.world.level.saveddata.SealLocks;
import io.github.mortuusars.mortaar.network.packet.Packet;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record ClientboundSyncDeathLockDataPacket(List<SealLock> locks) implements Packet {
    public static final ResourceLocation ID = Envelope.resource("sync_death_lock_data");
    public static final Type<ClientboundSyncDeathLockDataPacket> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSyncDeathLockDataPacket> STREAM_CODEC = StreamCodec.composite(
          SealLock.STREAM_CODEC.apply(ByteBufCodecs.list()), ClientboundSyncDeathLockDataPacket::locks,
          ClientboundSyncDeathLockDataPacket::new
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public boolean handle(PacketFlow direction, Player player) {
        SealLocks.setClientData(player.level(), locks);
        return true;
    }
}
