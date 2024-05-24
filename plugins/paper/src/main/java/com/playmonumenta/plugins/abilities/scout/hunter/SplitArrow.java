package com.playmonumenta.plugins.abilities.scout.hunter;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.scout.SwiftCuts;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.hunter.SplitArrowCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class SplitArrow extends Ability {

	private static final double SPLIT_ARROW_1_DAMAGE_PERCENT = 0.35;
	private static final double SPLIT_ARROW_2_DAMAGE_PERCENT = 0.60;
	private static final double SPLIT_ARROW_CHAIN_RANGE = 5;
	private static final PotionEffect SPECTRAL_ARROW_EFFECT = new PotionEffect(PotionEffectType.GLOWING, 200, 0);
	private static final int IFRAMES = 10;

	public static final String CHARM_DAMAGE = "Split Arrow Damage";
	public static final String CHARM_BOUNCES = "Split Arrow Bounces";
	public static final String CHARM_RANGE = "Split Arrow Range";

	public static final AbilityInfo<SplitArrow> INFO =
		new AbilityInfo<>(SplitArrow.class, "Split Arrow", SplitArrow::new)
			.linkedSpell(ClassAbility.SPLIT_ARROW)
			.scoreboardId("SplitArrow")
			.shorthandName("SA")
			.descriptions(
				"When you hit an enemy with an arrow, the next nearest enemy within " + (int) SPLIT_ARROW_CHAIN_RANGE + " blocks takes " + (int) (100 * SPLIT_ARROW_1_DAMAGE_PERCENT) + "% of the original arrow damage (ignores invulnerability frames).",
				"Damage to the second target is increased to " + (int) (100 * SPLIT_ARROW_2_DAMAGE_PERCENT) + "% of the original arrow damage.")
			.simpleDescription("Hitting a mob with a projectile deals part of the damage to a nearby mob.")
			.displayItem(Material.BLAZE_ROD);

	private final double mDamagePercent;
	private @Nullable SwiftCuts mSwiftCuts;
	private final SplitArrowCS mCosmetic;

	public SplitArrow(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamagePercent = isLevelOne() ? SPLIT_ARROW_1_DAMAGE_PERCENT : SPLIT_ARROW_2_DAMAGE_PERCENT;
		Bukkit.getScheduler().runTask(plugin, () -> {
			mSwiftCuts = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, SwiftCuts.class);
		});
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new SplitArrowCS());
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.PROJECTILE && event.getDamager() instanceof Projectile proj && EntityUtils.isAbilityTriggeringProjectile(proj, false) && EntityUtils.isHostileMob(enemy)) {
			double damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, event.getDamage() * mDamagePercent);
			int count = 1 + (int) CharmManager.getLevel(mPlayer, CHARM_BOUNCES);
			if (mSwiftCuts != null && mSwiftCuts.isEnhancementActive()) {
				count++;
			}
			double range = CharmManager.getRadius(mPlayer, CHARM_RANGE, SPLIT_ARROW_CHAIN_RANGE);
			LivingEntity sourceEnemy = enemy;
			List<LivingEntity> chainedMobs = new ArrayList<>();
			for (int i = 0; i < count; i++) {
				chainedMobs.add(sourceEnemy);
				List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(sourceEnemy.getLocation(), range);
				nearbyMobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));
				nearbyMobs.removeAll(chainedMobs);
				LivingEntity nearestMob = EntityUtils.getNearestMob(sourceEnemy.getLocation(), nearbyMobs);
				if (nearestMob == null) {
					break;
				}
				mCosmetic.splitArrowChain(mPlayer, sourceEnemy, nearestMob);
				Location loc = sourceEnemy.getEyeLocation();
				Location eye = nearestMob.getEyeLocation();
				Vector dir = LocationUtils.getDirectionTo(eye, loc);
				for (int j = 0; j < 50; j++) {
					loc.add(dir.clone().multiply(0.1));
					new PartialParticle(Particle.CRIT, loc, 2, 0.1, 0.1, 0.1, 0).spawnAsPlayerActive(mPlayer);
					if (loc.distance(eye) < 0.4) {
						break;
					}
				}

				if (!EntityUtils.hasArrowIframes(mPlugin, nearestMob)) {
					mCosmetic.splitArrowEffect(mPlayer, nearestMob);
					DamageUtils.damage(mPlayer, nearestMob, DamageType.OTHER, damage, mInfo.getLinkedSpell(), true, true);
					MovementUtils.knockAway(sourceEnemy, nearestMob, 0.125f, 0.35f, true);
					EntityUtils.applyArrowIframes(mPlugin, IFRAMES, nearestMob);
					if (event.getDamager() instanceof SpectralArrow) {
						nearestMob.addPotionEffect(SPECTRAL_ARROW_EFFECT);
					}
				}

				sourceEnemy = nearestMob;
			}
		}
		return false; // applies damage of type OTHER for damage of type PROJECTILE, which should not cause recursion with any other ability (or itself)
	}

}
