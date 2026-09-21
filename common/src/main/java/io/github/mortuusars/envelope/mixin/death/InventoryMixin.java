package io.github.mortuusars.envelope.mixin.death;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.seal.Seal;
import io.github.mortuusars.envelope.world.level.saveddata.SealLocks;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(Inventory.class)
public abstract class InventoryMixin {
    @Shadow
    @Final
    public Player player;

    @Unique
    private @Nullable ItemStack envelope$keptItem = null;

    @WrapOperation(method = "dropAll", at = @At(value = "INVOKE",
          target = "Lnet/minecraft/world/entity/player/Player;drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;"))
    public @Nullable ItemEntity onDropAllDrop(Player instance, ItemStack droppedItem, boolean dropAround, boolean includeThrowerName, Operation<ItemEntity> original) {
        if ((instance.isDeadOrDying() || instance.dead)
              && droppedItem.get(Envelope.DataComponents.SEAL) instanceof Seal seal
              && seal.lock().map(lock -> lock.isOwnedBy(instance) && lock.isLocked(instance.level())).orElse(false)) {
            Vec3 pos = instance.position();
            instance.level().playSound(null, pos.x, pos.y, pos.z, SoundEvents.SCULK_BLOCK_BREAK, SoundSource.PLAYERS,
                  1f, instance.getRandom().nextFloat() * 0.3f + 0.85f);
            if (instance.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SCULK_SOUL, pos.x, pos.y, pos.z, 5, 0.3, 0.3, 0.3, 0);
            }

            CustomData customData = droppedItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                  .update(tag -> tag.putBoolean("envelope_keep_in_inventory_on_transfer", true));
            droppedItem.set(DataComponents.CUSTOM_DATA, customData);

            envelope$keptItem = droppedItem;
            return null;
        }
        return original.call(instance, droppedItem, dropAround, includeThrowerName);
    }

    @SuppressWarnings("unchecked")
    @WrapOperation(method = "dropAll", at = @At(value = "INVOKE", target = "Ljava/util/List;set(ILjava/lang/Object;)Ljava/lang/Object;"))
    private <E> E onDropAllSet(List<E> instance, int i, E e, Operation<E> original) {
        if (envelope$keptItem != null) {
            e = (E) envelope$keptItem;
            envelope$keptItem = null;
        }

        return original.call(instance, i, e);
    }
}
