package com.playmonumenta.plugins.cosmetics.skills;

import org.bukkit.entity.Player;

public interface LockableCS {

	boolean isUnlocked(Player player);

	String[] getLockDesc();

}
