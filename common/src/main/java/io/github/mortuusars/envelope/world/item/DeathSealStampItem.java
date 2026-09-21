package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.world.item.component.seal.Seal;
import io.github.mortuusars.envelope.world.item.component.seal.SealMaterial;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.EitherHolder;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class DeathSealStampItem extends SealStampItem {
    public DeathSealStampItem(Properties properties) {
        super(properties);
    }

    @Override
    public Optional<EitherHolder<SealMaterial>> getMaterial(ItemStack stack) {
        return Optional.of(new EitherHolder<>(SealMaterial.SCULK));
    }

    @Override
    public boolean canDyeWith(ItemStack stack, DyeColor color) {
        return false;
    }

    @Override
    public boolean canApplyGold(ItemStack stack, Player player) {
        return false;
    }

    @Override
    public Seal createSeal(ItemStack stack, Player player) {
        return Seal.copy(super.createSeal(stack, player), player.registryAccess())
              .lockedBy(player)
              .build();
    }

    @Override
    protected ItemStack applySealToItem(Player player, SealableItem sealable, ItemStack target, Seal seal) {
        if (player.level() instanceof ServerLevel serverLevel) {
            seal.lock().ifPresent(lock -> lock.lock(serverLevel));
        }

        return super.applySealToItem(player, sealable, target, seal);
    }
}
