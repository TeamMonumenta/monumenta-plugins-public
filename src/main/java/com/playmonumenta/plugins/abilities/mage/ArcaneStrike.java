package com.playmonumenta.plugins.abilities.mage;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class ArcaneStrike extends Ability {

	private static final float ARCANE_STRIKE_RADIUS = 4.0f;
	private static final int ARCANE_STRIKE_1_DAMAGE = 5;
	private static final int ARCANE_STRIKE_2_DAMAGE = 8;
	private static final int ARCANE_STRIKE_BURN_DAMAGE_LVL_1 = 2;
	private static final int ARCANE_STRIKE_BURN_DAMAGE_LVL_2 = 4;
	private static final int ARCANE_STRIKE_COOLDOWN = 6 * 20;

	public ArcaneStrike(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.classId = 1;
		mInfo.specId = -1;
		mInfo.linkedSpell = Spells.ARCANE_STRIKE;
		mInfo.scoreboardId = "ArcaneStrike";
		mInfo.cooldown = ARCANE_STRIKE_COOLDOWN;
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		int arcaneStrike = getAbilityScore();
		LivingEntity damagee = (LivingEntity) event.getEntity();
		int extraDamage = arcaneStrike == 1 ? ARCANE_STRIKE_1_DAMAGE : ARCANE_STRIKE_2_DAMAGE;

		for (LivingEntity mob : EntityUtils.getNearbyMobs(damagee.getLocation(), ARCANE_STRIKE_RADIUS)) {
			int dmg = extraDamage;

			// Arcane strike extra fire damage
			// Check if (the mob is burning AND was not set on fire this tick) OR the mob has slowness
			//

			if (arcaneStrike > 0 && ((mob.getFireTicks() > 0 &&
			                          MetadataUtils.checkOnceThisTick(mPlugin, mob, Constants.ENTITY_COMBUST_NONCE_METAKEY)) ||
			                         mob.hasPotionEffect(PotionEffectType.SLOW))) {
				dmg += (arcaneStrike == 1 ? ARCANE_STRIKE_BURN_DAMAGE_LVL_1 : ARCANE_STRIKE_BURN_DAMAGE_LVL_2);
			}

			EntityUtils.damageEntity(mPlugin, mob, dmg, mPlayer, MagicType.ARCANE);
		}

		Location loc = damagee.getLocation();
		mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc.add(0, 1, 0), 50, 2.5, 1, 2.5, 0.001);
		mWorld.spawnParticle(Particle.SPELL_WITCH, loc.add(0, 1, 0), 200, 2.5, 1, 2.5, 0.001);
		mWorld.playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.5f, 1.5f);

		PlayerUtils.callAbilityCastEvent(mPlayer, Spells.ARCANE_STRIKE);
		putOnCooldown();
		return true;
	}

	@Override
	public boolean runCheck() {
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		if (InventoryUtils.isWandItem(mainHand)) {
			if (!MetadataUtils.checkOnceThisTick(mPlugin, mPlayer, Constants.ENTITY_DAMAGE_NONCE_METAKEY)) {
				return true;
			}
		}
		return false;
	}
}
