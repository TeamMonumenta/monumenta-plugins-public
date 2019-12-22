
package com.playmonumenta.plugins.bosses.spells.spells_kaul;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.utils.DamageUtils;
import com.playmonumenta.plugins.bosses.utils.Utils;

/*
 * Lighting Storm : if a player is higher than 2 blocks
 * from the ground, they get hit by thunders each 2s
 * (always active) (First time offenders should get some
 * form of chat message as a warning)
 */
public class SpellLightningStorm extends Spell {
	private int t = 0;
	private LivingEntity mBoss;
	private double mRange;
	private static final String LIGHTNING_STORM_TAG = "KaulLightningStormTag";
	private LivingEntity mCenter;
	private List<Player> mWarnedPlayers = new ArrayList<Player>();
	private static final Particle.DustOptions YELLOW_1_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 20), 1.0f);
	private static final Particle.DustOptions YELLOW_2_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 120), 1.0f);

	public SpellLightningStorm(LivingEntity boss, double range) {
		mBoss = boss;
		mRange = range;
		for (Entity e : boss.getWorld().getEntities()) {
			if (e.getScoreboardTags().contains(LIGHTNING_STORM_TAG) && e instanceof LivingEntity) {
				mCenter = (LivingEntity) e;
				break;
			}
		}
	}

	@Override
	public void run() {
		t--;
		if (t <= 0) {
			t = 6;
			for (Player player : Utils.playersInRange(mCenter.getLocation(), mRange)) {
				Location loc = player.getLocation();
				if (player.isOnGround()) {
					if (loc.getY() - mCenter.getLocation().getY() >= 2) {
						lightning(player);
					}
				} else {
					if (!loc.subtract(0, 5, 0).getBlock().getType().isSolid() && loc.getY() > 9.9) {
						lightning(player);
					}
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

		DamageUtils.damagePercent(mBoss, player, 0.4);
		if (!mWarnedPlayers.contains(player)) {
			mWarnedPlayers.add(player);
			player.sendMessage(ChatColor.AQUA
			                   + "That hurt! There must be a lightning storm above you. Staying close to the ground might help to not get struck again.");
		}
	}

	@Override
	public int duration() {
		return 0;
	}
}
