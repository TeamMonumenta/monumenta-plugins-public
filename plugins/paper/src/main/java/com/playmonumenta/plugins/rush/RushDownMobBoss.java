package com.playmonumenta.plugins.rush;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vindicator;
import org.bukkit.scheduler.BukkitRunnable;

public class RushDownMobBoss extends BossAbilityGroup {
	public static final String identityTag = "RushDownMob";
	public static final String SCALING_TAG = "RushSummonedMob";

	protected static final double HEIGHT = 2;

	private static final int SUMMONING_DURATION = 30;
	private static final EntityTargets TARGETS =
		new EntityTargets(EntityTargets.TARGETS.PLAYER, 50, EntityTargets.Limit.DEFAULT, List.of(EntityTargets.PLAYERFILTER.NOT_STEALTHED), EntityTargets.TagsListFiter.DEFAULT);

	public RushDownMobBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		if (!(boss instanceof Mob mob)) {
			return;
		}

		final List<Spell> passiveSpells = new ArrayList<>();

		final boolean hasAI = mob.hasAI();
		mob.setAI(false);
		mob.setInvulnerable(true);

		// Summoned mob not directly spawned by the waves
		if (ScoreboardUtils.checkTag(boss, SCALING_TAG)) {
			List<Player> players = (List<Player>) boss.getLocation().getNearbyPlayers(90);
			if (!players.isEmpty()) {
				RushArena arena = RushManager.mPlayerArenaMap.get(players.get(0));
				if (arena != null) {
					RushManager.scaleMobHealthMultiplayer(boss, arena.mRound);
					RushManager.scaleMobPastRound(boss, arena.mRound);
				}
			}
		}

		new BukkitRunnable() {
			int mTimer = 0;
			// Get mob location again, a tick later
			Location location = mob.getLocation();

			@Override
			public void run() {
				if (!boss.isValid()) {
					this.cancel();
					return;
				}
				if (mTimer == 0) {
					location = mob.getLocation();
					mob.setInvulnerable(false);
				}

				if (boss.getVehicle() == null) {
					EntityUtils.teleportStack(boss, location.add(0, HEIGHT / SUMMONING_DURATION, 0));
				}

				if (mTimer >= SUMMONING_DURATION) {
					this.cancel();
					return;
				}
				mTimer++;
			}

			@Override
			public synchronized void cancel() {
				super.cancel();
				List<? extends LivingEntity> targetsList = TARGETS.getTargetsList(mob);
				if (!targetsList.isEmpty()) {
					mob.setTarget(targetsList.get(0));
				}
				if (boss instanceof Vindicator) {
					passiveSpells.add(new SpellBlockBreak(boss));
				}
				boss.setAI(hasAI);
			}
		}.runTaskTimer(mPlugin, 1, 1);

		super.constructBoss(SpellManager.EMPTY, passiveSpells, 50, null, 0, 5);
	}

}
