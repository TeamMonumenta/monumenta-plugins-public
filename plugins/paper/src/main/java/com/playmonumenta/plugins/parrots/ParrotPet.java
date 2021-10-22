package com.playmonumenta.plugins.parrots;

import com.playmonumenta.plugins.parrots.ParrotManager.ParrotVariant;

import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;

public class ParrotPet {

	private String mName;
	private Parrot mParrot;
	private Player mPlayer;
	private Parrot.Variant mVariant;

	public ParrotPet(ParrotVariant variant, Player p) {
		this(variant, (Parrot)p.getWorld().spawnEntity(p.getLocation(), EntityType.PARROT), p);
	}

	public ParrotPet(ParrotVariant variant, Parrot spawnEntity, Player player) {
		mName = variant.getName();
		mParrot = spawnEntity;
		mPlayer = player;
		mVariant = variant.getVariant();

		mParrot.remove();
		mParrot.setSilent(true);
		mParrot.setCustomName(mName);
		mParrot.setCustomNameVisible(false);
		mParrot.setVariant(mVariant);
		mParrot.setSitting(true);
		mParrot.setOwner((AnimalTamer) mPlayer);
		mParrot.setTamed(true);
		mParrot.setAI(true);

	}

	public String getName() {
		return mName;
	}

	public Parrot getParrot() {
		return mParrot;
	}

	public Player getPlayer() {
		return mPlayer;
	}

	public Parrot.Variant getVariant() {
		return mVariant;
	}

	public void destroy() {
		mParrot.remove();
	}
}
