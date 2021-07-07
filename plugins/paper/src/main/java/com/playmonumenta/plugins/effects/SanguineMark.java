package com.playmonumenta.plugins.effects;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

import com.playmonumenta.plugins.utils.PlayerUtils;

public class SanguineMark extends Effect {

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(179, 0, 0), 1.0f);
	private double mHealPercent;

	public SanguineMark(double healPercent, int duration) {
		super(duration);
		mHealPercent = healPercent;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (fourHertz) {
			World world = entity.getWorld();
			Location loc = entity.getLocation().add(0, 1, 0);
			world.spawnParticle(Particle.SMOKE_NORMAL, loc, 4, 0.25, 0.5, 0.25, 0.02);
			world.spawnParticle(Particle.CRIMSON_SPORE, loc, 4, 0.25, 0.5, 0.25, 0);
			world.spawnParticle(Particle.REDSTONE, loc, 4, 0.2, 0.2, 0.2, 0.1, COLOR);
		}
	}

	@Override
	public boolean entityKilledEvent(EntityDeathEvent event) {
		if (event.getEntity().getKiller() != null) {
			 Player player = event.getEntity().getKiller();
			 double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
			 PlayerUtils.healPlayer(player, mHealPercent * maxHealth);
			 player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SLIME_SQUISH_SMALL, 1.0f, 0.8f);
		}
		return true;
	}

	@Override
	public String toString() {
		return String.format("SanguineMark duration:%d", this.getDuration());
	}
}
