package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.abilities.AbilityCollection;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.mage.Spellshock;
import com.playmonumenta.plugins.abilities.scout.BowMastery;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.Plugin;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Sound;
import org.bukkit.World;

public class AbilityUtils {

	private static final double PASSIVE_DAMAGE_ELITE_MODIFIER = 2.0;
	private static final double PASSIVE_DAMAGE_BOSS_MODIFIER = 1.5;

	public static void rogueDamageMob(Plugin mPlugin, Player player, LivingEntity damagee, double damage) {
		double correctDamage = damage;
		if (EntityUtils.isElite(damagee)) {
			correctDamage = damage * PASSIVE_DAMAGE_ELITE_MODIFIER;
		} else if (EntityUtils.isBoss(damagee)) {
			correctDamage = damage * PASSIVE_DAMAGE_BOSS_MODIFIER;
		}
		EntityUtils.damageEntity(mPlugin, damagee, correctDamage, player);
	}

	private static final int SPELL_SHOCK_SPELL_RADIUS = 4;
	private static final int SPELL_SHOCK_SPELL_DAMAGE = 3;
	private static final int SPELL_SHOCK_REGEN_DURATION = 51;
	private static final int SPELL_SHOCK_REGEN_AMPLIFIER = 1;
	private static final int SPELL_SHOCK_SPEED_DURATION = 120;
	private static final int SPELL_SHOCK_SPEED_AMPLIFIER = 0;
	private static final int SPELL_SHOCK_STAGGER_DURATION = (int)(0.6 * 20);
	private static final int SPELL_SHOCK_VULN_DURATION = 4 * 20;
	private static final int SPELL_SHOCK_VULN_AMPLIFIER = 3; // 20%

	public static void mageSpellshock(Plugin plugin, LivingEntity mob, float dmg, Player player, MagicType type) {
		boolean shocked = false;

		// TODO:
		// This isn't quite right - now a mage can't trigger spellshocked mobs tagged by a different mage
		Spellshock ss = (Spellshock)AbilityManager.getManager().getPlayerAbility(player, "SpellShock");
		if (ss != null && ss.shocked.contains(mob)) {
			shocked = true;
			World mWorld = mob.getWorld();
			// Hit a shocked mob with a real spell - extra damage

			int spellShock = ScoreboardUtils.getScoreboardValue(player, "SpellShock");
			if (spellShock > 1) {
				plugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF,
												new PotionEffect(PotionEffectType.REGENERATION,
																 SPELL_SHOCK_REGEN_DURATION,
																 SPELL_SHOCK_REGEN_AMPLIFIER, true, true));
				plugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF,
												new PotionEffect(PotionEffectType.SPEED,
																 SPELL_SHOCK_SPEED_DURATION,
																 SPELL_SHOCK_SPEED_AMPLIFIER, true, true));
			}

			// Consume the "charge"
			ss.shocked.remove(mob);

			Location loc = mob.getLocation().add(0, 1, 0);
			mWorld.spawnParticle(Particle.SPELL_WITCH, loc, 100, 1, 1, 1, 0.001);
			mWorld.spawnParticle(Particle.CRIT_MAGIC, loc, 75, 1, 1, 1, 0.25);
			mWorld.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 1.0f, 2.5f);
			mWorld.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 1.0f, 2.0f);
			mWorld.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 1.0f, 1.5f);
			for (Entity nearbyMob : EntityUtils.getNearbyMobs(mob.getLocation(), SPELL_SHOCK_SPELL_RADIUS)) {
				// Only damage hostile mobs and specifically not the mob originally hit
				if (nearbyMob != mob) {
					mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, SPELL_SHOCK_STAGGER_DURATION, 10, true, false));
					EntityUtils.damageEntity(plugin, (LivingEntity)nearbyMob, SPELL_SHOCK_SPELL_DAMAGE, player, type);
					((LivingEntity)nearbyMob).addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, SPELL_SHOCK_VULN_DURATION,
																			   SPELL_SHOCK_VULN_AMPLIFIER, false, true));
				}
			}

			dmg += SPELL_SHOCK_SPELL_DAMAGE;
		}

		// Apply damage to the hit mob all in one shot
		if (dmg > 0) {
			EntityUtils.damageEntity(plugin, mob, dmg, player, type);
		}

		if (shocked)
			mob.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, SPELL_SHOCK_VULN_DURATION,
			                                     SPELL_SHOCK_VULN_AMPLIFIER, false, true));
	}

	private static final int BOW_MASTER_1_DAMAGE = 3;
	private static final int BOW_MASTER_2_DAMAGE = 6;

	public static int getBowMasteryDamage(Player player) {
		int bowMastery = ScoreboardUtils.getScoreboardValue(player, "BowMastery");
		if (bowMastery > 0) {
			int bonusDamage = bowMastery == 1 ? BOW_MASTER_1_DAMAGE : BOW_MASTER_2_DAMAGE;
			return bonusDamage;
		}
		return 0;
	}

}
