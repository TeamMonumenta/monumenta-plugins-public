package com.playmonumenta.plugins.depths.abilities.flamecaller;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class VolcanicCombos extends DepthsAbility {
	public static final String ABILITY_NAME = "Volcanic Combos";
	public static final int[] DAMAGE = {6, 8, 10, 12, 14, 18};
	public static final int RADIUS = 4;
	public static final int FIRE_TICKS = 3 * 20;
	public static final int HIT_REQUIREMENT = 3;

	public static final DepthsAbilityInfo<VolcanicCombos> INFO =
		new DepthsAbilityInfo<>(VolcanicCombos.class, ABILITY_NAME, VolcanicCombos::new, DepthsTree.FLAMECALLER, DepthsTrigger.COMBO)
			.displayItem(Material.BLAZE_ROD)
			.descriptions(VolcanicCombos::getDescription)
			.singleCharm(false);

	private final int mHitRequirement;
	private final double mRadius;
	private final double mDamage;
	private final int mFireDuration;

	private int mComboCount = 0;

	public VolcanicCombos(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mHitRequirement = HIT_REQUIREMENT + (int) CharmManager.getLevel(mPlayer, CharmEffects.VOLCANIC_COMBOS_HIT_REQUIREMENT.mEffectName);
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.VOLCANIC_COMBOS_RADIUS.mEffectName, RADIUS);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.VOLCANIC_COMBOS_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mFireDuration = CharmManager.getDuration(mPlayer, CharmEffects.VOLCANIC_COMBOS_FIRE_DURATION.mEffectName, FIRE_TICKS);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (DepthsUtils.isValidComboAttack(event, mPlayer)) {
			mComboCount++;
			if (mComboCount >= mHitRequirement) {
				Location location = enemy.getLocation();
				for (LivingEntity mob : EntityUtils.getNearbyMobs(location, mRadius)) {
					EntityUtils.applyFire(mPlugin, mFireDuration, mob, mPlayer);
					DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, mDamage, mInfo.getLinkedSpell(), true);
				}
				World world = mPlayer.getWorld();
				for (int i = 0; i < 360; i += 45) {
					double rad = Math.toRadians(i);
					Location locationDelta = new Location(world, mRadius / 2.0 * FastUtils.cos(rad), 0.5, mRadius / 2.0 * FastUtils.sin(rad));
					location.add(locationDelta);
					new PartialParticle(Particle.FLAME, location, 1).spawnAsPlayerActive(mPlayer);
					location.subtract(locationDelta);
				}
				world.playSound(location, Sound.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 0.5f, 1);
				mComboCount = 0;
			}
			return true;
		}
		return false;
	}

	private static Description<VolcanicCombos> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<VolcanicCombos>(color)
			.add("Every ")
			.add(a -> a.mHitRequirement, HIT_REQUIREMENT, true)
			.add(" melee attacks, deal ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" magic damage to enemies in a ")
			.add(a -> a.mRadius, RADIUS)
			.add(" block radius and set those enemies on fire for ")
			.addDuration(a -> a.mFireDuration, FIRE_TICKS)
			.add(" seconds.");
	}


}
