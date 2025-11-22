package com.playmonumenta.plugins.depths.abilities.steelsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
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
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class DepthsSplitArrow extends DepthsAbility {

	private static final int SPLIT_ARROW_CHAIN_RANGE = 5;
	public static final String ABILITY_NAME = "Split Arrow";
	public static final double[] DAMAGE_MOD = {.40, .50, .60, .70, .80, 1.00};
	private static final PotionEffect SPECTRAL_ARROW_EFFECT = new PotionEffect(PotionEffectType.GLOWING, 200, 0);
	private static final int IFRAMES = 10;

	public static final DepthsAbilityInfo<DepthsSplitArrow> INFO =
		new DepthsAbilityInfo<>(DepthsSplitArrow.class, ABILITY_NAME, DepthsSplitArrow::new, DepthsTree.STEELSAGE, DepthsTrigger.PASSIVE)
			.displayItem(Material.CHAIN)
			.descriptions(DepthsSplitArrow::getDescription)
			.singleCharm(false);

	private final double mDamagePercent;
	private final double mRange;

	public DepthsSplitArrow(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamagePercent = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.SPLIT_ARROW_DAMAGE.mEffectName, DAMAGE_MOD[mRarity - 1]);
		mRange = CharmManager.getRadius(mPlayer, CharmEffects.SPLIT_ARROW_RANGE.mEffectName, SPLIT_ARROW_CHAIN_RANGE);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.PROJECTILE && event.getDamager() instanceof Projectile proj && EntityUtils.isAbilityTriggeringProjectile(proj, false) && EntityUtils.isHostileMob(enemy)) {
			double damage = mDamagePercent * event.getDamage();
			int count = 1 + (int) CharmManager.getLevel(mPlayer, CharmEffects.SPLIT_ARROW_BOUNCES.mEffectName);
			LivingEntity sourceEnemy = enemy;
			List<LivingEntity> chainedMobs = new ArrayList<>();
			for (int j = 0; j < count; j++) {
				chainedMobs.add(sourceEnemy);
				List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(sourceEnemy.getLocation(), mRange);
				nearbyMobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));
				nearbyMobs.removeAll(chainedMobs);
				LivingEntity nearestMob = EntityUtils.getNearestMob(sourceEnemy.getLocation(), nearbyMobs);
				if (nearestMob == null) {
					break;
				}
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
					world.playSound(eye, Sound.ENTITY_ARROW_HIT, SoundCategory.PLAYERS, 1, 1.2f);
					if (proj instanceof SpectralArrow) {
						nearestMob.addPotionEffect(SPECTRAL_ARROW_EFFECT);
					}
					DamageUtils.damage(mPlayer, nearestMob, DamageType.OTHER, damage, mInfo.getLinkedSpell(), true);
					MovementUtils.knockAway(enemy, nearestMob, 0.125f, 0.35f, true);
					EntityUtils.applyArrowIframes(mPlugin, IFRAMES, nearestMob);
				}
				sourceEnemy = nearestMob;
			}
		}

		return false; // applies damage of type OTHER for damage of type PROJECTILE, which should not cause recursion with any other ability (or itself)
	}

	private static Description<DepthsSplitArrow> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<>(() -> INFO, color)
			.add("When you shoot an enemy with a projectile, the nearest enemy within ")
			.add(a -> a.mRange, SPLIT_ARROW_CHAIN_RANGE)
			.add(" blocks takes ")
			.addPercent(a -> a.mDamagePercent, DAMAGE_MOD[rarity - 1], false, true)
			.add(" of the projectile's damage.");
	}
}

