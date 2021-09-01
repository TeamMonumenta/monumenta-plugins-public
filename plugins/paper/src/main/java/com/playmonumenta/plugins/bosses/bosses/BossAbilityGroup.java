package com.playmonumenta.plugins.bosses.bosses;

import java.util.List;
import java.util.logging.Level;

import com.destroystokyo.paper.event.entity.EntityPathfindEvent;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.events.SpellCastEvent;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.CustomEffectApplyEvent;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;



public abstract class BossAbilityGroup {
	@FunctionalInterface
	public interface PhaseAction {
		/**
		 * Function called whenever the boss changes phase
		 */
		void run(LivingEntity entity);
	}

	public static final int PASSIVE_RUN_INTERVAL_DEFAULT = Constants.QUARTER_TICKS_PER_SECOND;

	protected final Plugin mPlugin;
	protected final LivingEntity mBoss;
	private final String mIdentityTag;

	private BossBarManager mBossBar;
	private SpellManager mActiveSpells;
	private List<Spell> mPassiveSpells;
	private BukkitRunnable mTaskPassive = null;
	private BukkitRunnable mTaskActive = null;
	private boolean mUnloaded = false;
	private Integer mNextActiveTimer = 0;
	public boolean mDead = false;

	protected BossAbilityGroup(Plugin plugin, String identityTag, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
		mIdentityTag = identityTag;
		mBoss.addScoreboardTag(mIdentityTag);
	}

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

	public void constructBoss(SpellManager activeSpells,
	                          List<Spell> passiveSpells, int detectionRange, BossBarManager bossBar) {
		constructBoss(activeSpells, passiveSpells, detectionRange, bossBar, 100);
	}

	public void constructBoss(SpellManager activeSpells,
	                          List<Spell> passiveSpells, int detectionRange, BossBarManager bossBar, long spellDelay) {
		constructBoss(activeSpells, passiveSpells, detectionRange, bossBar, spellDelay, PASSIVE_RUN_INTERVAL_DEFAULT);

	}

	/* If detectionRange <= 0, will always run regardless of whether players are nearby */
	public void constructBoss(SpellManager activeSpells,
	                          List<Spell> passiveSpells, int detectionRange, BossBarManager bossBar, long spellDelay, long passiveIntervalTicks) {
		mBossBar = bossBar;
		mActiveSpells = activeSpells;
		mPassiveSpells = passiveSpells;

		mTaskPassive = new BukkitRunnable() {
			private int mMissingTicks = 0;

			@Override
			public void run() {
				if (mBossBar != null && !mDead) {
					mBossBar.update();
				}

				mMissingTicks += passiveIntervalTicks;
				if (mMissingTicks > 100) {
					mMissingTicks = 0;
					/* Check if somehow the boss entity is missing even though this is still running */
					if (isBossMissing()) {
						mPlugin.getLogger().warning("Boss " + mIdentityTag + " is missing but still registered as an active boss. Unloading...");
						this.cancel();
						BossManager mgr = BossManager.getInstance();
						if (mgr != null) {
							BossManager.getInstance().unload(mBoss, false);
						}
						// Just in case for some reason the boss is no longer registered with the manager...
						unload();
						return;
					}
				}

				/* Don't run abilities if players aren't present */
				if (detectionRange > 0 && BossUtils.getPlayersInRangeForHealthScaling(mBoss, detectionRange) < 1) {
					return;
				}

				if (mPassiveSpells != null && !EntityUtils.isSilenced(mBoss)) {
					for (Spell spell : mPassiveSpells) {
						spell.run();
					}
				}
			}
		};
		mTaskPassive.runTaskTimer(mPlugin, 1, passiveIntervalTicks);

		mTaskActive = new BukkitRunnable() {
			private boolean mDisabled = true;
			private int mMissingTicks = 0;

			@Override
			public void run() {
				mNextActiveTimer -= 2;
				mMissingTicks += 2;

				if (mNextActiveTimer > 0) {
					// Still waiting for the current spell to finish
					return;
				}

				if (mMissingTicks > 100) {
					mMissingTicks = 0;
					/* Check if somehow the boss entity is missing even though this is still running */
					if (isBossMissing()) {
						mPlugin.getLogger().warning("Boss " + mIdentityTag + " is missing but still registered as an active boss. Unloading...");
						this.cancel();
						BossManager mgr = BossManager.getInstance();
						if (mgr != null) {
							BossManager.getInstance().unload(mBoss, false);
						}
						// Just in case for some reason the boss is no longer registered with the manager...
						unload();
						return;
					}
				}

				/* Don't progress if players aren't present */
				if (detectionRange > 0 && BossUtils.getPlayersInRangeForHealthScaling(mBoss, detectionRange) < 1) {
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

				if (mActiveSpells != null && !EntityUtils.isSilenced(mBoss)) {
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
		mTaskActive.runTaskTimer(mPlugin, spellDelay, 2L);
	}

	public void forceCastSpell(Class<? extends Spell> spell) {
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

	public String getIdentityTag() {
		return mIdentityTag;
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
	 * Boss damaged another entity, includes projectiles
	 */
	public void bossDamagedEntity(EntityDamageByEntityEvent event) {

	}

	/*
	 * Boss was damaged
	 */
	public void bossDamagedByEntity(EntityDamageByEntityEvent event) {

	}

	/*
	 * Boss shot a projectile
	 */
	public void bossLaunchedProjectile(ProjectileLaunchEvent event) {

	}

	/*
	 * Boss-shot projectile hit something
	 */
	public void bossProjectileHit(ProjectileHitEvent event) {

	}

	/*
	 * Boss gets hit by a projectile
	 */
	public void bossHitByProjectile(ProjectileHitEvent event) {

	}

	/*
	 * Boss hit by area effect cloud
	 */
	public void areaEffectAppliedToBoss(AreaEffectCloudApplyEvent event) {

	}

	/*
	 * Boss-shot projectile hit something
	 */
	public void splashPotionAppliedToBoss(PotionSplashEvent event) {

	}

	public void entityPotionEffectEvent(EntityPotionEffectEvent event) {

	}

	public void bossCastAbility(SpellCastEvent event) {

	}

	public void bossPathfind(EntityPathfindEvent event) {

	}

	public void bossChangedTarget(EntityTargetEvent event) {

	}

	public void customEffectAppliedToBoss(CustomEffectApplyEvent event) {

	}

	/*
	 * Boss was stunned by a player. Mobs with the "Boss" tag can't be stunned
	 */
	public void bossStunned() {
		if (mActiveSpells != null) {
			mActiveSpells.cancelAll();
		}
	}

	/*
	 * Boss was confused by a player. Mobs with the "Boss" tag can't be confused
	 */
	public void bossConfused() {
		if (mActiveSpells != null) {
			mActiveSpells.cancelAll();
		}
	}

	/*
	 * Boss was silenced by a player. Mobs with the "Boss" tag can't be silenced
	 */
	public void bossSilenced() {
		if (mActiveSpells != null) {
			mActiveSpells.cancelAll();
		}
	}

	/*
	 * Called only the first time the boss is summoned into the world
	 *
	 * Useful to set the bosses health / armor / etc. based on # of players
	 */
	public void init() {

	}

	/*
	 * Called when the boss dies
	 *
	 * Useful to use setblock or a command to trigger post-fight logic
	 */
	public void death(EntityDeathEvent event) {

	}

	/*
	 * Called when nearby enemy dies within 12 blocks
	 *
	 * For performance reasons any boss that uses this MUST also override
	 * hasNearbyEntityDeathTrigger to return true
	 */
	public void nearbyEntityDeath(EntityDeathEvent event) {}

	public boolean hasNearbyEntityDeathTrigger() {
		return false;
	}

	/*
	 * Called when a player breaks a block within 60 blocks
	 *
	 * For performance reasons any boss that uses this MUST also override
	 * hasNearbyBlockBreakTrigger to return true
	 *
	 * WARNING: VERY PERFORMANCE INTENSIVE
	 */
	public void nearbyBlockBreak(BlockBreakEvent event) {}

	public boolean hasNearbyBlockBreakTrigger() {
		return false;
	}

	/*
	 * Called when a player dies within 75 blocks
	 *
	 * For performance reasons any boss that uses this MUST also override
	 * hasNearbyPlayerDeathTrigger to return true
	 *
	 */
	public void nearbyPlayerDeath(PlayerDeathEvent event) {}

	public boolean hasNearbyPlayerDeathTrigger() {
		return false;
	}


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
		/* Even if we unload twice, really cancel these tasks */
		if (mTaskPassive != null && !mTaskPassive.isCancelled()) {
			mTaskPassive.cancel();
		}
		if (mTaskActive != null && !mTaskActive.isCancelled()) {
			mTaskActive.cancel();
		}

		/* Make sure we don't accidentally call the main unload sequence twice */
		if (!mUnloaded) {
			mUnloaded = true;

			if (mActiveSpells != null) {
				mActiveSpells.cancelAll();
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

	/* Check if somehow the boss entity is missing even though this is still running */
	private boolean isBossMissing() {
		Location bossLoc = mBoss.getLocation();
		for (Entity entity : bossLoc.getWorld().getNearbyEntities(bossLoc, 4, 4, 4)) {
			if (entity.getUniqueId().equals(mBoss.getUniqueId())) {
				return false;
			}
		}
		return true;
	}
}
