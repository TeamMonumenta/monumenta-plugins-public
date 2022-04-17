package com.playmonumenta.plugins.abilities.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Skirmisher extends Ability {

	private static final double GROUPED_FLAT_DAMAGE = 1;
	private static final double GROUPED_FLAT_DAMAGE_2 = 2;
	private static final double GROUPED_PERCENT_DAMAGE_1 = 0.1;
	private static final double GROUPED_PERCENT_DAMAGE_2 = 0.15;
	private static final double SKIRMISHER_FRIENDLY_RADIUS = 2.5;
	private static final int MOB_COUNT_CUTOFF = 1;
	private static final double ENHANCEMENT_SPLASH_RADIUS = 3;
	private static final double ENHANCEMENT_SPLASH_PERCENT_DAMAGE = 0.3;

	private final double mIsolatedPercentDamage;
	private final double mIsolatedFlatDamage;

	public Skirmisher(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Skirmisher");
		mInfo.mLinkedSpell = ClassAbility.SKIRMISHER;
		mInfo.mScoreboardId = "Skirmisher";
		mInfo.mShorthandName = "Sk";
		mInfo.mDescriptions.add("When dealing melee damage to a mob that has at least one other mob within 2.5 blocks, deal + 1 + 10% final damage.");
		mInfo.mDescriptions.add("The damage bonus now also applies to mobs not targeting you, and the damage bonus is increased to 2 + 15% final damage done.");
		mInfo.mDescriptions.add("When you hit an enemy with a sword, the nearest enemy within " + ENHANCEMENT_SPLASH_RADIUS + " blocks takes " + ENHANCEMENT_SPLASH_PERCENT_DAMAGE * 100 + "% of the original attack's damage (ignores invulnerability frames).");
		mDisplayItem = new ItemStack(Material.BONE, 1);
		mIsolatedPercentDamage = isLevelOne() ? GROUPED_PERCENT_DAMAGE_1 : GROUPED_PERCENT_DAMAGE_2;
		mIsolatedFlatDamage = isLevelOne() ? GROUPED_FLAT_DAMAGE : GROUPED_FLAT_DAMAGE_2;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (mPlayer != null) {
			Location loc = enemy.getLocation();

			// If Enhanced and triggers on a melee strike,
			if (isEnhanced() && event.getType() == DamageType.MELEE) {
				LivingEntity selectedEnemy = EntityUtils.getNearestMob(loc, ENHANCEMENT_SPLASH_RADIUS, enemy);

				if (selectedEnemy != null) {
					DamageUtils.damage(mPlayer, selectedEnemy, DamageType.OTHER, event.getOriginalDamage() * ENHANCEMENT_SPLASH_PERCENT_DAMAGE, mInfo.mLinkedSpell, true);
				}
			}

			if (event.getAbility() != mInfo.mLinkedSpell && (event.getType() == DamageType.MELEE || event.getType() == DamageType.MELEE_SKILL || event.getType() == DamageType.MELEE_ENCH)) {
				if (EntityUtils.getNearbyMobs(loc, SKIRMISHER_FRIENDLY_RADIUS, enemy).size() >= MOB_COUNT_CUTOFF
					|| (isLevelTwo() && enemy instanceof Mob mob && !mPlayer.equals(mob.getTarget()))) {
					World world = mPlayer.getWorld();
					world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.5f);
					world.playSound(loc, Sound.BLOCK_IRON_TRAPDOOR_CLOSE, 1, 0.5f);
					world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 0.5f);
					loc.add(0, 1, 0);
					new PartialParticle(Particle.SMOKE_NORMAL, loc, 10, 0.35, 0.5, 0.35, 0.05).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.SPELL_MOB, loc, 10, 0.35, 0.5, 0.35, 0.00001).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.CRIT, loc, 10, 0.25, 0.5, 0.25, 0.55).spawnAsPlayerActive(mPlayer);

					event.setDamage((event.getDamage() + mIsolatedFlatDamage) * (1 + mIsolatedPercentDamage));
				}
			}
		}
		return false; // only changes event damage
	}

	@Override
	public boolean runCheck() {
		return InventoryUtils.rogueTriggerCheck(mPlugin, mPlayer);
	}
}

