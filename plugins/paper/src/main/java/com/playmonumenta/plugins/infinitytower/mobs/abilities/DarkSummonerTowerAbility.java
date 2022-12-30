package com.playmonumenta.plugins.infinitytower.mobs.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.infinitytower.TowerConstants;
import com.playmonumenta.plugins.infinitytower.TowerFileUtils;
import com.playmonumenta.plugins.infinitytower.TowerGame;
import com.playmonumenta.plugins.infinitytower.TowerGameUtils;
import com.playmonumenta.plugins.infinitytower.TowerMob;
import com.playmonumenta.plugins.infinitytower.mobs.TowerMobClass;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

public class DarkSummonerTowerAbility extends TowerAbility {

	private final List<String> SUMMONS = Arrays.asList("ITSarintultheUnseen", "ITZirinkelthePrecise", "ITKazarthuntheMighty", "ITVerkantaltheCunning");

	public DarkSummonerTowerAbility(Plugin plugin, String identityTag, LivingEntity boss, TowerGame game, TowerMob mob, boolean isPlayerMob) {
		super(plugin, identityTag, boss, game, mob, isPlayerMob);


		Spell spell = new Spell() {

			@Override
			public void run() {
				if (mGame.isTurnEnded()) {
					return;
				}
				try {
					LivingEntity livingEntity = Objects.requireNonNull((LivingEntity) LibraryOfSoulsIntegration.summon(mBoss.getLocation(), SUMMONS.get(FastUtils.RANDOM.nextInt(SUMMONS.size()))));
					String mobName = livingEntity.getName().replace("IT", "");
					livingEntity.customName(TowerGameUtils.getMobNameComponent(mobName, mIsPlayerMob));
					livingEntity.setCustomNameVisible(true);
					livingEntity.addScoreboardTag(mob.mInfo.mMobRarity.getTag() + "_" + mob.mMobLevel);
					livingEntity.addScoreboardTag(TowerMobClass.SPECIAL.getTag());
					livingEntity.addScoreboardTag(TowerConstants.MOB_TAG);
					livingEntity.addScoreboardTag(mIsPlayerMob ? TowerConstants.MOB_TAG_PLAYER_TEAM : TowerConstants.MOB_TAG_FLOOR_TEAM);
					if (mIsPlayerMob) {
						game.mPlayerMobs.add(livingEntity);
					} else {
						game.mFloorMobs.add(livingEntity);
					}

					if (livingEntity instanceof Mob mob && mBoss instanceof Mob boss1) {
						mob.setTarget(boss1.getTarget());
					}
					TowerGameUtils.startMob(livingEntity, null, game, mIsPlayerMob);
				} catch (Exception e) {
					TowerFileUtils.warning("Exception while spawning mob for DarkSummoner. Reason: " + e.getMessage());
				}
			}

			@Override
			public int cooldownTicks() {
				return 120;
			}

			@Override
			public boolean canRun() {
				return !mGame.isGameEnded() && !mGame.isTurnEnded();
			}
		};

		SpellManager active = new SpellManager(List.of(spell));

		super.constructBoss(active, Collections.emptyList(), -1, null, 20);
	}
}
