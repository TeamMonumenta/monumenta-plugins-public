package com.playmonumenta.plugins.command.commands;

import com.playmonumenta.plugins.command.AbstractPlayerCommand;
import com.playmonumenta.plugins.command.CommandContext;
import com.playmonumenta.plugins.managers.potion.PotionManager;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;

public class TrackedEffect extends AbstractPlayerCommand {
    private final PotionManager mPotionManager;

    public TrackedEffect(Plugin plugin, PotionManager potionManager) {
        super(
            "trackedEffect",
            "Like /effect but use potion manager to track effect",
            plugin
        );
        this.mPotionManager = potionManager;
    }

    @Override
    protected void configure(ArgumentParser parser) {
        parser.addArgument("effect")
            .help("the effect name");
        parser.addArgument("seconds")
            .help("duration of the effect")
            .type(Integer.class);
        parser.addArgument("amplifier")
            .help("effect tier starting at 0")
            .type(Integer.class);
    }

    @Override
    protected boolean run(CommandContext context) {
        //noinspection OptionalGetWithoutIsPresent - checked before being called
        final Player player = context.getPlayer().get();
        final String effect = context.getNamespace().getString("effect");
        final Integer seconds = context.getNamespace().getInt("seconds");
        final Integer amplifier = context.getNamespace().getInt("amplifier");

        PotionEffectType type = PotionEffectType.getByName(effect);
        if (type == null) {
            sendErrorMessage(context, "Invalid PotionEffectType '" + effect + "'");
            sendErrorMessage(context, "Valid values: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/potion/PotionEffectType.html");
            return false;
        }

        if (seconds <= 0) {
            sendErrorMessage(context, "Seconds must be >= 0");
            return false;
        }

        if (amplifier < 0 || amplifier > 255) {
            sendErrorMessage(context, "Amplifier must be between 0 and 255 (inclusive)");
            return false;
        }

        /* Apply potion via potion manager */
        mPotionManager.addPotion(player, PotionID.APPLIED_POTION,
            new PotionEffect(type, seconds * 20, amplifier, true, true));
        mPotionManager.applyBestPotionEffect(player);

        sendMessage(context, "Applied " + type.toString() + ":" + Integer.toString(amplifier + 1) +
            " to player '" + player.getName() + "' for " + Integer.toString(seconds) + "s");

        return true;
    }
}
