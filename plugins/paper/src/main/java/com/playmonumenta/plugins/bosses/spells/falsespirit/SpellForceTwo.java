package com.playmonumenta.plugins.bosses.spells.falsespirit;

import com.playmonumenta.plugins.bosses.bosses.FalseSpirit;
import com.playmonumenta.plugins.bosses.spells.SpellBaseAoE;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SpellForceTwo extends SpellBaseAoE {

	public SpellForceTwo(Plugin plugin, LivingEntity launcher, int radius, int time) {
		super(plugin, launcher, radius, time, 0, true, Sound.ENTITY_IRON_GOLEM_ATTACK);
	}

	@Override
	protected void chargeAuraAction(Location loc) {
		World world = loc.getWorld();
		world.spawnParticle(Particle.SMOKE_LARGE, loc, 1, mRadius / 2.0, mRadius / 2.0, mRadius / 2.0, 0.05);
	}

	@Override
	protected void chargeCircleAction(Location loc) {
		World world = loc.getWorld();
		world.spawnParticle(Particle.CRIT_MAGIC, loc, 1, 0.25, 0.25, 0.25, 0.1);
	}

	@Override
	protected void outburstAction(Location loc) {
		World world = loc.getWorld();
		world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.65f);
		world.playSound(loc, Sound.ENTITY_GHAST_SHOOT, 1f, 0.5f);
		world.playSound(loc, Sound.ENTITY_GUARDIAN_HURT, 1f, 0.8f);
		world.spawnParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 0.5, 0), 100, 0.5, 0, 0.5, 0.8f);
	}

	@Override
	protected void circleOutburstAction(Location loc) {
		World world = loc.getWorld();
		world.spawnParticle(Particle.SMOKE_LARGE, loc, 1, 0.1, 0.1, 0.1, 0.3);
		world.spawnParticle(Particle.SMOKE_NORMAL, loc, 2, 0.25, 0.25, 0.25, 0.1);
	}

	@Override
	protected void dealDamageAction(Location loc) {
		for (Player player : PlayerUtils.playersInRange(mLauncher.getLocation(), mRadius, true)) {

			double distance = player.getLocation().distance(loc);
			if (distance < mRadius) {
				player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 5, 8));
				player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 20 * 5, -4));
				MovementUtils.knockAway(mLauncher, player, 1.2f, false);
			}
			player.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, player.getLocation().clone().add(0, 1, 0), 4, 0.25, 0.5, 0.25, 0);
		}
	}

	@Override
	public int cooldownTicks() {
		return 3 * 20;
	}

	@Override
	public boolean canRun() {
		for (Player player : PlayerUtils.playersInRange(mLauncher.getLocation(), FalseSpirit.detectionRange, true)) {
			if (mLauncher.getLocation().distance(player.getLocation()) < FalseSpirit.meleeRange) {
				return true;
			}
		}
		return false;
	}
}
