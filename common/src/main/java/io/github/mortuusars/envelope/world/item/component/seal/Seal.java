package io.github.mortuusars.envelope.world.item.component.seal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.item.component.SealLock;
import io.github.mortuusars.mortaar.resources.Resource;
import io.github.mortuusars.mortaar.util.GameTimeId;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record Seal(Holder<SealMaterial> material, Holder<SealSymbol> impression, Component signature,
                   Optional<UUID> playerUuid, Optional<SealLock> lock) implements TooltipComponent {
    public static final Codec<Seal> CODEC = RecordCodecBuilder.create(i -> i.group(
          SealMaterial.CODEC.fieldOf("material").forGetter(Seal::material),
          SealSymbol.CODEC.fieldOf("impression").forGetter(Seal::impression),
          ComponentSerialization.CODEC.optionalFieldOf("signature", CommonComponents.EMPTY).forGetter(Seal::signature),
          UUIDUtil.LENIENT_CODEC.optionalFieldOf("player_id").forGetter(Seal::playerUuid),
          SealLock.CODEC.optionalFieldOf("lock").forGetter(Seal::lock)
    ).apply(i, Seal::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Seal> STREAM_CODEC = StreamCodec.composite(
          SealMaterial.STREAM_CODEC, Seal::material,
          SealSymbol.STREAM_CODEC, Seal::impression,
          ComponentSerialization.STREAM_CODEC, Seal::signature,
          ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), Seal::playerUuid,
          ByteBufCodecs.optional(SealLock.STREAM_CODEC), Seal::lock,
          Seal::new
    );

    public String getSignatureAsId() {
        return signature.getString();
    }

    // --

    public static Builder create(HolderLookup.Provider registries) {
        return new Builder(registries);
    }

    public static Builder copy(Seal seal,HolderLookup.Provider registries) {
        return new Builder(registries)
              .material(seal.material())
              .impression(seal.impression())
              .signature(seal.signature())
              .playerUuid(seal.playerUuid().orElse(null))
              .setLock(seal.lock().orElse(null));
    }

    public static class Builder {
        protected @Nullable Holder<SealMaterial> material;
        protected @Nullable Holder<SealSymbol> impression;
        protected @Nullable Component signature;
        protected @Nullable UUID playerUuid;
        protected @Nullable SealLock lock;

        protected HolderLookup.Provider registries;

        public Builder(HolderLookup.Provider registries) {
            this.registries = registries;
        }

        public Builder material(@Nullable Holder<SealMaterial> material) {
            this.material = material;
            return this;
        }

        public Builder material(@Nullable ResourceKey<SealMaterial> material) {
            this.material = material != null ? Resource.getOrThrow(material, registries) : null;
            return this;
        }

        public Builder impression(@Nullable Holder<SealSymbol> impression) {
            this.impression = impression;
            return this;
        }

        public Builder impression(@Nullable ResourceKey<SealSymbol> impression) {
            this.impression = impression != null ? Resource.getOrThrow(impression, registries) : null;
            return this;
        }

        public Builder signature(@Nullable Component signature) {
            this.signature = signature;
            return this;
        }

        public Builder playerUuid(@Nullable UUID playerUuid) {
            this.playerUuid = playerUuid;
            return this;
        }

        public Builder owner(Player player) {
            this.signature = Component.literal(player.getScoreboardName());
            this.playerUuid = player.getUUID();
            return this;
        }

        public Builder setLock(@Nullable SealLock lock) {
            this.lock = lock;
            return this;
        }

        public Builder lockedBy(Player player) {
            this.lock = new SealLock(player.getScoreboardName(), GameTimeId.create(player.level()));
            return this;
        }

        public Seal build() {
            return new Seal(
                  Objects.requireNonNullElseGet(material, () -> Resource.getOrThrow(SealMaterial.WAX, registries)),
                  Objects.requireNonNullElseGet(impression, () -> Resource.getOrThrow(SealSymbol.DEFAULT, registries)),
                  Objects.requireNonNullElse(signature, CommonComponents.EMPTY),
                  Optional.ofNullable(playerUuid),
                  Optional.ofNullable(lock)
            );
        }
    }
}