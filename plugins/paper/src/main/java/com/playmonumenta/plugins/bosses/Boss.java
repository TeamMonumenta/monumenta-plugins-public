package com.playmonumenta.plugins.bosses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.destroystokyo.paper.event.entity.EntityPathfindEvent;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.events.SpellCastEvent;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.CustomEffectApplyEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Boss {
	private final Plugin mPlugin;
	private final List<BossAbilityGroup> mAbilities;
	private @Nullable Entity mLastHitBy;

	public Boss(Plugin plugin, BossAbilityGroup ability) {
		mPlugin = plugin;
		mAbilities = new ArrayList<BossAbilityGroup>(5);
		mAbilities.add(ability);
	}

	public void add(BossAbilityGroup ability) {
		mAbilities.add(ability);
	}

	public void onHurt(DamageEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			if (!event.isCancelled()) {
				ability.onHurt(event);

				for (Spell passive : ability.getPassives()) {
					passive.onHurt(event);
				}

				for (Spell active : ability.getActiveSpells()) {
					active.onHurt(event);
				}
			}
		}
	}

	public void onHurtByEntity(DamageEvent event, Entity damager) {
		for (BossAbilityGroup ability : mAbilities) {
			if (!event.isCancelled()) {
				ability.onHurtByEntity(event, damager);

				for (Spell passive : ability.getPassives()) {
					passive.onHurtByEntity(event, damager);
				}

				for (Spell active : ability.getActiveSpells()) {
					active.onHurtByEntity(event, damager);
				}
			}
		}
	}

	public void onHurtByEntityWithSource(DamageEvent event, Entity damager, LivingEntity source) {
		for (BossAbilityGroup ability : mAbilities) {
			if (!event.isCancelled()) {
				ability.onHurtByEntityWithSource(event, damager, source);

				for (Spell passive : ability.getPassives()) {
					passive.onHurtByEntityWithSource(event, damager, source);
				}

				for (Spell active : ability.getActiveSpells()) {
					active.onHurtByEntityWithSource(event, damager, source);
				}
			}
		}
	}

	public void onDamage(DamageEvent event, LivingEntity damagee) {
		for (BossAbilityGroup ability : mAbilities) {
			if (!event.isCancelled()) {
				ability.onDamage(event, damagee);

				for (Spell passive : ability.getPassives()) {
					passive.onDamage(event, damagee);
				}

				for (Spell active : ability.getActiveSpells()) {
					active.onDamage(event, damagee);
				}
			}
		}
	}

	public void bossLaunchedProjectile(ProjectileLaunchEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			if (!event.isCancelled()) {
				ability.bossLaunchedProjectile(event);

				for (Spell passive : ability.getPassives()) {
					passive.bossLaunchedProjectile(event);
				}

				for (Spell active : ability.getActiveSpells()) {
					active.bossLaunchedProjectile(event);
				}
			}
		}
	}

	public void bossProjectileHit(ProjectileHitEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			ability.bossProjectileHit(event);

			for (Spell passive : ability.getPassives()) {
				passive.bossProjectileHit(event);
			}

			for (Spell active : ability.getActiveSpells()) {
				active.bossProjectileHit(event);
			}
		}
	}

	public void bossHitByProjectile(ProjectileHitEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			ability.bossHitByProjectile(event);
			for (Spell passive : ability.getPassives()) {
				passive.bossHitByProjectile(event);
			}

			for (Spell active : ability.getActiveSpells()) {
				active.bossHitByProjectile(event);
			}
		}
	}

	public void areaEffectAppliedToBoss(AreaEffectCloudApplyEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			ability.areaEffectAppliedToBoss(event);

			for (Spell passive : ability.getPassives()) {
				passive.areaEffectAppliedToBoss(event);
			}

			for (Spell active : ability.getActiveSpells()) {
				active.areaEffectAppliedToBoss(event);
			}
		}
	}

	public void splashPotionAppliedToBoss(PotionSplashEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			ability.splashPotionAppliedToBoss(event);

			for (Spell passive : ability.getPassives()) {
				passive.splashPotionAppliedToBoss(event);
			}

			for (Spell active : ability.getActiveSpells()) {
				active.splashPotionAppliedToBoss(event);
			}
		}
	}

	public void bossCastAbility(SpellCastEvent event) {
		if (EntityUtils.isSilenced(event.getBoss())) {
			return;
		}
		for (BossAbilityGroup ability : mAbilities) {
			ability.bossCastAbility(event);
			for (Spell passive : ability.getPassives()) {
				passive.bossCastAbility(event);
			}

			for (Spell active : ability.getActiveSpells()) {
				active.bossCastAbility(event);
			}
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

	/*
	 * Boss was stunned by a player. Mobs with the "Boss" tag can't be stunned
	 */
	public void bossStunned() {
		for (BossAbilityGroup ability : mAbilities) {
			ability.bossStunned();
		}
	}

	/*
	 * Boss was confused by a player. Mobs with the "Boss" tag can't be confused
	 */
	public void bossConfused() {
		for (BossAbilityGroup ability : mAbilities) {
			ability.bossConfused();
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
			new BukkitRunnable() {
				@Override
				public void run() {
					mAbilities.clear();
				}
			}.runTaskLater(mPlugin, 0);
		}
	}

	public void death(EntityDeathEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			ability.mDead = true;
			ability.death(event);
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
		List<String> tags = mAbilities.stream().map(ability -> ability.getIdentityTag()).collect(Collectors.toList());
		Collections.sort(tags);
		return tags;
	}
}
