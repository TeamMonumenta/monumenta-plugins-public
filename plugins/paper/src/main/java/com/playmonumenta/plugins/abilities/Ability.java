package com.playmonumenta.plugins.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.AbilityCooldownDecrease;
import com.playmonumenta.plugins.effects.AbilityCooldownIncrease;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.PotionEffectApplyEvent;
import com.playmonumenta.plugins.itemstats.enchantments.Aptitude;
import com.playmonumenta.plugins.itemstats.enchantments.Ineptitude;
import com.playmonumenta.plugins.itemstats.infusions.Epoch;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NavigableSet;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

public abstract class Ability {
	protected final Plugin mPlugin;
	protected final AbilityInfo<?> mInfo;
	protected final Player mPlayer;
	private @Nullable Integer mScore = null;

	protected List<? extends AbilityTriggerInfo<?>> mCustomTriggers;

	private final List<BukkitTask> mCancelOnDeath = new ArrayList<>(0);

	public Ability(Plugin plugin, Player player, AbilityInfo<?> info) {
		mPlugin = plugin;
		mPlayer = player;
		mInfo = info;
		mCustomTriggers = info.getTriggers().stream()
			                  .map(ti -> ti.withCustomTrigger(info, player))
			                  .toList();
	}

	/**
	 * Gets the AbilityInfo object, which contains the small data side of the ability itself, and is required to have for any ability.
	 *
	 * @return the AbilityInfo object. Never null.
	 */
	public AbilityInfo<?> getInfo() {
		return mInfo;
	}

	public List<? extends AbilityTriggerInfo<?>> getCustomTriggers() {
		return mCustomTriggers;
	}

	// TODO useless?

	/**
	 * Checks if the player still has this ability.
	 */
	public boolean playerHasAbility() {
		return mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(mPlayer, getClass()) != null;
	}

	/**
	 * Mark a task to be cancelled on death. Note that the task is not cancelled when the player removes the ability or gets silenced.
	 */
	protected void cancelOnDeath(BukkitTask task) {
		mCancelOnDeath.add(task);
	}

	public boolean isOnCooldown() {
		@Nullable AbilityInfo<?> abilityInfo = getInfo();
		if (abilityInfo != null) {
			@Nullable ClassAbility spell = abilityInfo.getLinkedSpell();
			if (spell != null) {
				return isOnCooldown(spell);
			}
		}
		return false;
	}

	private boolean isOnCooldown(ClassAbility spell) {
		return mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), spell);
	}

	public int getModifiedCooldown(int baseCooldown) {
		//Epoch and Ability Enchantment implementation
		//Percents are negative so (1 + percent) is between 0 and 1 in most cases
		double epochPercent = Epoch.getCooldownPercentage(mPlugin, mPlayer);
		double aptitudePercent = Aptitude.getCooldownPercentage(mPlugin, mPlayer);
		double ineptitudePercent = Ineptitude.getCooldownPercentage(mPlugin, mPlayer);

		//Potion effects
		double effectPercent = 0;

		NavigableSet<AbilityCooldownIncrease> effInc = Plugin.getInstance().mEffectManager.getEffects(mPlayer, AbilityCooldownIncrease.class);
		if (effInc != null && !effInc.isEmpty()) {
			Effect inc = effInc.last();
			effectPercent += inc.getMagnitude(); // this is always positive
		}

		NavigableSet<AbilityCooldownDecrease> effDec = Plugin.getInstance().mEffectManager.getEffects(mPlayer, AbilityCooldownDecrease.class);
		if (effDec != null && !effDec.isEmpty()) {
			Effect dec = effDec.last();
			effectPercent -= dec.getMagnitude(); // this is always positive
		}

		return (int) (baseCooldown * (1 + epochPercent) * (1 + aptitudePercent) * (1 + ineptitudePercent) * (1 + effectPercent));
	}

	/**
	 * This ability's cooldown modified by enchantments of items worn by the player
	 */
	public int getModifiedCooldown() {
		return getModifiedCooldown(getInfo().getModifiedCooldown(mPlayer, getAbilityScore()));
	}

	public void putOnCooldown() {
		putOnCooldown(getModifiedCooldown());
	}

	public void putOnCooldown(int cooldown) {
		AbilityInfo<?> info = getInfo();
		if (info.getLinkedSpell() != null) {
			if (!mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), info.getLinkedSpell())) {
				mPlugin.mTimers.addCooldown(mPlayer, info.getLinkedSpell(), cooldown);
				PlayerUtils.callAbilityCastEvent(mPlayer, info.getLinkedSpell());
			}
		}
	}

	//Events
	//---------------------------------------------------------------------------------------------------------------

	public boolean blockBreakEvent(BlockBreakEvent event) {
		return true;
	}

	public boolean abilityCastEvent(AbilityCastEvent event) {
		return true;
	}

	/**
	 * Called when any kind of damage is dealt.
	 *
	 * @param event The damage event
	 * @param enemy The entity that was damaged
	 * @return Whether this ability should no longer receive damage event in the same tick.
	 * For abilities with no cooldown or ignoring cooldown, this should generally return true unless the code prevents recursion.
	 */
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		return false;
	}

	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {

	}

	public void onHurtFatal(DamageEvent event) {

	}

	public boolean playerHitByProjectileEvent(ProjectileHitEvent event) {
		return true;
	}

	public boolean playerCombustByEntityEvent(EntityCombustByEntityEvent event) {
		return true;
	}

	// Specifically called for: AbstractArrows and Snowballs
	public boolean playerShotProjectileEvent(Projectile projectile) {
		return true;
	}

	public boolean playerConsumeArrowEvent() {
		return true;
	}

	public boolean playerThrewSplashPotionEvent(ThrownPotion potion) {
		return true;
	}

	public boolean playerThrewLingeringPotionEvent(ThrownPotion potion) {
		return true;
	}

	public boolean playerSplashedByPotionEvent(Collection<LivingEntity> affectedEntities,
											   ThrownPotion potion, PotionSplashEvent event) {
		return true;
	}

	// Called when entities are hit by a potion a player threw
	public boolean playerSplashPotionEvent(Collection<LivingEntity> affectedEntities,
										   ThrownPotion potion, PotionSplashEvent event) {
		return true;
	}

	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {

	}

	public void entityDeathRadiusEvent(EntityDeathEvent event, boolean shouldGenDrops) {

	}

	public double entityDeathRadius() {
		return 0;
	}

	public @Nullable Location entityDeathRadiusCenterLocation() {
		return mPlayer.getLocation();
	}

	public void projectileHitEvent(ProjectileHitEvent event, Projectile proj) {

	}

	public void playerExtendedSneakEvent() {

	}

	public void entityTargetLivingEntityEvent(EntityTargetLivingEntityEvent event) {

	}

	public void potionApplyEvent(PotionEffectApplyEvent event) {

	}

	final void playerDeathEventFinal(PlayerDeathEvent event) {
		for (BukkitTask activeTask : mCancelOnDeath) {
			activeTask.cancel();
		}
		playerDeathEvent(event);
	}

	public void playerDeathEvent(PlayerDeathEvent event) {

	}

	public void playerAnimationEvent(PlayerAnimationEvent event) {

	}

	public void playerItemConsumeEvent(PlayerItemConsumeEvent event) {

	}

	public void playerItemDamageEvent(PlayerItemDamageEvent event) {

	}

	public void playerRegainHealthEvent(EntityRegainHealthEvent event) {

	}

	public void playerQuitEvent(PlayerQuitEvent event) {

	}

	public void playerTeleportEvent(PlayerTeleportEvent event) {

	}

	public void blockWithShieldEvent() {

	}

	//---------------------------------------------------------------------------------------------------------------

	//Other
	//---------------------------------------------------------------------------------------------------------------

	public void setupClassPotionEffects() {

	}

	void periodicTriggerFinal(boolean twoHertz, boolean oneSecond, int ticks) {
		if (oneSecond) {
			mCancelOnDeath.removeIf(task -> !Bukkit.getScheduler().isQueued(task.getTaskId()));
		}
		periodicTrigger(twoHertz, oneSecond, ticks);
	}

	// Every 5 ticks - 4 times a second.
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {

	}

	//---------------------------------------------------------------------------------------------------------------

	/*
	 * For performance, this caches the first scoreboard lookup for future use
	 */
	public int getAbilityScore(Ability this) {
		AbilityInfo<?> info = mInfo;
		if (info.getScoreboard() != null) {
			if (mScore == null) {
				mScore = ScoreboardUtils.getScoreboardValue(mPlayer, info.getScoreboard()).orElse(0);
			}
			return mScore;
		}
		return 0;
	}

	public boolean isLevelOne() {
		return getAbilityScore() % 2 == 1;
	}

	public boolean isLevelTwo() {
		return getAbilityScore() % 2 == 0 && getAbilityScore() > 0;
	}

	public boolean isEnhanced() {
		return getAbilityScore() > 2 && ServerProperties.getAbilityEnhancementsEnabled(mPlayer);
	}

	public @Nullable Component getLevelHover(boolean useShorthand) {
		return mInfo.getLevelHover(mPlayer, getAbilityScore(), useShorthand, true);
	}

	@Override
	public String toString() {
		return String.format("%s: %d", this.getClass().getName().replaceAll("com.playmonumenta.plugins.abilities.", ""), getAbilityScore());
	}

	/*
	 * This is called when a player's class is refreshed REGARDLESS OF WHETHER THE PLAYER HAS
	 * THE ABILITY, so any effects of the ability that persist outside of the plugin, like
	 * attributes or tags, should be removed here.
	 *
	 * Treat this as a static method (i.e. use the player argument, not mPlayer nor any other
	 * instance variable) because it is called from the reference abilities list, not the
	 * actual ability object associated with the player.
	 */
	public void remove(Player player) {

	}

	/* When called, the ability is no longer applicable to the player and any active runnables should be cancelled */
	public void invalidate() {

	}

	/**
	 * Return the current mode of this ability, or null for default mode or no modes. This is used by the client mod to display different icons for different modes.
	 */
	public @Nullable String getMode() {
		return null;
	}

}
