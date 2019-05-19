package com.playmonumenta.plugins.abilities.mage;

import java.util.Random;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.VectorUtils;

public class ArcaneStrike extends Ability {

	private static final float ARCANE_STRIKE_RADIUS = 4.0f;
	private static final int ARCANE_STRIKE_1_DAMAGE = 5;
	private static final int ARCANE_STRIKE_2_DAMAGE = 8;
	private static final int ARCANE_STRIKE_BURN_DAMAGE_LVL_1 = 2;
	private static final int ARCANE_STRIKE_BURN_DAMAGE_LVL_2 = 4;
	private static final int ARCANE_STRIKE_COOLDOWN = 6 * 20;

	public ArcaneStrike(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.ARCANE_STRIKE;
		mInfo.scoreboardId = "ArcaneStrike";
		mInfo.cooldown = ARCANE_STRIKE_COOLDOWN;
	}

	private static final Particle.DustOptions ARCANE_STRIKE_COLOR_1 = new Particle.DustOptions(Color.fromRGB(220, 147, 249), 1.0f);
	private static final Particle.DustOptions ARCANE_STRIKE_COLOR_2 = new Particle.DustOptions(Color.fromRGB(217, 122, 255), 1.0f);
	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		int arcaneStrike = getAbilityScore();
		LivingEntity damagee = (LivingEntity) event.getEntity();
		int extraDamage = arcaneStrike == 1 ? ARCANE_STRIKE_1_DAMAGE : ARCANE_STRIKE_2_DAMAGE;

		boolean hasSpellShock = AbilityManager.getManager().getPlayerAbility(mPlayer, Spellshock.class) != null;

		for (LivingEntity mob : EntityUtils.getNearbyMobs(damagee.getLocation(), ARCANE_STRIKE_RADIUS, mPlayer)) {
			int dmg = extraDamage;

			// Arcane strike extra fire damage
			// Check if (the mob is burning AND was not set on fire this tick) OR the mob has slowness
			if (mob.hasPotionEffect(PotionEffectType.SLOW) || mob.getFireTicks() > 0 &&
			    MetadataUtils.checkOnceThisTick(mPlugin, mob, Constants.ENTITY_COMBUST_NONCE_METAKEY)) {
				if (mob instanceof Player) {
					dmg += 2;
				} else {
					dmg += (arcaneStrike == 1 ? ARCANE_STRIKE_BURN_DAMAGE_LVL_1 : ARCANE_STRIKE_BURN_DAMAGE_LVL_2);
				}
			}

			EntityUtils.damageEntity(mPlugin, mob, dmg, mPlayer, MagicType.ARCANE);
		}
		{
			Location loc = damagee.getLocation();
			mWorld.spawnParticle(Particle.DRAGON_BREATH, loc.clone().add(0, 1, 0), 75, 0, 0, 0, 0.25);
			mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc.clone().add(0, 1, 0), 35, 0, 0, 0, 0.2);
			mWorld.spawnParticle(Particle.SPELL_WITCH, loc.clone().add(0, 1, 0), 150, 2.5, 1, 2.5, 0.001);
			mWorld.playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.75f, 1.5f);
		}

		Location loc = mPlayer.getLocation().add(mPlayer.getLocation().getDirection().multiply(0.5));
		mWorld.playSound(loc, Sound.ENTITY_WITHER_SHOOT, 0.75f, 1.65f);
		mWorld.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.75f, 0.5f);
		new BukkitRunnable() {
			double d = 30;
			@Override
			public void run() {
				Vector vec;
				for (double r = 1; r < 4; r += 0.5) {
					for (double degree = d; degree < d + 60; degree += 8) {
						double radian1 = Math.toRadians(degree);
						vec = new Vector(Math.cos(radian1) * r, 1, Math.sin(radian1) * r);
						vec = VectorUtils.rotateXAxis(vec, -loc.getPitch());
						vec = VectorUtils.rotateYAxis(vec, loc.getYaw());

						Location l = loc.clone().add(vec);
						mWorld.spawnParticle(Particle.REDSTONE, l, 1, 0.1, 0.1, 0.1, ARCANE_STRIKE_COLOR_1);
						mWorld.spawnParticle(Particle.REDSTONE, l, 1, 0.1, 0.1, 0.1, ARCANE_STRIKE_COLOR_2);
					}
				}
				d += 60;
				if (d >= 150) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
		putOnCooldown();
		return true;
	}

	@Override
	public boolean runCheck() {
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		if (InventoryUtils.isWandItem(mainHand)) {
			return true;
		}
		return false;
	}
}
