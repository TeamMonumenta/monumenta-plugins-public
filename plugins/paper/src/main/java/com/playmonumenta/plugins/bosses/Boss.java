package com.playmonumenta.plugins.bosses;

import com.destroystokyo.paper.event.entity.EntityPathfindEvent;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.events.SpellCastEvent;
import com.playmonumenta.plugins.events.CustomEffectApplyEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class Boss {
	private final Plugin mPlugin;
	private final List<BossAbilityGroup> mAbilities;
	private @Nullable Entity mLastHitBy;

	public Boss(Plugin plugin, BossAbilityGroup ability) {
		mPlugin = plugin;
		mAbilities = new ArrayList<>(5);
		mAbilities.add(ability);
	}

	public void add(BossAbilityGroup ability) {
		mAbilities.add(ability);
	}

	public void onHurt(DamageEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			if (!event.isCancelled()) {
				ability.onHurt(event);
				ability.triggerOnSpells(spell -> spell.onHurt(event));
			}
		}
	}

	public void onHurtByEntity(DamageEvent event, Entity damager) {
		for (BossAbilityGroup ability : mAbilities) {
			if (!event.isCancelled()) {
				ability.onHurtByEntity(event, damager);
				ability.triggerOnSpells(spell -> spell.onHurtByEntity(event, damager));
			}
		}
	}

	public void onHurtByEntityWithSource(DamageEvent event, Entity damager, LivingEntity source) {
		for (BossAbilityGroup ability : mAbilities) {
			if (!event.isCancelled()) {
				ability.onHurtByEntityWithSource(event, damager, source);
				ability.triggerOnSpells(spell -> spell.onHurtByEntityWithSource(event, damager, source));
			}
		}
	}

	public void onDamage(DamageEvent event, LivingEntity damagee) {
		for (BossAbilityGroup ability : mAbilities) {
			if (!event.isCancelled()) {
				ability.onDamage(event, damagee);
				ability.triggerOnSpells(spell -> spell.onDamage(event, damagee));
			}
		}
	}

	public void bossLaunchedProjectile(ProjectileLaunchEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			if (!event.isCancelled()) {
				ability.bossLaunchedProjectile(event);
				ability.triggerOnSpells(spell -> spell.bossLaunchedProjectile(event));
			}
		}
	}

	public void bossProjectileHit(ProjectileHitEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			ability.bossProjectileHit(event);
			ability.triggerOnSpells(spell -> spell.bossProjectileHit(event));
		}
	}

	public void bossHitByProjectile(ProjectileHitEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			ability.bossHitByProjectile(event);
			ability.triggerOnSpells(spell -> spell.bossHitByProjectile(event));
		}
	}

	public void areaEffectAppliedToBoss(AreaEffectCloudApplyEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			ability.areaEffectAppliedToBoss(event);
			ability.triggerOnSpells(spell -> spell.areaEffectAppliedToBoss(event));
		}
	}

	public void splashPotionAppliedToBoss(PotionSplashEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			ability.splashPotionAppliedToBoss(event);
			ability.triggerOnSpells(spell -> spell.splashPotionAppliedToBoss(event));
		}
	}

	public void bossSplashPotion(PotionSplashEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			ability.bossSplashPotion(event);
			ability.triggerOnSpells(spell -> spell.bossSplashPotion(event));
		}
	}

	public void bossCastAbility(SpellCastEvent event) {
		if (EntityUtils.isSilenced(event.getBoss())) {
			return;
		}
		try {
			for (BossAbilityGroup ability : mAbilities) {
				ability.bossCastAbility(event);
				ability.triggerOnSpells(spell -> spell.bossCastAbility(event));
			}
		} catch (ConcurrentModificationException cme) {
			MMLog.severe("Caught CME in SpellCastEvent of " + event.getBoss().getName());
			cme.printStackTrace();
		}
	}

	public void bossPathfind(EntityPathfindEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			ability.bossPathfind(event);
		}
	}

	public void bossChangedTarget(EntityTargetEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			ability.bossChangedTarget(event);
		}
	}

	public void customEffectAppliedToBoss(CustomEffectApplyEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			ability.customEffectAppliedToBoss(event);
		}
	}

	public void bossExploded(EntityExplodeEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			ability.bossExploded(event);
		}
	}

	// Only acts on fire applied by the plugin
	public void bossIgnited(int ticks) {
		for (BossAbilityGroup ability : mAbilities) {
			ability.bossIgnited(ticks);
		}
	}

	public void onPassengerHurt(DamageEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			ability.bossPassengerHurt(event);
		}
	}

	/*
	 * Boss was stunned by a player. Mobs with the "Boss" tag can't be stunned
	 */
	public void bossStunned() {
		for (BossAbilityGroup ability : mAbilities) {
			ability.bossStunned();
		}
	}

	/*
	 * Boss was silenced by a player. Mobs with the "Boss" tag can't be silenced
	 */
	public void bossSilenced() {
		for (BossAbilityGroup ability : mAbilities) {
			ability.bossSilenced();
		}
	}

	public void bossKnockedAway(float speed) {
		for (BossAbilityGroup ability : mAbilities) {
			ability.bossKnockedAway(speed);
		}
	}

	public List<BossAbilityGroup> getAbilities() {
		return new ArrayList<BossAbilityGroup>(mAbilities);
	}

	public void unload(boolean shuttingDown) {
		/* NOTE
		 *
		 * Unload will cause state to be serialized to the mob's equipment. This is fine if
		 * only one of the BossAbilityGroup's has data to serialize - but if not, only the
		 * last ability will actually have saved data, and the boss will fail to initialize
		 * later.
		 *
		 * Overcoming this limitation requires substantial refactoring.
		 */
		for (BossAbilityGroup ability : mAbilities) {
			ability.unload();
		}

		if (!shuttingDown) {
			/*
			 * Clear mAbilities the next available chance - this prevents ConcurrentModificationException's
			 * when the boss dies or is unloaded as a result of abilities
			 */
			Bukkit.getScheduler().runTask(mPlugin, mAbilities::clear);
		}
	}

	public void clearAbilities() {
		Bukkit.getScheduler().runTask(mPlugin, mAbilities::clear);
	}

	public void removeAbility(String identityTag) {
		for (BossAbilityGroup abilityGroup : getAbilities()) {
			if (abilityGroup.getIdentityTag().equals(identityTag)) {
				abilityGroup.unload();
				mAbilities.remove(abilityGroup);
			}
		}

		if (mAbilities.isEmpty()) {
			BossManager.getInstance().unload(this, false);
		}
	}

	public void death(@Nullable EntityDeathEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			ability.mDead = true;
			ability.death(event);

			ability.triggerOnSpells(spell -> spell.onDeath(event));
		}
	}

	public void nearbyEntityDeath(EntityDeathEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			ability.nearbyEntityDeath(event);
		}
	}

	public boolean hasNearbyEntityDeathTrigger() {
		for (BossAbilityGroup ability : mAbilities) {
			if (ability.hasNearbyEntityDeathTrigger()) {
				return true;
			}
		}
		return false;
	}

	public void nearbyBlockBreak(BlockBreakEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			ability.nearbyBlockBreak(event);
		}
	}

	public boolean hasNearbyBlockBreakTrigger() {
		for (BossAbilityGroup ability : mAbilities) {
			if (ability.hasNearbyBlockBreakTrigger()) {
				return true;
			}
		}
		return false;
	}

	public void nearbyPlayerDeath(PlayerDeathEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			ability.nearbyPlayerDeath(event);
		}
	}

	public boolean hasNearbyPlayerDeathTrigger() {
		for (BossAbilityGroup ability : mAbilities) {
			if (ability.hasNearbyPlayerDeathTrigger()) {
				return true;
			}
		}
		return false;
	}

	public void entityPotionEffectEvent(EntityPotionEffectEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			ability.entityPotionEffectEvent(event);
		}
	}

	public @Nullable Entity getLastHitBy() {
		return mLastHitBy;
	}

	public void setLastHitBy(@Nullable Entity getLastHitBy) {
		this.mLastHitBy = getLastHitBy;
	}

	public List<String> getIdentityTags() {
		List<String> tags = mAbilities.stream().map(BossAbilityGroup::getIdentityTag).sorted().toList();
		return tags;
	}
}
