package com.playmonumenta.bossfights;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;

import com.playmonumenta.bossfights.bosses.BossAbilityGroup;
import com.playmonumenta.bossfights.spells.Spell;

public class Boss {
	List<BossAbilityGroup> mAbilities;

	public Boss(BossAbilityGroup ability) {
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
				for (Spell passives : ability.getPassives()) {
					passives.bossDamagedByEntity(event);
				}
			}
		}
	}

	public void bossDamagedEntity(EntityDamageByEntityEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			if (!event.isCancelled()) {
				ability.bossDamagedEntity(event);
				for (Spell passives : ability.getPassives()) {
					passives.bossDamagedEntity(event);
				}
			}
		}
	}

	public void bossLaunchedProjectile(ProjectileLaunchEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			if (!event.isCancelled()) {
				ability.bossLaunchedProjectile(event);
				for (Spell passives : ability.getPassives()) {
					passives.bossLaunchedProjectile(event);
				}
			}
		}
	}

	public void bossProjectileHit(ProjectileHitEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			ability.bossProjectileHit(event);
			for (Spell passives : ability.getPassives()) {
				passives.bossProjectileHit(event);
			}
		}
	}

	public void bossHitByProjectile(ProjectileHitEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			ability.bossHitByProjectile(event);
			for (Spell passives : ability.getPassives()) {
				passives.bossHitByProjectile(event);
			}
		}
	}

	public void areaEffectAppliedToBoss(AreaEffectCloudApplyEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			ability.areaEffectAppliedToBoss(event);
			for (Spell passives : ability.getPassives()) {
				passives.areaEffectAppliedToBoss(event);
			}
		}
	}

	public void splashPotionAppliedToBoss(PotionSplashEvent event) {
		for (BossAbilityGroup ability : mAbilities) {
			ability.splashPotionAppliedToBoss(event);
			for (Spell passives : ability.getPassives()) {
				passives.splashPotionAppliedToBoss(event);
			}
		}
	}

	public void unload() {
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
		mAbilities.clear();
	}

	public void death() {
		for (BossAbilityGroup ability : mAbilities) {
			ability.death();
		}
	}
}
