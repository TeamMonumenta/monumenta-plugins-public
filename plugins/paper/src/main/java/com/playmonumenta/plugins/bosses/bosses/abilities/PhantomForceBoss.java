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
	private double mWeakenAmount;
	private int mWeakenDuration;

	public PhantomForceBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		boss.setInvulnerable(true);
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	public void spawn(Player player, double damage, double weakenAmount, int weakenDuration, ItemStatManager.PlayerItemStats playerItemStats) {
		mPlayer = player;
		mDamage = damage;
		mWeakenAmount = weakenAmount;
		mWeakenDuration = weakenDuration;
		mPlayerItemStats = playerItemStats;
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		event.setCancelled(true);
		attack(damagee);
	}

	public void attack(LivingEntity damagee) {
		if (mPlayer == null) {
			return;
		}

		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_PHANTOM_BITE, SoundCategory.PLAYERS, 0.7f, 0.8f);
		new PartialParticle(Particle.SQUID_INK, LocationUtils.getEntityCenter(damagee), 10).delta(0.5).spawnAsPlayerActive(mPlayer);

		DamageUtils.damage(mPlayer, damagee, new DamageEvent.Metadata(DamageType.MELEE_SKILL, ClassAbility.PHANTOM_FORCE, mPlayerItemStats), mDamage, true, true, false);
		EntityUtils.applyWeaken(Plugin.getInstance(), mWeakenDuration, mWeakenAmount, damagee);

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
