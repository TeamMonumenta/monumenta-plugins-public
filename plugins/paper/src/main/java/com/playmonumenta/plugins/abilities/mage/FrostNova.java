package com.playmonumenta.plugins.abilities.mage;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class FrostNova extends Ability {

	private static final float FROST_NOVA_RADIUS = 6.0f;
	private static final int FROST_NOVA_1_DAMAGE = 4;
	private static final int FROST_NOVA_2_DAMAGE = 8;
	private static final int FROST_NOVA_1_AMPLIFIER = 1;
	private static final int FROST_NOVA_2_AMPLIFIER = 3;
	private static final int FROST_NOVA_COOLDOWN = 18 * 20;
	private static final int FROST_NOVA_DURATION = 4 * 20;

	private int mDamage;
	private int mSlownessAmplifier;

	public FrostNova(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Frost Nova");
		mInfo.linkedSpell = Spells.FROST_NOVA;
		mInfo.scoreboardId = "FrostNova";
		mInfo.cooldown = FROST_NOVA_COOLDOWN;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;
		mDamage = getAbilityScore() == 1 ? FROST_NOVA_1_DAMAGE : FROST_NOVA_2_DAMAGE;
		mSlownessAmplifier = getAbilityScore() == 1 ? FROST_NOVA_1_AMPLIFIER : FROST_NOVA_2_AMPLIFIER;
	}

	@Override
	public void cast(Action action) {
		for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), FROST_NOVA_RADIUS, mPlayer)) {
			Vector velocity = mob.getVelocity();
			EntityUtils.damageEntity(mPlugin, mob, mDamage, mPlayer, MagicType.ICE);
			mob.setVelocity(velocity);
			if (EntityUtils.isElite(mob) || EntityUtils.isBoss(mob)) {
				PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.SLOW, FROST_NOVA_DURATION, mSlownessAmplifier - 1, true, false));
			} else {
				PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.SLOW, FROST_NOVA_DURATION, mSlownessAmplifier, true, false));
			}

			if (mob.getFireTicks() > 1) {
				mob.setFireTicks(1);
			}
		}

		// Extinguish fire on all nearby players
		for (Player player : PlayerUtils.playersInRange(mPlayer.getLocation(), FROST_NOVA_RADIUS)) {
			if (player.getFireTicks() > 1) {
				player.setFireTicks(1);
			}
		}

		new BukkitRunnable() {
			double radius = 0;
			Location loc = mPlayer.getLocation();
			@Override
			public void run() {
				radius += 1.25;
				for (double j = 0; j < 360; j += 18) {
					double radian1 = Math.toRadians(j);
					loc.add(Math.cos(radian1) * radius, 0.15, Math.sin(radian1) * radius);
					mWorld.spawnParticle(Particle.CLOUD, loc, 1, 0, 0, 0, 0.1);
					mWorld.spawnParticle(Particle.CRIT_MAGIC, loc, 8, 0, 0, 0, 0.65);
					loc.subtract(Math.cos(radian1) * radius, 0.15, Math.sin(radian1) * radius);
				}

				if (radius >= FROST_NOVA_RADIUS + 1) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
		Location loc = mPlayer.getLocation().add(0, 1, 0);
		mWorld.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1, 0.65f);
		mWorld.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1, 0.45f);
		mWorld.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.25f);
		mWorld.spawnParticle(Particle.CLOUD, loc, 25, 0, 0, 0, 0.35);
		mWorld.spawnParticle(Particle.SPIT, loc, 35, 0, 0, 0, 0.45);
		mWorld.playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.5f, 1.0f);
		putOnCooldown();
	}

	@Override
	public boolean runCheck() {
		if (mPlayer.isSneaking()) {
			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
			return InventoryUtils.isWandItem(mainHand);
		}
		return false;
	}

}
