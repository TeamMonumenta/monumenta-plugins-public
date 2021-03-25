
package com.playmonumenta.plugins.bosses.spells.kaul;

import java.util.ArrayList;
import java.util.List;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;



/*
 * Lighting Storm : if a player is higher than 2 blocks
 * from the ground, they get hit by thunders each 2s
 * (always active) (First time offenders should get some
 * form of chat message as a warning)
 */
public class SpellLightningStorm extends Spell {
	private int mTicks = 0;
	private LivingEntity mBoss;
	private double mRange;
	private static final String LIGHTNING_STORM_TAG = "KaulLightningStormTag";
	private Location mCenter;
	private List<Player> mWarnedPlayers = new ArrayList<Player>();
	private static final Particle.DustOptions YELLOW_1_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 20), 1.0f);
	private static final Particle.DustOptions YELLOW_2_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 120), 1.0f);

	public SpellLightningStorm(LivingEntity boss, double range) {
		mBoss = boss;
		mRange = range;
		for (Entity e : boss.getWorld().getEntities()) {
			if (e.getScoreboardTags().contains(LIGHTNING_STORM_TAG) && e instanceof LivingEntity) {
				mCenter = e.getLocation();
				break;
			}
		}
	}

	@Override
	public void run() {
		mTicks--;
		if (mTicks <= 0) {
			mTicks = 6;

			double arenaSurfaceY = mCenter.getY(); // This should be 8.0 based on the armour stand placement
			for (Player player : PlayerUtils.playersInRange(mCenter, mRange)) {
				double playerY = player.getLocation().getY();

				// If standing on heightened ground or going up a climbable to cheese,
				// enforce stricter 2-block threshold.
				// Eg disallow pillaring up
				int yThreshold = 2;

				// If truly in the air (not on ground and not climbing - like the Thunder Step requirement),
				// have relaxed 5-block threshold.
				// Eg recoil, jump boost, Primordial yeet
				if (PlayerUtils.isAirborne(player)) {
					// When the player is > 5 blocks above the arena surface,
					// the block Y-5 below them is above the surface level of the arena.
					// This would usually not be solid, similar to the previous implementation's !isSolid() check
					yThreshold = 5;
				}

				if (playerY >= arenaSurfaceY + yThreshold) {
					lightning(player);
				}
			}
		}
	}

	private void lightning(Player player) {
		World world = player.getWorld();
		Location strike = player.getLocation().add(0, 10, 0);
		Location loc = player.getLocation();
		for (int i = 0; i < 10; i++) {
			strike.subtract(0, 1, 0);
			world.spawnParticle(Particle.REDSTONE, strike, 10, 0.3, 0.3, 0.3, YELLOW_1_COLOR);
			world.spawnParticle(Particle.REDSTONE, strike, 10, 0.3, 0.3, 0.3, YELLOW_2_COLOR);
		}
		world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 15, 0, 0, 0, 0.25);
		world.spawnParticle(Particle.FLAME, loc, 50, 0, 0, 0, 0.175);
		world.spawnParticle(Particle.SMOKE_LARGE, loc, 15, 0, 0, 0, 0.25);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1, 0.9f);
		world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1, 1);

		/* Too-high lightning hits you directly and can not be blocked */
		BossUtils.bossDamagePercent(mBoss, player, 0.5, (Location)null);
		if (!mWarnedPlayers.contains(player)) {
			mWarnedPlayers.add(player);
			player.sendMessage(ChatColor.AQUA
			                   + "That hurt! There must be a lightning storm above you. Staying close to the ground might help to not get struck again.");
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
