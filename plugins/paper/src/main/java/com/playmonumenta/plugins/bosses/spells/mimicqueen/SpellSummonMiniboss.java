package com.playmonumenta.plugins.bosses.spells.mimicqueen;

import com.playmonumenta.plugins.bosses.bosses.MimicQueen;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.loot.Lootable;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;


public class SpellSummonMiniboss extends Spell {

	private LivingEntity mMiniboss;

	private LivingEntity mBoss;
	private Plugin mPlugin;

	public SpellSummonMiniboss(Plugin plugin, LivingEntity boss) {
		mBoss = boss;
		mPlugin = plugin;

		mMiniboss = null;
	}

	@Override
	public void run() {
		mMiniboss = null;
		Location loc = mBoss.getLocation();
		mBoss.getWorld().playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 5, 0);
		mBoss.getWorld().playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 5, 0);
		mBoss.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, loc, 6, 0, 0, 0, 0.5, null, true);

		List<Player> players = PlayerUtils.playersInRange(loc, MimicQueen.detectionRange, true);
		final int playerCount = ((players.size() > 1) ? players.size() : 1);

		new BukkitRunnable() {
			int mTicks = 0;
			int mCount = 0;
			@Override
			public void run() {
				if (mCount >= playerCount) {
					this.cancel();
				}

				if (mTicks >= 15) {
					//Default is baby mimic
					String mob = "BabyMimic";
					int random = FastUtils.RANDOM.nextInt(5);
					switch (random) {
						default:
						case 0:
							//BabyMimic
							break;
						case 1:
							//GamblingMimic
							mob = "GamblingMimic";
							break;
						case 2:
							//FurnaceMimic
							mob = "FurnaceMimic";
							break;
						case 3:
							//CraftingMimic
							mob = "CraftingMimic";
							break;
						case 4:
							//CircusMimic
							mob = "CircusMimic";
							break;
					}

					Location loc = mBoss.getLocation();

					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.HOSTILE, 5, 0);
					mBoss.getWorld().spawnParticle(Particle.CLOUD, loc, 6, 0, 0, 0, 0.5);
					mMiniboss = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, mob);
					if (mMiniboss != null && mMiniboss instanceof Lootable) {
						((Lootable)mMiniboss).clearLootTable();
					}
					mCount++;
					mTicks = 0;
				}
				mTicks += 5;
			}
		}.runTaskTimer(mPlugin, 0, 5);
	}

	@Override
	public boolean canRun() {
		return mMiniboss == null || mMiniboss.isDead() || !mMiniboss.isValid();
	}

	@Override
	public int cooldownTicks() {
		return 20 * 5;
	}

}
