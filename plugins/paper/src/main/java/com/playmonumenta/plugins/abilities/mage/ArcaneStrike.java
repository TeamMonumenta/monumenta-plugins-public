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
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.VectorUtils;

public class ArcaneStrike extends Ability {

	private static final Particle.DustOptions ARCANE_STRIKE_COLOR_1 = new Particle.DustOptions(Color.fromRGB(220, 147, 249), 1.0f);
	private static final Particle.DustOptions ARCANE_STRIKE_COLOR_2 = new Particle.DustOptions(Color.fromRGB(217, 122, 255), 1.0f);

	private static final float ARCANE_STRIKE_RADIUS = 4.0f;
	private static final int ARCANE_STRIKE_1_DAMAGE = 5;
	private static final int ARCANE_STRIKE_2_DAMAGE = 8;
	private static final int ARCANE_STRIKE_1_BONUS_DAMAGE = 2;
	private static final int ARCANE_STRIKE_2_BONUS_DAMAGE = 4;
	private static final int ARCANE_STRIKE_COOLDOWN = 6 * 20;

	private final int mDamageBonus;
	private final int mDamageBonusAffected;

	public ArcaneStrike(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Arcane Strike");
		mInfo.linkedSpell = Spells.ARCANE_STRIKE;
		mInfo.scoreboardId = "ArcaneStrike";
		mInfo.mShorthandName = "AS";
		mInfo.mDescriptions.add("When you attack an enemy with a wand, you unleash an arcane explosion dealing 5 damage to all mobs in a 4 block radius around the target. Enemies that are on fire or slowed take 2 extra damage. Arcane strike can not trigger Spellshock's static. Cooldown: 6s.");
		mInfo.mDescriptions.add("The damage is increased to 8. Mobs that are on fire or slowed take 4 additional damage.");
		mInfo.cooldown = ARCANE_STRIKE_COOLDOWN;
		mDamageBonus = getAbilityScore() == 1 ? ARCANE_STRIKE_1_DAMAGE : ARCANE_STRIKE_2_DAMAGE;
		mDamageBonusAffected = getAbilityScore() == 1 ? ARCANE_STRIKE_1_BONUS_DAMAGE : ARCANE_STRIKE_2_BONUS_DAMAGE;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK) {
			LivingEntity damagee = (LivingEntity) event.getEntity();

			for (LivingEntity mob : EntityUtils.getNearbyMobs(damagee.getLocation(), ARCANE_STRIKE_RADIUS, mPlayer)) {
				int dmg = mDamageBonus;

				// Arcane Strike extra damage if on fire or slowed (but effect not applied this tick)
				if ((mob.hasPotionEffect(PotionEffectType.SLOW)
				     && !MetadataUtils.happenedThisTick(mPlugin, mob, Constants.ENTITY_SLOWED_NONCE_METAKEY, 0))
				    || (mob.getFireTicks() > 0
				        && !MetadataUtils.happenedThisTick(mPlugin, mob, Constants.ENTITY_COMBUST_NONCE_METAKEY, 0))) {
					dmg += mDamageBonusAffected;
				}

				Vector velocity = mob.getVelocity();
				EntityUtils.damageEntity(mPlugin, mob, dmg, mPlayer, MagicType.ARCANE, true, mInfo.linkedSpell, true, false);
				mob.setVelocity(velocity);
			}

			Location locD = damagee.getLocation().add(0, 1, 0);
			mWorld.spawnParticle(Particle.DRAGON_BREATH, locD, 75, 0, 0, 0, 0.25);
			mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, locD, 35, 0, 0, 0, 0.2);
			mWorld.spawnParticle(Particle.SPELL_WITCH, locD, 150, 2.5, 1, 2.5, 0.001);
			mWorld.playSound(locD, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.75f, 1.5f);

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
		}

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
