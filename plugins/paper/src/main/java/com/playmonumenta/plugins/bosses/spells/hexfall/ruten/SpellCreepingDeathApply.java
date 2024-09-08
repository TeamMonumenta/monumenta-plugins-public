package com.playmonumenta.plugins.bosses.spells.hexfall.ruten;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.hexfall.Ruten;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.hexfall.CreepingDeath;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SpellCreepingDeathApply extends Spell {

	private static final int APPLY_EFFECT_INTERVAL = 10;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mSpawnLoc;
	private int mTicks = 0;

	public SpellCreepingDeathApply(Plugin plugin, LivingEntity boss, Location spawnLoc) {
		mPlugin = plugin;
		mBoss = boss;
		mSpawnLoc = spawnLoc;
	}

	@Override
	public void run() {

		if (mTicks >= APPLY_EFFECT_INTERVAL) {
			for (Player player : HexfallUtils.getPlayersInRuten(mSpawnLoc)) {
				if (Ruten.getTendencyAtPlayer(player).equals(Ruten.AnimaTendency.DEATH) || LocationUtils.xzDistance(player.getLocation(), mSpawnLoc) > Ruten.arenaRadius) {
					DamageUtils.damage(mBoss, player, DamageEvent.DamageType.TRUE, 1, null, true, false, CreepingDeath.GENERIC_NAME);
					CreepingDeath creepingDeath = mPlugin.mEffectManager.getActiveEffect(player, CreepingDeath.class);
					if (creepingDeath != null) {
						int stacks = (int) creepingDeath.getMagnitude();
						creepingDeath.setStacks(stacks + 1);
						player.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 0.1f * ((float) stacks + 1 / 4f), 0.1f);
					} else {
						mPlugin.mEffectManager.addEffect(player, CreepingDeath.GENERIC_NAME, new CreepingDeath(20 * 300, mPlugin));
					}
				}
			}
			mTicks = 0;
		}

		mTicks++;
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
