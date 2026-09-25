package io.github.mortuusars.envelope.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.mortuusars.envelope.world.item.component.SealLock;
import net.minecraft.commands.CommandSourceStack;

public class SealLockArgument implements ArgumentType<SealLock> {
    public static SealLockArgument sealLock() {
        return new SealLockArgument();
    }

    public static SealLock getSealLock(CommandContext<CommandSourceStack> context, String name) {
        return context.getArgument(name, SealLock.class);
    }

    @Override
    public SealLock parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();

        while (reader.canRead() && !Character.isWhitespace(reader.peek())) {
            reader.skip();
        }

        String text = reader.getString().substring(start, reader.getCursor());
        return SealLock.parse(text).getOrThrow();
    }
}