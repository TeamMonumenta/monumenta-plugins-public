package com.playmonumenta.plugins.bosses.bosses.abilities;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class HuntingCompanionBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_huntingcompanion";
	public static final int detectionRange = 64;

	private List<UUID> mStunnedMobs;

	private @Nullable Player mPlayer;
	private double mDamage = 0;
	private int mStunTime = 0;
	private @Nullable ItemStatManager.PlayerItemStats mPlayerItemStats;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new HuntingCompanionBoss(plugin, boss);
	}

	public HuntingCompanionBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, identityTag, boss);

		mStunnedMobs = new ArrayList<>();
		boss.setInvulnerable(true);

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	public void spawn(Player player, double damage, int stunTime, ItemStatManager.PlayerItemStats playerItemStats) {
		mPlayer = player;
		mDamage = damage;
		mStunTime = stunTime;
		mPlayerItemStats = playerItemStats;
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (mPlayer != null && mPlayerItemStats != null) {
			event.setCancelled(true);

			DamageEvent damageEvent = new DamageEvent(damagee, mPlayer, mPlayer, DamageType.PROJECTILE_SKILL, ClassAbility.HUNTING_COMPANION, mDamage);
			damageEvent.setDelayed(true);
			damageEvent.setPlayerItemStat(mPlayerItemStats);
			DamageUtils.damage(damageEvent, true, true, null);

			mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_FOX_BITE, 1.5f, 1.0f);
			UUID uuid = damagee.getUniqueId();
			if (!mStunnedMobs.contains(uuid)) {
				EntityUtils.applyStun(com.playmonumenta.plugins.Plugin.getInstance(), mStunTime, damagee);
				mStunnedMobs.add(uuid);
			}
		}
	}

	@Override
	public void bossChangedTarget(EntityTargetEvent event) {
		Entity target = event.getTarget();
		Set<String> tags = target.getScoreboardTags();
		if (!EntityUtils.isHostileMob(target) || (tags != null && tags.contains(AbilityUtils.IGNORE_TAG))) {
			event.setCancelled(true);
		}
	}
}
