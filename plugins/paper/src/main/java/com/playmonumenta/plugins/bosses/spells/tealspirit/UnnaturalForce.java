package com.playmonumenta.plugins.bosses.spells.tealspirit;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class UnnaturalForce extends Spell {
	private static final String ABILITY_NAME = "Unnatural Force";
	private static final int DAMAGE = 70;
	private static final int COOLDOWN = 20 * 20;
	private static final int CAST_TIME = 20 * 3;
	private static final int DISPLAY_TIME = 20 * 2;
	private static final int RANGE = 50;
	private static final int MAX_HEIGHT = 1;
	private Plugin mPlugin;
	private LivingEntity mBoss;
	private Location mSpawnLoc;
	private ChargeUpManager mChargeUp;
	private int mInnerRadius;
	private int mOuterRadius;

	public UnnaturalForce(Plugin plugin, LivingEntity boss, Location spawnLoc, int innerRadius, int outerRadius) {
		this.mPlugin = plugin;
		this.mBoss = boss;
		this.mSpawnLoc = spawnLoc;
		this.mInnerRadius = innerRadius;
		this.mOuterRadius = outerRadius;

		String prefix = "";
		if (mInnerRadius == 0) {
			prefix = "Inner ";
		} else {
			prefix = "Outer ";
		}

		this.mChargeUp = new ChargeUpManager(mBoss, CAST_TIME, ChatColor.GOLD + "Channeling " + ChatColor.YELLOW + prefix + ABILITY_NAME,
			BarColor.YELLOW, BarStyle.SOLID, RANGE);
	}

	@Override
	public void run() {
		World world = mSpawnLoc.getWorld();
		mChargeUp.setTime(0);
		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {
				if (mChargeUp.getTime() % 5 == 0 && mChargeUp.getTime() <= DISPLAY_TIME) {
					for (double deg = 0; deg < 360; deg += 3) {
						double cos = FastUtils.cosDeg(deg);
						double sin = FastUtils.sinDeg(deg);

						for (int x = mInnerRadius; x < mOuterRadius; x++) {
							for (int i = 0; i < MAX_HEIGHT; i++) {
								world.spawnParticle(Particle.SPELL_WITCH, mSpawnLoc.clone().add(cos * x, i, sin * x), 1, 0.1, 0.1, 0.1, 0);
							}
						}
					}
				}

				if (mChargeUp.nextTick()) {
					world.playSound(mSpawnLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 10.5f, 2);
					world.playSound(mSpawnLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 10.5f, 1);

					int inc = 3;

					for (double deg = 0; deg < 360; deg += inc * 2) {
						double cos = FastUtils.cosDeg(deg);
						double sin = FastUtils.sinDeg(deg);

						for (int x = mInnerRadius; x < mOuterRadius; x += inc) {
							Location loc = mSpawnLoc.clone().add(cos * x, 0, sin * x);

							world.spawnParticle(Particle.SMOKE_NORMAL, loc, 1, 0.15, 0.15, 0.15, 0);
							if (deg % 4 == 0) {
								world.spawnParticle(Particle.BLOCK_DUST, loc, 1, 0.15, 0.1, 0.15, 0.75, Material.DEEPSLATE_TILES.createBlockData());
							} else {
								world.spawnParticle(Particle.BLOCK_DUST, loc, 1, 0.15, 0.1, 0.15, 0.75, Material.POLISHED_DEEPSLATE.createBlockData());
							}

							if (deg % 30 == 0) {
								world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 1, 0.15, 0.1, 0.15, 0.25);
							}

						}
					}

					for (Player p : PlayerUtils.playersInRange(mSpawnLoc, mOuterRadius, true)) {
						if (!PlayerUtils.playersInRange(mSpawnLoc, mInnerRadius, true).contains(p)) {
							DamageUtils.damage(mBoss, p, DamageEvent.DamageType.TRUE, DAMAGE, null, false, true, "Creeping Darkness");
							MovementUtils.knockAway(mSpawnLoc, p, 0, .75f, false);
						}
					}

					this.cancel();
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN;
	}
}
