package com.playmonumenta.plugins.bosses.spells.hexfall.ruten;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SpellRutenDialogue extends Spell {

	private final Component mText;
	private final int mCooldown;
	private final Location mSpawnLoc;

	public SpellRutenDialogue(Component text, int cooldown, Location mSpawnLoc) {
		mText = text;
		mCooldown = cooldown;
		this.mSpawnLoc = mSpawnLoc;
	}

	@Override
	public void run() {
		for (Player player : HexfallUtils.getPlayersInRuten(mSpawnLoc)) {
			MessagingUtils.sendNPCMessage(player, "Ru'Ten", mText);
		}
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}
}
