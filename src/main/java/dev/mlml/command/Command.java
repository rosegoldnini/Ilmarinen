package dev.mlml.command;

import dev.mlml.command.argument.ArgumentBase;
import dev.mlml.command.argument.StringArgument;
import lombok.Getter;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.internal.utils.PermissionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Getter
public abstract class Command {
    private final String[] keywords;
    private final String name;
    private final String description;
    private final EnumSet<Permission> permissions;
    private final List<ArgumentBase<?>> arguments = new ArrayList<>();

    private static final Logger logger = LoggerFactory.getLogger(Command.class);

    public Command(ArgumentBase<?>... args) {
        CommandInfo ci = this.getClass().getAnnotation(CommandInfo.class);
        if (Objects.isNull(ci)) {
            throw new IllegalStateException(this.getClass().getName() + " did not have a CommandInfo annotation!");
        }

        keywords = ci.keywords();
        name = ci.name();
        description = ci.description();
        permissions = EnumSet.noneOf(Permission.class);
        permissions.addAll(Arrays.asList(ci.permissions()));

        boolean seenRequired = args[0].isRequired(),
                seenVArgs = args[0].getClass() == StringArgument.class && ((StringArgument) args[0]).isVArgs();

        for (int i = 1; i < args.length; i++) {
            ArgumentBase<?> arg = args[i];
            if (seenVArgs) {
                throw new IllegalStateException(name + " has an argument after a variable argument!");
            }
            if (arg.isRequired() && seenRequired) {
                throw new IllegalStateException(name + " has a required argument after an optional one!");
            }
            seenRequired = arg.isRequired();
        }

        arguments.addAll(List.of(args));

        logger.debug("Created command {} with {} arguments", name, args.length);
    }

    public abstract void execute(Context ctx);

    public boolean canExecute(Member member, GuildChannel channel) {
        return PermissionUtil.checkPermission(channel.getPermissionContainer(),
                                              member,
                                              permissions.toArray(new Permission[0])
        );
    }

    public boolean canExecute(Member member) {
        return PermissionUtil.checkPermission(member,
                                              permissions.toArray(new Permission[0])
        );
    }

}