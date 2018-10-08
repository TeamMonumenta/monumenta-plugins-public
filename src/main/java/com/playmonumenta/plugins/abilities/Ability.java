package com.playmonumenta.plugins.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.World;

public abstract class Ability {
	protected final Plugin mPlugin;
	protected final World mWorld;
	protected final Random mRandom;
	protected final AbilityInfo mInfo;
	private Integer mScore = null;

	public Ability(Plugin plugin, World world, Random random, Player player) {
		mPlugin = plugin;
		mWorld = world;
		mRandom = random;
		mInfo = new AbilityInfo();
	}

	/**
	 * This is used when the ability is casted manually when its
	 * AbilityTrigger (Right Click/Left Click), along with whatever
	 * runCheck() may contain, is correct.
	 * @return if the player managed to cast the spell successfully.
	 */
	public boolean cast(Player player) {
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
	public boolean runCheck(Player player) {
		return true;
	}

	public boolean isOnCooldown(Player player) {
		AbilityInfo info = getInfo();
		if (info.linkedSpell != null) {
			if (mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), info.linkedSpell)) {
				return true;
			}
		}
		return false;
	}

	public void putOnCooldown(Player player) {
		AbilityInfo info = getInfo();
		if (info.linkedSpell != null) {
			if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), info.linkedSpell)) {
				mPlugin.mTimers.AddCooldown(player.getUniqueId(), info.linkedSpell, info.cooldown);
			}
		}
	}

	/**
	 * A combination of both runCheck and isOnCooldown.
	 * @param player
	 * @return
	 */
	public boolean canCast(Player player) {
		if (runCheck(player) && !isOnCooldown(player)) {
			return true;
		}
		return false;
	}

	//Events
	//---------------------------------------------------------------------------------------------------------------

	public boolean LivingEntityDamagedByPlayerEvent(Player player, EntityDamageByEntityEvent event) {
		return true;
	}

	public boolean PlayerDamagedByLivingEntityEvent(Player player, EntityDamageByEntityEvent event) {
		return true;
	}

	public boolean EntityDeathEvent(Player player, EntityDeathEvent event, boolean shouldGenDrops) {
		return true;
	}

	public boolean PlayerDamagedByProjectileEvent(Player player, EntityDamageByEntityEvent event) {
		return true;
	}

	public boolean LivingEntityShotByPlayerEvent(Player player, Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		return true;
	}

	public boolean PlayerShotArrowEvent(Player player, Arrow arrow) {
		return true;
	}

	public void PlayerItemHeldEvent(Player player, ItemStack mainHand, ItemStack offHand) { }

	public void PlayerRespawnEvent(Player player) { }

	//---------------------------------------------------------------------------------------------------------------

	//Other
	//---------------------------------------------------------------------------------------------------------------

	public void setupClassPotionEffects(Player player) { }

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
	protected int getAbilityScore(Player player) {
		AbilityInfo info = getInfo();
		if (info.scoreboardId != null) {
			if (mScore == null) {
				mScore = ScoreboardUtils.getScoreboardValue(player, info.scoreboardId);
			}
			return mScore;
		}
		return 0;
	}
}
