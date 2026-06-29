package net.methadrenaline.smpcreative.madialogs.dialog;

import org.bukkit.configuration.ConfigurationSection;

public record DialogSettings(int soundDelayTicks, String author) {
    public static DialogSettings defaults() {
        return new DialogSettings(4, "SMP&Creative");
    }

    public static DialogSettings from(ConfigurationSection section) {
        DialogSettings defaults = defaults();
        if (section == null) {
            return defaults;
        }

        return new DialogSettings(
                section.getInt("sound-delay-ticks", defaults.soundDelayTicks()),
                section.getString("author", defaults.author())
        );
    }
}
