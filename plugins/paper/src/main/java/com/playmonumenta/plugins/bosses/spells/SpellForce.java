package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SpellForce extends SpellBaseAoE {

	public SpellForce(Plugin plugin, LivingEntity launcher, int radius, int time, int cooldown, boolean needNearPlayers) {
		super(plugin, launcher, radius, time, cooldown, true, needNearPlayers, Sound.ENTITY_IRON_GOLEM_ATTACK, 1, 1);
	}

	public SpellForce(Plugin plugin, LivingEntity launcher, int radius, int time, int cooldown) {
		this(plugin, launcher, radius, time, cooldown, true);
	}

	public SpellForce(Plugin plugin, LivingEntity launcher, int radius, int time) {
		this(plugin, launcher, radius, time, 160);
	}

	@Override
	protected void chargeAuraAction(Location loc) {
		new PartialParticle(Particle.SMOKE_LARGE, loc, 1, mRadius / 2.0, mRadius / 2.0, mRadius / 2.0, 0.05).spawnAsEntityActive(mLauncher);
	}

	@Override
	protected void chargeCircleAction(Location loc) {
		new PartialParticle(Particle.CRIT_MAGIC, loc, 1, 0.25, 0.25, 0.25, 0.1).spawnAsEntityActive(mLauncher);
	}

	@Override
	protected void outburstAction(Location loc) {
		World world = loc.getWorld();
		world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1.5f, 0.65f);
		world.playSound(loc, Sound.ENTITY_GHAST_SHOOT, SoundCategory.HOSTILE, 1f, 0.5f);
		world.playSound(loc, Sound.ENTITY_GUARDIAN_HURT, SoundCategory.HOSTILE, 1f, 0.8f);
		new PartialParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 0.5, 0), 100, 0.5, 0, 0.5, 0.8f).spawnAsEntityActive(mLauncher);
	}

	@Override
	protected void circleOutburstAction(Location loc) {
		new PartialParticle(Particle.SMOKE_LARGE, loc, 1, 0.1, 0.1, 0.1, 0.3).spawnAsEntityActive(mLauncher);
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 2, 0.25, 0.25, 0.25, 0.1).spawnAsEntityActive(mLauncher);
	}

	@Override
	protected void dealDamageAction(Location loc) {
		for (Player player : PlayerUtils.playersInRange(mLauncher.getLocation(), mRadius, true)) {

			double distance = player.getLocation().distance(loc);
			if (distance < mRadius / 3.0) {
				player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 2));
				MovementUtils.knockAway(mLauncher, player, 3.0f, false);
			} else if (distance < (mRadius * 2.0) / 3.0) {
				player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 1));
				MovementUtils.knockAway(mLauncher, player, 2.1f, false);
			} else if (distance < mRadius) {
				player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 0));
				MovementUtils.knockAway(mLauncher, player, 1.2f, false);
			}
			new PartialParticle(Particle.VILLAGER_ANGRY, player.getLocation().clone().add(0, 1, 0), 4, 0.25, 0.5, 0.25, 0).spawnAsEntityActive(mLauncher);
		}
	}

}
