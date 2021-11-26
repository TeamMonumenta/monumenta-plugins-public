package com.playmonumenta.plugins.parrots;

import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.parrots.ParrotManager.ParrotVariant;

public class ParrotPet {

	private String mName;
	private Player mPlayer;
	private Parrot.Variant mVariant;

	public ParrotPet(ParrotVariant variant, Player player) {
		mName = variant.getName();
		mPlayer = player;
		mVariant = variant.getVariant();
	}

	public Parrot spawnParrot() {
		Parrot parrot = (Parrot) mPlayer.getWorld().spawnEntity(mPlayer.getLocation().add(0, -256, 0), EntityType.PARROT);
		parrot.addScoreboardTag(ParrotManager.PARROT_TAG);
		parrot.setCustomName(mName);
		parrot.setVariant(mVariant);
		parrot.remove();
		parrot.setSilent(true);
		parrot.setCustomNameVisible(false);
		parrot.setSitting(true);
		parrot.setOwner((AnimalTamer) mPlayer);
		parrot.setTamed(true);
		parrot.setAI(true);
		parrot.setInvisible(true);
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
