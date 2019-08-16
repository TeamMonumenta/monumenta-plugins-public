package com.playmonumenta.plugins.abilities.mage;

import java.util.Random;

import org.bukkit.Particle;
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
import com.playmonumenta.plugins.utils.MetadataUtils;
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

	private int mDamage;
	private Spells mLastSpellCast;

	public Channeling(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "Channeling";
		mDamage = getAbilityScore() == 1 ? CHANNELING_1_DAMAGE : CHANNELING_2_DAMAGE;
	}

	@Override
	public boolean AbilityCastEvent(AbilityCastEvent event) {
		mLastSpellCast = event.getAbility();
		return true;
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (mLastSpellCast != null && event.getEntity() instanceof LivingEntity && event.getCause() == DamageCause.ENTITY_ATTACK
			&& !MetadataUtils.happenedThisTick(mPlugin, mPlayer, EntityUtils.PLAYER_DEALT_CUSTOM_DAMAGE_METAKEY, 0)) {
			event.setDamage(event.getDamage() + mDamage);
			LivingEntity mob = (LivingEntity) event.getEntity();
			if (mLastSpellCast == Spells.MAGMA_SHIELD || mLastSpellCast == Spells.STARFALL) {
				mWorld.spawnParticle(Particle.FLAME, mob.getLocation(), 30, 0, 0.5, 0, 0.1);
				EntityUtils.applyFire(mPlugin, CHANNELING_EFFECT_DURATION, mob);
			} else if (mLastSpellCast == Spells.FROST_NOVA || mLastSpellCast == Spells.BLIZZARD) {
				mWorld.spawnParticle(Particle.SNOWBALL, mob.getLocation(), 30, 0, 0.5, 0, 0.5);
				PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.SLOW, CHANNELING_EFFECT_DURATION, 1, false, true));
			} else {
				mWorld.spawnParticle(Particle.FIREWORKS_SPARK, mob.getLocation(), 30, 0, 0.5, 0, 0.25);
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