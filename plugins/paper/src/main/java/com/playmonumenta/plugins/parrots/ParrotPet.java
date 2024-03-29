package com.playmonumenta.plugins.parrots;

import com.playmonumenta.plugins.parrots.ParrotManager.ParrotVariant;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;

public class ParrotPet {

	private final String mName;
	private final Player mPlayer;
	private final Parrot.Variant mVariant;

	public ParrotPet(ParrotVariant variant, Player player) {
		mName = variant.getName();
		mPlayer = player;
		mVariant = variant.getVariant();
	}

	public Parrot spawnParrot(Location location) {
		Parrot parrot = (Parrot) location.getWorld().spawnEntity(location, EntityType.PARROT);
		parrot.customName(Component.text(mName));
		parrot.setVariant(mVariant);
		parrot.setSilent(true);
		parrot.setCustomNameVisible(false);
		parrot.setSitting(true);
		parrot.setOwner(mPlayer);
		parrot.setTamed(true);
		parrot.setAI(true);
		parrot.setLootTable(null);
		return parrot;
	}

	public String getName() {
		return mName;
	}

	public Player getPlayer() {
		return mPlayer;
	}

	public Parrot.Variant getVariant() {
		return mVariant;
	}
}
