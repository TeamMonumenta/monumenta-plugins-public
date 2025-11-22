package com.playmonumenta.plugins.depths.abilities.flamecaller;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
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
import org.jetbrains.annotations.Nullable;

public class VolcanicCombos extends DepthsAbility {
	public static final String ABILITY_NAME = "Volcanic Combos";
	public static final int COOLDOWN = 6 * 20;
	public static final int[] DAMAGE = {8, 10, 12, 14, 16, 20};
	public static final int RADIUS = 4;
	public static final int FIRE_TICKS = 3 * 20;
	private static final Particle.DustOptions YELLOW_COLOR = new Particle.DustOptions(Color.fromRGB(250, 180, 0), 1.0f);
	private static final Particle.DustOptions ORANGE_COLOR = new Particle.DustOptions(Color.fromRGB(240, 140, 0), 1.0f);

	public static final String CHARM_COOLDOWN = "Volcanic Combos Cooldown";

	public static final DepthsAbilityInfo<VolcanicCombos> INFO =
		new DepthsAbilityInfo<>(VolcanicCombos.class, ABILITY_NAME, VolcanicCombos::new, DepthsTree.FLAMECALLER, DepthsTrigger.COMBO)
			.linkedSpell(ClassAbility.VOLCANIC_COMBOS)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.BLAZE_ROD)
			.descriptions(VolcanicCombos::getDescription)
			.singleCharm(false);

	private final double mRadius;
	private final double mDamage;
	private final int mFireDuration;

	public VolcanicCombos(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.VOLCANIC_COMBOS_RADIUS.mEffectName, RADIUS);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.VOLCANIC_COMBOS_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mFireDuration = CharmManager.getDuration(mPlayer, CharmEffects.VOLCANIC_COMBOS_FIRE_DURATION.mEffectName, FIRE_TICKS);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (DepthsUtils.isValidComboAttack(event, mPlayer) && !isOnCooldown()) {
			putOnCooldown();

			activate(enemy, mPlayer, mPlugin, mRadius, mDamage, mFireDuration, mInfo.getLinkedSpell());
		}
		return false;
	}

	public static void activate(LivingEntity enemy, Player player) {
		activate(enemy, player, Plugin.getInstance(), RADIUS, DAMAGE[0], FIRE_TICKS, null);
	}

	public static void activate(LivingEntity enemy, Player player, Plugin plugin, double radius, double damage, int fireDuration, @Nullable ClassAbility classAbility) {
		Location location = enemy.getLocation();
		Location playerLoc = player.getLocation().add(player.getLocation().getDirection().multiply(0.5));
		for (LivingEntity mob : EntityUtils.getNearbyMobs(location, radius)) {
			EntityUtils.applyFire(plugin, fireDuration, mob, player);
			DamageUtils.damage(player, mob, DamageType.MAGIC, damage, classAbility, true);
		}
		World world = player.getWorld();

		new PartialParticle(Particle.SMOKE_LARGE, LocationUtils.getHalfHeightLocation(enemy), 30, 0, 0, 0, 0.2).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FLAME, LocationUtils.getHalfHeightLocation(enemy), 65, 0, 0, 0, 0.25).spawnAsPlayerActive(player);

		new PartialParticle(Particle.LAVA, location, 30, 0.2, 0.5, 0.2, 1).spawnAsPlayerActive(player);

		new PPCircle(Particle.FLAME, location, 0.8)
			.count(25).randomizeAngle(true)
			.spawnAsPlayerActive(player);
		new PPCircle(Particle.CAMPFIRE_COSY_SMOKE, location, 0.8)
			.count(15).randomizeAngle(true)
			.delta(0, 2.5, 0).directionalMode(true).extraRange(0.0, 0.1)
			.spawnAsPlayerActive(player);
		new PPCircle(Particle.FLAME, location, 0.8)
			.count(25).randomizeAngle(true)
			.delta(0, 1.25, 0).directionalMode(true).extraRange(0.0, 0.1)
			.spawnAsPlayerActive(player);
		new PPCircle(Particle.FLAME, location, 0.8)
			.count(60).randomizeAngle(true)
			.delta(0, 1.25, 0).directionalMode(true).extraRange(0.1, 0.4)
			.spawnAsPlayerActive(player);

		new BukkitRunnable() {
			double mD = 30;

			@Override
			public void run() {
				Vector vec;
				for (double degree = mD; degree < mD + 30; degree += 8) {
					double radian1 = Math.toRadians(degree);
					double cos = FastUtils.cos(radian1);
					double sin = FastUtils.sin(radian1);
					for (double r = 1; r < radius; r += 0.5) {
						vec = new Vector(cos * r, 1, sin * r);
						vec = VectorUtils.rotateXAxis(vec, playerLoc.getPitch());
						vec = VectorUtils.rotateYAxis(vec, playerLoc.getYaw());

						Location l = playerLoc.clone().add(vec);
						new PartialParticle(Particle.REDSTONE, l, 1, 0.1, 0.1, 0.1, YELLOW_COLOR).spawnAsPlayerActive(player);
						new PartialParticle(Particle.REDSTONE, l, 1, 0.1, 0.1, 0.1, ORANGE_COLOR).spawnAsPlayerActive(player);
					}
				}
				mD += 30;
				if (mD >= 150) {
					this.cancel();
				}
			}

		}.runTaskTimer(plugin, 0, 1);

		world.playSound(location, Sound.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.2f, 0.7f);
		world.playSound(location, Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 1.1f, 0.5f);
		world.playSound(location, Sound.ENTITY_STRIDER_STEP_LAVA, SoundCategory.PLAYERS, 1.2f, 0.5f);
		world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.2f, 0.7f);
		world.playSound(location, Sound.BLOCK_LAVA_EXTINGUISH, SoundCategory.PLAYERS, 1.2f, 2f);
	}

	private static Description<VolcanicCombos> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<>(() -> INFO, color)
			.add("When you melee attack an enemy, deal ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" magic damage to enemies in a ")
			.add(a -> a.mRadius, RADIUS)
			.add(" block radius and set those enemies on fire for ")
			.addDuration(a -> a.mFireDuration, FIRE_TICKS)
			.add(" seconds.")
			.addCooldown(COOLDOWN);
	}

}
