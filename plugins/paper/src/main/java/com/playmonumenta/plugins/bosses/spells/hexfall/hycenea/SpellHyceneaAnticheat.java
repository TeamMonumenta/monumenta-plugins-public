package com.playmonumenta.plugins.bosses.spells.hexfall.hycenea;

import com.playmonumenta.plugins.bosses.bosses.hexfall.HyceneaRageOfTheWolf;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.classes.Alchemist;
import com.playmonumenta.plugins.classes.Cleric;
import com.playmonumenta.plugins.classes.Mage;
import com.playmonumenta.plugins.classes.Rogue;
import com.playmonumenta.plugins.classes.Scout;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.classes.Warlock;
import com.playmonumenta.plugins.classes.Warrior;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SpellHyceneaAnticheat extends Spell {

	private final HyceneaRageOfTheWolf mHycenea;
	private final LivingEntity mBoss;
	private final Location mSpawnLoc;
	private static final double SIZE_UP = 27.0;
	private static final double SIZE_DOWN = 18.0;
	private static final double SIZE_POSX = 36.5;
	private static final double SIZE_POSZ = 36.5;
	private static final double SIZE_NEGX = 36.5;
	private static final double SIZE_NEGZ = 37.5;

	public SpellHyceneaAnticheat(HyceneaRageOfTheWolf hycenea, LivingEntity boss, Location spawnLoc) {
		mHycenea = hycenea;
		mBoss = boss;
		mSpawnLoc = spawnLoc;
	}

	@Override
	public void run() {
		for (Player player : PlayerUtils.playersInRange(mSpawnLoc, HyceneaRageOfTheWolf.detectionRange, true)) {
			if (!HexfallUtils.playerInHycenea(player) && player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
				Location location = player.getLocation();
				double x = location.getX();
				double y = location.getY();
				double z = location.getZ();
				double sx = mSpawnLoc.getX();
				double sy = mSpawnLoc.getY();
				double sz = mSpawnLoc.getZ();

				if (x <= sx + SIZE_POSX && x >= sx - SIZE_NEGX && y <= sy + SIZE_UP && y >= sy - SIZE_DOWN && z <= sz + SIZE_POSZ && z >= sz - SIZE_NEGZ) {
					PlayerUtils.killPlayer(player, mBoss, null, true, true, true);
					AuditListener.logPlayer("[Hexfall] " + player.getName() + " was inside a Hycenea boss fight when they shouldn't have been.");
				}
			}
		}

		for (Player player : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
			int classNum = AbilityUtils.getClassNum(player);
			if (classNum == Alchemist.CLASS_ID || classNum == Cleric.CLASS_ID || classNum == Mage.CLASS_ID || classNum == Shaman.CLASS_ID || classNum == Warlock.CLASS_ID) {
				mHycenea.mSteelAdvancement = false;
			}
			if (classNum == Scout.CLASS_ID || classNum == Rogue.CLASS_ID || classNum == Warrior.CLASS_ID) {
				mHycenea.mSpellAdvancement = false;
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
