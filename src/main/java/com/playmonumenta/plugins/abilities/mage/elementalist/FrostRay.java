package com.playmonumenta.plugins.abilities.mage.elementalist;

import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;

/*
 * Sneak and right-click to trigger a channeled frost ray (range: 12 blocks),
 * firing into the direction you are looking at and dealing 5 / 7 damage per
 * 0.5s, applying Slowness II to affected mobs. The direction of the ray follows
 * the direction you are facing, and it lasts up to 5 / 6 s as long as you do
 * not move. Cooldown: 35 s / 25 s
 *
 * NOTE: Particle effects need flair
 */

public class FrostRay extends Ability {
	private static final int FROST_RAY_1_COOLDOWN = 35 * 20;
	private static final int FROST_RAY_2_COOLDOWN = 25 * 20;
	private static final int FROST_RAY_1_DURATION = 5 * 20;
	private static final int FROST_RAY_2_DURATION = 6 * 20;
	private static final int FROST_RAY_RANGE = 12;
	private static final int FROST_RAY_1_DAMAGE = 5;
	private static final int FROST_RAY_2_DAMAGE = 7;
	private static final int FROST_RAY_SLOWNESS_LEVEL = 1;
	private static final int FROST_RAY_SLOWNESS_DURATION = 2 * 20;
	private static final double FROST_RAY_RADIUS = 0.65;

	public FrostRay(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.FROST_RAY;
		mInfo.scoreboardId = "FrostRay";
		mInfo.cooldown = getAbilityScore() == 1 ? FROST_RAY_1_COOLDOWN : FROST_RAY_2_COOLDOWN;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public boolean runCheck() {
		return mPlayer.isSneaking();
	}

	@Override
	public boolean cast() {
		mWorld.spawnParticle(Particle.FIREWORKS_SPARK, mPlayer.getLocation(), 30, 0.75f, 0.25f, 0.75f, 0.5f); //Rudimentary effects
		int damage = getAbilityScore() == 1 ? FROST_RAY_1_DAMAGE : FROST_RAY_2_DAMAGE;
		new BukkitRunnable() {
			int t = 0;
			Location playerLocation = mPlayer.getLocation();

			@Override
			public void run() {
				t++;
				Location location = mPlayer.getLocation();
				Vector increment = location.getDirection();
				List<Mob> mobs = EntityUtils.getNearbyMobs(location, FROST_RAY_RANGE);
				if (!playerLocation.equals(location)) {
					this.cancel();
				}
				for (int i = 0; i <= FROST_RAY_RANGE; i++) {
					location.add(increment);
					mWorld.spawnParticle(Particle.FIREWORKS_SPARK, location, 5, 0.05f, 0.05f, 0.05f, 0); //Rudimentary effects
					if (t % 10 == 0) {
						ListIterator<Mob> iter = mobs.listIterator();
						while (iter.hasNext()) {
							LivingEntity le = iter.next();
							if (le.getLocation().distance(location) < FROST_RAY_RADIUS) {
								EntityUtils.damageEntity(mPlugin, le, damage, mPlayer);
								le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, FROST_RAY_SLOWNESS_DURATION, FROST_RAY_SLOWNESS_LEVEL, true, false));
								iter.remove();
							}
						}
					}
					if (location.getBlock().getType().isSolid()) {
						location.subtract(increment.multiply(0.5));
						mWorld.spawnParticle(Particle.CLOUD, location, 30, 0, 0, 0, 0.125f);
						mWorld.playSound(location, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.65f);
						mWorld.playSound(location, Sound.ENTITY_ARROW_HIT, 1, 0.9f);
						break;
					}
				}
			}

		}.runTaskTimer(mPlugin, 1, 1);

		putOnCooldown();
		return true;
	}
}
