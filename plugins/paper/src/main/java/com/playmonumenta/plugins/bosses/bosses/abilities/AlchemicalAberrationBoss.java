package com.playmonumenta.plugins.bosses.bosses.abilities;

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
	private int mBleedDuration = 0;
	private double mBleedAmount = 0;
	private @Nullable ItemStatManager.PlayerItemStats mPlayerItemStats;
	private EsotericEnhancementsCS mCosmetic = new EsotericEnhancementsCS();

	public AlchemicalAberrationBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		boss.setInvulnerable(true);

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	public void spawn(Player player, double damage, double radius, int bleedDuration, double bleedAmount, ItemStatManager.PlayerItemStats playerItemStats, EsotericEnhancementsCS cosmetic) {
		mPlayer = player;
		mDamage = damage;
		mRadius = radius;
		mBleedDuration = bleedDuration;
		mBleedAmount = bleedAmount;
		mPlayerItemStats = playerItemStats;
		mCosmetic = cosmetic;
	}

	@Override
	public void bossExploded(EntityExplodeEvent event) {
		for (LivingEntity entity : EntityUtils.getNearbyMobs(mBoss.getLocation(), mRadius, mBoss)) {
			DamageUtils.damage(mPlayer, entity, new DamageEvent.Metadata(DamageEvent.DamageType.MAGIC, ClassAbility.ESOTERIC_ENHANCEMENTS, mPlayerItemStats), mDamage, true, false, false);
			EntityUtils.applyBleed(com.playmonumenta.plugins.Plugin.getInstance(), mBleedDuration, mBleedAmount, entity);
			if (mPlayer != null) {
				mCosmetic.explosionEffects(mPlayer, mBoss, mRadius);
			}
		}
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		// Set damage to 0 for non-boss hostile mobs to apply normal knockback. Bosses should not take any knockback
		if (EntityUtils.isHostileMob(damagee)
			&& !EntityUtils.isCCImmuneMob(damagee)) {
			event.setFlatDamage(0);
		} else {
			event.setCancelled(true);
		}
	}

	@Override
	public void bossChangedTarget(EntityTargetEvent event) {
		Entity target = event.getTarget();
		if (!EntityUtils.isHostileMob(target) || target.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG)) {
			event.setCancelled(true);
		}
	}

}
