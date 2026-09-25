package io.github.mortuusars.envelope.world.item;

import io.github.mortuusars.envelope.Config;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.inventory.tooltip.SealDieTooltip;
import io.github.mortuusars.envelope.world.item.component.seal.*;
import io.github.mortuusars.mortaar.Platform;
import io.github.mortuusars.mortaar.client.Minecrft;
import io.github.mortuusars.mortaar.resources.Resource;
import io.github.mortuusars.mortaar.util.supporter.Supporters;
import io.github.mortuusars.mortaar.world.item.ApplicatorItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class SealStampItem extends Item implements ApplicatorItem {
    public SealStampItem(Properties properties) {
        super(properties);
    }

    // -- Material

    public Optional<EitherHolder<SealMaterial>> getMaterial(ItemStack stack) {
        return Optional.ofNullable(stack.get(Envelope.DataComponents.SEAL_STAMP_MATERIAL));
    }

    public Holder<SealMaterial> getMaterialOrDefault(ItemStack stack, HolderLookup.Provider registries) {
        return getMaterial(stack)
              .flatMap(eitherHolder -> eitherHolder.unwrap(registries))
              .orElseGet(() -> Resource.getOrThrow(SealMaterial.WAX, registries));
    }

    public boolean canDyeWith(ItemStack stack, DyeColor color) {
        if (!stack.has(Envelope.DataComponents.SEAL_STAMP_MATERIAL)) {
            return true;
        }

        return getMaterial(stack)
              .map(e -> !e.key().equals(SealMaterial.fromDyeColor(color)))
              .orElse(false);
    }

    public boolean canApplyGold(ItemStack stack, Player player) {
        return Supporters.isEligibleForGoldenRewards(player.getUUID());
    }

    // -- Die

    public Optional<EitherHolder<SealSymbol>> getDie(ItemStack stack) {
        return Optional.ofNullable(stack.get(Envelope.DataComponents.SEAL_STAMP_DIE));
    }

    public Holder<SealSymbol> getDieOrDefault(ItemStack stack, HolderLookup.Provider registries, @Nullable Player player) {
        return getDie(stack)
              .flatMap(eitherHolder -> eitherHolder.unwrap(registries))
              .orElseGet(() -> Resource.getOrThrow(SealSymbol.firstCharOrDefault(player), registries));
    }

    // --

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> components, TooltipFlag flag) {
        HolderLookup.Provider registries = context.registries();
        if (flag.isAdvanced() && registries != null) {
            getMaterial(stack)
                  .flatMap(t -> t.unwrap(registries))
                  .flatMap(Holder::unwrapKey)
                  .ifPresent(key -> {
                      components.add(Component.literal("Material: ").withStyle(ChatFormatting.DARK_GRAY)
                            .append(Component.literal(key.location().toString()).withStyle(ChatFormatting.GRAY)));
                  });

            getDie(stack)
                  .flatMap(t -> t.unwrap(registries))
                  .flatMap(Holder::unwrapKey)
                  .ifPresent(key -> {
                      components.add(Component.literal("Die: ").withStyle(ChatFormatting.DARK_GRAY)
                            .append(Component.literal(key.location().toString()).withStyle(ChatFormatting.GRAY)));
                  });
        }
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        if (stack.has(Envelope.DataComponents.SEAL_STAMP_DIE)
              || !Platform.isClient()
              || !Config.Client.HIDE_DEFAULT_SEAL_STAMP_DIE_TOOLTIP_OUTSIDE_OF_INVENTORY.get()
              || Client.isInInventory(stack)) {
            return Optional.of(new SealDieTooltip(getDie(stack)));
        }

        return Optional.empty();
    }

    @Override
    public boolean shouldRenderSlotTooltipWhileCarrying(Player player, AbstractContainerMenu menu, Slot slot, ItemStack carried) {
        if (!slot.allowModification(player)) {
            return false;
        }

        return slot.getItem().has(Envelope.DataComponents.SEAL)
              || (slot.getItem().getItem() instanceof SealableItem sealable && sealable.canSeal(player.level(), slot.getItem()));
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) {
            return false;
        }

        if (!slot.allowModification(player)) {
            player.playSound(SoundEvents.COMPARATOR_CLICK);
            return true;
        }

        ItemStack target = slot.getItem();

        @Nullable Seal existingSeal = target.get(Envelope.DataComponents.SEAL);
        if (existingSeal != null
              && existingSeal.getSignatureAsId().equals(player.getScoreboardName())
              && canApplyGold(stack, player)
              && (existingSeal.lock().isEmpty() || !existingSeal.lock().get().isLocked(player.level()))) {
            Seal newSeal = Seal.copy(existingSeal, player.registryAccess())
                  .material(existingSeal.material().is(SealMaterial.GOLD)
                        ? getMaterialOrDefault(stack, player.registryAccess())
                        : Resource.getOrThrow(SealMaterial.GOLD, player.registryAccess()))
                  .owner(player)
                  .build();

            target.set(Envelope.DataComponents.SEAL, newSeal);
            slot.set(target);
            player.playSound(Envelope.SoundEvents.SEAL_STAMP.get(), 1f, player.getRandom().nextFloat() * 0.4f + 0.80f);
            return true;
        }

        if (!(target.getItem() instanceof SealableItem sealable) || !sealable.canSeal(player.level(), target)) {
            player.playSound(SoundEvents.COMPARATOR_CLICK);
            return true;
        }

        Seal seal = createSeal(stack, player);
        ItemStack sealResult = applySealToItem(stack, player, sealable, target, seal);
        slot.set(sealResult);
        onSealApplied(stack, player, sealable, target, seal);

        return true;
    }

    // -- Seal

    public Seal createSeal(ItemStack stack, Player player) {
        return Seal.create(player.registryAccess())
              .material(getMaterialOrDefault(stack, player.registryAccess()))
              .impression(getDieOrDefault(stack, player.registryAccess(), player))
              .owner(player)
              .build();
    }

    protected ItemStack applySealToItem(ItemStack stack, Player player, SealableItem sealable, ItemStack target, Seal seal) {
        return sealable.seal(player.level(), target, seal);
    }

    protected void onSealApplied(ItemStack stack, Player player, SealableItem sealable, ItemStack target, Seal seal) {
        player.playSound(Envelope.SoundEvents.SEAL_STAMP.get(), 0.75f, player.getRandom().nextFloat() * 0.4f + 0.80f);
        player.awardStat(Envelope.Stats.SEALS_APPLIED.get());
    }

    // --

    public static class Client {
        public static boolean isInInventory(ItemStack stack) {
            if (Minecrft.player().containerMenu instanceof AbstractContainerMenu menu) {
                for (Slot slot : menu.slots) {
                    if (slot.getItem() == stack) {
                        return true;
                    }
                }
                return false;
            }

            for (ItemStack item : Minecrft.player().getInventory().items) {
                if (item == stack) {
                    return true;
                }
            }

            return false;
        }
    }
}