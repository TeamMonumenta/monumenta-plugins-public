package com.playmonumenta.plugins.infinitytower.mobs.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.infinitytower.TowerConstants;
import com.playmonumenta.plugins.infinitytower.TowerGame;
import com.playmonumenta.plugins.infinitytower.TowerMob;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

public class ImmortalTowerAbility extends TowerAbility {
	public static final String IMMORTAL_TAG = "IMMORTAL";

	public ImmortalTowerAbility(Plugin plugin, String identityTag, LivingEntity boss, TowerGame game, TowerMob mob, boolean isPlayerMob) {
		super(plugin, identityTag, boss, game, mob, isPlayerMob);

		mBoss.addScoreboardTag(IMMORTAL_TAG);
		mBoss.addScoreboardTag(TowerConstants.MOB_TAG_UNTARGETABLE);

		Spell spell = new Spell() {

			@Override
			public void run() {
				boolean die = true;
				for (LivingEntity entity : !mIsPlayerMob ? mGame.getFloorMobs() : mGame.getPlayerMobs()) {
					if (!entity.getScoreboardTags().contains(IMMORTAL_TAG)) {
						die = false;
						break;
					}
				}

				if (die) {
					mBoss.remove();
					Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
						mGame.towerMobsDied(mBoss);
					}, 0);
				}

			}

			@Override
			public int cooldownTicks() {
				return 10;
			}
		};

		SpellManager active = new SpellManager(List.of(spell));

		super.constructBoss(active, Collections.emptyList(), -1, null, 20);


	}
}
