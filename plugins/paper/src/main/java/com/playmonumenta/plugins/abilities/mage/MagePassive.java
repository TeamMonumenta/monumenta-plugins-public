package com.playmonumenta.plugins.abilities.mage;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

public class MagePassive extends Ability {

	private static final int TIME_TO_FIRE_RESISTANCE = 20 * 2;
	private static final int FIRE_RESISTANCE_DURATION = 20 * 4;

	private int mTicksOnFire = 0;

	public MagePassive(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, null);
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, "Class") == 1;
	}

	@Override
	public void periodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		if (mTicksOnFire > TIME_TO_FIRE_RESISTANCE) {
			mTicksOnFire = 0;
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
					new PotionEffect(PotionEffectType.FIRE_RESISTANCE, FIRE_RESISTANCE_DURATION, 0, false, true));
		}

		if (mPlayer.getFireTicks() > 0 && mPlayer.getPotionEffect(PotionEffectType.FIRE_RESISTANCE) == null) {
			mTicksOnFire += 5;
		}
	}

	@Override
	public boolean playerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		if (event.getDamager() instanceof Player) {
			event.setDamage(event.getDamage() * 1.15);
		}
		return true;
	}

}
