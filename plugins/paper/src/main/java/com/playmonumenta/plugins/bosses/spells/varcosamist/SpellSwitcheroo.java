package com.playmonumenta.plugins.bosses.spells.varcosamist;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellSwitcheroo extends Spell {
	private static final int MAX_RANGE = 50;

	private final int mTpDelay;
	private final Plugin mPlugin;
	private final Entity mLauncher;
	private final int mDuration;
	private final String mDio;

	public SpellSwitcheroo(Plugin plugin, Entity launcher, int duration, int chargeTime, String dio) {
		mPlugin = plugin;
		mLauncher = launcher;
		mDuration = duration;
		mTpDelay = chargeTime;
		mDio = dio;
	}

	@Override
	public void run() {
		List<Player> players = PlayerUtils.playersInRange(mLauncher.getLocation(), MAX_RANGE, false);
		while (!players.isEmpty()) {
			Player target = players.get(FastUtils.RANDOM.nextInt(players.size()));

			/* Do not teleport to players in safezones */
			if (ZoneUtils.hasZoneProperty(target, ZoneProperty.RESIST_5)) {
				/* This player is in a safe area - don't tp to them */
				players.remove(target);
			} else {
				launch(target);
				animation(target);
				break;
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return mDuration;
	}

	private void launch(Player target) {
		new BukkitRunnable() {
			@Override
			public void run() {
				if (target.getLocation().distance(mLauncher.getLocation()) > MAX_RANGE) {
					return;
				}

				// set targetLoc to target and slightly elevate it
				Location targetLoc = target.getLocation();
				targetLoc.setY(target.getLocation().getY() + 0.1f);
				// set mobLoc to mLauncher and slightly elevate it
				Location mobLoc = mLauncher.getLocation();
				mobLoc.setY(mLauncher.getLocation().getY() + 0.1f);
				World world = mLauncher.getWorld();

				world.spawnParticle(Particle.SPELL_WITCH, mLauncher.getLocation().add(0, mLauncher.getHeight() / 2, 0), 30, 0.25, 0.45, 0.25, 1);
				world.spawnParticle(Particle.SMOKE_LARGE, mLauncher.getLocation().add(0, mLauncher.getHeight() / 2, 0), 12, 0, 0.45, 0, 0.125);
				mLauncher.teleport(targetLoc);
				target.teleport(mobLoc);
				world.spawnParticle(Particle.SPELL_WITCH, targetLoc.clone().add(0, mLauncher.getHeight() / 2, 0), 30, 0.25, 0.45, 0.25, 1);
				world.spawnParticle(Particle.SMOKE_LARGE, targetLoc.clone().add(0, mLauncher.getHeight() / 2, 0), 12, 0, 0.45, 0, 0.125);
				mLauncher.getWorld().playSound(mLauncher.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 3f, 0.7f);

				world.spawnParticle(Particle.SMOKE_LARGE, targetLoc, 65, 0, 0, 0, 0.1);
				world.spawnParticle(Particle.EXPLOSION_LARGE, targetLoc, 4, 0, 0, 0, 0);
				world.playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 2, 0.85f);


				if (mLauncher instanceof LivingEntity le) {

					for (Player player : PlayerUtils.playersInRange(targetLoc, 4, false)) {
						DamageUtils.damage(le, player, DamageType.MAGIC, 35, null, false, true, "Shadow Sneak");
					}

					new BukkitRunnable() {

						int mT = 0;
						Location mLoc = mobLoc;
						@Override
						public void run() {
							mT += 2;
							world.spawnParticle(Particle.CRIT, mLoc, 12, 1.5, 0.15, 1.5, 0.05);
							world.spawnParticle(Particle.REDSTONE, mLoc, 4, 1.5, 0.15, 1.5, 0.025, new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.0f));

							if (mT % 10 == 0) {
								for (Player player : PlayerUtils.playersInRange(mobLoc, 3, true)) {
									BossUtils.bossDamagePercent(le, player, 0.05, (Location) null, "Phantom Snare");
								}
							}

							if (mT >= 20 * 5) {
								this.cancel();
							}
						}

					}.runTaskTimer(mPlugin, 10, 2);
				} else {
					target.sendMessage("This boss is not a living entity!");
				}

			}

		}.runTaskLater(mPlugin, mTpDelay);
	}

	private void animation(Player target) {
		target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITCH_AMBIENT, 1.4f, 0.5f);

		PlayerUtils.executeCommandOnNearbyPlayers(mLauncher.getLocation(), 50, "tellraw @s [\"\",{\"text\":\"" + mDio + "\",\"color\":\"red\"}]");

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mTicks++;
				Location particleLoc = mLauncher.getLocation().add(new Location(mLauncher.getWorld(), -0.5f, 0f, 0.5f));
				particleLoc.getWorld().spawnParticle(Particle.PORTAL, particleLoc, 10, 1, 1, 1, 0.03);

				if (mTicks > mTpDelay) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}
}
