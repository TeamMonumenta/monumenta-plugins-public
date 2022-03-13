package com.playmonumenta.plugins.infinitytower.mobs.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.infinitytower.TowerGame;
import com.playmonumenta.plugins.infinitytower.TowerMob;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.entity.LivingEntity;

public class CurseOfTheJungleTowerAbility extends TowerAbility {

	private static final ParticlesList MOB_PARTICLE_ALLY = ParticlesList.fromString("[(REDSTONE,2,1,1,1,0,#ffffff,0.5)]");
	private static final ParticlesList MOB_PARTICLE_ENEMY = ParticlesList.fromString("[(REDSTONE,2,1,1,1,0,#8B0000,0.5)]");

	private static final ParticlesList AIR_PARTICLE_ALLY = ParticlesList.fromString("[(REDSTONE,50,8,8,8,0.4,#ffffff,0.9)]");
	private static final ParticlesList AIR_PARTICLE_ENEMY = ParticlesList.fromString("[(REDSTONE,50,8,8,8,0.4,#8B0000,0.9)]");


	public CurseOfTheJungleTowerAbility(Plugin plugin, String identityTag, LivingEntity boss, TowerGame game, TowerMob mob, boolean isPlayerMob) {
		super(plugin, identityTag, boss, game, mob, isPlayerMob);

		Spell spell = new Spell() {
			int mTimer = 0;

			@Override
			public void run() {
				if (mIsPlayerMob) {
					MOB_PARTICLE_ALLY.spawn(mBoss.getLocation());
					AIR_PARTICLE_ALLY.spawn(mBoss.getLocation());
				} else {
					MOB_PARTICLE_ENEMY.spawn(mBoss.getLocation());
					AIR_PARTICLE_ENEMY.spawn(mBoss.getLocation());
				}
				if (mTimer % 5 == 0) {
					List<LivingEntity> targets = (mIsPlayerMob ? mGame.getFloorMobs() : mGame.getPlayerMobs());
					for (LivingEntity target : targets) {
						 if (target.getLocation().distance(mBoss.getLocation()) <= 8) {
							 if (EffectManager.getInstance() != null) {
								 EffectManager.getInstance().addEffect(target, "ITCurseOfTheSpeed", new PercentSpeed(20, -0.2, "ITCurseOfTheSpeed"));
							 }
							 EntityUtils.applyWeaken(Plugin.getInstance(), 20, 0.2, target);
						 }

					}
					if (mTimer % 20 == 0) {
						mTimer = 0;
						for (LivingEntity target : targets) {
							if (target.getLocation().distance(mBoss.getLocation()) <= 8) {
								DamageUtils.damage(mBoss, target, DamageEvent.DamageType.MAGIC, 1.0, null, true, false);
							}
						}
					}
				}
				mTimer++;
			}

			@Override
			public int cooldownTicks() {
				return 1;
			}
		};

		SpellManager active = new SpellManager(List.of(spell));

		super.constructBoss(active, Collections.emptyList(), -1, null, 30);

	}
}
