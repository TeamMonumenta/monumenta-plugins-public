package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

public class StarCommunion extends ZeroArgumentEffect {
	public static final String GENERIC_NAME = "StarCommunion";
	public static final String effectID = "StarCommunion";

	public static final double HEALTH_THRESHOLD = 0.9;
	public static final int ON_HIT_DURATION = 4 * 20;
	public static final double SLOW_AMOUNT = 0.15;

	private static final Particle.DustOptions DARK_COLOR = new Particle.DustOptions(Color.fromRGB(111, 0, 255), 1.0f);
	private static final Particle.DustOptions LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(154, 77, 255), 1.0f);

	public StarCommunion(int duration) {
		super(duration, effectID);
	}

	@Override
	public void onDamage(LivingEntity entity, DamageEvent event, LivingEntity enemy) {
		if (entity instanceof Player player && event.getDamage() > 0) {
			if (player.getHealth() / EntityUtils.getMaxHealth(player) >= HEALTH_THRESHOLD) {
				Plugin plugin = Plugin.getInstance();
				EntityUtils.applySlow(plugin, ON_HIT_DURATION, SLOW_AMOUNT, enemy);
				Location loc = enemy.getLocation().add(0, enemy.getHeight() / 2, 0);
				new PartialParticle(Particle.REDSTONE, loc, 2, 0.25, 0.25, 0.25, 0.1, LIGHT_COLOR).spawnAsPlayerBuff((Player) entity);
				new PartialParticle(Particle.REDSTONE, loc, 2, 0.5, 0.5, 0.5, 0, LIGHT_COLOR).spawnAsPlayerBuff((Player) entity);
				new PartialParticle(Particle.REDSTONE, loc, 2, 0.5, 0.5, 0.5, 0.1, DARK_COLOR).spawnAsPlayerBuff((Player) entity);
			}
		}
	}

	@Override
	public boolean isBuff() {
		return true;
	}

	public static StarCommunion deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();

		return new StarCommunion(duration);
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return "Star Communion";
	}

	@Override
	public String toString() {
		return String.format("StarCommunion duration:%d", this.getDuration());
	}

}
