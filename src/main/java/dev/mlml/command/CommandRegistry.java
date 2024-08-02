package dev.mlml.command;

import lombok.SneakyThrows;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CommandRegistry {
    private static final Logger logger = LoggerFactory.getLogger(CommandRegistry.class);

    private static final Set<Command> commands = new HashSet<>();
    private static final Map<Command, Map<String, Long>> cooldowns = new HashMap<>();

    private static long getCooldown(Command command, String userId) {
        if (!cooldowns.containsKey(command)) {
            cooldowns.put(command, new HashMap<>());
        }

        Map<String, Long> cooldownMap = cooldowns.get(command);
        if (!cooldownMap.containsKey(userId)) {
            return 0;
        }

        if (System.currentTimeMillis() - cooldownMap.get(userId) > command.getCooldown() * 1000L) {
            cooldownMap.remove(userId);
            return 0;
        }

        return command.getCooldown() * 1000L - (System.currentTimeMillis() - cooldownMap.get(userId));
    }

    private static void setCooldown(Command command, String userId) {
        if (!cooldowns.containsKey(command)) {
            cooldowns.put(command, new HashMap<>());
        }

        cooldowns.get(command).put(userId, System.currentTimeMillis());
    }

    public static void registerClass(Class<? extends Command> commandClass) {
        if (commands.stream().anyMatch(command -> command.getClass().equals(commandClass))) {
            logger.error("Command class already registered: {}", commandClass.getName());
            return;
        }

        Command instance;
        try {
            instance = commandClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            logger.error("Failed to register command class: {}, {}", commandClass.getName(), e.getMessage());
            return;
        }

        commands.add(instance);
    }

    @Nullable
    public static Command getCommandClassByKeyword(String name) {
        return commands.stream()
                       .filter(command -> Arrays.asList(command.getKeywords()).contains(name))
                       .findFirst()
                       .orElse(null);
    }

    @SneakyThrows
    public static void executeCommand(Message message) {
        if (message.getAuthor().isBot()) {
            return;
        }

        Context ctx = new Context(message);

        if (!ctx.isValidCommand) {
            return;
        }

        Command command = getCommandClassByKeyword(ctx.command);
        if (Objects.isNull(command)) {
            message.reply("Command not found!").complete();
            return;
        }

        if (!command.canExecute(ctx.member, ctx.gChannel)) {
            message.reply("You don't have permission to execute this command!").complete();
            return;
        }

        long cooldown = getCooldown(command, ctx.getMember().getId());
        if (cooldown > 0) {
            message.reply(String.format("You must wait %d seconds before using this command again", cooldown / 1000))
                   .complete();
            return;
        }

        if (ctx.parse(command)) {
            logger.debug("Failed to parse command: {}", command.getName());
            message.reply("Usage: " + command.getKeywords()[0] + " " + command.getUsage()).complete();
            return;
        }

        setCooldown(command, ctx.getMember().getId());

        logger.debug("Executing command: {}", command.getName());
        command.execute(ctx);
    }
}
