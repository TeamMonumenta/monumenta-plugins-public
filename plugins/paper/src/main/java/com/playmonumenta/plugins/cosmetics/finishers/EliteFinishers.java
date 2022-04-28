package com.playmonumenta.plugins.cosmetics.finishers;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class EliteFinishers {

	// Delegate based on elite finisher name
	public static void activateFinisher(Player p, Entity killedMob, Location loc, String finisherName) {
		//TODO add more finisher cases

		switch (finisherName) {
			case LightningFinisher.NAME:
				LightningFinisher.run(p, killedMob, loc);
				break;
			case WarmFireworkFinisher.NAME:
				WarmFireworkFinisher.run(p, killedMob, loc);
				break;
			case CoolFireworkFinisher.NAME:
				CoolFireworkFinisher.run(p, killedMob, loc);
				break;
			case VictoryThemeFinisher.NAME:
				VictoryThemeFinisher.run(p, killedMob, loc);
				break;
			case BirthdayThemeFinisher.NAME:
				BirthdayThemeFinisher.run(p, killedMob, loc);
				break;
			case CakeifyFinisher.NAME:
				CakeifyFinisher.run(p, killedMob, loc);
				break;
			default:
				break;
		}

	}
}
