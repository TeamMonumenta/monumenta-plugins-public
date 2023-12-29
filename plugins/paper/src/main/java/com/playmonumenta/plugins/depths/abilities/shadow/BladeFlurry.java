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
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class BladeFlurry extends DepthsAbility {

	public static final String ABILITY_NAME = "Blade Flurry";
	public static final int COOLDOWN = 20 * 6;
	public static final int[] DAMAGE = {8, 10, 12, 14, 16, 20};
	public static final int RADIUS = 3;
	public static final int[] SILENCE_DURATION = {20, 25, 30, 35, 40, 50};
	public static final Color SLASH_COLOR_TIP = Color.fromRGB(40, 19, 102);
	public static final Color SLASH_COLOR_BASE = Color.fromRGB(83, 60, 153);

	public static final String CHARM_COOLDOWN = "Blade Flurry Cooldown";

	public static final DepthsAbilityInfo<BladeFlurry> INFO =
		new DepthsAbilityInfo<>(BladeFlurry.class, ABILITY_NAME, BladeFlurry::new, DepthsTree.SHADOWDANCER, DepthsTrigger.SHIFT_RIGHT_CLICK)
			.linkedSpell(ClassAbility.BLADE_FLURRY)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", BladeFlurry::cast, DepthsTrigger.SHIFT_RIGHT_CLICK))
			.displayItem(Material.IRON_SWORD)
			.descriptions(BladeFlurry::getDescription);

	private final double mRadius;
	private final double mDamage;
	private final int mSilenceDuration;

	public BladeFlurry(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.BLADE_FLURRY_RADIUS.mEffectName, RADIUS);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.BLADE_FLURRY_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mSilenceDuration = CharmManager.getDuration(mPlayer, CharmEffects.BLADE_FLURRY_SILENCE_DURATION.mEffectName, SILENCE_DURATION[mRarity - 1]);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();

		World mWorld = mPlayer.getWorld();
		Location loc = mPlayer.getEyeLocation().add(0, -0.5, 0);
		loc.setPitch(0);
		for (LivingEntity mob : Hitbox.approximateCylinder(loc.clone().add(0, 0.05, 0), loc.clone().subtract(0, 0.1, 0), mRadius, true).accuracy(0.5).getHitMobs()) {
			EntityUtils.applySilence(mPlugin, mSilenceDuration, mob);
			DamageUtils.damage(mPlayer, mob, DamageType.MELEE_SKILL, mDamage, mInfo.getLinkedSpell());
			MovementUtils.knockAway(mPlayer, mob, 0.8f, true);
		}
		ParticleUtils.drawCleaveArc(loc, mRadius * 0.7, 160, -80, 260, 6, 0, 0, 0.2, 60,
			(Location l, int ring) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0,
				new Particle.DustOptions(
					ParticleUtils.getTransition(SLASH_COLOR_BASE, SLASH_COLOR_TIP, ring / 8D),
					0.6f + (ring * 0.1f)
				)).spawnAsPlayerActive(mPlayer));

		ParticleUtils.drawCleaveArc(loc, mRadius * 0.7, 20, -80, 260, 6, 0, 0, 0.2, 60,
			(Location l, int ring) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0,
				new Particle.DustOptions(
					ParticleUtils.getTransition(SLASH_COLOR_BASE, SLASH_COLOR_TIP, ring / 8D),
					0.6f + (ring * 0.1f)
				)).spawnAsPlayerActive(mPlayer));
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.9f, 0.9f);
		mWorld.playSound(mPlayer.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 0.7f, 1.4f);
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_WITCH_THROW, SoundCategory.PLAYERS, 0.5f, 0.5f);
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.PLAYERS, 1f, 1.9f);
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.2f, 0.9f);

		new BukkitRunnable() {
			int mTicks = 0;
			double mRadians = Math.toRadians(75);
			double mReverseRadians = Math.toRadians(105);
			final Location mLoc = mPlayer.getEyeLocation();

			@Override
			public void run() {
				if (mTicks == 0) {
					mLoc.setDirection(mPlayer.getLocation().getDirection().setY(0).normalize());
				}
				Vector vec = new Vector(FastUtils.cos(mRadians) * mRadius / 1.5, -0.5, FastUtils.sin(mRadians) * mRadius / 1.5);
				vec = VectorUtils.rotateXAxis(vec, mLoc.getPitch());
				vec = VectorUtils.rotateYAxis(vec, mLoc.getYaw());

				Vector reverseVec = new Vector(FastUtils.cos(mReverseRadians) * mRadius / 1.5, -0.5, FastUtils.sin(mReverseRadians) * mRadius / 1.5);
				reverseVec = VectorUtils.rotateXAxis(reverseVec, mLoc.getPitch());
				reverseVec = VectorUtils.rotateYAxis(reverseVec, mLoc.getYaw());

				Location bladeLoc = mPlayer.getEyeLocation().add(vec);
				Location reverseBladeLoc = mPlayer.getEyeLocation().add(reverseVec);
				new PartialParticle(Particle.SWEEP_ATTACK, bladeLoc, 3, 0.5, 0.25, 0.5, 0).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.SPELL_WITCH, bladeLoc, 6, 0.5, 0.25, 0.5, 0.1).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.REDSTONE, bladeLoc, 6, 0.5, 0.25, 0.5, 0, new Particle.DustOptions(SLASH_COLOR_BASE, 1)).spawnAsPlayerActive(mPlayer);

				new PartialParticle(Particle.SWEEP_ATTACK, reverseBladeLoc, 3, 0.5, 0.25, 0.5, 0).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.SPELL_WITCH, reverseBladeLoc, 6, 0.5, 0.25, 0.5, 0.1).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.REDSTONE, reverseBladeLoc, 6, 0.5, 0.25, 0.5, 0, new Particle.DustOptions(SLASH_COLOR_BASE, 1)).spawnAsPlayerActive(mPlayer);

				mWorld.playSound(bladeLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.2f, 0.75f);
				mWorld.playSound(bladeLoc, Sound.ITEM_ARMOR_EQUIP_CHAIN, SoundCategory.PLAYERS, 2f, 0.9f);
				mWorld.playSound(bladeLoc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1f, 0.5f);
				mWorld.playSound(bladeLoc, Sound.ITEM_FLINTANDSTEEL_USE, SoundCategory.PLAYERS, 0.7f, 0.5f);

				if (mTicks >= 4) {
					this.cancel();
					mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_PHANTOM_DEATH, SoundCategory.PLAYERS, 1.0f, 0.8f);
					mWorld.playSound(mPlayer.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 0.4f, 0.6f);
					mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.PLAYERS, 0.4f, 1.5f);
					mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.5f, 0.5f);
				}

				mTicks++;
				mRadians += Math.toRadians(45);
				mReverseRadians -= Math.toRadians(45);
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);

		return true;
	}

	private static Description<BladeFlurry> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<BladeFlurry>(color)
			.add("Right click while sneaking and holding a weapon to deal ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" melee damage in a ")
			.add(a -> a.mRadius, RADIUS)
			.add(" block radius around you. Affected mobs are silenced for ")
			.addDuration(a -> a.mSilenceDuration, SILENCE_DURATION[rarity - 1], false, true)
			.add(" seconds and knocked away slightly.")
			.addCooldown(COOLDOWN);
	}


}

