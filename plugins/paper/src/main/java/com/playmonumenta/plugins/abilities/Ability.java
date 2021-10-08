package com.playmonumenta.plugins.abilities;

import java.util.Collection;

import org.bukkit.GameMode;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.enchantments.abilities.BaseAbilityEnchantment;
import com.playmonumenta.plugins.enchantments.infusions.delves.Epoch;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.events.CustomDamageEvent;
import com.playmonumenta.plugins.events.PotionEffectApplyEvent;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import net.kyori.adventure.text.Component;



public abstract class Ability {
	protected final Plugin mPlugin;
	public final AbilityInfo mInfo;
	protected final Player mPlayer;
	private Integer mScore = null;
	public ItemStack mDisplayItem;

	public Ability(Plugin plugin, @Nullable Player player, String displayName) {
		mPlugin = plugin;
		mPlayer = player;
		mInfo = new AbilityInfo();
		mInfo.mDisplayName = displayName;
	}

	public String getDisplayName() {
		return mInfo.mDisplayName;
	}

	public String getScoreboard() {
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
	 * @return true or false
	 */
	public boolean runCheck() {
		return true;
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

	public boolean isTimerActive(@NotNull ClassAbility spell) {
		return mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), spell);
	}

	public void putOnCooldown() {
		AbilityInfo info = getInfo();
		if (info.mLinkedSpell != null) {
			if (!mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), info.mLinkedSpell)) {
				//Epoch and Ability Enchantment implementation
				//Percents are negative so (1 + percent) is between 0 and 1 in most cases
				double epochPercent = Epoch.getCooldownPercentage(mPlugin, mPlayer);

				Class<? extends BaseAbilityEnchantment> abilityEnchantment = getCooldownEnchantment();
				double abilityEnchantmentPercent = 0;
				if (abilityEnchantment != null) {
					abilityEnchantmentPercent = mPlugin.mTrackingManager.mPlayers.getPlayerCustomEnchantLevel(mPlayer, abilityEnchantment) / 100.0;
				}

				int cooldown = (int) (info.mCooldown * (1 + epochPercent) * (1 + abilityEnchantmentPercent));

				mPlugin.mTimers.addCooldown(mPlayer, info.mLinkedSpell, cooldown);
				PlayerUtils.callAbilityCastEvent(mPlayer, info.mLinkedSpell);
			}
		}
	}

	/**
	 * A combination of both runCheck and isOnCooldown.
	 * @return true or false
	 */
	public final boolean canCast() {
		return runCheck() && !isOnCooldown() && mPlayer.getGameMode() != GameMode.SPECTATOR;
	}

	//Events
	//---------------------------------------------------------------------------------------------------------------

	public boolean onStealthAttack(EntityDamageByEntityEvent event) {
		return true;
	}

	public boolean blockBreakEvent(BlockBreakEvent event) {
		return true;
	}

	public boolean abilityCastEvent(AbilityCastEvent event) {
		return true;
	}

	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		return true;
	}

	public boolean playerDamagedEvent(EntityDamageEvent event) {
		return true;
	}

	public boolean playerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		return true;
	}

	public boolean playerDamagedByProjectileEvent(EntityDamageByEntityEvent event) {
		return true;
	}

	public boolean playerHitByProjectileEvent(ProjectileHitEvent event) {
		return true;
	}

	public boolean playerCombustByEntityEvent(EntityCombustByEntityEvent event) {
		return true;
	}

	public boolean livingEntityShotByPlayerEvent(Projectile proj, LivingEntity damagee, EntityDamageByEntityEvent event) {
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

	public void projectileHitEvent(ProjectileHitEvent event, Projectile proj) {

	}

	public void playerItemHeldEvent(ItemStack mainHand, ItemStack offHand) {

	}

	public void playerExtendedSneakEvent() {

	}

	public void playerDealtCustomDamageEvent(CustomDamageEvent event) {

	}

	public void entityTargetLivingEntityEvent(EntityTargetLivingEntityEvent event) {

	}

	// Do not use this for regular abilities
	public void playerDealtUnregisteredCustomDamageEvent(CustomDamageEvent event) {

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

	public boolean has1SecondTrigger() {
		return false;
	}

	public boolean has2SecondTrigger() {
		return false;
	}

	public boolean has40SecondTrigger() {
		return false;
	}

	public boolean has60SecondTrigger() {
		return false;
	}

	// Every 5 ticks - 4 times a second.
	// Same rate as BaseEnchantment
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {

	}

	//---------------------------------------------------------------------------------------------------------------
	/*
	 * By default, players can only use abilities if the ability has a scoreboard defined and it is nonzero
	 * For different conditions, an ability must override this method
	 */
	public boolean canUse(Player player) {
		return mInfo.mScoreboardId != null && ScoreboardUtils.getScoreboardValue(player, mInfo.mScoreboardId) > 0;
	}

	/*
	 * For performance, this caches the first scoreboard lookup for future use
	 */
	public int getAbilityScore() {
		AbilityInfo info = getInfo();
		if (mPlayer != null && info.mScoreboardId != null) {
			if (mScore == null) {
				mScore = ScoreboardUtils.getScoreboardValue(mPlayer, info.mScoreboardId);
			}
			return mScore;
		}
		return 0;
	}

	public Component getLevelHover(boolean useShorthand) {
		return mInfo.getLevelHover(getAbilityScore(), useShorthand);
	}

	public Class<? extends BaseAbilityEnchantment> getCooldownEnchantment() {
		return null;
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
