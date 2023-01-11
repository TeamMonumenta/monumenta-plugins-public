package com.playmonumenta.plugins.abilities.warrior.guardian;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Challenge extends Ability {

	private static final String PERCENT_DAMAGE_DEALT_EFFECT_NAME = "ChallengePercentDamageDealtEffect";
	private static final int DURATION = 20 * 10;
	private static final double PERCENT_DAMAGE_DEALT_EFFECT_1 = 0.15;
	private static final double PERCENT_DAMAGE_DEALT_EFFECT_2 = 0.3;
	private static final EnumSet<DamageType> AFFECTED_DAMAGE_TYPES = EnumSet.of(
		DamageType.MELEE,
		DamageType.MELEE_SKILL,
		DamageType.MELEE_ENCH
	);

	private static final int ABSORPTION_PER_MOB_1 = 1;
	private static final int ABSORPTION_PER_MOB_2 = 2;
	private static final int MAX_ABSORPTION_1 = 4;
	private static final int MAX_ABSORPTION_2 = 8;
	private static final int CHALLENGE_RANGE = 14;
	private static final int COOLDOWN = 20 * 20;

	public static final String CHARM_DURATION = "Challenge Duration";
	public static final String CHARM_DAMAGE = "Challenge Damage";
	public static final String CHARM_ABSORPTION_PER = "Challenge Absorption Health Per Mob";
	public static final String CHARM_ABSORPTION_MAX = "Challenge Max Absorption Health";
	public static final String CHARM_RANGE = "Challenge Range";
	public static final String CHARM_COOLDOWN = "Challenge Cooldown";

	public static final AbilityInfo<Challenge> INFO =
		new AbilityInfo<>(Challenge.class, "Challenge", Challenge::new)
			.linkedSpell(ClassAbility.CHALLENGE)
			.scoreboardId("Challenge")
			.shorthandName("Ch")
			.descriptions(
				("Left-clicking while sneaking makes all enemies within %s blocks target you. " +
					 "You gain %s Absorption per affected mob (up to %s Absorption) for %s seconds and +%s%% melee damage for %s seconds. Cooldown: %ss.")
					.formatted(CHALLENGE_RANGE, ABSORPTION_PER_MOB_1, MAX_ABSORPTION_1, StringUtils.ticksToSeconds(DURATION),
						StringUtils.multiplierToPercentage(PERCENT_DAMAGE_DEALT_EFFECT_1),
						StringUtils.ticksToSeconds(DURATION), StringUtils.ticksToSeconds(COOLDOWN)),
				"You gain %s Absorption per affected mob (up to %s Absorption) and +%s%% melee damage instead."
					.formatted(ABSORPTION_PER_MOB_2, MAX_ABSORPTION_2, StringUtils.multiplierToPercentage(PERCENT_DAMAGE_DEALT_EFFECT_2)))
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Challenge::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true)))
			.displayItem(new ItemStack(Material.IRON_AXE, 1));

	private final double mPercentDamageDealtEffect;
	private final double mAbsorptionPerMob;
	private final double mMaxAbsorption;

	public Challenge(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mPercentDamageDealtEffect = (isLevelOne() ? PERCENT_DAMAGE_DEALT_EFFECT_1 : PERCENT_DAMAGE_DEALT_EFFECT_2) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE);
		mAbsorptionPerMob = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION_PER, isLevelOne() ? ABSORPTION_PER_MOB_1 : ABSORPTION_PER_MOB_2);
		mMaxAbsorption = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION_MAX, isLevelOne() ? MAX_ABSORPTION_1 : MAX_ABSORPTION_2);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		Location loc = mPlayer.getLocation();
		List<LivingEntity> mobs = new Hitbox.SphereHitbox(loc, CharmManager.getRadius(mPlayer, CHARM_RANGE, CHALLENGE_RANGE)).getHitMobs();
		if (!mobs.isEmpty()) {
			int duration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION);
			AbsorptionUtils.addAbsorption(mPlayer, mAbsorptionPerMob * mobs.size(), mMaxAbsorption, duration);
			mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_DAMAGE_DEALT_EFFECT_NAME, new PercentDamageDealt(duration, mPercentDamageDealtEffect, AFFECTED_DAMAGE_TYPES));

			for (LivingEntity mob : mobs) {
				if (mob instanceof Mob) {
					EntityUtils.applyTaunt(mPlugin, mob, mPlayer);
				}
			}

			World world = mPlayer.getWorld();
			world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 2, 1);
			new PartialParticle(Particle.FLAME, loc, 25, 0.4, 1, 0.4, 0.7f).spawnAsPlayerActive(mPlayer);
			loc.add(0, 1.25, 0);
			new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 250, 0, 0, 0, 0.425).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.CRIT, loc, 300, 0, 0, 0, 1).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.CRIT_MAGIC, loc, 300, 0, 0, 0, 1).spawnAsPlayerActive(mPlayer);

			putOnCooldown();
		}
	}

}
