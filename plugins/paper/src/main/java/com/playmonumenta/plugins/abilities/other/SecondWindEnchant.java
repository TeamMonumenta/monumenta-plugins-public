package com.playmonumenta.plugins.abilities.other;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.enchantments.evasions.SecondWind;
import com.playmonumenta.plugins.tracking.PlayerTracking;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;

public class SecondWindEnchant extends Ability {

	private static final int SECOND_WIND_IFRAMES = 20 * 2;

	public SecondWindEnchant(Plugin plugin, World world, Player player) {
		super(plugin, world, player, null);
	}

	@Override
	public boolean playerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		execute(event);
		return true;
	}

	@Override
	public boolean playerDamagedByProjectileEvent(EntityDamageByEntityEvent event) {
		execute(event);
		return true;
	}

	private void execute(EntityDamageByEntityEvent event) {
		if (PlayerTracking.getInstance().getPlayerCustomEnchantLevel(mPlayer, SecondWind.class) > 0
				&& mPlayer.getHealth() + AbsorptionUtils.getAbsorption(mPlayer) < EntityUtils.getRealFinalDamage(event)) {
			Location loc = mPlayer.getLocation().add(0, 1, 0);
			EvasionEnchant evasion = AbilityManager.getManager().getPlayerAbility(mPlayer, EvasionEnchant.class);

			double originalDamage = event.getDamage() / (1 - evasion.mLastCounterAmountConsumed / 25.0);
			evasion.mCounter -= 5;
			evasion.mLastCounterAmountConsumed += 5;
			event.setDamage(originalDamage * (1 - evasion.mLastCounterAmountConsumed / 25.0));

			mWorld.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1.5f);
			mWorld.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1f, 2f);
			mWorld.playSound(loc, Sound.ITEM_TOTEM_USE, 0.5f, 0.2f);
			mWorld.spawnParticle(Particle.CLOUD, loc, 50, 0, 0, 0, 0.5);
			mWorld.spawnParticle(Particle.TOTEM, loc, 50, 0, 0, 0, 0.5);

			new BukkitRunnable() {
				@Override
				public void run() {
					mPlayer.setNoDamageTicks(SECOND_WIND_IFRAMES);
				}
			}.runTaskLater(mPlugin, 1);
		}
	}

	@Override
	public boolean canUse(Player player) {
		return true;
	}

}
