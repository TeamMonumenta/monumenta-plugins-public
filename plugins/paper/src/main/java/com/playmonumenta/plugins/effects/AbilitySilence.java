package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.particle.PartialParticle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class AbilitySilence extends ZeroArgumentEffect {
	public static final String GENERIC_NAME = "Silence";
	public static final String effectID = "AbilitySilence";

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1.0f);

	public AbilitySilence(int duration) {
		super(duration, effectID);
	}

	@Override
	public double getMagnitude() {
		// This is useful so that the "active" effect is always the correct duration
		return getDuration();
	}

	@Override
	public void entityGainEffect(Entity entity) {
		if (entity instanceof Player player) {
			player.sendActionBar(Component.text("You are silenced! You cannot use abilities for " + getDuration() / 20 + "s", NamedTextColor.DARK_RED));
			AbilityManager.getManager().updateSilence(player, true);
		}
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (entity instanceof Player player) {
			Bukkit.getScheduler().runTask(Plugin.getInstance(),
				() -> AbilityManager.getManager().updateSilence(player, false));
			ClientModHandler.silenced(player, 0);
		}
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (oneHertz) {
			if (entity instanceof Player) {
				entity.sendActionBar(Component.text("You are silenced! You cannot use abilities for " + getDuration() / 20 + "s", NamedTextColor.DARK_RED));
			}
		}

		if (fourHertz) {
			new PartialParticle(Particle.REDSTONE, entity.getLocation().add(0, 0.5, 0), 4, 0.2, 0.4, 0.2, 0, COLOR).spawnAsEntityBuff(entity);
		}
	}

	public static AbilitySilence deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();

		return new AbilitySilence(duration);
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return ChatColor.RED + "Silence";
	}

	@Override
	public String toString() {
		return String.format("AbilitySilence duration:%d", this.getDuration());
	}
}
