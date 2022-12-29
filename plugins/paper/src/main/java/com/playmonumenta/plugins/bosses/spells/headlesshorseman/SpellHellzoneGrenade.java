package com.playmonumenta.plugins.bosses.spells.headlesshorseman;

import com.playmonumenta.plugins.bosses.bosses.HeadlessHorsemanBoss;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/*
 * Hellzone Grenade - The horseman fires a pumpkin (Fireball with pumpkin block maybe) that
explodes on contact with the ground or a player, dealing NM20/HM35 damage in a 3 block radius.
Enemies hit are ignited for 3 seconds. The area affected leaves behind a lingering AoE if it hits
the ground, this lingering fire is also 3 blocks in radius and deals 5% max health damage every 0.5
seconds to players and ignites them for 3 seconds. (Does not leave the lingering thing behind if
it hits a player or the horseman)

Falling Block Projectile
 */

public class SpellHellzoneGrenade extends Spell {
	private static final int RADIUS = 3;

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private double mRange;
	private Location mCenter;
	private int mCooldownTicks;
	private int mCount;

	public SpellHellzoneGrenade(Plugin plugin, LivingEntity entity, Location center, double range, int cooldown, int count) {
		mPlugin = plugin;
		mBoss = entity;
		mRange = range;
		mCenter = center;
		mCooldownTicks = cooldown;
		mCount = count;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		// Choose random player within range that has line of sight to boss
		List<Player> players = PlayerUtils.playersInRange(mCenter, mRange, false);
		players.removeIf(player -> player.getLocation().distance(mCenter) <= 5);


		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mTicks++;

				new PartialParticle(Particle.FLAME, mBoss.getLocation(), 40, 0, 0, 0, 0.1).spawnAsBoss();
				new PartialParticle(Particle.SMOKE_NORMAL, mBoss.getLocation(), 35, 0, 0, 0, 0.1).spawnAsBoss();
				world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 3, 0.75f);
				world.playSound(mBoss.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 3, 0.65f);
				Collections.shuffle(players);
				for (Player player : players) {
					if (LocationUtils.hasLineOfSight(mBoss, player)) {
						launch(player);
						break;
					}
				}
				if (mTicks >= mCount) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 15);
	}

	public void launch(Player target) {
		Location sLoc = mBoss.getLocation();
		sLoc.setY(sLoc.getY() + 1.7f);
		try {
			FallingBlock fallingBlock = sLoc.getWorld().spawnFallingBlock(sLoc, Material.JACK_O_LANTERN.createBlockData());
			fallingBlock.setDropItem(false);

			Location pLoc = target.getLocation();
			Location tLoc = fallingBlock.getLocation();
			Vector vect = new Vector(pLoc.getX() - tLoc.getX(), 0, pLoc.getZ() - tLoc.getZ());
			vect.normalize().multiply(pLoc.distance(tLoc) / 20).setY(0.7f);
			fallingBlock.setVelocity(vect);

			PartialParticle flameTrail = new PartialParticle(Particle.FLAME, fallingBlock.getLocation(), 3, 0.25, .25, .25, 0.025);
			PartialParticle smokeTrail = new PartialParticle(Particle.SMOKE_NORMAL, fallingBlock.getLocation(), 2, 0.25, .25, .25, 0.025);

			new BukkitRunnable() {
				World mWorld = mBoss.getWorld();

				@Override
				public void run() {
					// Particles while flying through the air
					Location particleLoc = fallingBlock.getLocation().add(0, fallingBlock.getHeight() / 2, 0);
					flameTrail.location(particleLoc).spawnAsBoss();
					smokeTrail.location(particleLoc).spawnAsBoss();

					if (fallingBlock.isOnGround() || !fallingBlock.isValid()) {
						// Landed on ground
						fallingBlock.remove();
						Location loc = fallingBlock.getLocation();

						loc.getBlock().setType(Material.AIR);
						new PartialParticle(Particle.FLAME, loc, 150, 0, 0, 0, 0.165).spawnAsBoss();
						new PartialParticle(Particle.SMOKE_LARGE, loc, 65, 0, 0, 0, 0.1).spawnAsBoss();
						new PartialParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 0).spawnAsBoss();
						mWorld.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2, 0.85f);

						for (Player player : PlayerUtils.playersInRange(loc, 4, true)) {
							if (mCenter.distance(player.getLocation()) < HeadlessHorsemanBoss.detectionRange) {
								BossUtils.blockableDamage(mBoss, player, DamageType.MAGIC, 35, "Hellzone Grenades", loc);
								// Shields don't stop fire!
								EntityUtils.applyFire(com.playmonumenta.plugins.Plugin.getInstance(), 20 * 3, player, mBoss);
							}
						}

						PartialParticle smokeMarker = new PartialParticle(Particle.SMOKE_LARGE, loc, 4, 1.5, 0.15, 1.5, 0.025);
						PPCircle flameMarker = new PPCircle(Particle.FLAME, loc, RADIUS).ringMode(true).count(12).delta(0.15);

						new BukkitRunnable() {
							int mTicks = 0;

							@Override
							public void run() {
								mTicks += 2;
								smokeMarker.spawnAsBoss();
								flameMarker.spawnAsBoss();

								if (mTicks % 10 == 0) {
									for (Player player : PlayerUtils.playersInRange(loc, 3, true)) {
										if (mCenter.distance(player.getLocation()) < HeadlessHorsemanBoss.arenaSize && LocationUtils.hasLineOfSight(mBoss, player)) {
											/* Fire aura can not be blocked */
											BossUtils.bossDamagePercent(mBoss, player, 0.1, "Hellzone Grenades");
											EntityUtils.applyFire(com.playmonumenta.plugins.Plugin.getInstance(), 20 * 3, player, mBoss);
										}
									}
								}

								if (mBoss.isDead() || mTicks >= 20 * 60 + 30) {
									this.cancel();
								}
							}

						}.runTaskTimer(mPlugin, 10, 2);
						this.cancel();


					}
				}

			}.runTaskTimer(mPlugin, 0, 1);
		} catch (Exception e) {
			mPlugin.getLogger().warning("Failed to summon grenade for hellzone grenade toss: " + e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public int cooldownTicks() {
		return mCooldownTicks;
	}

}
