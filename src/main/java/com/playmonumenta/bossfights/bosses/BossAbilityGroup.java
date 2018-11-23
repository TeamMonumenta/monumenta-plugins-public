package com.playmonumenta.bossfights.bosses;

import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitScheduler;

import com.playmonumenta.bossfights.BossBarManager;
import com.playmonumenta.bossfights.Plugin;
import com.playmonumenta.bossfights.SpellManager;
import com.playmonumenta.bossfights.spells.Spell;
import com.playmonumenta.bossfights.utils.SerializationUtils;
import com.playmonumenta.bossfights.utils.Utils;

public abstract class BossAbilityGroup {
	private Plugin mPlugin;
	private LivingEntity mBoss;
	private BossBarManager mBossBar;
	private SpellManager mActiveSpells;
	private int mTaskIDpassive = -1;
	private int mTaskIDactive = -1;
	private boolean mUnloaded = false;


	public void constructBoss(Plugin plugin, String identityTag, LivingEntity boss, SpellManager activeSpells,
	                          List<Spell> passiveSpells, int detectionRange, BossBarManager bossBar) {
		mPlugin = plugin;
		mBoss = boss;
		mBossBar = bossBar;
		mActiveSpells = activeSpells;

		mBoss.setRemoveWhenFarAway(false);
		mBoss.addScoreboardTag(identityTag);

		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable passive = new Runnable() {
			@Override
			public void run() {
				if (mBossBar != null) {
					mBossBar.update();
				}

				/* Don't run abilities if players aren't present */
				if (Utils.playersInRange(mBoss.getLocation(), detectionRange).isEmpty()) {
					return;
				}

				if (passiveSpells != null) {
					for (Spell spell : passiveSpells) {
						spell.run();
					}
				}
			}
		};
		mTaskIDpassive = scheduler.scheduleSyncRepeatingTask(plugin, passive, 1L, 5L);

		Runnable active = new Runnable() {
			Integer mNextActiveTimer = 0;

			@Override
			public void run() {
				mNextActiveTimer -= 2;

				if (mNextActiveTimer > 0) {
					// Still waiting for the current spell to finish
					return;
				}

				/* Check if somehow the boss entity is missing even though this is still running */
				boolean bossCheck = true;
				Location bossLoc = mBoss.getLocation();
				for (Entity entity : bossLoc.getWorld().getNearbyEntities(bossLoc, 4, 4, 4))
					if (entity.getUniqueId().equals(mBoss.getUniqueId())) {
						bossCheck = false;
					}
				if (bossCheck) {
					mPlugin.getLogger().log(Level.WARNING,
					                        "Boss is missing but still registered as an active boss. Unloading...");
					mPlugin.mBossManager.unload(mBoss);
					unload();
					return;
				}

				/* Don't progress if players aren't present */
				if (Utils.playersInRange(mBoss.getLocation(), detectionRange).isEmpty()) {
					return;
				}

				if (activeSpells != null) {
					// Run the next spell and store how long before the next spell can run
					mNextActiveTimer = activeSpells.runNextSpell();
				}
			}
		};
		mTaskIDactive = scheduler.scheduleSyncRepeatingTask(plugin, active, 100L, 2L);
	}

	/*
	 * Called only the first time the boss is summoned into the world
	 *
	 * Useful to set the bosses health / armor / etc. based on # of players
	 */
	public void init() {};

	/*
	 * Called when the boss dies
	 *
	 * Useful to use setblock or a command to trigger post-fight logic
	 */
	public void death() {};

	/*
	 * Called when the mob is unloading and we need to save its metadata
	 *
	 * Needed whenever the boss needs more parameters to instantiate than just
	 * the boss mob itself (tele to spawn location, end location to set block, etc.)
	 */
	public String serialize() {
		return null;
	}

	/*
	 * Called when the chunk the boss is in unloads. Also called after death()
	 *
	 * Probably don't need to override this method, but if you do, call it
	 * via super.unload()
	 */
	public void unload() {
		/* Make sure we don't accidentally unload twice */
		if (!mUnloaded) {
			mUnloaded = true;

			mActiveSpells.cancelAll();

			BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
			if (mTaskIDpassive != -1) {
				scheduler.cancelTask(mTaskIDpassive);
			}
			if (mTaskIDactive != -1) {
				scheduler.cancelTask(mTaskIDactive);
			}
			if (mBossBar != null) {
				mBossBar.remove();
			}

			if (mBoss.isValid() && mBoss.getHealth() > 0) {
				String content = serialize();
				if (content != null && !content.isEmpty()) {
					try {
						SerializationUtils.storeDataOnEntity(mBoss, content);
					} catch (Exception ex) {
						mPlugin.getLogger().log(Level.SEVERE, "Failed to save data to entity: ", ex);
					}
				}
			}
		}
	}
}
