package com.playmonumenta.plugins.bosses.spells.hexfall.hycenea;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SpellBlueTransition extends Spell {

	private final Location mSpawnLoc;
	private final int mCooldown;

	public SpellBlueTransition(Location mSpawnLoc, int mCooldown) {
		this.mSpawnLoc = mSpawnLoc;
		this.mCooldown = mCooldown;
	}

	@Override
	public void run() {
		for (Player player : HexfallUtils.getPlayersInHycenea(mSpawnLoc)) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, false, false, false));
			player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20, 0, false, false, false));
			player.playSound(player.getLocation(), Sound.ENTITY_WITCH_CELEBRATE, SoundCategory.HOSTILE, 1f, 1f);
			player.teleport(mSpawnLoc.clone().add(0, 4, -7));
		}
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}
}
