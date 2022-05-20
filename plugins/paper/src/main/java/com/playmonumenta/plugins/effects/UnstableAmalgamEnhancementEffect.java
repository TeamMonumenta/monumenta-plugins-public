package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.abilities.alchemist.UnstableAmalgam;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class UnstableAmalgamEnhancementEffect extends Effect {

	private final @NotNull Player mPlayer;
	private final @NotNull UnstableAmalgam mUnstableAmalgam;
	private final @NotNull ItemStatManager.PlayerItemStats mStats;

	public UnstableAmalgamEnhancementEffect(int duration, @NotNull Player alchemist, @NotNull UnstableAmalgam unstableAmalgam, @NotNull ItemStatManager.PlayerItemStats stats) {
		super(duration);
		mPlayer = alchemist;
		mUnstableAmalgam = unstableAmalgam;
		mStats = stats;
	}

	@Override
	public void onDamage(LivingEntity entity, DamageEvent event, LivingEntity enemy) {
		Location eyeLoc = entity.getEyeLocation();
		Location enemyEyeLoc = enemy.getEyeLocation();
		Vector dir = enemyEyeLoc.clone().toVector().subtract(eyeLoc.toVector()).normalize();
		ThrownPotion potion = mPlayer.launchProjectile(ThrownPotion.class);
		potion.teleport(eyeLoc);
		potion.setVelocity(dir);
		//we may need to multiply the velocity a bit more?
		mUnstableAmalgam.setEnhancementThrowPotion(potion, mStats);

		clearEffect();
	}

	@Override public String toString() {
		return String.format("UnstableAmalgamEnhancement duration:%d from:%s", this.getDuration(), mPlayer.getName());
	}
}
