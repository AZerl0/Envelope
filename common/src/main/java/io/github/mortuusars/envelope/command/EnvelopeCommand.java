package io.github.mortuusars.envelope.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import io.github.mortuusars.envelope.command.argument.AddressArgument;
import io.github.mortuusars.envelope.command.suggestion.AddressSuggestions;
import io.github.mortuusars.envelope.util.Colors;
import io.github.mortuusars.envelope.world.item.component.SealLock;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.type.BlockAddress;
import io.github.mortuusars.envelope.world.mail.address.type.PlayerAddress;
import io.github.mortuusars.mortaar.command.argument.GameTimeIdArgument;
import io.github.mortuusars.mortaar.util.GameTimeId;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class EnvelopeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(Commands.literal("envelope")
              .requires((stack) -> stack.hasPermission(2))
              .then(Commands.literal("send")
                    .then(Commands.argument("mail", ItemArgument.item(context))
                          .executes(c -> sendMail(c, ItemArgument.getItem(c, "mail"), Address.UNKNOWN))
                          .then(Commands.argument("sender", CompoundTagArgument.compoundTag())
                                .executes(c -> sendMail(c,
                                      ItemArgument.getItem(c, "mail"),
                                      parseAddress(c, CompoundTagArgument.getCompoundTag(c, "sender")))))))
              .then(Commands.literal("broadcast")
                    .then(Commands.argument("mail", ItemArgument.item(context))
                          .executes(c -> broadcastMail(c, ItemArgument.getItem(c, "mail"), Address.UNKNOWN))
                          .then(Commands.argument("sender", CompoundTagArgument.compoundTag())
                                .executes(c -> broadcastMail(c,
                                      ItemArgument.getItem(c, "mail"),
                                      parseAddress(c, CompoundTagArgument.getCompoundTag(c, "sender")))))))
              .then(Commands.literal("mailbox")
                    .then(Commands.literal("list")
                          .executes(EnvelopeCommand::listAllMailboxes)
                          .then(Commands.literal("default")
                                .executes(EnvelopeCommand::listDefaultMailboxes)))
                    .then(Commands.literal("position")
                          .then(Commands.argument("address", AddressArgument.block())
                                .suggests(AddressSuggestions.block())
                                .executes(c -> mailboxPosition(c, AddressArgument.getBlock(c, "address"))))))
              .then(Commands.literal("seal_locks")
                    .then(Commands.literal("create")
                          .then(Commands.argument("owner", StringArgumentType.word())
                                .executes(c -> createSealLock(c,
                                      StringArgumentType.getString(c, "owner"),
                                      Optional.empty()))
                                .then(Commands.argument("id", GameTimeIdArgument.id())
                                      .executes(c -> createSealLock(c,
                                            StringArgumentType.getString(c, "owner"),
                                            Optional.of(GameTimeIdArgument.getId(c, "id")))))))
                    .then(Commands.literal("unlock")
                          .then(Commands.argument("owner", StringArgumentType.word())
                                .executes(c -> unlockSealLock(c,
                                      StringArgumentType.getString(c, "owner"),
                                      Optional.empty()))
                                .then(Commands.argument("id", LongArgumentType.longArg(1))
                                      .executes(c -> unlockSealLock(c,
                                            StringArgumentType.getString(c, "owner"),
                                            Optional.of(GameTimeIdArgument.getId(c, "id"))))))))
              .then(EnvelopeDebugCommand.commands()));
    }

    // -- Mail

    private static int sendMail(CommandContext<CommandSourceStack> context, ItemInput item, Address sender) throws CommandSyntaxException {
        ServerLevel level = context.getSource().getLevel();
        ItemStack mail = item.createItemStack(1, false);

        Address recipient = Mail.getRecipientOrUnknown(mail);

        if (mail.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Cannot send: mail is empty."));
            return 1;
        }

        if (recipient.equals(Address.UNKNOWN)) {
            context.getSource().sendFailure(Component.literal("Cannot send: recipient is not defined."));
            return 2;
        }

        if (recipient.resolve(level.getEnvelopeMailService()).equals(Address.UNKNOWN)) {
            context.getSource().sendFailure(Component.literal("Cannot send: recipient not found."));
            return 3;
        }

        MailService.of(level).getDeliveryManager()
              .startService(Delivery.draft()
                    .deliver(mail)
                    .from(sender)
                    .to(recipient));

        Component message = Component.literal("Mail sent to ").append(recipient.format().asRecipient().toComponent());
        context.getSource().sendSuccess(() -> message, true);

        return 0;
    }

    private static int broadcastMail(CommandContext<CommandSourceStack> context, ItemInput item, Address sender) throws CommandSyntaxException {
        ServerLevel level = context.getSource().getLevel();
        MailService service = level.getEnvelopeMailService();
        ItemStack mail = item.createItemStack(1, false);

        Map<PlayerAddress, BlockAddress> defaultAddresses = service.getKnownPlayers().getDefaultAddresses();

        if (defaultAddresses.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Cannot broadcast: no recipients."));
            return 1;
        }

        int count = 0;

        for (Map.Entry<PlayerAddress, BlockAddress> entry : defaultAddresses.entrySet()) {
            ItemStack broadcastedMail = Mail.setRecipient(mail.copy(), entry.getKey());
            MailService.of(level).getDeliveryManager()
                  .startService(Delivery.draft()
                        .deliver(broadcastedMail)
                        .from(sender)
                        .to(entry.getKey()));
            count++;
        }

        Component message = Component.literal("Broadcasted mail to " + count + " recipient(s)");
        context.getSource().sendSuccess(() -> message, true);

        return 0;
    }

    private static Address parseAddress(CommandContext<CommandSourceStack> context, CompoundTag tag) {
        return Address.CODEC.parse(context.getSource().registryAccess().createSerializationContext(NbtOps.INSTANCE), tag).getOrThrow();
    }

    // -- Mailbox

    private static int listAllMailboxes(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        Set<BlockAddress> addresses = MailService.of(level).getMailboxes().getAllAddresses();

        if (!addresses.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("All mailboxes:"), true);
            for (BlockAddress address : addresses) {
                context.getSource().sendSuccess(() -> copyableAddressAndPos(address,
                      MailService.of(level).getMailboxes().getPositionOf(address)), true);
            }
        } else {
            context.getSource().sendSuccess(() ->
                  Component.literal("There are no known mailboxes."), true);
        }
        return 0;
    }

    private static int listDefaultMailboxes(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();

        Map<PlayerAddress, BlockAddress> defaultAddresses = MailService.of(level).getKnownPlayers().getDefaultAddresses();

        if (!defaultAddresses.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("Default addresses:"), true);

            defaultAddresses.forEach((playerAddress, address) -> {
                Optional<BlockPos> position = MailService.of(level).getMailboxes().getPositionOf(address);
                context.getSource().sendSuccess(() -> Component.literal(playerAddress.getString())
                      .append(" - ")
                      .append(copyableAddressAndPos(address, position)), true);
            });
        } else {
            context.getSource().sendSuccess(() ->
                  Component.literal("There are no default mailboxes."), true);
        }

        return 0;
    }

    private static int mailboxPosition(CommandContext<CommandSourceStack> context, BlockAddress address) {
        ServerLevel level = context.getSource().getLevel();
        if (!MailService.of(level).getMailboxes().exists(address)) {
            context.getSource().sendFailure(address.getComponent().append(" does not exist."));
            return 1;
        }

        MailService.of(level).getMailboxes().getPositionOf(address)
              .ifPresentOrElse(
                    pos -> context.getSource().sendSuccess(() -> copyableAddressAndPos(address, Optional.of(pos)), true),
                    () -> context.getSource().sendFailure(address.getComponent()
                          .append(" does not have a position associated with it.")));
        return 0;
    }

    private static MutableComponent copyableAddressAndPos(Address address, Optional<BlockPos> pos) {
        String name = address.getString();
        String posStr = pos.map(BlockPos::toShortString).orElse("");
        String posToCopy = posStr.replace(",", "");

        return Component.literal(name)
              .withStyle(Style.EMPTY
                    .withColor(Colors.ADDRESS_NEUTRAL)
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Copy Address")
                          .append("\n")
                          .append(Component.literal(name).withStyle(ChatFormatting.GRAY))))
                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, name)))
              .append(Component.literal("@[" + posStr + "]").withStyle(Style.EMPTY
                    .withColor(ChatFormatting.WHITE)
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Copy Position")
                          .append("\n")
                          .append(Component.literal(posToCopy).withStyle(ChatFormatting.GRAY))))
                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, posToCopy))));
    }

    // -- Seal Locks

    private static int createSealLock(CommandContext<CommandSourceStack> context, String owner, Optional<GameTimeId> id) {
        if (owner.isBlank()) {
            context.getSource().sendFailure(Component.literal("Lock owner cannot be empty."));
            return 0;
        }

        ServerLevel level = context.getSource().getLevel();
        SealLock lock = new SealLock(owner, id.orElseGet(() -> GameTimeId.create(level)));
        lock.lock(level);

        context.getSource().sendSuccess(() -> {
            String serialized = SealLock.CODEC.encodeStart(level.registryAccess().createSerializationContext(JsonOps.INSTANCE), lock)
                  .getOrThrow()
                  .toString()
                  .replace("\"", "");

            return Component.literal("Lock created: ").append(Component.literal(serialized)
                  .withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Copy")))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, serialized))));
        }, true);
        return 0;
    }

    private static int unlockSealLock(CommandContext<CommandSourceStack> context, String owner, Optional<GameTimeId> id) {
        if (owner.isBlank()) {
            context.getSource().sendFailure(Component.literal("Lock owner cannot be empty."));
            return 0;
        }

        id.ifPresentOrElse(
              existingId -> {
                  SealLock lock = new SealLock(owner, existingId);
                  if (lock.unlock(context.getSource().getLevel())) {
                      context.getSource().sendSuccess(() -> Component.literal("Unlocked lock from " + owner), true);
                  } else {
                      context.getSource().sendFailure(Component.literal(owner + " does not have any locked locks."));
                  }
              },
              () -> {
                  if (SealLock.unlockAllFrom(owner, context.getSource().getLevel())) {
                      context.getSource().sendSuccess(() -> Component.literal("Unlocked all locks from " + owner), true);
                  } else {
                      context.getSource().sendFailure(Component.literal(owner + " does not have any locked locks."));
                  }
              });

        return 0;
    }
}