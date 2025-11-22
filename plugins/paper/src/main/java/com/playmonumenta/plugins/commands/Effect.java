package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.PotionUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.PotionEffectArgument;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public class Effect {
	private static final String COMMAND = "effect";

	@SuppressWarnings("unchecked")
	public static void register(Plugin plugin) {
		CommandPermission perms = CommandPermission.fromString("minecraft.command.effect");

		/* Unregister the default /effect command */
		CommandAPI.unregister("effect");

		/* Add effects (/effect give) */
		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new LiteralArgument("give"));
		arguments.add(new EntitySelectorArgument.ManyEntities("entity"));
		arguments.add(new PotionEffectArgument("effect"));

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.withOptionalArguments(
				new IntegerArgument("seconds", 1),
				new IntegerArgument("amplifier", 0),
				new BooleanArgument("hideParticles")
			)
			.executes((sender, args) -> {
				giveEffect(plugin, sender, (Collection<Entity>) args.get("entity"), args.getUnchecked("effect"), args.getOrDefaultUnchecked("seconds", 30), args.getOrDefaultUnchecked("amplifier", 0), args.getOrDefaultUnchecked("hideParticles", false));
			}).register();

		arguments.add(new LiteralArgument("infinite"));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.withOptionalArguments(
				new IntegerArgument("amplifier", 0),
				new BooleanArgument("hideParticles")
			)
			.executes((sender, args) -> {
				giveEffect(plugin, sender, (Collection<Entity>) args.get("entity"), args.getUnchecked("effect"), -1, args.getOrDefaultUnchecked("amplifier", 0), args.getOrDefaultUnchecked("hideParticles", false));
			}).register();


		/* Clear effects (/effect clear) */
		arguments.clear();
		arguments.add(new LiteralArgument("clear"));
		arguments.add(new EntitySelectorArgument.ManyEntities("entity"));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.withOptionalArguments(new PotionEffectArgument("effect"))
			.executes((sender, args) -> {
				clearEffect(plugin, sender, (Collection<Entity>) args.get("entity"), args.getUnchecked("effect"));
			})
			.register();
	}

	private static void giveEffect(Plugin plugin, CommandSender sender, Collection<Entity> entities,
	                               PotionEffectType type, int seconds, int amplifier, boolean hideParticles) {
		PotionManager manager = plugin.mPotionManager;
		World world = null;

		int durationTicks = seconds * 20;
		durationTicks = PotionUtils.isInfinite(seconds) ? PotionEffect.INFINITE_DURATION : durationTicks;
		for (Entity e : entities) {
			world = e.getWorld();
			if (e instanceof Player player && manager != null) {
				// This is a player - use the potion manager

				/* Apply potion via potion manager */
				manager.addPotion(player, PotionID.APPLIED_POTION,
					new PotionEffect(type, durationTicks, amplifier, true, !hideParticles));
			} else if (e instanceof LivingEntity entity) {
				// Not a player - apply the effect directly
				entity.addPotionEffect(new PotionEffect(type, durationTicks, amplifier, false, !hideParticles));
			}
		}
		if (world != null && Boolean.TRUE.equals(world.getGameRuleValue(GameRule.SEND_COMMAND_FEEDBACK))) {
			sender.sendMessage("Applied " + type + ":" + (amplifier + 1) +
				" to entities " + (seconds != PotionEffect.INFINITE_DURATION ? "for " + seconds + "s." : "infinitely."));
		}
	}

	private static void clearEffect(Plugin plugin, CommandSender sender, Collection<Entity> entities,
	                                @Nullable PotionEffectType type) {
		PotionManager manager = plugin.mPotionManager;

		World world = null;
		for (Entity e : entities) {
			world = e.getWorld();
			if (e instanceof Player player && manager != null) {
				// This is a player - use the potion manager

				if (type == null) {
					// Clear all effects
					manager.clearAllPotions(player);
				} else {
					// Clear one effect
					manager.clearPotionEffectType(player, type);
				}
			} else if (e instanceof LivingEntity entity) {
				// Not a player - clear the effect directly
				if (type == null) {
					// Clear all effects
					// Copy the list to prevent ConcurrentModificationException's
					for (PotionEffect effect : new ArrayList<>(entity.getActivePotionEffects())) {
						entity.removePotionEffect(effect.getType());
					}
				} else {
					// Clear one effect
					entity.removePotionEffect(type);
				}
			}
		}

		if (world != null && Boolean.TRUE.equals(world.getGameRuleValue(GameRule.SEND_COMMAND_FEEDBACK))) {
			if (type == null) {
				sender.sendMessage("Cleared all effects from entities");
			} else {
				sender.sendMessage("Cleared " + type + " effect from entities");
			}
		}
	}
}
