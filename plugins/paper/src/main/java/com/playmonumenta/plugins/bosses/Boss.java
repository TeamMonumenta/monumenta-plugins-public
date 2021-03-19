package com.playmonumenta.plugins.bosses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.entity.Entity;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.destroystokyo.paper.event.entity.EntityPathfindEvent;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.events.SpellCastEvent;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.CustomEffectApplyEvent;

public class Boss {
	private final Plugin mPlugin;
	private final List<BossAbilityGroup> mAbilities;
	private Entity mLastHitBy;

	public Boss(Plugin plugin, BossAbilityGroup ability) {
		mPlugin = plugin;
		mAbilities = new ArrayList<BossAbilityGroup>(5);
		mAbilities.add(ability);
	}

	public void add(BossAbilityGroup ability) {
		mAbilities.add(ability);
	}

	public void bossDamagedByEntity(EntityDamageByEntityEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			if (!event.isCancelled()) {
				ability.bossDamagedByEntity(event);

				if (ability.getPassives() != null && !ability.getPassives().isEmpty()) {
					for (Spell passives : ability.getPassives()) {
						passives.bossDamagedByEntity(event);
					}
				}

				if (ability.getActiveSpells() != null && !ability.getActiveSpells().isEmpty()) {
					for (Spell actives : ability.getActiveSpells()) {
						actives.bossDamagedByEntity(event);
					}
				}
			}
		}
	}

	public void bossDamagedEntity(EntityDamageByEntityEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			if (!event.isCancelled()) {
				ability.bossDamagedEntity(event);

				if (ability.getPassives() != null && !ability.getPassives().isEmpty()) {
					for (Spell passives : ability.getPassives()) {
						passives.bossDamagedEntity(event);
					}
				}

				if (ability.getActiveSpells() != null && !ability.getActiveSpells().isEmpty()) {
					for (Spell actives : ability.getActiveSpells()) {
						actives.bossDamagedEntity(event);
					}
				}
			}
		}
	}

	public void bossLaunchedProjectile(ProjectileLaunchEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			if (!event.isCancelled()) {
				ability.bossLaunchedProjectile(event);

				if (ability.getPassives() != null && !ability.getPassives().isEmpty()) {
					for (Spell passives : ability.getPassives()) {
						passives.bossLaunchedProjectile(event);
					}
				}

				if (ability.getActiveSpells() != null && !ability.getActiveSpells().isEmpty()) {
					for (Spell actives : ability.getActiveSpells()) {
						actives.bossLaunchedProjectile(event);
					}
				}
			}
		}
	}

	public void bossProjectileHit(ProjectileHitEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			ability.bossProjectileHit(event);
			if (ability.getPassives() != null && !ability.getPassives().isEmpty()) {
				for (Spell passives : ability.getPassives()) {
					passives.bossProjectileHit(event);
				}
			}

			if (ability.getActiveSpells() != null && !ability.getActiveSpells().isEmpty()) {
				for (Spell actives : ability.getActiveSpells()) {
					actives.bossProjectileHit(event);
				}
			}
		}
	}

	public void bossHitByProjectile(ProjectileHitEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			ability.bossHitByProjectile(event);
			if (ability.getPassives() != null && !ability.getPassives().isEmpty()) {
				for (Spell passives : ability.getPassives()) {
					passives.bossHitByProjectile(event);
				}
			}

			if (ability.getActiveSpells() != null && !ability.getActiveSpells().isEmpty()) {
				for (Spell actives : ability.getActiveSpells()) {
					actives.bossHitByProjectile(event);
				}
			}
		}
	}

	public void areaEffectAppliedToBoss(AreaEffectCloudApplyEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			ability.areaEffectAppliedToBoss(event);

			if (ability.getPassives() != null && !ability.getPassives().isEmpty()) {
				for (Spell passives : ability.getPassives()) {
					passives.areaEffectAppliedToBoss(event);
				}
			}

			if (ability.getActiveSpells() != null && !ability.getActiveSpells().isEmpty()) {
				for (Spell actives : ability.getActiveSpells()) {
					actives.areaEffectAppliedToBoss(event);
				}
			}
		}
	}

	public void splashPotionAppliedToBoss(PotionSplashEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			ability.splashPotionAppliedToBoss(event);

			if (ability.getPassives() != null && !ability.getPassives().isEmpty()) {
				for (Spell passives : ability.getPassives()) {
					passives.splashPotionAppliedToBoss(event);
				}
			}

			if (ability.getActiveSpells() != null && !ability.getActiveSpells().isEmpty()) {
				for (Spell actives : ability.getActiveSpells()) {
					actives.splashPotionAppliedToBoss(event);
				}
			}
		}
	}

	public void bossCastAbility(SpellCastEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			ability.bossCastAbility(event);
			if (ability.getPassives() != null && !ability.getPassives().isEmpty()) {
				for (Spell passives : ability.getPassives()) {
					passives.bossCastAbility(event);
				}
			}

			if (ability.getActiveSpells() != null && !ability.getActiveSpells().isEmpty()) {
				for (Spell actives : ability.getActiveSpells()) {
					actives.bossCastAbility(event);
				}
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

	public Entity getLastHitBy() {
		return mLastHitBy;
	}

	public void setLastHitBy(Entity getLastHitBy) {
		this.mLastHitBy = getLastHitBy;
	}

	public List<String> getIdentityTags() {
		List<String> tags = mAbilities.stream().map(ability -> ability.getIdentityTag()).collect(Collectors.toList());
		Collections.sort(tags);
		return tags;
	}
}
