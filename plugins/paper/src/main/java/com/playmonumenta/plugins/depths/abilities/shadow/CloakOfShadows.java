package com.playmonumenta.plugins.depths.abilities.shadow;

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
import com.playmonumenta.plugins.effects.PercentDamageDealtSingle;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.EnumSet;
import java.util.List;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class CloakOfShadows extends DepthsAbility {

	public static final String ABILITY_NAME = "Cloak of Shadows";
	public static final int COOLDOWN = 20 * 15;
	public static final int WEAKEN_DURATION = 20 * 6;
	public static final int[] STEALTH_DURATION = {30, 35, 40, 45, 50, 60};
	public static final double[] WEAKEN_AMPLIFIER = {0.2, 0.25, 0.3, 0.35, 0.4, 0.5};
	public static final double[] DAMAGE = {0.4, 0.5, 0.6, 0.7, 0.8, 1};
	public static final int DAMAGE_DURATION = 4 * 20;
	private static final double VELOCITY = 0.7;
	private static final int RADIUS = 5;

	public static final String CHARM_COOLDOWN = "Cloak of Shadows Cooldown";

	public static final DepthsAbilityInfo<CloakOfShadows> INFO =
		new DepthsAbilityInfo<>(CloakOfShadows.class, ABILITY_NAME, CloakOfShadows::new, DepthsTree.SHADOWDANCER, DepthsTrigger.SHIFT_LEFT_CLICK)
			.linkedSpell(ClassAbility.CLOAK_OF_SHADOWS)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", CloakOfShadows::cast, DepthsTrigger.SHIFT_LEFT_CLICK))
			.displayItem(Material.BLACK_CONCRETE)
			.descriptions(CloakOfShadows::getDescription);

	private final double mRadius;
	private final double mWeakenAmplifier;
	private final int mStealthDuration;
	private final int mWeakenDuration;
	private final double mDamage;

	public CloakOfShadows(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.CLOAK_OF_SHADOWS_RADIUS.mEffectName, RADIUS);
		mWeakenAmplifier = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.CLOAK_OF_SHADOWS_WEAKEN_AMPLIFIER.mEffectName, WEAKEN_AMPLIFIER[mRarity - 1]);
		mStealthDuration = CharmManager.getDuration(mPlayer, CharmEffects.CLOAK_OF_SHADOWS_STEALTH_DURATION.mEffectName, STEALTH_DURATION[mRarity - 1]);
		mWeakenDuration = CharmManager.getDuration(mPlayer, CharmEffects.CLOAK_OF_SHADOWS_WEAKEN_DURATION.mEffectName, WEAKEN_DURATION);
		mDamage = DAMAGE[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.CLOAK_OF_SHADOWS_DAMAGE_MULTIPLIER.mEffectName);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getEyeLocation();
		Item bomb = AbilityUtils.spawnAbilityItem(world, loc, Material.BLACK_CONCRETE, "Shadow Bomb", false, VELOCITY, true, true);
		world.playSound(loc, Sound.ENTITY_SNOWBALL_THROW, SoundCategory.PLAYERS, 1, 0.15f);

		putOnCooldown();
		AbilityUtils.applyStealth(mPlugin, mPlayer, mStealthDuration, null);

		mPlugin.mEffectManager.addEffect(mPlayer, "CloakOfShadowsDamageEffect", new PercentDamageDealtSingle(DAMAGE_DURATION, mDamage, EnumSet.of(DamageEvent.DamageType.MELEE)));

		new BukkitRunnable() {

			int mExpire = 0;

			@Override
			public void run() {
				if (bomb.isOnGround()) {
					Location l = bomb.getLocation();
					double mult = mRadius / RADIUS;
					new PartialParticle(Particle.CAMPFIRE_COSY_SMOKE, l, (int) (30 * mult), 3 * mult, 0, 3 * mult).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.EXPLOSION_NORMAL, l, (int) (30 * mult), 2 * mult, 0, 2 * mult).spawnAsPlayerActive(mPlayer);
					world.playSound(l, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1, 0.15f);
					List<LivingEntity> mobs = EntityUtils.getNearbyMobs(l, mRadius);
					for (LivingEntity mob : mobs) {
						EntityUtils.applyWeaken(mPlugin, mWeakenDuration, mWeakenAmplifier, mob);
					}

					bomb.remove();
					this.cancel();
				}
				mExpire++;
				if (mExpire >= 10 * 20) {
					bomb.remove();
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);

		return true;
	}

	private static Description<CloakOfShadows> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<CloakOfShadows>(color)
			.add("Left click while sneaking and holding a weapon to throw a shadow bomb, which explodes on landing, applying ")
			.addPercent(a -> a.mWeakenAmplifier, WEAKEN_AMPLIFIER[rarity - 1], false, true)
			.add(" weaken for ")
			.addDuration(a -> a.mWeakenDuration, WEAKEN_DURATION)
			.add(" seconds to mobs within ")
			.add(a -> a.mRadius, RADIUS)
			.add(" blocks. You enter stealth for ")
			.addDuration(a -> a.mStealthDuration, STEALTH_DURATION[rarity - 1], false, true)
			.add(" seconds upon casting and the next instance of melee damage you deal within ")
			.addDuration(DAMAGE_DURATION)
			.add(" seconds deals ")
			.addPercent(a -> a.mDamage, DAMAGE[rarity - 1], false, true)
			.add(" more damage.")
			.addCooldown(COOLDOWN);
	}


}

