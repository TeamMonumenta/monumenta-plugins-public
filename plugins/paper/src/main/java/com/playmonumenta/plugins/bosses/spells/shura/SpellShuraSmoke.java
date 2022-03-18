package com.playmonumenta.plugins.bosses.spells.shura;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.player.PPGroundCircle;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellShuraSmoke extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private Location mCenter;
	private double mRange;
	private int mRadius = 3;

	public SpellShuraSmoke(Plugin plugin, LivingEntity boss, Location center, double range) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = center;
		mRange = range;
	}

	@Override public void run() {
		// Choose random player within range that has line of sight to boss
		List<Player> players = PlayerUtils.playersInRange(mCenter, mRange, true);
		Collections.shuffle(players);
		Player p = players.get(0);

		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_CREEPER_PRIMED, SoundCategory.HOSTILE, 1, 2);
		Location spawnLoc = mBoss.getLocation().add(0, 1.7, 0);
		FallingBlock block = world.spawnFallingBlock(spawnLoc, Bukkit.createBlockData(Material.COAL_BLOCK));
		block.setDropItem(false);

		Location pLoc = p.getLocation();
		Location tLoc = block.getLocation();
		Vector vect = new Vector(pLoc.getX() - tLoc.getX(), 0, pLoc.getZ() - tLoc.getZ());
		vect.normalize().multiply(pLoc.distance(tLoc) / 20).setY(0.7f);
		block.setVelocity(vect);

		PartialParticle smokeTrail = new PartialParticle(Particle.SMOKE_NORMAL, block.getLocation(), 2, 0.25, .25, .25, 0.025);

		new BukkitRunnable() {
			World mWorld = mBoss.getWorld();
			@Override
			public void run() {
				// Particles while flying through the air
				Location particleLoc = block.getLocation().add(0, block.getHeight() / 2, 0);
				smokeTrail.location(particleLoc).spawnAsBoss();

				if (block.isOnGround() || !block.isValid()) {
					// Landed on ground
					block.remove();
					Location loc = block.getLocation();

					loc.getBlock().setType(Material.AIR);
					new PartialParticle(Particle.FLAME, loc, 150, 0, 0, 0, 0.165).spawnAsBoss();
					new PartialParticle(Particle.SMOKE_LARGE, loc, 65, 0, 0, 0, 0.1).spawnAsBoss();
					new PartialParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 0).spawnAsBoss();
					mWorld.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2, 0.85f);

					PartialParticle smokeMarker = new PartialParticle(Particle.SMOKE_LARGE, loc, 5, 1.5, 0.15, 1.5, 0.025);
					PPGroundCircle indicator = new PPGroundCircle(Particle.SPELL_WITCH, loc, 3, 0.1, 0, 0.1, 0).init(mRadius, true);
					new BukkitRunnable() {
						int mTicks = 0;
						@Override
						public void run() {
							mTicks += 2;
							smokeMarker.spawnAsBoss();
							indicator.spawnAsBoss();

							if (mTicks % 20 == 0) {
								for (Player player : PlayerUtils.playersInRange(loc, mRadius, true)) {
									player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 3, 4));
									player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 3, 3));
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
	}

	@Override public int cooldownTicks() {
		return 4 * 20;
	}
}
