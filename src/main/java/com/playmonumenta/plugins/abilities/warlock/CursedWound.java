package com.playmonumenta.plugins.abilities.warlock;

import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class CursedWound extends Ability {

	private static final int CURSED_WOUND_EFFECT_LEVEL = 1;
	private static final int CURSED_WOUND_DURATION = 6 * 20;
	private static final int CURSED_WOUND_RADIUS = 3;
	private static final int CURSED_WOUND_1_DAMAGE = 1;
	private static final int CURSED_WOUND_2_DAMAGE = 2;

	public CursedWound(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "CursedWound";
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		LivingEntity damagee = (LivingEntity) event.getEntity();
		int cursedWound = getAbilityScore();
		if (EntityUtils.isHostileMob(damagee)) {
			mPlayer.getWorld().spawnParticle(Particle.LAVA, damagee.getLocation().add(0, 1, 0), 4, 0.15, 0.15, 0.15, 0.0);
			PotionUtils.applyPotion(mPlayer, damagee, new PotionEffect(PotionEffectType.WITHER, CURSED_WOUND_DURATION, CURSED_WOUND_EFFECT_LEVEL, false, true));
			int damageMult = (cursedWound == 1) ? CURSED_WOUND_1_DAMAGE : CURSED_WOUND_2_DAMAGE;
			EntityUtils.damageEntity(mPlugin, damagee, damageMult, mPlayer);
		}

		if (cursedWound > 1 && PlayerUtils.isCritical(mPlayer)) {
			for (LivingEntity mob : EntityUtils.getNearbyMobs(damagee.getLocation(), CURSED_WOUND_RADIUS)) {
				PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.WITHER, CURSED_WOUND_DURATION, CURSED_WOUND_EFFECT_LEVEL, true, false));
			}
		}
		return true;
	}

	@Override
	public boolean runCheck() {
		return InventoryUtils.isScytheItem(mPlayer.getInventory().getItemInMainHand());
	}

}
