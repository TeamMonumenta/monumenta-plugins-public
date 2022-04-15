package com.playmonumenta.plugins.bosses.bosses.abilities;

import com.playmonumenta.plugins.abilities.scout.HuntingCompanion;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.plugin.Plugin;

public class HuntingCompanionBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_huntingcompanion";
	public static final int detectionRange = 64;

	private List<UUID> mStunnedMobs;

	private @Nullable Player mPlayer;
	private double mDamage = 0;
	private int mStunDuration = 0;
	private int mBleedDuration = 0;
	private double mBleedAmount = 0;
	private @Nullable ItemStatManager.PlayerItemStats mPlayerItemStats;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new HuntingCompanionBoss(plugin, boss);
	}

	public HuntingCompanionBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		mStunnedMobs = new ArrayList<>();
		boss.setInvulnerable(true);

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	public void spawn(Player player, double damage, int stunDuration, int bleedDuration, double bleedAmount, ItemStatManager.PlayerItemStats playerItemStats) {
		mPlayer = player;
		mDamage = damage;
		mStunDuration = stunDuration;
		mBleedDuration = bleedDuration;
		mBleedAmount = bleedAmount;
		mPlayerItemStats = playerItemStats;
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (mPlayer != null && mPlayerItemStats != null) {
			event.setCancelled(true);

			DamageUtils.damage(mPlayer, damagee, new DamageEvent.Metadata(DamageType.PROJECTILE_SKILL, ClassAbility.HUNTING_COMPANION, mPlayerItemStats), mDamage, true, true, false);

			if (mStunDuration > 0) {
				mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_FOX_BITE, 1.5f, 1.0f);
				UUID uuid = damagee.getUniqueId();
				if (!mStunnedMobs.contains(uuid)) {
					EntityUtils.applyStun(com.playmonumenta.plugins.Plugin.getInstance(), mStunDuration, damagee);
					mStunnedMobs.add(uuid);
				}
			}

			if (mBleedDuration > 0) {
				HuntingCompanion.eagleSounds(mBoss.getLocation());
				EntityUtils.applyBleed(com.playmonumenta.plugins.Plugin.getInstance(), mBleedDuration, mBleedAmount, damagee);
			}
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
