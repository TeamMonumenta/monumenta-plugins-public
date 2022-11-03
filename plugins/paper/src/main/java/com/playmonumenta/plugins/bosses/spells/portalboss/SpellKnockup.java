package com.playmonumenta.plugins.bosses.spells.portalboss;

import com.playmonumenta.plugins.bosses.bosses.BeastOfTheBlackFlame;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellKnockup extends Spell {
	private int mCooldown = 0;
	private LivingEntity mBoss;
	private Plugin mPlugin;

	private int mRadius = 3;

	public SpellKnockup(LivingEntity boss, Plugin plugin, int cooldown) {
		mBoss = boss;
		mPlugin = plugin;
		mCooldown = cooldown;
	}

	@Override
	public void run() {

		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), BeastOfTheBlackFlame.detectionRange, true);
		World world = mBoss.getWorld();

		List<Location> locs = new ArrayList<>();
		for (Player p : players) {
			locs.add(p.getLocation());
		}

		new BukkitRunnable() {
			private int mTicks = 0;
			@Override
			public void run() {
				if (mTicks >= 20 * 1.25) {

					for (double degree = 0; degree < 360; degree += 15) {
						if (FastUtils.RANDOM.nextDouble() < 0.8) {
							double radian = Math.toRadians(degree);
							double cos = FastUtils.cos(radian);
							double sin = FastUtils.sin(radian);

							for (Location loc : locs) {
								loc.add(cos * mRadius, 0.5, sin * mRadius);
								world.spawnParticle(Particle.SQUID_INK, loc, 1, 0, 0, 0, 0);
								loc.subtract(cos * mRadius, 0.5, sin * mRadius);
							}
						}
					}

					for (Location loc : locs) {
						world.spawnParticle(Particle.LAVA, loc, 20, 2, 0.1, 2, 0.25);
						world.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.5f, 2);
						world.playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.5f, 1);

						for (Player p : PlayerUtils.playersInRange(loc, mRadius, true)) {
							DamageUtils.damage(mBoss, p, DamageType.MAGIC, 85, null, false, true, "Error Field");
							MovementUtils.knockAway(loc, p, 0f, 1f, false);
						}
					}

					this.cancel();
				}

				if (mTicks % 6 == 0) {
					for (double degree = 0; degree < 360; degree += 15) {
						double radian = Math.toRadians(degree);
						double cos = FastUtils.cos(radian);
						double sin = FastUtils.sin(radian);

						for (Location loc : locs) {
							loc.add(cos * mRadius, 0.5, sin * mRadius);
							if (FastUtils.RANDOM.nextDouble() < 0.5) {
								world.spawnParticle(Particle.SPELL_WITCH, loc, 1, 0, 0, 0, 0);
							} else {
								world.spawnParticle(Particle.END_ROD, loc, 1, 0, 0, 0, 0);
							}
							loc.subtract(cos * mRadius, 0.5, sin * mRadius);
						}
					}
				}

				for (Location loc : locs) {
					world.spawnParticle(Particle.BLOCK_DUST, loc, 10, 2, 0.1, 2, 0.25, Material.BONE_BLOCK.createBlockData());
					world.spawnParticle(Particle.LAVA, loc, 5, 2, 0.1, 2, 0.25);

					if (mTicks % 10 == 0) {
						world.playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1, 0f);
					}
				}

				mTicks += 2;
			}
		}.runTaskTimer(mPlugin, 0, 2);
	}


	@Override
	public int cooldownTicks() {
		return mCooldown;
	}
}
