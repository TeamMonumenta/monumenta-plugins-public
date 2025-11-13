package com.playmonumenta.plugins.bosses.bosses.abilities;

import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.harbinger.EsotericEnhancementsCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.Collections;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class AlchemicalAberrationBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_alchemicalaberration";
	public static final int detectionRange = 64;

	private @Nullable Player mPlayer;
	private double mDamage = 0;
	private double mRadius = 0;
	private int mSlowDuration = 0;
	private double mSlowAmount = 0;
	private double mKnockbackMultiplier = 1;
	private @Nullable ItemStatManager.PlayerItemStats mPlayerItemStats;
	private EsotericEnhancementsCS mCosmetic = new EsotericEnhancementsCS();

	public AlchemicalAberrationBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		boss.setInvulnerable(true);

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	public void spawn(Player player, double damage, double radius, int slowDuration, double slowAmount, double knockbackMultiplier, ItemStatManager.PlayerItemStats playerItemStats, EsotericEnhancementsCS cosmetic) {
		mPlayer = player;
		mDamage = damage;
		mRadius = radius;
		mSlowDuration = slowDuration;
		mSlowAmount = slowAmount;
		mKnockbackMultiplier = knockbackMultiplier;
		mPlayerItemStats = playerItemStats;
		mCosmetic = cosmetic;
	}

	@Override
	public void bossExploded(EntityExplodeEvent event) {
		for (LivingEntity entity : EntityUtils.getNearbyMobs(mBoss.getLocation(), mRadius, mBoss)) {
			DamageUtils.damage(mPlayer, entity, new DamageEvent.Metadata(DamageEvent.DamageType.MAGIC, ClassAbility.ESOTERIC_ENHANCEMENTS, mPlayerItemStats), mDamage, true, false, false);
			EntityUtils.applySlow(com.playmonumenta.plugins.Plugin.getInstance(), mSlowDuration, mSlowAmount, entity);
			if (mPlayer != null) {
				mCosmetic.explosionEffects(mPlayer, mBoss, mRadius);
			}
		}
	}

	@Override
	public void bossKnockedBackEntity(EntityKnockbackByEntityEvent event) {
		LivingEntity knocked = event.getEntity();
		if (!EntityUtils.isHostileMob(knocked) || EntityUtils.isCCImmuneMob(knocked)) {
			event.setCancelled(true);
			return;
		}
		double kbr = EntityUtils.getAttributeBaseOrDefault(knocked, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 0);
		event.setAcceleration(event.getAcceleration().multiply(Math.max(0, (1 - kbr) * mKnockbackMultiplier)));
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		event.setFlatDamage(0);
	}

	@Override
	public void bossChangedTarget(EntityTargetEvent event) {
		Entity target = event.getTarget();
		if (!EntityUtils.isHostileMob(target) || target.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG)) {
			event.setCancelled(true);
		}
	}

}
