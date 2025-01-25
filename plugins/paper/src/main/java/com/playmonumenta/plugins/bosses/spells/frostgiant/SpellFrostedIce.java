package com.playmonumenta.plugins.bosses.spells.frostgiant;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.bosses.FrostGiant;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.DamageUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

/* Frost Giant melts frosted ice that he walks over and players get damaged while standing on frosted ice */
public final class SpellFrostedIce extends Spell {
	private static final String SPELL_NAME = "Frosted Ice";
	private final FrostGiant mFrostGiant;
	private int mTicks = 0;

	public SpellFrostedIce(final FrostGiant frostGiant) {
		mFrostGiant = frostGiant;
	}

	@Override
	public void run() {
		mTicks += BossAbilityGroup.PASSIVE_RUN_INTERVAL_DEFAULT;
		breakFrostedIce();
		if (mTicks % Constants.HALF_TICKS_PER_SECOND == 0) {
			damagePlayersOnIce();
			mTicks = 0;
		}
	}

	@Override
	public int cooldownTicks() {
		return 1;
	}

	private void breakFrostedIce() {
		final Location testLoc = mFrostGiant.mBoss.getLocation();
		final double testLocX = testLoc.getX();
		final double testLocZ = testLoc.getZ();
		testLoc.setY(testLoc.getY() - 1);

		for (double x = testLocX - 2; x <= testLocX + 2; x++) {
			for (double z = testLocZ - 2; z <= testLocZ + 2; z++) {
				testLoc.setX(x);
				testLoc.setZ(z);
				if (testLoc.getBlock().getType() == FrostGiant.ICE_TYPE) {
					testLoc.getBlock().setType(Material.CRACKED_STONE_BRICKS);
				}
			}
		}
	}

	private void damagePlayersOnIce() {
		for (final Player player : mFrostGiant.getArenaParticipants()) {
			final Block playerBlock = player.getLocation().getBlock();
			if ((playerBlock.getRelative(BlockFace.DOWN).getType() != Material.AIR || playerBlock.getType() != Material.AIR)
				&& (playerBlock.getRelative(BlockFace.DOWN).getType() == FrostGiant.ICE_TYPE || playerBlock.getType() == FrostGiant.ICE_TYPE)) {
				DamageUtils.damage(mFrostGiant.mBoss, player, DamageEvent.DamageType.MAGIC, 18, null, false, false, SPELL_NAME);
			}
		}
	}
}
