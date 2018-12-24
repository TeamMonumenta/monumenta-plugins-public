package com.playmonumenta.plugins.rawcommands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.managers.potion.PotionManager;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.BooleanArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;
import io.github.jorelali.commandapi.api.arguments.IntegerArgument;
import io.github.jorelali.commandapi.api.arguments.LiteralArgument;
import io.github.jorelali.commandapi.api.arguments.PotionEffectArgument;

public class Effect {
	@SuppressWarnings("unchecked")
	public static void register(Plugin plugin) {
		CommandPermission perms = CommandPermission.fromString("minecraft.command.effect");

		/* Unregister the default /effect command */
		CommandAPI.getInstance().unregister("effect");

		/* Add effects (/effect give) */
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("give", new LiteralArgument("give"));
		arguments.put("entity", new EntitySelectorArgument(EntitySelector.MANY_ENTITIES));
		arguments.put("effect", new PotionEffectArgument());

		CommandAPI.getInstance().register("effect",
		                                  perms,
		                                  arguments,
		                                  (sender, args) -> {
		                                      giveEffect(plugin, sender, (Collection<Entity>)args[0],
		                                                 (PotionEffectType)args[1], 30, 0, false);
		                                  }
		);

		arguments.put("seconds", new IntegerArgument(1));
		CommandAPI.getInstance().register("effect",
		                                  perms,
		                                  arguments,
		                                  (sender, args) -> {
		                                      giveEffect(plugin, sender, (Collection<Entity>)args[0],
		                                                 (PotionEffectType)args[1], (Integer)args[2],
		                                                 0, false);
		                                  }
		);

		arguments.put("amplifier", new IntegerArgument(0));
		CommandAPI.getInstance().register("effect",
		                                  perms,
		                                  arguments,
		                                  (sender, args) -> {
		                                      giveEffect(plugin, sender, (Collection<Entity>)args[0],
		                                                 (PotionEffectType)args[1], (Integer)args[2],
		                                                 (Integer)args[3], false);
		                                  }
		);

		arguments.put("hideParticles", new BooleanArgument());
		CommandAPI.getInstance().register("effect",
		                                  perms,
		                                  arguments,
		                                  (sender, args) -> {
		                                      giveEffect(plugin, sender, (Collection<Entity>)args[0],
		                                                 (PotionEffectType)args[1], (Integer)args[2],
		                                                 (Integer)args[3], (Boolean)args[4]);
		                                  }
		);

		/* Clear effects (/effect clear) */
		arguments = new LinkedHashMap<>();

		arguments.put("clear", new LiteralArgument("clear"));
		arguments.put("entity", new EntitySelectorArgument(EntitySelector.MANY_ENTITIES));

		CommandAPI.getInstance().register("effect",
		                                  perms,
		                                  arguments,
		                                  (sender, args) -> {
		                                      clearEffect(plugin, sender, (Collection<Entity>)args[0],
		                                                  null);
		                                  }
		);

		arguments.put("effect", new PotionEffectArgument());
		CommandAPI.getInstance().register("effect",
		                                  perms,
		                                  arguments,
		                                  (sender, args) -> {
		                                      clearEffect(plugin, sender, (Collection<Entity>)args[0],
		                                                 (PotionEffectType)args[1]);
		                                  }
		);
	}

	private static void giveEffect(Plugin plugin, CommandSender sender, Collection<Entity>entities,
	                               PotionEffectType type, int seconds, int amplifier, boolean hideParticles) {
		PotionManager manager = plugin.mPotionManager;

		for (Entity e : entities) {
			if (e instanceof Player && manager != null) {
				// This is a player - use the potion manager
				Player player = (Player)e;

				/* Apply potion via potion manager */
				manager.addPotion(player, PotionID.APPLIED_POTION,
				                  new PotionEffect(type, seconds * 20, amplifier, true, true));
			} else if (e instanceof LivingEntity) {
				LivingEntity entity = (LivingEntity)e;
				// Not a player - apply the effect directly
				entity.addPotionEffect(new PotionEffect(type, seconds * 20, amplifier, false, !hideParticles));
			}
		}
		sender.sendMessage("Applied " + type.toString() + ":" + Integer.toString(amplifier + 1) +
		                   " to entities for " + Integer.toString(seconds) + "s");
	}

	private static void clearEffect(Plugin plugin, CommandSender sender, Collection<Entity>entities,
	                                PotionEffectType type) {
		PotionManager manager = plugin.mPotionManager;

		for (Entity e : entities) {
			if (e instanceof Player && manager != null) {
				// This is a player - use the potion manager
				Player player = (Player)e;

				if (type == null) {
					// Clear all effects
					manager.clearAllPotions(player);
				} else {
					// Clear one effect
					manager.clearPotionEffectType(player, type);
				}
			} else if (e instanceof LivingEntity) {
				LivingEntity entity = (LivingEntity)e;
				// Not a player - clear the effect directly
				if (type == null) {
					// Clear all effects
					// Copy the list to prevent ConcurrentModificationException's
					for (PotionEffect effect : new ArrayList<PotionEffect>(entity.getActivePotionEffects())) {
						entity.removePotionEffect(effect.getType());
					}
				} else {
					// Clear one effect
					entity.removePotionEffect(type);
				}
			}
		}

		if (type == null) {
			sender.sendMessage("Cleared all effects from entities");
		} else {
			sender.sendMessage("Cleared " + type.toString() + " effect from entities");
		}
	}
}
