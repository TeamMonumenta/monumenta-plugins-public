package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;

import java.util.Random;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Stray;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.Particle;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.World;

public class ElementalArrows extends Ability {
	private static final int ELEMENTAL_ARROWS_ICE_DURATION = 8 * 20;
	private static final int ELEMENTAL_ARROWS_ICE_EFFECT_LVL = 1;
	private static final int ELEMENTAL_ARROWS_FIRE_DURATION = 5 * 20;
	private static final double ELEMENTAL_ARROWS_RADIUS = 4.0;

	public ElementalArrows(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.classId = 1;
		mInfo.specId = -1;
		mInfo.linkedSpell = Spells.ELEMENTAL_ARROWS;
		mInfo.scoreboardId = "Elemental";
	}

	@Override
	public boolean LivingEntityShotByPlayerEvent(Player player, Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		int elementalArrows = getAbilityScore(player);
		if (elementalArrows > 0) {
			if (arrow.hasMetadata("FireArrow")) {
				if (elementalArrows == 1) {
					damagee.setFireTicks(ELEMENTAL_ARROWS_FIRE_DURATION);
					AbilityUtils.mageSpellshock(mPlugin, damagee, (damagee instanceof Stray) ? 8 : 0, player, MagicType.FIRE);
				} else if (elementalArrows == 2) {
					for (LivingEntity mob : EntityUtils.getNearbyMobs(damagee.getLocation(), ELEMENTAL_ARROWS_RADIUS)) {
						if (mob != damagee) {
							mob.setFireTicks(ELEMENTAL_ARROWS_FIRE_DURATION);
							AbilityUtils.mageSpellshock(mPlugin, mob, 0, player, MagicType.FIRE);
						}
					}
				}
			} else if (arrow.hasMetadata("IceArrow")) {
				if (elementalArrows == 1) {
					damagee.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, ELEMENTAL_ARROWS_ICE_DURATION, ELEMENTAL_ARROWS_ICE_EFFECT_LVL, false, true));
					AbilityUtils.mageSpellshock(mPlugin, damagee, 0, player, MagicType.ICE);
				} else if (elementalArrows == 2) {
					for (LivingEntity mob : EntityUtils.getNearbyMobs(damagee.getLocation(), ELEMENTAL_ARROWS_RADIUS)) {
						mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, ELEMENTAL_ARROWS_ICE_DURATION, ELEMENTAL_ARROWS_ICE_EFFECT_LVL, false, true));
						AbilityUtils.mageSpellshock(mPlugin, mob, (damagee instanceof Blaze) ? 8 : 0, player, MagicType.ICE);
					}
				}
			}
		}

		return true;
	}

	@Override
	public boolean PlayerShotArrowEvent(Player player, Arrow arrow) {
		int elementalArrows = getAbilityScore(player);
		if (elementalArrows > 0) {
			if (player.isSneaking()) {
				//  If sneaking, Ice Arrow
				arrow.setFireTicks(0);
				arrow.setMetadata("IceArrow", new FixedMetadataValue(mPlugin, 0));
				mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.SNOW_SHOVEL);
			} else {
				//  else Fire Arrow
				arrow.setFireTicks(ELEMENTAL_ARROWS_FIRE_DURATION);
				arrow.setMetadata("FireArrow", new FixedMetadataValue(mPlugin, 0));
				mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.FLAME);
			}
		}

		return true;
	}
}
