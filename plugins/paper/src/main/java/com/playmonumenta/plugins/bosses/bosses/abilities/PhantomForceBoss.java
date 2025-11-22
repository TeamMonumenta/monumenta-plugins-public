package com.playmonumenta.plugins.bosses.bosses.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.Collections;
import java.util.Set;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTargetEvent;
import org.jetbrains.annotations.Nullable;

public class PhantomForceBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_phantomforce";
	public static final int detectionRange = 64;

	private @Nullable Player mPlayer = null;
	private @Nullable ItemStatManager.PlayerItemStats mPlayerItemStats;
	private double mDamage;
	private double mRadius;
	private double mVulnerabilityAmount;
	private int mVulnerabilityDuration;

	public PhantomForceBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	public void spawn(Player player, double damage, double radius, double vulnerabilityAmount, int vulnerabilityDuration, ItemStatManager.PlayerItemStats playerItemStats) {
		mPlayer = player;
		mDamage = damage;
		mRadius = radius;
		mVulnerabilityAmount = vulnerabilityAmount;
		mVulnerabilityDuration = vulnerabilityDuration;
		mPlayerItemStats = playerItemStats;
	}

	@Override
	public void onHurtByEntity(DamageEvent event, Entity damager) {
		if (damager instanceof Player && event.getType() == DamageType.MELEE) {
			explode();
		} else {
			event.setCancelled(true);
		}
	}

	// can trigger in DepthsListener
	public void explode() {
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1f, 0.7f);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_PHANTOM_DEATH, SoundCategory.PLAYERS, 1f, 1.75f);
		world.playSound(mBoss.getLocation(), Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.7f, 1.6f);

		if (mPlayer != null) {
			new PartialParticle(Particle.SQUID_INK, LocationUtils.getEntityCenter(mBoss), 10).delta(0.5).spawnAsPlayerActive(mPlayer);
		}

		EntityUtils.getNearbyMobs(mBoss.getLocation(), mRadius, mBoss).forEach(mob -> {
			DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageType.MELEE_SKILL, ClassAbility.PHANTOM_FORCE, mPlayerItemStats), mDamage, true, true, false);
			EntityUtils.applyVulnerability(Plugin.getInstance(), mVulnerabilityDuration, mVulnerabilityAmount, mob);
		});
		mBoss.remove();
	}

	@Override
	public void bossChangedTarget(EntityTargetEvent event) {
		Entity target = event.getTarget();
		if (target != null) {
			Set<String> tags = target.getScoreboardTags();
			if (!EntityUtils.isHostileMob(target) || tags.contains(AbilityUtils.IGNORE_TAG) || (target instanceof LivingEntity le && DamageUtils.isImmuneToDamage(le, DamageType.MELEE_SKILL))) {
				event.setCancelled(true);
			}
		}
	}
}
