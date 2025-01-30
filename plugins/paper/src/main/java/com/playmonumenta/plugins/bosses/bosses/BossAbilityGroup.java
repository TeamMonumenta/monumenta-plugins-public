package com.playmonumenta.plugins.bosses.bosses;

import com.destroystokyo.paper.event.entity.EntityPathfindEvent;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.events.SpellCastEvent;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.chunk.ChunkManager;
import com.playmonumenta.plugins.events.CustomEffectApplyEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;


public abstract class BossAbilityGroup {
	public static final int PASSIVE_RUN_INTERVAL_DEFAULT = Constants.QUARTER_TICKS_PER_SECOND;
	public final LivingEntity mBoss;
	protected final Plugin mPlugin;
	private final String mIdentityTag;

	private @Nullable BossBarManager mBossBar;
	public SpellManager mActiveSpells;
	private List<Spell> mPassiveSpells;
	private boolean mPreventSameSpellTwiceInARow;
	private @Nullable BukkitRunnable mTaskPassive = null;
	private @Nullable BukkitRunnable mTaskActive = null;
	private boolean mUnloaded = false;
	private int mNextActiveTimer = 0;
	public boolean mDead = false;

	protected BossAbilityGroup(Plugin plugin, String identityTag, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
		mIdentityTag = identityTag;
		mBoss.addScoreboardTag(mIdentityTag);
		mActiveSpells = SpellManager.EMPTY;
		mPassiveSpells = Collections.emptyList();
	}

	public void changePhase(SpellManager activeSpells, List<Spell> passiveSpells,
							@Nullable Consumer<LivingEntity> phaseAction) {
		if (phaseAction != null) {
			phaseAction.accept(mBoss);
		}

		mActiveSpells.cancelAll();
		mActiveSpells = activeSpells;
		mPassiveSpells = passiveSpells;
		MMLog.fine("Changed phase for " + mIdentityTag + ". Boss's health is currently at " +
			100 * mBoss.getHealth() / EntityUtils.getMaxHealth(mBoss) + "%. " + mActiveSpells.toString());
	}

	public void changePassivePhase(List<Spell> passiveSpells) {
		mPassiveSpells = passiveSpells;
	}

	public void constructBoss(BossAbilityGroup this, Spell activeSpell, int detectionRange) {
		constructBoss(activeSpell, detectionRange, null);
	}

	public void constructBoss(BossAbilityGroup this, Spell activeSpell, int detectionRange,
							  @Nullable BossBarManager bossBar) {
		constructBoss(activeSpell, detectionRange, bossBar, 100);
	}

	public void constructBoss(BossAbilityGroup this, Spell activeSpell, int detectionRange,
							  @Nullable BossBarManager bossBar, long spellDelay) {
		constructBoss(List.of(activeSpell), Collections.emptyList(), detectionRange, bossBar, spellDelay);
	}

	public void constructBoss(BossAbilityGroup this, List<Spell> activeSpells, List<Spell> passiveSpells,
							  int detectionRange, @Nullable BossBarManager bossBar, long spellDelay) {
		constructBoss(new SpellManager(activeSpells), passiveSpells, detectionRange, bossBar, spellDelay);
	}

	public void constructBoss(BossAbilityGroup this,
							  SpellManager activeSpells, List<Spell> passiveSpells, int detectionRange,
							  @Nullable BossBarManager bossBar) {
		constructBoss(activeSpells, passiveSpells, detectionRange, bossBar, 100);
	}

	public void constructBoss(BossAbilityGroup this,
							  SpellManager activeSpells, List<Spell> passiveSpells, int detectionRange,
							  @Nullable BossBarManager bossBar, long spellDelay) {
		constructBoss(activeSpells, passiveSpells, detectionRange, bossBar, spellDelay, PASSIVE_RUN_INTERVAL_DEFAULT);
	}

	public void constructBoss(BossAbilityGroup this,
							  SpellManager activeSpells, List<Spell> passiveSpells, int detectionRange,
							  @Nullable BossBarManager bossBar, long spellDelay, long passiveIntervalTicks) {
		constructBoss(activeSpells, passiveSpells, detectionRange, bossBar, spellDelay, passiveIntervalTicks, false);
	}

	/* If detectionRange <= 0, will always run regardless of whether players are nearby */
	public void constructBoss(BossAbilityGroup this, SpellManager activeSpells, List<Spell> passiveSpells,
							  int detectionRange, @Nullable BossBarManager bossBar, long spellDelay,
							  long passiveIntervalTicks, boolean preventSameSpellTwiceInARow) {
		mBossBar = bossBar;
		mActiveSpells = activeSpells;
		mPassiveSpells = passiveSpells;
		mPreventSameSpellTwiceInARow = preventSameSpellTwiceInARow;

		mTaskPassive = new BukkitRunnable() {
			private long mMissingTicks = 0;

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
						handleMissingBoss();
						cancel();
						return;
					}
				}

				/* Don't run abilities if players aren't present */
				if (detectionRange > 0 && PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true).isEmpty()) {
					return;
				}

				boolean silenced = EntityUtils.isSilenced(mBoss);
				if (mPassiveSpells != null) {
					for (Spell spell : mPassiveSpells) {
						if (!silenced || spell.bypassSilence()) {
							spell.run();
						}
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
						handleMissingBoss();
						cancel();
						return;
					}
				}

				/* Don't progress if players aren't present */
				if (detectionRange > 0 && PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true).isEmpty()) {
					if (!mDisabled) {
						/* Cancel all the spells just in case they were activated */
						mDisabled = true;

						mActiveSpells.cancelAll();
					}
					return;
				}

				/* Some spells might have been run - so when this next deactivates they need to be cancelled */
				mDisabled = false;

				if (!EntityUtils.isSilenced(mBoss)) {
					// Run the next spell and store how long before the next spell can run
					mNextActiveTimer = mActiveSpells.runNextSpell(mPreventSameSpellTwiceInARow);

					// The event goes after the spell casts because Kaul's abilities take place where he previously was.
					Spell spell = mActiveSpells.getLastCastedSpell();
					if (spell != null) {
						SpellCastEvent event = new SpellCastEvent(mBoss, BossAbilityGroup.this, spell);
						Bukkit.getPluginManager().callEvent(event);
					}
				}
			}
		};
		mTaskActive.runTaskTimer(mPlugin, spellDelay, 2L);
	}

	private void handleMissingBoss() {
		MMLog.warning("Boss " + mIdentityTag + " is missing" + (mBoss.isValid() ? " (but valid)" : "") +
			" but still registered as an active boss. It has been removed and untracked via the fallback system.");
		BossManager.getInstance().unload(mBoss, true);
		// usb: this is triggering when it shouldn't, don't remove bosses if they might be persistant
		// mBoss.remove();
	}

	public void forceCastRandomSpell() {
		List<Spell> spells = mActiveSpells.getSpells();
		if (!spells.isEmpty()) {
			Spell spell = spells.get(FastUtils.RANDOM.nextInt(spells.size()));
			forceCastSpell(spell.getClass());
		}
	}

	public void forceCastSpell(Class<? extends Spell> spell) {
		mNextActiveTimer = mActiveSpells.forceCastSpell(spell);
		Spell sp = mActiveSpells.getLastCastedSpell();
		if (sp != null) {
			SpellCastEvent event = new SpellCastEvent(mBoss, this, sp);
			Bukkit.getPluginManager().callEvent(event);
		} else {
			MMLog.severe("Warning: Boss '" + mIdentityTag + "' attempted to force cast '" + spell.toString() +
				"' but boss does not have this spell!");
		}
	}

	public String getIdentityTag() {
		return mIdentityTag;
	}

	public @Nullable BossBarManager getBossBar() {
		return mBossBar;
	}

	/********************************************************************************
	 * Event Handlers
	 *******************************************************************************/

	public List<Spell> getPassives() {
		return mPassiveSpells;
	}

	public List<Spell> getActiveSpells() {
		return mActiveSpells.getSpells();
	}

	/*
	 * Boss damaged an entity
	 */
	public void onDamage(DamageEvent event, LivingEntity damagee) {

	}

	/*
	 * Boss was hurt, with or without an entity
	 */
	public void onHurt(DamageEvent event) {

	}

	/*
	 * Boss was hurt by an entity
	 */
	public void onHurtByEntity(DamageEvent event, Entity damager) {

	}

	/*
	 * Boss was hurt by an entity with a source (source is usually the same as the entity)
	 */
	public void onHurtByEntityWithSource(DamageEvent event, Entity damager, LivingEntity source) {

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

	/*
	 *  Boss throws a splash potion
	 */
	public void bossSplashPotion(PotionSplashEvent event) {

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

	public void bossExploded(EntityExplodeEvent event) {

	}

	// Only acts on fire applied by the plugin
	public void bossIgnited(int ticks) {

	}

	public void bossPassengerHurt(DamageEvent event) {

	}

	/*
	 * Boss was stunned by a player. Mobs with the "Boss" tag can't be stunned
	 */
	public void bossStunned() {
		mActiveSpells.cancelAll();
	}

	/*
	 * Boss was silenced by a player. Mobs with the "Boss" tag can't be silenced
	 */
	public void bossSilenced() {
		mActiveSpells.cancelAll();
	}

	public void bossKnockedAway(float speed) {

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
	public void death(@Nullable EntityDeathEvent event) {

	}

	/*
	 * Called when nearby enemy dies within 12 blocks
	 *
	 * For performance reasons any boss that uses this MUST also override
	 * hasNearbyEntityDeathTrigger to return true
	 */
	public void nearbyEntityDeath(EntityDeathEvent event) {

	}

	public boolean hasNearbyEntityDeathTrigger() {
		return false;
	}

	public double maxEntityDeathRange() {
		return 0;
	}

	/*
	 * Called when a player breaks a block within 60 blocks
	 *
	 * For performance reasons any boss that uses this MUST also override
	 * hasNearbyBlockBreakTrigger to return true
	 *
	 * WARNING: VERY PERFORMANCE INTENSIVE
	 */
	public void nearbyBlockBreak(BlockBreakEvent event) {

	}

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
	public void nearbyPlayerDeath(PlayerDeathEvent event) {

	}

	public boolean hasNearbyPlayerDeathTrigger() {
		return false;
	}


	/*
	 * Called when the mob is unloading and we need to save its metadata
	 *
	 * Needed whenever the boss needs more parameters to instantiate than just
	 * the boss mob itself (tele to spawn location, end location to set block, etc.)
	 */
	public @Nullable String serialize() {
		return null;
	}

	public void saveData() {
		String content = serialize();
		if (content != null && !content.isEmpty()) {
			try {
				SerializationUtils.storeDataOnEntity(mBoss, mIdentityTag, content);
			} catch (Exception ex) {
				MMLog.severe("Failed to save data to entity: ", ex);
			}
		}
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

			mActiveSpells.cancelAll();

			if (mBossBar != null) {
				mBossBar.remove();
			}

			// In a future minecraft version, NBT saving when the entity is unloading and invalid may not work properly
			if (mBoss.getHealth() > 0) {
				saveData();
			}
		}
	}

	/* Check if somehow the boss entity is missing even though this is still running */
	private boolean isBossMissing() {
		Location bossLoc = mBoss.getLocation();
		if (!bossLoc.isWorldLoaded() || !ChunkManager.isChunkLoaded(bossLoc)) {
			return true;
		}
		for (Entity entity : bossLoc.getWorld().getNearbyEntities(bossLoc, 4, 4, 4)) {
			if (entity.getUniqueId().equals(mBoss.getUniqueId())) {
				return false;
			}
		}
		return true;
	}

	public boolean hasRunningSpell() {
		return !mActiveSpells.isEmpty() && mActiveSpells.getSpells().stream().anyMatch(Spell::isRunning);
	}

	public final boolean hasRunningSpellOfType(Class<?>... spellTypes) {
		Predicate<Spell> isOfArgumentType = s -> Arrays.stream(spellTypes).anyMatch(type -> type.isInstance(s));
		return !mActiveSpells.isEmpty() && mActiveSpells.getSpells().stream().anyMatch(s -> s.isRunning() && isOfArgumentType.test(s));
	}

	public final void triggerOnSpells(Consumer<Spell> action) {
		mActiveSpells.getSpells().forEach(action);
		mPassiveSpells.forEach(action);
	}

}
