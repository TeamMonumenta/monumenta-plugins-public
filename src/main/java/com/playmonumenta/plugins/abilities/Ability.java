package com.playmonumenta.plugins.abilities;

import com.google.gson.JsonObject;

import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import java.util.Collection;
import java.util.Random;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.World;

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
	 * @return if the player managed to cast the spell successfully.
	 */
	public boolean cast() {
		return true;
	}

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
		if (info.linkedSpell != null) {
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
			}
		}
	}

	/**
	 * A combination of both runCheck and isOnCooldown.
	 * @param player
	 * @return
	 */
	public boolean canCast() {
		if (runCheck() && !isOnCooldown()) {
			return true;
		}
		return false;
	}

	//Events
	//---------------------------------------------------------------------------------------------------------------

	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		return true;
	}

	public boolean PlayerDamagedEvent(EntityDamageByEntityEvent event) {
		return true;
	}

	public boolean PlayerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		return true;
	}

	public boolean EntityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		return true;
	}

	public boolean PlayerDamagedByProjectileEvent(EntityDamageByEntityEvent event) {
		return true;
	}

	public boolean LivingEntityShotByPlayerEvent(Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		return true;
	}

	public boolean PlayerShotArrowEvent(Arrow arrow) {
		return true;
	}

	public void ProjectileHitEvent(ProjectileHitEvent event, Arrow arrow) { }

	// Called when a player throws a splash potion
	public boolean PlayerSplashPotionEvent(Collection<LivingEntity> affectedEntities,
	                                       ThrownPotion potion, PotionSplashEvent event) {
		if (mPlayer != null) {
			//  All affected players need to have the effect added to their potion manager.
			for (LivingEntity entity : affectedEntities) {
				if (entity instanceof Player) {
					// Thrown by a player - negative effects are not allowed for other players
					// Don't remove negative effects if it was the same player that threw the potion
					if ((!entity.equals(mPlayer)) && PotionUtils.hasNegativeEffects(potion.getEffects())) {
						// If a thrown potion contains any negative effects, don't apply *any* effects to other players
						event.setIntensity(entity, 0);
					}

					mPlugin.mPotionManager.addPotion((Player)entity, PotionID.APPLIED_POTION, potion.getEffects(),
					                                 event.getIntensity(entity));
				}
			}

		}
		return true;
	}

	public void PlayerItemHeldEvent(ItemStack mainHand, ItemStack offHand) { }

	public void PlayerRespawnEvent() { }

	public boolean PlayerExtendedSneakEvent(Player player) {
		return true;
	}

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

	public void PeriodicTrigger(boolean twoHertz, boolean oneSecond, boolean twoSeconds, boolean fourtySeconds, boolean sixtySeconds, int originalTime) {}

	//---------------------------------------------------------------------------------------------------------------
	public boolean canUse(Player player) {
		AbilityInfo info = getInfo();
		if (info.classId == ScoreboardUtils.getScoreboardValue(player, "Class")
		    && (info.specId < 0 || info.specId == ScoreboardUtils.getScoreboardValue(player, "Specialization"))
		    && (info.scoreboardId == null || ScoreboardUtils.getScoreboardValue(player, info.scoreboardId) > 0)) {
			return true;
		}
		return false;
	}

	/*
	 * For performance, this caches the first scoreboard lookup for future use
	 */
	protected int getAbilityScore() {
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
