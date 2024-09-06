package com.playmonumenta.plugins.bosses.spells.hexfall.hycenea;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SpellHyceneaDialogue extends Spell {

	private final Component mText;
	private final int mCooldown;
	private final Location mSpawnLoc;
	private final boolean mNPCMessage;

	public SpellHyceneaDialogue(Component text, int cooldown, Location mSpawnLoc, boolean mNPCMessage) {
		mText = text;
		mCooldown = cooldown;
		this.mSpawnLoc = mSpawnLoc;
		this.mNPCMessage = mNPCMessage;
	}

	@Override
	public void run() {
		for (Player player : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
			if (mNPCMessage) {
				MessagingUtils.sendNPCMessage(player, "Hycenea", mText);
			} else {
				player.sendMessage(mText);
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}
}
