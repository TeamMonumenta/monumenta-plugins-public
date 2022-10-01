package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import java.util.EnumSet;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class CrystalineBlessing extends ZeroArgumentEffect {
	public static final String GENERIC_NAME = "CrystalineBlessing";
	public static final String effectID = "CrystalineBlessing";

	private static final int DUR = 3 * 20;
	private static final double DAMAGE_PERCENT = 0.2;
	private static final String ATTR_NAME = "CrystalineBlessing";
	private static final EnumSet<DamageType> AFFECTED_DAMAGE_TYPES = EnumSet.of(
		DamageType.MELEE,
		DamageType.MELEE_ENCH,
		DamageType.MELEE_SKILL,
		DamageType.PROJECTILE,
		DamageType.PROJECTILE_SKILL,
		DamageType.MAGIC
	);

	private static final Particle.DustOptions DARK_COLOR = new Particle.DustOptions(Color.fromRGB(131, 63, 171), 1.0f);
	private static final Particle.DustOptions LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(201, 127, 245), 1.0f);

	public CrystalineBlessing(int duration) {
		super(duration, effectID);
	}

	public static CrystalineBlessing deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();

		return new CrystalineBlessing(duration);
	}

	@Override
	public boolean isBuff() {
		return true;
	}

	@Override
	public String toString() {
		return String.format("CrystalineBlessing duration:%d", this.getDuration());
	}

	@Override
	public void onHurtByEntity(LivingEntity entity, DamageEvent event, Entity enemy) {
		if (event.getType() == DamageType.MAGIC || event.getType() == DamageType.PROJECTILE) {
			Plugin.getInstance().mEffectManager.addEffect(entity, ATTR_NAME, new PercentDamageDealt(DUR, DAMAGE_PERCENT, AFFECTED_DAMAGE_TYPES));
			Plugin.getInstance().mEffectManager.addEffect(entity, "CrystalParticles", new Aesthetics(DUR,
					(e, fourHertz, twoHertz, oneHertz) -> {
						// Tick effect
						Location loc = entity.getLocation().add(0, 1, 0);
						new PartialParticle(Particle.REDSTONE, loc, 2, 0.25, 0.25, 0.25, 0.1, LIGHT_COLOR).spawnAsPlayerBuff((Player) entity);
						new PartialParticle(Particle.REDSTONE, loc, 2, 0.5, 0.5, 0.5, 0, LIGHT_COLOR).spawnAsPlayerBuff((Player) entity);
						new PartialParticle(Particle.REDSTONE, loc, 2, 0.5, 0.5, 0.5, 0.1, DARK_COLOR).spawnAsPlayerBuff((Player) entity);
					}, (e) -> {
					// Lose effect
					Location loc = entity.getLocation();
					entity.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1f, 0.85f);
					new PartialParticle(Particle.REDSTONE, loc, 2, 0.25, 0.25, 0.25, 0.1, LIGHT_COLOR).spawnAsPlayerBuff((Player) entity);
					new PartialParticle(Particle.REDSTONE, loc, 2, 0.5, 0.5, 0.5, 0, LIGHT_COLOR).spawnAsPlayerBuff((Player) entity);
					new PartialParticle(Particle.REDSTONE, loc, 2, 0.5, 0.5, 0.5, 0.1, DARK_COLOR).spawnAsPlayerBuff((Player) entity);
				})
			);

			// Aesthetics
			entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_PLACE, 1, 0.85f);

		}
	}
}
