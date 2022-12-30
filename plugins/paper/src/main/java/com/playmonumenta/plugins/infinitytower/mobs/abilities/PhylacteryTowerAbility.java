package com.playmonumenta.plugins.infinitytower.mobs.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.infinitytower.TowerGame;
import com.playmonumenta.plugins.infinitytower.TowerMob;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class PhylacteryTowerAbility extends TowerAbility {

	private static final int TIMER_RESPAWN = 20 * 6;

	public PhylacteryTowerAbility(Plugin plugin, String identityTag, LivingEntity boss, TowerGame game, TowerMob mob, boolean isPlayerMob) {
		super(plugin, identityTag, boss, game, mob, isPlayerMob);

	}


	@Override
	public void death(@Nullable EntityDeathEvent event) {
		super.death(event);

		List<LivingEntity> mobs = mIsPlayerMob ? mGame.getPlayerMobs() : mGame.getFloorMobs();

		for (LivingEntity mob : mobs) {
			if (!mob.isDead() && mob.isValid()) {
				castReborn();
				return;
			}
		}


	}

	private void castReborn() {
		List<LivingEntity> list = new ArrayList<>();
		Objects.requireNonNull(mMob).spawnPuppet(mGame, list, mIsPlayerMob);
		new BukkitRunnable() {
			int mTimer = 0;

			@Override
			public void run() {
				if (mGame.isTurnEnded() || mGame.isGameEnded()) {
					cancel();
				}

				if (isCancelled()) {
					return;
				}

				if (mTimer >= TIMER_RESPAWN) {
					cancel();
					if (mIsPlayerMob) {
						mGame.mPlayerMobs.add(Objects.requireNonNull(mMob).spawn(mGame, true));
					} else {
						mGame.mFloorMobs.add(Objects.requireNonNull(mMob).spawn(mGame, false));
					}
				}

				mTimer += 10;
			}


			@Override
			public synchronized void cancel() throws IllegalStateException {
				super.cancel();
				for (LivingEntity target : list) {
					target.remove();
				}
			}
		}.runTaskTimer(mPlugin, 0, 10);

	}
}
