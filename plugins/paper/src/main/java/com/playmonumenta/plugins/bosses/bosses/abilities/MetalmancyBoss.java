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
import javax.annotation.Nullable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.Set;

public class MetalmancyBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_metalmancy";
	public static final int detectionRange = 64;

	private @Nullable Player mPlayer;
	private double mDamage = 0;
	private @Nullable ItemStatManager.PlayerItemStats mPlayerItemStats;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new MetalmancyBoss(plugin, boss);
	}

	public MetalmancyBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, identityTag, boss);

		boss.setInvulnerable(true);

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	public void spawn(Player player, double damage, ItemStatManager.PlayerItemStats playerItemStats) {
		mPlayer = player;
		mDamage = damage;
		mPlayerItemStats = playerItemStats;
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (mPlayer != null && mPlayerItemStats != null) {
			event.setCancelled(true);

			DamageUtils.damage(mPlayer, damagee, new DamageEvent.Metadata(DamageType.PROJECTILE_SKILL, ClassAbility.METALMANCY, mPlayerItemStats), mDamage, true, true, null);

			if (damagee instanceof Mob mob) {
				mob.setTarget(mBoss);
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
