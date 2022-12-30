package com.playmonumenta.plugins.infinitytower.mobs.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.infinitytower.TowerConstants;
import com.playmonumenta.plugins.infinitytower.TowerFileUtils;
import com.playmonumenta.plugins.infinitytower.TowerGame;
import com.playmonumenta.plugins.infinitytower.TowerGameUtils;
import com.playmonumenta.plugins.infinitytower.TowerManager;
import com.playmonumenta.plugins.infinitytower.TowerMob;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public class SvalgotGhalkorTowerAbility extends TowerAbility {

	private static final String SVALGOT_LOS = "ITSvalgot";
	private static final String GHALKOR_LOS = "ITGhalkor";

	public SvalgotGhalkorTowerAbility(Plugin plugin, String identityTag, LivingEntity boss, TowerGame game, TowerMob mob, boolean isPlayerMob) {
		super(plugin, identityTag, boss, game, mob, isPlayerMob);

		try {
			Location mobLoc = boss.getLocation();
			Location svalgotLoc = mobLoc.clone().add(0, 0, 0.25);
			Location ghalkorLoc = mobLoc.clone().add(0, 0, -0.25);

			final LivingEntity svalgot = Objects.requireNonNull((LivingEntity) LibraryOfSoulsIntegration.summon(svalgotLoc, SVALGOT_LOS));
			svalgot.setCustomNameVisible(true);
			svalgot.customName(TowerGameUtils.getMobNameComponent("Svalgot", isPlayerMob));

			final LivingEntity ghalkor = Objects.requireNonNull((LivingEntity) LibraryOfSoulsIntegration.summon(ghalkorLoc, GHALKOR_LOS));
			ghalkor.setCustomNameVisible(true);
			ghalkor.customName(TowerGameUtils.getMobNameComponent("Ghalkor", isPlayerMob));

			svalgot.addScoreboardTag(mob.mInfo.mMobRarity.getTag() + "_" + mob.mMobLevel);
			ghalkor.addScoreboardTag(mob.mInfo.mMobRarity.getTag() + "_" + mob.mMobLevel);

			ghalkor.addScoreboardTag(mob.mInfo.mMobClass.getTag());
			svalgot.addScoreboardTag(mob.mInfo.mMobClass.getTag());

			ghalkor.addScoreboardTag(mIsPlayerMob ? TowerConstants.MOB_TAG_PLAYER_TEAM : TowerConstants.MOB_TAG_FLOOR_TEAM);
			svalgot.addScoreboardTag(mIsPlayerMob ? TowerConstants.MOB_TAG_PLAYER_TEAM : TowerConstants.MOB_TAG_FLOOR_TEAM);

			ghalkor.addScoreboardTag(TowerConstants.MOB_TAG);
			svalgot.addScoreboardTag(TowerConstants.MOB_TAG);

			TowerGameUtils.startMob(svalgot, null, game, isPlayerMob);
			TowerGameUtils.startMob(ghalkor, null, game, isPlayerMob);


			if (BossManager.getInstance() != null) {
				BossManager.getInstance().createBossInternal(svalgot, new VoidWalkerTowerAbility(plugin, "Void Walker", svalgot, game, mob, mIsPlayerMob));
				BossManager.getInstance().createBossInternal(ghalkor, new ForgemasterTowerAbility(plugin, "Forge master", ghalkor, game, mob, mIsPlayerMob));
			}

			if (mIsPlayerMob) {
				Bukkit.getScheduler().runTaskLater(TowerManager.mPlugin, () -> {
					mGame.mPlayerMobs.add(ghalkor);
					mGame.mPlayerMobs.add(svalgot);
					boss.remove();
				}, 1);
			} else {
				Bukkit.getScheduler().runTaskLater(TowerManager.mPlugin, () -> {
					mGame.mFloorMobs.add(ghalkor);
					mGame.mFloorMobs.add(svalgot);
					boss.remove();
				}, 1);
			}

		} catch (Exception e) {
			TowerFileUtils.warning("Catch an exception while spawning Svalgot & Ghalkor. Reason: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
