package dev.mlml.command;

import dev.mlml.Config;
import dev.mlml.command.argument.ArgumentBase;
import dev.mlml.command.argument.ParsedArgumentList;
import dev.mlml.command.argument.StringArgument;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Getter
public class Context {
    private static final Logger logger = LoggerFactory.getLogger(Context.class);

    protected final Message message;
    protected final Member member;
    protected final GuildChannel gChannel;
    protected final Channel channel;

    protected final String[] parts;
    protected final String command;
    protected final String[] args;

    protected final boolean isValidCommand;

    protected final ParsedArgumentList parsedArguments;

    public Context(Message message) {
        String prefix = Config.getServerConfig(message.getGuildId()).prefix;

        this.isValidCommand = message.getContentRaw().startsWith(prefix);

        this.message = message;
        this.member = message.getMember();
        this.gChannel = message.getGuildChannel();
        this.channel = message.getChannel();


        if (!isValidCommand) {
            this.parts = new String[0];
            this.command = "";
            this.args = new String[0];
            this.parsedArguments = new ParsedArgumentList();
            return;
        }

        this.parsedArguments = new ParsedArgumentList();

        String[] parts = split(message);

        this.parts = parts;
        this.command = parts[0].substring(prefix.length());
        this.args = new String[parts.length - 1];
        System.arraycopy(parts, 1, this.args, 0, parts.length - 1);
    }

    public static String[] split(Message message) {
        return message.getContentRaw().split(" +");
    }

    public boolean parse(Command command) {
        List<? extends ArgumentBase<?>> commandarguments = command.getArguments();

        int offset = 0;
        for (int i = 0; i < commandarguments.size(); i++) {
            ArgumentBase<?> arg = commandarguments.get(i);
            logger.debug("Parsing argument: {}, offset: {}", arg.getName(), offset);
            if (arg.getClass() == StringArgument.class && ((StringArgument) arg).isVArgs()) {
                logger.debug("Variable argument found, skipping to end");
                parsedArguments.add(arg, String.join(" ", List.of(args).subList(i + offset, args.length)));
                break;
            }
            if (arg.isRequired() && i > args.length + i + offset) {
                logger.debug("Required argument not found: {}, args.length: {}, offset: {}",
                             arg.getName(),
                             args.length,
                             offset
                );
                return true;
            }
            ParsedArgumentList.ParsedArg next = parsedArguments.add(arg, args[i + offset]);
            if (next.skip()) {
                offset--;
                logger.debug("Seeking back one argument, offset now: {}", offset);
                continue;
            }
            logger.debug("Parsed argument: {}, value: {}", arg.getName(), next.getValue());
        }

        return false;
    }

    public ParsedArgumentList.ParsedArg getArgument(String name) {
        return parsedArguments.getArguments().stream()
                              .filter(parsedArg -> parsedArg.getName().equals(name))
                              .findFirst()
                              .orElse(null);
    }
}