package com.playmonumenta.plugins.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.PotionEffectApplyEvent;
import com.playmonumenta.plugins.itemstats.enchantments.Aptitude;
import com.playmonumenta.plugins.itemstats.enchantments.Ineptitude;
import com.playmonumenta.plugins.itemstats.infusions.Epoch;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.Collection;


public abstract class Ability {
	protected final Plugin mPlugin;
	public final AbilityInfo mInfo;
	protected final @Nullable Player mPlayer;
	private @Nullable Integer mScore = null;
	public ItemStack mDisplayItem;

	public Ability(Plugin plugin, @Nullable Player player, @Nullable String displayName) {
		mPlugin = plugin;
		mPlayer = player;
		mInfo = new AbilityInfo();
		mInfo.mDisplayName = displayName;
	}

	public @Nullable String getDisplayName() {
		return mInfo.mDisplayName;
	}

	public @Nullable String getScoreboard() {
		return mInfo.mScoreboardId;
	}

	/**
	 * This is used when the ability is casted manually when its
	 * AbilityTrigger (Right Click/Left Click), along with whatever
	 * runCheck() may contain, is correct.
	 */
	public void cast(Action trigger) {

	}

	/**
	 * Gets the AbilityInfo object, which contains the small data side of the ability itself, and is required to have for any ability.
	 * @return the AbilityInfo object. Never null.
	 */
	public AbilityInfo getInfo() {
		return mInfo;
	}

	/**
	 * A custom check if additional checks are needed. For example, if you need to check if a player is looking up or down.
	 *
	 * @return true or false
	 */
	public boolean runCheck() {
		return true;
	}

	/**
	 * Priority order in event handling, with lower values being handled earlier than higher ones.
	 * <p>
	 * Some references:
	 * <ul>
	 * <li>Default is 1000
	 * <li>Delve modifiers are around 2000
	 * <li>Abilities that need a final damage amount are around 5000
	 * <li>Lifeline abilities are around 10000
	 * </ul>
	 *
	 * @return the priority order
	 */
	public double getPriorityAmount() {
		return 1000;
	}

	public boolean isOnCooldown() {
		AbilityInfo abilityInfo = getInfo();
		if (!abilityInfo.mIgnoreCooldown) {
			// If not ignoring cooldowns, go by timer
			return isTimerActive();
		}
		// Otherwise, never say on cooldown
		return false;
	}

	// Whether skill is on cooldown, without factoring in mIgnoreCooldown
	public boolean isTimerActive() {
		@Nullable AbilityInfo abilityInfo = getInfo();
		if (abilityInfo != null) {
			@Nullable ClassAbility spell = abilityInfo.mLinkedSpell;
			if (spell != null) {
				return isTimerActive(spell);
			}
		}
		return false;
	}

	public boolean isTimerActive(ClassAbility spell) {
		return mPlayer != null && mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), spell);
	}

	public int getModifiedCooldown(int baseCooldown) {
		//Epoch and Ability Enchantment implementation
		//Percents are negative so (1 + percent) is between 0 and 1 in most cases
		double epochPercent = Epoch.getCooldownPercentage(mPlugin, mPlayer);
		double aptitudePercent = Aptitude.getCooldownPercentage(mPlugin, mPlayer);
		double ineptitudePercent = Ineptitude.getCooldownPercentage(mPlugin, mPlayer);

		return (int) (baseCooldown * (1 + epochPercent) * (1 + aptitudePercent) * (1 + ineptitudePercent));
	}

	/**
	 * This ability's cooldown modified by enchantments of items worn by the player
	 */
	public int getModifiedCooldown() {
		return getModifiedCooldown(getInfo().mCooldown);
	}

	public void putOnCooldown() {
		if (mPlayer == null) {
			return;
		}
		AbilityInfo info = getInfo();
		if (info.mLinkedSpell != null) {
			if (!mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), info.mLinkedSpell)) {
				mPlugin.mTimers.addCooldown(mPlayer, info.mLinkedSpell, getModifiedCooldown());
				PlayerUtils.callAbilityCastEvent(mPlayer, info.mLinkedSpell);
			}
		}
	}

	/**
	 * A combination of both runCheck and isOnCooldown.
	 * @return true or false
	 */
	public final boolean canCast() {
		return mPlayer != null && runCheck() && !isOnCooldown() && mPlayer.getGameMode() != GameMode.SPECTATOR;
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

	public boolean playerShotArrowEvent(AbstractArrow arrow) {
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
		if (mPlayer != null) {
			return mPlayer.getLocation();
		}
		return null;
	}

	public void projectileHitEvent(ProjectileHitEvent event, Projectile proj) {

	}

	public void playerExtendedSneakEvent() {

	}

	public void entityTargetLivingEntityEvent(EntityTargetLivingEntityEvent event) {

	}

	// Do not use this for regular abilities
	public void playerDealtUnregisteredCustomDamageEvent(DamageEvent event) {

	}

	public void potionApplyEvent(PotionEffectApplyEvent event) {

	}

	public void playerDeathEvent(PlayerDeathEvent event) {

	}

	public void playerAnimationEvent(PlayerAnimationEvent event) {

	}

	public void playerItemConsumeEvent(PlayerItemConsumeEvent event) {

	}

	public void playerItemDamageEvent(PlayerItemDamageEvent event) {

	}

	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {

	}

	public void playerQuitEvent(PlayerQuitEvent event) {

	}

	//---------------------------------------------------------------------------------------------------------------

	//Other
	//---------------------------------------------------------------------------------------------------------------

	public void setupClassPotionEffects() {

	}

	// Every 5 ticks - 4 times a second.
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {

	}

	//---------------------------------------------------------------------------------------------------------------
	/*
	 * By default, players can only use abilities if the ability has a scoreboard defined and it is nonzero
	 * For different conditions, an ability must override this method
	 */
	public boolean canUse(Player player) {
		return mInfo.mScoreboardId != null && ScoreboardUtils.getScoreboardValue(player, mInfo.mScoreboardId).orElse(0) > 0;
	}

	/*
	 * For performance, this caches the first scoreboard lookup for future use
	 */
	public int getAbilityScore(Ability this) {
		AbilityInfo info = mInfo;
		if (mPlayer != null && info != null && info.mScoreboardId != null) {
			if (mScore == null) {
				mScore = ScoreboardUtils.getScoreboardValue(mPlayer, info.mScoreboardId).orElse(0);
			}
			return mScore;
		}
		return 0;
	}

	public @Nullable Component getLevelHover(boolean useShorthand) {
		return mInfo.getLevelHover(getAbilityScore(), useShorthand);
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
}
