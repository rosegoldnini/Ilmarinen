package dev.mlml.command.argument;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class OptionArgument extends ArgumentBase<String> {
    final String[] options;

    public OptionArgument(String name, String description, boolean isRequired, String[] options) {
        super(name, description, isRequired);

        this.options = options;
    }

    @Override
    public String parse(String input) {
        if (!List.of(options).contains(input)) {
            return null;
        }
        return input;
    }

    @Override
    public String getDescription() {
        return super.getDescription() + " - Options: " + String.join(", ", options);
    }

    public static class Builder extends ArgumentBase.Builder<OptionArgument.Builder, String, OptionArgument> {
        List<String> options = new ArrayList<>();

        public Builder(String name) {
            super(name);
        }

        public Builder addOption(String option) {
            options.add(option);
            return getThis();
        }

        public Builder addOptions(String... options) {
            for (String option : options) {
                addOption(option);
            }
            return getThis();
        }

        @Override
        public OptionArgument get() {
            return new OptionArgument(name, description, isRequired, options.toArray(new String[0]));
        }
    }
}
