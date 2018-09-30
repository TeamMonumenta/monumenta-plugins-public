package com.playmonumenta.plugins.player.data;

import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin.Classes;
import com.playmonumenta.plugins.abilities.AbilityCollection;
import com.playmonumenta.plugins.specializations.ClassSpecialization;

/**
 * This class supports all of the in-game player data. Supports Classes, Specializations and spells.
 * @author FirelordWeaponry (Fire)
 *
 */
public class PlayerInfo {

	private final Player player;

	public AbilityCollection abilities = null;
	public int classId = 0;
	public int specId = 0;

	public PlayerInfo(Player player) {
		this.player = player;
	}

	//Just here if ever needed
	public Player getPlayer() { return player; }

}
