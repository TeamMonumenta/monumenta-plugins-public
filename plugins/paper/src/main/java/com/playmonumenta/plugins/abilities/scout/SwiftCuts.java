package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SwiftCuts extends Ability {
	private static final double CONSECUTIVE_PERCENT_DAMAGE_1 = 0.2;
	private static final double CONSECUTIVE_PERCENT_DAMAGE_2 = 0.35;
	private static final double SWEEP_RADIUS = 2.5;
	private static final double PERCENT_AOE_DAMAGE_1 = 0.1;
	private static final double PERCENT_AOE_DAMAGE_2 = 0.2;
	private static final double ENHANCEMENT_DAMAGE_PERCENT = 0.35;

	public static final String CHARM_DAMAGE = "Swift Cuts Damage";

	private final double mConsecutivePercentDamage;
	private final double mPercentAoEDamage;

	private @Nullable LivingEntity mLastTarget = null;
	private int mEnhancementHits = 0;

	public SwiftCuts(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Swift Cuts");
		mInfo.mScoreboardId = "SwiftCuts";
		mInfo.mShorthandName = "SC";
		mInfo.mDescriptions.add("If you perform a melee attack on the same mob 2 or more times in a row, each hit after the first does +20% damage and deals 10% of the damage to all other mobs in a 2.5 block radius.");
		mInfo.mDescriptions.add("Bonus damage increased to +35%, sweep damage increased to 20%.");
		mInfo.mDescriptions.add("Every third fully charged melee attack in a row against the same mob deals " + ENHANCEMENT_DAMAGE_PERCENT * 100 + "% more damage.");
		mDisplayItem = new ItemStack(Material.STONE_SWORD, 1);
		mConsecutivePercentDamage = (isLevelOne() ? CONSECUTIVE_PERCENT_DAMAGE_1 : CONSECUTIVE_PERCENT_DAMAGE_2) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE);
		mPercentAoEDamage = isLevelOne() ? PERCENT_AOE_DAMAGE_1 : PERCENT_AOE_DAMAGE_2;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (mPlayer != null) {
			if (event.getType() == DamageType.MELEE || event.getType() == DamageType.MELEE_SKILL || event.getType() == DamageType.MELEE_ENCH) {
				if (enemy.equals(mLastTarget)) {
					Location loc = enemy.getLocation();
					World world = mPlayer.getWorld();
					world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.5f);
					new PartialParticle(Particle.SWEEP_ATTACK, loc, 2, 0.25, 0.35, 0.25, 0.001).spawnAsPlayerActive(mPlayer);

					event.setDamage(event.getDamage() * (1 + mConsecutivePercentDamage));

					for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, SWEEP_RADIUS, enemy)) {
						DamageUtils.damage(mPlayer, mob, DamageType.OTHER, event.getDamage() * (1 + mPercentAoEDamage), mInfo.mLinkedSpell, true, true);
					}

					if (isEnhanced()) {
						// If swung with full charge
						if (mPlayer.getAttackCooldown() == 1.0) {
							mEnhancementHits += 1;
						}

						if (mEnhancementHits == 3) {
							event.setDamage(event.getDamage() * (1 + ENHANCEMENT_DAMAGE_PERCENT));
							mEnhancementHits = 0;
						}
					}
				} else {
					mLastTarget = enemy;

					if (isEnhanced()) {
						mEnhancementHits = 0;

						// If swung with full charge
						if (mPlayer.getAttackCooldown() == 1.0) {
							mEnhancementHits += 1;
						}
					}
				}
			}
		}
		return false; // only changes event damage
	}

}
