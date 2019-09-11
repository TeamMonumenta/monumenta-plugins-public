package com.playmonumenta.plugins.abilities.warlock;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.events.CustomDamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class CursedWound extends Ability {

	private static final int CURSED_WOUND_EFFECT_LEVEL = 1;
	private static final int CURSED_WOUND_DURATION = 6 * 20;
	private static final int CURSED_WOUND_RADIUS = 3;
	private static final int CURSED_WOUND_1_DAMAGE = 1;
	private static final int CURSED_WOUND_2_DAMAGE = 2;

	private final int damageBonus;

	public CursedWound(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "CursedWound";
		damageBonus = getAbilityScore() == 1 ? CURSED_WOUND_1_DAMAGE : CURSED_WOUND_2_DAMAGE;

	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK) {
			LivingEntity damagee = (LivingEntity) event.getEntity();
			BlockData fallingDustData = Material.ANVIL.createBlockData();
			if (EntityUtils.isHostileMob(damagee)) {
				mWorld.spawnParticle(Particle.FALLING_DUST, damagee.getLocation().add(0, damagee.getHeight() / 2, 0), 3,
				                     (damagee.getWidth() / 2) + 0.1, damagee.getHeight() / 3, (damagee.getWidth() / 2) + 0.1, fallingDustData);
				mWorld.spawnParticle(Particle.SPELL_MOB, damagee.getLocation().add(0, damagee.getHeight() / 2, 0), 6,
				                     (damagee.getWidth() / 2) + 0.1, damagee.getHeight() / 3, (damagee.getWidth() / 2) + 0.1, 0);
				PotionUtils.applyPotion(mPlayer, damagee, new PotionEffect(PotionEffectType.WITHER, CURSED_WOUND_DURATION, CURSED_WOUND_EFFECT_LEVEL, false, true));
				event.setDamage(event.getDamage() + damageBonus);
				CustomDamageEvent customDamageEvent = new CustomDamageEvent(mPlayer, damagee, 0, null);
				Bukkit.getPluginManager().callEvent(customDamageEvent);
			}

			if (getAbilityScore() > 1 && PlayerUtils.isCritical(mPlayer)) {
				for (LivingEntity mob : EntityUtils.getNearbyMobs(damagee.getLocation(), CURSED_WOUND_RADIUS, mPlayer)) {
					mWorld.spawnParticle(Particle.FALLING_DUST, mob.getLocation().add(0, mob.getHeight() / 2, 0), 3,
					                     (mob.getWidth() / 2) + 0.1, mob.getHeight() / 3, (mob.getWidth() / 2) + 0.1, fallingDustData);
					mWorld.spawnParticle(Particle.SPELL_MOB, mob.getLocation().add(0, mob.getHeight() / 2, 0), 6,
					                     (mob.getWidth() / 2) + 0.1, mob.getHeight() / 3, (mob.getWidth() / 2) + 0.1, 0);
					PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.WITHER, CURSED_WOUND_DURATION, CURSED_WOUND_EFFECT_LEVEL, true, false));
				}
			}
		}

		return true;
	}

	@Override
	public boolean runCheck() {
		return InventoryUtils.isScytheItem(mPlayer.getInventory().getItemInMainHand());
	}

}
