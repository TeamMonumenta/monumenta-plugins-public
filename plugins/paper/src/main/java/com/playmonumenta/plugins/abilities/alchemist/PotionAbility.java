package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.scheduler.BukkitRunnable;

public abstract class PotionAbility extends Ability {

	private double mDamage;

	public PotionAbility(Plugin plugin, @Nullable Player player,
	                     String displayName, double damage1, double damage2) {
		super(plugin, player, displayName);

		// getAbilityScore() doesn't work until we set the mInfo.mScoreboard in the child class
		new BukkitRunnable() {
			@Override
			public void run() {
				mDamage = isLevelOne() ? damage1 : damage2;
			}
		}.runTaskLater(mPlugin, 1);
	}

	public void apply(LivingEntity mob, boolean isGruesome, ItemStatManager.PlayerItemStats playerItemStats) {

	}

	public void applyToPlayer(Player player, ThrownPotion potion, boolean isGruesome) {

	}

	public void createAura(Location loc, ThrownPotion potion, ItemStatManager.PlayerItemStats playerItemStats) {

	}

	public double getDamage() {
		return mDamage;
	}
}
