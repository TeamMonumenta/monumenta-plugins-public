package com.playmonumenta.plugins.abilities.mage;

import java.util.Random;

import org.bukkit.Bukkit;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

/*
 * Channeling: After casting a spell, your next melee hit deals 3 / 6 extra damage.
 * Depending on the spell type cast (fire, ice, arcane), your attack will also
 * set the hit enemy on fire, apply slowness II, or apply weakness I for 4 seconds.
 */

public class Channeling extends Ability {

	private static final int CHANNELING_1_DAMAGE = 3;
	private static final int CHANNELING_2_DAMAGE = 6;
	private static final int CHANNELING_EFFECT_DURATION = 20 * 4;

	private static final Particle.DustOptions CHANNELING_COLOR = new Particle.DustOptions(Color.fromRGB(91, 187, 255), 0.75f);

	private int mDamage;
	private Spells mLastSpellCast;

	public Channeling(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Channeling");
		mInfo.scoreboardId = "Channeling";
		mDamage = getAbilityScore() == 1 ? CHANNELING_1_DAMAGE : CHANNELING_2_DAMAGE;
	}

	@Override
	public boolean abilityCastEvent(AbilityCastEvent event) {
		if (mLastSpellCast != null && event.getAbility() == Spells.ARCANE_STRIKE) {
			//Do not replace previous with arcane strike
		} else {
			mLastSpellCast = event.getAbility();
		}
		return true;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (mLastSpellCast != null && event.getEntity() instanceof LivingEntity && event.getCause() == DamageCause.ENTITY_ATTACK) {
			event.setDamage(event.getDamage() + mDamage);
			LivingEntity mob = (LivingEntity) event.getEntity();
			Location loc = mob.getLocation();
			mWorld.playSound(loc, Sound.BLOCK_ENDER_CHEST_OPEN, 1f, 1.5f);
			mWorld.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 2f);
			mWorld.spawnParticle(Particle.REDSTONE, loc.clone().add(0, 1, 0), 25, 0.45, 0.65, 0.45, 0, CHANNELING_COLOR);
			mWorld.spawnParticle(Particle.FIREWORKS_SPARK, loc.clone().add(0, 1, 0), 8, 0.25, 0.5, 0.25, 0.05);
			mWorld.spawnParticle(Particle.FIREWORKS_SPARK, loc, 8, 0.35, 0, 0.35, 0.12);

			if (mLastSpellCast == Spells.MAGMA_SHIELD || mLastSpellCast == Spells.STARFALL) {
				mWorld.spawnParticle(Particle.FLAME, loc.clone().add(0, 1, 0), 18, 0.25, 0.5, 0.25, 0.05);
				mWorld.spawnParticle(Particle.SMOKE_NORMAL, loc.clone().add(0, 1, 0), 10, 0.25, 0.5, 0.25, 0.05);
				mWorld.spawnParticle(Particle.LAVA, loc.clone().add(0, 1, 0), 4, 0.25, 0.5, 0.25, 0.0);
				mWorld.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 1f, 0.5f);
				EntityUtils.applyFire(mPlugin, CHANNELING_EFFECT_DURATION, mob);
			} else if (mLastSpellCast == Spells.FROST_NOVA || mLastSpellCast == Spells.BLIZZARD) {
				mWorld.spawnParticle(Particle.FALLING_DUST, loc.clone().add(0, 1, 0), 35, 0.45, 0.5, 0.45, 0, Bukkit.createBlockData("snow_block"));
				mWorld.spawnParticle(Particle.SNOWBALL, loc.clone().add(0, 1, 0), 35, 0.25, 0.5, 0.25, 0.3);
				mWorld.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, 1f, 0.65f);
				mWorld.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR, 1f, 2f);
				PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.SLOW, CHANNELING_EFFECT_DURATION, 1, false, true));
			} else {
				mWorld.spawnParticle(Particle.SPELL_WITCH, loc.clone().add(0, 1, 0), 15, 0.25, 0.5, 0.25, 0.25);
				mWorld.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 1.25f);
				PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.WEAKNESS, CHANNELING_EFFECT_DURATION, 0, false, true));
			}
			mLastSpellCast = null;
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
