package com.playmonumenta.plugins.abilities;

import java.util.Collection;
import java.util.Random;

import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LingeringPotion;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SplashPotion;
import org.bukkit.entity.ThrownPotion;
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
import org.bukkit.inventory.ItemStack;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.events.BossAbilityDamageEvent;
import com.playmonumenta.plugins.events.CustomDamageEvent;
import com.playmonumenta.plugins.events.PotionEffectApplyEvent;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

public abstract class Ability {
	protected final Plugin mPlugin;
	protected final World mWorld;
	protected final Random mRandom;
	protected final AbilityInfo mInfo;
	protected final Player mPlayer;
	private Integer mScore = null;

	public Ability(Plugin plugin, World world, Random random, Player player) {
		mPlugin = plugin;
		mWorld = world;
		mRandom = random;
		mPlayer = player;
		mInfo = new AbilityInfo();
	}

	/**
	 * This is used when the ability is casted manually when its
	 * AbilityTrigger (Right Click/Left Click), along with whatever
	 * runCheck() may contain, is correct.
	 */
	public void cast() { }

	/**
	 * Gets the AbilityInfo object, which contains the small data side of the ability itself, and is required to have for any ability.
	 * @return the AbilityInfo object, if one exists. If not, it returns null.
	 */
	public AbilityInfo getInfo() {
		return mInfo;
	}

	/**
	 * A custom check if additional checks are needed. For example, if you need to check if a player is looking up or down.
	 * @param player
	 * @return true or false
	 */
	public boolean runCheck() {
		return true;
	}

	public boolean isOnCooldown() {
		AbilityInfo info = getInfo();
		if (info.linkedSpell != null && !info.ignoreCooldown) {
			if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), info.linkedSpell)) {
				return true;
			}
		}
		return false;
	}

	public void putOnCooldown() {
		AbilityInfo info = getInfo();
		if (info.linkedSpell != null) {
			if (!mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), info.linkedSpell)) {
				mPlugin.mTimers.AddCooldown(mPlayer.getUniqueId(), info.linkedSpell, info.cooldown);
				PlayerUtils.callAbilityCastEvent(mPlayer, info.linkedSpell);
			}
		}
	}

	/**
	 * A combination of both runCheck and isOnCooldown.
	 * @param player
	 * @return
	 */
	public final boolean canCast() {
		if (runCheck() && !isOnCooldown()) {
			return true;
		}
		return false;
	}

	//Events
	//---------------------------------------------------------------------------------------------------------------

	public boolean AbilityCastEvent(AbilityCastEvent event) {
		return true;
	}

	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		return true;
	}

	public boolean PlayerDamagedEvent(EntityDamageEvent event) {
		return true;
	}

	public boolean PlayerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		return true;
	}

	public boolean PlayerDamagedByProjectileEvent(EntityDamageByEntityEvent event) {
		return true;
	}

	public boolean PlayerHitByProjectileEvent(ProjectileHitEvent event) {
		return true;
	}

	public boolean PlayerCombustByEntityEvent(EntityCombustByEntityEvent event) {
		return true;
	}

	public boolean LivingEntityShotByPlayerEvent(Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		return true;
	}

	public boolean PlayerShotArrowEvent(Arrow arrow) {
		return true;
	}

	public boolean PlayerThrewSplashPotionEvent(SplashPotion potion) {
		return true;
	}

	public boolean PlayerThrewLingeringPotionEvent(LingeringPotion potion) {
		return true;
	}

	public boolean PlayerSplashedByPotionEvent(Collection<LivingEntity> affectedEntities,
	                                           ThrownPotion potion, PotionSplashEvent event) {
		return true;
	}

	// Called when entities are hit by a potion a player threw
	public boolean PlayerSplashPotionEvent(Collection<LivingEntity> affectedEntities,
	                                       ThrownPotion potion, PotionSplashEvent event) {
		return true;
	}

	public void EntityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) { }

	public void EntityDeathRadiusEvent(EntityDeathEvent event, boolean shouldGenDrops) { }

	public double EntityDeathRadius() {
		return 0;
	}

	public void ProjectileHitEvent(ProjectileHitEvent event, Arrow arrow) { }

	public void PlayerItemHeldEvent(ItemStack mainHand, ItemStack offHand) { }

	public void PlayerExtendedSneakEvent() { }

	public void PlayerDealtCustomDamageEvent(CustomDamageEvent event) { }

	public void EntityTargetLivingEntityEvent(EntityTargetLivingEntityEvent event) { }

	public void PotionApplyEvent(PotionEffectApplyEvent event) { }

	public void PlayerDeathEvent(PlayerDeathEvent event) { }

	public void PlayerAnimationEvent(PlayerAnimationEvent event) { }

	public void PlayerDamagedByBossEvent(BossAbilityDamageEvent event) { }

	public void PlayerItemConsumeEvent(PlayerItemConsumeEvent event) { }

	public void PlayerItemDamageEvent(PlayerItemDamageEvent event) { }

	//---------------------------------------------------------------------------------------------------------------

	//Other
	//---------------------------------------------------------------------------------------------------------------

	public void setupClassPotionEffects() { }

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

	public void PeriodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {}

	//---------------------------------------------------------------------------------------------------------------
	/*
	 * By default, players can only use abilities if the ability has a scoreboard defined and it is nonzero
	 * For different conditions, an ability must override this method
	 */
	public boolean canUse(Player player) {
		if (mInfo.scoreboardId != null && ScoreboardUtils.getScoreboardValue(player, mInfo.scoreboardId) > 0) {
			return true;
		}
		return false;
	}

	/*
	 * For performance, this caches the first scoreboard lookup for future use
	 */
	public int getAbilityScore() {
		AbilityInfo info = getInfo();
		if (mPlayer != null && info.scoreboardId != null) {
			if (mScore == null) {
				mScore = new Integer(ScoreboardUtils.getScoreboardValue(mPlayer, info.scoreboardId));
			}
			return mScore;
		}
		return 0;
	}

	public JsonObject getAsJsonObject() {
		JsonObject obj = mInfo.getAsJsonObject();
		obj.addProperty("score", getAbilityScore());
		return obj;
	}
}
