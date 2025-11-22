package com.playmonumenta.plugins.cosmetics.skills.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;


public class TouchofRadianceCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.TOUCH_OF_RADIANCE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.GLOW_BERRIES;
	}

	private static final Color ORANGE_1 = Color.fromRGB(255, 195, 60);
	private static final Color ORANGE_2 = Color.fromRGB(255, 160, 30);

	public void tickEffect(Player player) {
		new PartialParticle(Particle.CRIT, LocationUtils.getEntityCenter(player), 5).delta(0.7).spawnAsPlayerBuff(player);
		new PartialParticle(Particle.TRIAL_SPAWNER_DETECTION, LocationUtils.getEntityCenter(player), 3).delta(1).spawnAsPlayerBuff(player);
	}

	public void loseEffect(Player player) {
		player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CONDUIT_DEACTIVATE, SoundCategory.PLAYERS, 0.9f, 0.9f);
	}

	public void castOnPlayer(Player player, Player target) {
		player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 0.9f, 1.2f);
		target.getWorld().playSound(target.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.3f, 1.2f);

		new PartialParticle(Particle.WAX_ON, LocationUtils.getHalfHeightLocation(target)).count(12).delta(0.3).extra(6).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FLAME, LocationUtils.getHalfHeightLocation(target)).count(6).delta(0.3).extra(0.1).spawnAsPlayerActive(player);

		createLink(player, player, target);
		createLink(player, target, player);
	}

	public void castOnHeretic(Player player, LivingEntity target) {
		player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 1.1f, 1.2f);
		target.getWorld().playSound(target.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 1.4f, 1.5f);

		new PartialParticle(Particle.EXPLOSION_LARGE, LocationUtils.getHalfHeightLocation(target)).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FLASH, LocationUtils.getHalfHeightLocation(target)).spawnAsPlayerActive(player);
		new PartialParticle(Particle.WAX_OFF, LocationUtils.getHalfHeightLocation(target)).count(12).delta(0.3).extra(6).spawnAsPlayerActive(player);
		new PartialParticle(Particle.END_ROD, LocationUtils.getHalfHeightLocation(target)).count(6).delta(0.3).extra(0.2).spawnAsPlayerActive(player);

		createLink(player, player, target);
		createLink(player, target, player);
	}

	public void applyWeakness(Player player, LivingEntity target, double radius) {
		new PPCircle(Particle.TRIAL_SPAWNER_DETECTION, target.getLocation(), radius).countPerMeter(2).ringMode(true).spawnAsPlayerActive(player);
	}

	private void createLink(Player caster, LivingEntity source, LivingEntity target) {
		Vector dir = VectorUtils.randomUnitVector().add(LocationUtils.getDirectionTo(target.getLocation(), source.getLocation())).normalize().multiply(1.2);
		if (dir.getY() < 0) {
			dir.setY(0.25 * dir.getY());
		}
		new BukkitRunnable() {
			final Location mL = source.getEyeLocation().add(dir);
			int mT = 0;
			double mArcCurve = 0;
			Vector mD = dir.clone();

			@Override
			public void run() {
				mT++;

				Location to = target.getEyeLocation();

				for (int i = 0; i < 6; i++) {
					if (mT <= 2) {
						mD = dir.clone();
					} else {
						mArcCurve += 0.105;
						mD = dir.clone().add(LocationUtils.getDirectionTo(to, mL).multiply(mArcCurve));
					}

					if (mD.length() > 0.3) {
						mD.normalize().multiply(0.3);
					}

					mL.add(mD);

					for (int j = 0; j < 1; j++) {
						Color c = FastUtils.RANDOM.nextBoolean() ? ORANGE_1 : ORANGE_2;
						double red = c.getRed() / 255.0;
						double green = c.getGreen() / 255.0;
						double blue = c.getBlue() / 255.0;
						new PartialParticle(Particle.SPELL_MOB, mL.clone(), 1, red, green, blue, 1).directionalMode(true).spawnAsPlayerActive(caster);
					}
					if (i % 2 == 0) {
						new PartialParticle(Particle.REDSTONE, mL, 1).data(new Particle.DustOptions(FastUtils.RANDOM.nextBoolean() ? ORANGE_1 : ORANGE_2, 1.2f)).spawnAsPlayerActive(caster);
					}

					if (mT > 5 && mL.distance(to) < 0.5) {
						this.cancel();
						return;
					}
				}

				if (mT >= 100) {
					this.cancel();
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
