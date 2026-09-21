package io.github.mortuusars.envelope.world.item.component;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.github.mortuusars.envelope.world.level.saveddata.SealLocks;
import io.github.mortuusars.mortaar.util.GameTimeId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Defines a lock that can be locked or unlocked, depending on its presence in {@link SealLocks}.
 */
public record SealLock(String owner, GameTimeId id) {
//    public static final Codec<SealLock> CODEC = RecordCodecBuilder.create(i -> i.group(
//          Codec.STRING.fieldOf("owner").forGetter(SealLock::owner),
//          GameTimeId.CODEC.fieldOf("id").forGetter(SealLock::id)
//    ).apply(i, SealLock::new));

    public static final Codec<SealLock> CODEC = Codec.STRING.comapFlatMap(SealLock::parse, SealLock::toString);

    public static final StreamCodec<RegistryFriendlyByteBuf, SealLock> STREAM_CODEC = StreamCodec.composite(
          ByteBufCodecs.STRING_UTF8, SealLock::owner,
          GameTimeId.STREAM_CODEC, SealLock::id,
          SealLock::new
    );

    public static DataResult<SealLock> parse(String string) {
        if (StringUtil.isNullOrEmpty(string)) {
            return DataResult.error(() -> "SealLock cannot be parsed from null or empty string.");
        }

        int hash = string.indexOf('#');
        if (hash == -1) {
            return DataResult.error(() -> "Incorrect format for SealLock string. Expected format: 'owner#1D' Got: " + string);
        }

        try {
            String owner = string.substring(0, hash);
            GameTimeId id = GameTimeId.parseFromHex(string.substring(hash + 1)).getOrThrow();
            return DataResult.success(new SealLock(owner, id));
        } catch (Exception e) {
            return DataResult.error(() -> "Failed to parse SealLock: " + e.getMessage());
        }
    }

    @Override
    public @NotNull String toString() {
        return owner + "#" + id;
    }

    // --

    public boolean isOwnedBy(Player player) {
        return owner.equals(player.getScoreboardName());
    }

    public boolean isLocked(Level level) {
        return SealLocks.get(level).isLocked(this);
    }

    public boolean isLockedFor(Player player) {
        return !isOwnedBy(player) && isLocked(player.level());
    }

    public boolean lock(Level level) {
        return SealLocks.get(level).lock(this);
    }

    public boolean unlock(Level level) {
        return SealLocks.get(level).unlock(this);
    }

    public static boolean unlockAllFrom(String owner, Level level) {
        return SealLocks.get(level).unlockAllFrom(owner);
    }
}
