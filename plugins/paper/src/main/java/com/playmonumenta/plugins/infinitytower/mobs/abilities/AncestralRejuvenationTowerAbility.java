package com.playmonumenta.plugins.infinitytower.mobs.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.infinitytower.TowerGame;
import com.playmonumenta.plugins.infinitytower.TowerMob;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

public class AncestralRejuvenationTowerAbility extends TowerAbility {
	public AncestralRejuvenationTowerAbility(Plugin plugin, String identityTag, LivingEntity boss, TowerGame game, TowerMob mob, boolean isPlayerMob, double amount) {
		super(plugin, identityTag, boss, game, mob, isPlayerMob);

		Spell spell = new Spell() {
			final double mHealAmount = amount;
			@Override
			public void run() {
				List<LivingEntity> list = !mIsPlayerMob ? mGame.getFloorMobs() : mGame.getPlayerMobs();

				for (LivingEntity target : list) {
					if (target.isDead() || !target.isValid()) {
						continue;
					}
					double healt = target.getHealth();
					double maxHealt = target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
					healt = Math.min(maxHealt, healt + mHealAmount);
					target.setHealth(healt);
					Location loc = target.getEyeLocation();
					new PartialParticle(Particle.HEART, loc, 10, 0.05, 0.02, 0.05, 0.5).spawnAsEntityActive(mBoss);
				}
			}

			@Override
			public int cooldownTicks() {
				return 80;
			}
		};

		SpellManager active = new SpellManager(List.of(spell));

		super.constructBoss(active, Collections.emptyList(), -1, null, (int) (FastUtils.RANDOM.nextDouble() * 100) + 20);
	}
}
