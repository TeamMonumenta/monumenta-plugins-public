package com.playmonumenta.plugins.abilities.scout.hunter;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class SplitArrow extends Ability {

	private static final double SPLIT_ARROW_1_DAMAGE_PERCENT = 0.35;
	private static final double SPLIT_ARROW_2_DAMAGE_PERCENT = 0.60;
	private static final double SPLIT_ARROW_CHAIN_RANGE = 5;
	private static final PotionEffect SPECTRAL_ARROW_EFFECT = new PotionEffect(PotionEffectType.GLOWING, 200, 0);
	private static final int IFRAMES = 10;

	private final double mDamagePercent;

	public SplitArrow(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Split Arrow");
		mInfo.mLinkedSpell = ClassAbility.SPLIT_ARROW;
		mInfo.mScoreboardId = "SplitArrow";
		mInfo.mShorthandName = "SA";
		mInfo.mDescriptions.add("When you hit an enemy with an arrow, the next nearest enemy within 5 blocks takes " + (int) (100 * SPLIT_ARROW_1_DAMAGE_PERCENT) + "% of the original arrow damage (ignores invulnerability frames).");
		mInfo.mDescriptions.add("Damage to the second target is increased to " + (int) (100 * SPLIT_ARROW_2_DAMAGE_PERCENT) + "% of the original arrow damage.");
		mDisplayItem = new ItemStack(Material.BLAZE_ROD, 1);

		mDamagePercent = isLevelOne() ? SPLIT_ARROW_1_DAMAGE_PERCENT : SPLIT_ARROW_2_DAMAGE_PERCENT;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.PROJECTILE && event.getDamager() instanceof AbstractArrow && EntityUtils.isHostileMob(enemy)) {
			LivingEntity nearestMob = EntityUtils.getNearestMob(enemy.getLocation(), SPLIT_ARROW_CHAIN_RANGE, enemy);

			if (nearestMob != null && !nearestMob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG)) {
				Location loc = enemy.getEyeLocation();
				Location eye = nearestMob.getEyeLocation();
				Vector dir = LocationUtils.getDirectionTo(eye, loc);
				World world = mPlayer.getWorld();
				for (int i = 0; i < 50; i++) {
					loc.add(dir.clone().multiply(0.1));
					new PartialParticle(Particle.CRIT, loc, 2, 0.1, 0.1, 0.1, 0).spawnAsPlayerActive(mPlayer);
					if (loc.distance(eye) < 0.4) {
						break;
					}
				}

				if (!EntityUtils.hasArrowIframes(mPlugin, nearestMob)) {
					new PartialParticle(Particle.CRIT, eye, 30, 0, 0, 0, 0.6).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.CRIT_MAGIC, eye, 20, 0, 0, 0, 0.6).spawnAsPlayerActive(mPlayer);
					world.playSound(eye, Sound.ENTITY_ARROW_HIT, 1, 1.2f);

					DamageUtils.damage(mPlayer, nearestMob, DamageType.OTHER, event.getDamage() * mDamagePercent, mInfo.mLinkedSpell, true, true);
					MovementUtils.knockAway(enemy, nearestMob, 0.125f, 0.35f, true);
					EntityUtils.applyArrowIframes(mPlugin, IFRAMES, nearestMob);
					if (event.getDamager() instanceof SpectralArrow) {
						nearestMob.addPotionEffect(SPECTRAL_ARROW_EFFECT);
					}
				}
			}
		}
		return false; // applies damage of type OTHER for damage of type PROJECTILE, which should not cause recursion with any other ability (or itself)
	}

}
