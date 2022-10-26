package com.playmonumenta.plugins.bosses.spells.imperialconstruct;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.ImperialConstruct;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellCrash extends Spell {

	private LivingEntity mBoss;
	private Plugin mPlugin;

	private Location mCurrentLoc;

	public SpellCrash(LivingEntity boss, Plugin plugin, Location currentLoc) {
		mBoss = boss;
		mPlugin = plugin;
		mCurrentLoc = currentLoc.clone();
	}

	@Override
	public void run() {
		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 1f, 1f);
		List<Player> players = PlayerUtils.playersInRange(mCurrentLoc, ImperialConstruct.detectionRange, true);
		for (Player p : players) {
			p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 7, 0, false, true));
		}

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				if (mTicks >= 20 * 3) {
					Location tempLoc = mCurrentLoc.clone();
					for (int z = -21; z <= 21; z++) {
						for (int x = -25; x <= 25; x++) {
							for (int y = -6; y <= 10; y++) {
								tempLoc.set(mCurrentLoc.getX() + x, mCurrentLoc.getY() + y, mCurrentLoc.getZ() + z);
								Material mat = tempLoc.getBlock().getType();
								if (mat == Material.COBBLESTONE || mat == Material.BLACKSTONE || mat == Material.POLISHED_BLACKSTONE_BRICKS || mat == Material.CRYING_OBSIDIAN || mat == Material.TUFF || mat == Material.BEDROCK) {
									tempLoc.getBlock().setType(Material.AIR);
								}
							}
						}
					}

					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 50f, 0f);

					this.cancel();
					return;
				}

				if (mTicks % 10 == 0) {
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, SoundCategory.HOSTILE, 50f, 0f);
				}

				new PartialParticle(Particle.DRAGON_BREATH, mCurrentLoc, 400, 25, 0.3).spawnAsBoss();

				mTicks += 2;
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runnable);

	}

	public void setLocation(Location loc) {
		mCurrentLoc = loc.clone();
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
