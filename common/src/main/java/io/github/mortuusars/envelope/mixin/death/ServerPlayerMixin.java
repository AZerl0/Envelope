package io.github.mortuusars.envelope.mixin.death;

import com.mojang.authlib.GameProfile;
import io.github.mortuusars.envelope.world.item.component.SealLock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player {
    public ServerPlayerMixin(Level level, BlockPos pos, float yRot, GameProfile gameProfile) {
        super(level, pos, yRot, gameProfile);
    }

    @Shadow
    public abstract ServerLevel serverLevel();

    @Inject(method = "die", at = @At("RETURN"))
    private void onDie(DamageSource damageSource, CallbackInfo ci) {
        SealLock.unlockAllFrom(getScoreboardName(), serverLevel());
    }
}
