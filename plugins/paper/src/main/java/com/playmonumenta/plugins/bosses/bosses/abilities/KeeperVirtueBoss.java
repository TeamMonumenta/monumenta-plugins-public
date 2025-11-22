package com.playmonumenta.plugins.bosses.bosses.abilities;

import com.playmonumenta.plugins.abilities.cleric.seraph.KeeperVirtue;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.cleric.seraph.KeeperVirtueCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Collections;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class KeeperVirtueBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_keepervirtue";
	public static final int detectionRange = 64;

	public @Nullable Player mPlayer = null;
	private @Nullable KeeperVirtue mAbility = null;
	private double mDamage = 0;
	private int mAttackDelay = 0;
	private double mAttackDrain = 0;
	private double mHealing = 0;
	private int mHealDelay = 0;
	private double mHealDrain = 0;
	private double mVulnAmplifier = 0;
	private int mVulnDuration = 0;
	private int mLastHealTick = 0;
	private int mLastAttackTick = 0;
	private @Nullable KeeperVirtueCS mCosmetic = null;

	public KeeperVirtueBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		boss.setInvulnerable(true);
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	public void spawn(Player player, double damage, int attackDelay, double damageDrain, double healing, int healDelay, double healDrain, double vulnAmplifier, int vulnDuration, KeeperVirtue ability, KeeperVirtueCS cosmetic) {
		mPlayer = player;
		mDamage = damage;
		mAttackDelay = attackDelay;
		mAttackDrain = damageDrain;
		mHealing = healing;
		mHealDelay = healDelay;
		mHealDrain = healDrain;
		mVulnAmplifier = vulnAmplifier;
		mVulnDuration = vulnDuration;
		mAbility = ability;
		mCosmetic = cosmetic;
		mBoss.customName(mCosmetic.getComponentName());
	}

	public void resetActionTicks() {
		mLastAttackTick = Bukkit.getCurrentTick();
		mLastHealTick = mLastAttackTick;
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		event.setCancelled(true);
		attack(damagee);
	}

	public int healPlayer(Player player) {
		if (mPlayer == null || Bukkit.getCurrentTick() - mHealDelay < mLastHealTick) {
			return 0;
		}
		Location loc = mBoss.getEyeLocation();
		PlayerUtils.healPlayer(com.playmonumenta.plugins.Plugin.getInstance(), player, mHealing * EntityUtils.getMaxHealth(player), mPlayer);
		if (mCosmetic != null) {
			mCosmetic.healPlayer(mPlayer, player, mBoss);
		}
		mLastHealTick = Bukkit.getCurrentTick();

		if (mBoss.getHealth() - mHealDrain <= 0) {
			if (mCosmetic != null) {
				mCosmetic.allayOnDeath(mBoss, loc);
			}
			mBoss.remove();
			if (mAbility != null) {
				mAbility.putOnCooldown();
				mAbility.invalidate();
			}
		} else {
			mBoss.setHealth(mBoss.getHealth() - mHealDrain);
		}
		ClientModHandler.updateAbility(mPlayer, ClassAbility.KEEPER_VIRTUE);
		return 1;
	}

	public void attack(LivingEntity damagee) {
		if (mPlayer == null || Bukkit.getCurrentTick() - mAttackDelay < mLastAttackTick) {
			return;
		}
		Location loc = mBoss.getEyeLocation();
		DamageUtils.damage(mPlayer, damagee, new DamageEvent.Metadata(DamageType.MAGIC, ClassAbility.KEEPER_VIRTUE), mDamage, true, false, false);
		MovementUtils.knockAway(loc, damagee, 0.3f, 0.1f);
		if (mVulnAmplifier > 0) {
			EntityUtils.applyVulnerability(com.playmonumenta.plugins.Plugin.getInstance(), mVulnDuration, mVulnAmplifier, damagee);
		}
		if (mCosmetic != null) {
			mCosmetic.attackHeretic(mPlayer, damagee, mBoss);
		}
		mLastAttackTick = Bukkit.getCurrentTick();

		if (mBoss.getHealth() - mAttackDrain <= 0) {
			mBoss.getWorld().playSound(loc, Sound.ENTITY_ALLAY_DEATH, 1f, 1f);
			mBoss.remove();
			if (mAbility != null) {
				mAbility.putOnCooldown();
				mAbility.invalidate();
			}
		} else {
			mBoss.setHealth(mBoss.getHealth() - mAttackDrain);
		}
		ClientModHandler.updateAbility(mPlayer, ClassAbility.KEEPER_VIRTUE);
	}

	@Override
	public void bossChangedTarget(EntityTargetEvent event) {
		Entity target = event.getTarget();
		if (target != null) {
			Set<String> tags = target.getScoreboardTags();
			if (!EntityUtils.isHostileMob(target) || tags.contains(AbilityUtils.IGNORE_TAG) || (target instanceof LivingEntity le && DamageUtils.isImmuneToDamage(le, DamageType.MAGIC))) {
				event.setCancelled(true);
			}
		}
	}
}
