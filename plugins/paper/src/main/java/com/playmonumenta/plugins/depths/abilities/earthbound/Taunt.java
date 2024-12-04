package com.playmonumenta.plugins.depths.abilities.earthbound;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
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
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

public class Taunt extends DepthsAbility {

	public static final String ABILITY_NAME = "Taunt";
	private static final int COOLDOWN = 20 * 18;
	private static final double[] ABSORPTION = {1, 1.25, 1.5, 1.75, 2, 2.5};
	private static final int CAST_RANGE = 12;
	private static final int MAX_MOBS = 6;
	private static final int ABSORPTION_DURATION = 20 * 8;
	private static final double[] DAMAGE_BONUS = {6, 7.5, 9, 10.5, 12, 15};

	public static final String CHARM_COOLDOWN = "Taunt Cooldown";

	public static final DepthsAbilityInfo<Taunt> INFO =
		new DepthsAbilityInfo<>(Taunt.class, ABILITY_NAME, Taunt::new, DepthsTree.EARTHBOUND, DepthsTrigger.SHIFT_LEFT_CLICK)
			.linkedSpell(ClassAbility.TAUNT)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Taunt::cast, DepthsTrigger.SHIFT_LEFT_CLICK))
			.displayItem(Material.GOLDEN_CHESTPLATE)
			.descriptions(Taunt::getDescription);

	private final double mRange;
	private final double mAbsorptionPerMob;
	private final double mMaxMobs;
	private final int mDuration;
	private final double mDamageBonus;

	public Taunt(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRange = CharmManager.getRadius(mPlayer, CharmEffects.TAUNT_RANGE.mEffectName, CAST_RANGE);
		mAbsorptionPerMob = CharmManager.getLevel(mPlayer, CharmEffects.TAUNT_ABSORPTION_PER_MOB.mEffectName) + ABSORPTION[mRarity - 1];
		mMaxMobs = CharmManager.getLevel(mPlayer, CharmEffects.TAUNT_MAX_ABSORPTION_MOBS.mEffectName) + MAX_MOBS;
		mDuration = CharmManager.getDuration(mPlayer, CharmEffects.TAUNT_ABSORPTION_DURATION.mEffectName, ABSORPTION_DURATION);
		mDamageBonus = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.TAUNT_DAMAGE_BONUS.mEffectName, DAMAGE_BONUS[mRarity - 1]);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		Location loc = mPlayer.getLocation();
		World world = mPlayer.getWorld();
		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, mRange);
		mobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));

		if (!mobs.isEmpty()) {
			putOnCooldown();
			// add rarity% absorption for each affected mob, up to 6
			AbsorptionUtils.addAbsorption(mPlayer, Math.min(mobs.size(), mMaxMobs) * mAbsorptionPerMob, mMaxMobs * mAbsorptionPerMob, mDuration);
			for (LivingEntity le : mobs) {
				EntityUtils.applyTaunt(le, mPlayer);
				new PartialParticle(Particle.BLOCK_CRACK, le.getLocation(), 50, 0.1, 0.1, 0.1, 0.1, Material.DIRT.createBlockData()).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.FIREWORKS_SPARK, le.getLocation(), 30, 0.1, 0.1, 0.1, 0.2).spawnAsPlayerActive(mPlayer);
			}
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.PLAYERS, 1, 1.2f);
			new PartialParticle(Particle.BLOCK_CRACK, mPlayer.getLocation(), 50, 0.1, 0.1, 0.1, 0.1, Material.DIRT.createBlockData()).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.FIREWORKS_SPARK, mPlayer.getLocation(), 30, 0.1, 0.1, 0.1, 0.2).spawnAsPlayerActive(mPlayer);

			return true;
		}
		return false;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE && PlayerUtils.isFallingAttack(mPlayer)
			&& ((enemy instanceof Mob mob && mob.getTarget() != null && mob.getTarget().equals(mPlayer)) || EntityUtils.isStunned(enemy))) {

			Location loc = enemy.getEyeLocation();
			World world = mPlayer.getWorld();
			world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 0.75f, 0.5f);
			world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 0.75f, 0.5f);
			world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.PLAYERS, 0.75f, 0.5f);
			new PartialParticle(Particle.FALLING_DUST, loc, 10, 0.3, 0.3, 0.3).data(Material.DIRT.createBlockData()).spawnAsPlayerActive(mPlayer);

			DamageUtils.damage(mPlayer, enemy, DamageType.MELEE_SKILL, mDamageBonus, mInfo.getLinkedSpell(), true, false);

			return true;
		}
		return false;
	}

	private static Description<Taunt> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<Taunt>(color)
			.add("Left click while sneaking to force all enemies within ")
			.add(a -> a.mRange, CAST_RANGE)
			.add(" blocks to target you, and you gain ")
			.add(a -> a.mAbsorptionPerMob, ABSORPTION[rarity - 1], false, null, true)
			.add(" absorption for every enemy (up to ")
			.add(a -> a.mMaxMobs, MAX_MOBS)
			.add(" enemies) afflicted, for ")
			.addDuration(a -> a.mDuration, ABSORPTION_DURATION)
			.add(" seconds. Passively, you deal an additional ")
			.addDepthsDamage(a -> a.mDamageBonus, DAMAGE_BONUS[rarity - 1], true)
			.add(" melee damage on critical strikes to mobs that are targeting you or stunned.")
			.addCooldown(COOLDOWN);
	}


}
