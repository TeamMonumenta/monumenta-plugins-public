package com.playmonumenta.plugins.bosses.bosses;

import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import com.destroystokyo.paper.event.entity.EntityPathfindEvent;
import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.events.SpellCastEvent;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;

public abstract class BossAbilityGroup {
	@FunctionalInterface
	public interface PhaseAction {
		/**
		 * Function called whenever the boss changes phase
		 */
		void run(LivingEntity entity);
	}

	protected Plugin mPlugin;
	private LivingEntity mBoss;
	private String mIdentityTag;
	private BossBarManager mBossBar;
	private SpellManager mActiveSpells;
	private List<Spell> mPassiveSpells;
	private int mTaskIDpassive = -1;
	private int mTaskIDactive = -1;
	private boolean mUnloaded = false;
	private Integer mNextActiveTimer = 0;
	public boolean mDead = false;

	public void changePhase(SpellManager activeSpells,
	                        List<Spell> passiveSpells, PhaseAction action) {

		if (action != null) {
			action.run(mBoss);
		}

		if (mActiveSpells != null) {
			mActiveSpells.cancelAll();
		}

		mActiveSpells = activeSpells;
		mPassiveSpells = passiveSpells;
	}

	public void constructBoss(Plugin plugin, String identityTag, LivingEntity boss, SpellManager activeSpells,
	                          List<Spell> passiveSpells, int detectionRange, BossBarManager bossBar) {
		constructBoss(plugin, identityTag, boss, activeSpells, passiveSpells, detectionRange, bossBar, 100);
	}

	public void constructBoss(Plugin plugin, String identityTag, LivingEntity boss, SpellManager activeSpells,
	                          List<Spell> passiveSpells, int detectionRange, BossBarManager bossBar, long spellDelay) {
		mPlugin = plugin;
		mBoss = boss;
		mIdentityTag = identityTag;
		mBossBar = bossBar;
		mActiveSpells = activeSpells;
		mPassiveSpells = passiveSpells;

		mBoss.addScoreboardTag(identityTag);

		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		Runnable passive = new Runnable() {
			@Override
			public void run() {
				if (mBossBar != null && !mDead) {
					mBossBar.update();
				}

				/* Don't run abilities if players aren't present */
				if (PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange).isEmpty()) {
					return;
				}

				if (mPassiveSpells != null) {
					for (Spell spell : mPassiveSpells) {
						spell.run();
					}
				}
			}
		};
		mTaskIDpassive = scheduler.scheduleSyncRepeatingTask(plugin, passive, 1L, 5L);

		Runnable active = new Runnable() {
			private boolean mDisabled = true;


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
				for (Entity entity : bossLoc.getWorld().getNearbyEntities(bossLoc, 4, 4, 4)) {
					if (entity.getUniqueId().equals(mBoss.getUniqueId())) {
						bossCheck = false;
					}
				}
				if (bossCheck) {
					mPlugin.getLogger().warning("Boss is missing but still registered as an active boss. Unloading...");
					BossManager mgr = BossManager.getInstance();
					if (mgr != null) {
						BossManager.getInstance().unload(mBoss);
					}
					// Just in case for some reason the boss is no longer registered with the manager...
					unload();
					return;
				}

				/* Don't progress if players aren't present */
				if (PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange).isEmpty()) {
					if (!mDisabled) {
						/* Cancel all the spells just in case they were activated */
						mDisabled = true;

						if (mActiveSpells != null) {
							mActiveSpells.cancelAll();
						}
					}
					return;
				}

				/* Some spells might have been run - so when this next deactivates they need to be cancelled */
				mDisabled = false;

				if (mActiveSpells != null) {
					// Run the next spell and store how long before the next spell can run
					mNextActiveTimer = mActiveSpells.runNextSpell();

					// The event goes after the spell casts because Kaul's abilities take place where he previously was.
					Spell spell = mActiveSpells.getLastCastedSpell();
					if (spell != null) {
						SpellCastEvent event = new SpellCastEvent(mBoss, spell);
						Bukkit.getPluginManager().callEvent(event);
					}
				}
			}
		};
		mTaskIDactive = scheduler.scheduleSyncRepeatingTask(plugin, active, spellDelay, 2L);
	}

	public void forceCastSpell(Class<?> spell) {
		if (mActiveSpells != null) {
			mNextActiveTimer = mActiveSpells.forceCastSpell(spell);
			Spell sp = mActiveSpells.getLastCastedSpell();
			if (sp != null) {
				SpellCastEvent event = new SpellCastEvent(mBoss, sp);
				Bukkit.getPluginManager().callEvent(event);
			} else {
				mPlugin.getLogger().severe("Warning: Boss '" + mIdentityTag + "' attempted to force cast '" + spell.toString() + "' but boss does not have this spell!");
			}
		} else {
			mPlugin.getLogger().severe("Warning: Boss '" + mIdentityTag + "' attempted to force cast '" + spell.toString() + "' but there are no active spells!");
		}
	}

	/********************************************************************************
	 * Event Handlers
	 *******************************************************************************/

	public List<Spell> getPassives() {
		return mPassiveSpells;
	}

	public List<Spell> getActiveSpells() {
		if (mActiveSpells != null) {
			return mActiveSpells.getSpells();
		} else {
			return null;
		}
	}

	/*
	 * Boss damaged another entity
	 */
	public void bossDamagedEntity(EntityDamageByEntityEvent event) {};

	/*
	 * Boss was damaged
	 */
	public void bossDamagedByEntity(EntityDamageByEntityEvent event) {};

	/*
	 * Boss shot a projectile
	 */
	public void bossLaunchedProjectile(ProjectileLaunchEvent event) {};

	/*
	 * Boss-shot projectile hit something
	 */
	public void bossProjectileHit(ProjectileHitEvent event) {};

	/*
	 * Boss gets hit by a projectile
	 */
	public void bossHitByProjectile(ProjectileHitEvent event) {};

	/*
	 * Boss hit by area effect cloud
	 */
	public void areaEffectAppliedToBoss(AreaEffectCloudApplyEvent event) {};

	/*
	 * Boss-shot projectile hit something
	 */
	public void splashPotionAppliedToBoss(PotionSplashEvent event) {};

	public void bossCastAbility(SpellCastEvent event) {};

	public void bossPathfind(EntityPathfindEvent event) {};

	public void bossChangedTarget(EntityTargetEvent event) {};

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

			if (mActiveSpells != null) {
				mActiveSpells.cancelAll();
			}

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
