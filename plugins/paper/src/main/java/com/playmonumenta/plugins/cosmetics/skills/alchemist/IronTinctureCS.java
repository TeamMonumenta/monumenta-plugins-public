package com.playmonumenta.plugins.cosmetics.skills.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class IronTinctureCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.IRON_TINCTURE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SPLASH_POTION;
	}

	public String tinctureName() {
		return "Iron Tincture";
	}

	public void onGroundEffect(Location location, Player caster, int twoTicks) {
		new PartialParticle(Particle.SPELL, location, 3, 0, 0, 0, 0.1).spawnAsPlayerActive(caster);
	}

	public void tinctureExpireEffects(Location location, Player caster) {
	}

	public void pickupEffects(Location location, Player p) {
		location.getWorld().playSound(location, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1, 0.85f);
		new PartialParticle(Particle.BLOCK_DUST, location, 50, 0.1, 0.1, 0.1, 0.1, Material.GLASS.createBlockData()).spawnAsPlayerActive(p);
		new PartialParticle(Particle.FIREWORKS_SPARK, location, 30, 0.1, 0.1, 0.1, 0.2).spawnAsPlayerActive(p);
	}

	public void pickupEffectsForPlayer(Player player, Location tinctureLocation) {
		World world = player.getWorld();
		world.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 1.2f, 1.0f);
		new PartialParticle(Particle.FLAME, player.getLocation(), 30, 0.25, 0.1, 0.25, 0.125).spawnAsPlayerActive(player);
		new BukkitRunnable() {
			double mRotation = 0;
			double mY = 0.15;
			final double mRadius = 1.15;

			@Override
			public void run() {
				mRotation += 20;
				mY += 0.175;
				for (int i = 0; i < 3; i++) {
					double degree = mRotation + (i * 120);
					Location loc = player.getLocation().add(FastUtils.cos(degree) * mRadius, mY, FastUtils.sinDeg(degree) * mRadius);
					new PartialParticle(Particle.FLAME, loc, 1, 0.05, 0.05, 0.05, 0.05).minimumCount(0).spawnAsPlayerActive(player);
					new PartialParticle(Particle.SPELL_INSTANT, loc, 2, 0.05, 0.05, 0.05, 0).minimumCount(0).spawnAsPlayerActive(player);
				}

				if (mY >= 1.8) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

}
