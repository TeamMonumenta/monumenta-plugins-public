package com.playmonumenta.plugins.depths.abilities.dawnbringer;

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
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class BottledSunlight extends DepthsAbility {

	public static final String ABILITY_NAME = "Bottled Sunlight";
	private static final int COOLDOWN = 30 * 20;
	private static final int[] ABSORPTION = {8, 10, 12, 14, 16, 20};
	private static final int BOTTLE_THROW_COOLDOWN = 10 * 20;
	private static final int BOTTLE_ABSORPTION_DURATION = 20 * 30;
	private static final double BOTTLE_VELOCITY = 0.7;
	private static final int BOTTLE_TICK_PERIOD = 2;
	private static final int EFFECT_DURATION_REDUCTION = 10 * 20;

	public static final String CHARM_COOLDOWN = "Bottled Sunlight Cooldown";

	public static final DepthsAbilityInfo<BottledSunlight> INFO =
		new DepthsAbilityInfo<>(BottledSunlight.class, ABILITY_NAME, BottledSunlight::new, DepthsTree.DAWNBRINGER, DepthsTrigger.SHIFT_RIGHT_CLICK)
			.linkedSpell(ClassAbility.BOTTLED_SUNLIGHT)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", BottledSunlight::cast, DepthsTrigger.SHIFT_RIGHT_CLICK))
			.displayItem(Material.HONEY_BOTTLE)
			.descriptions(BottledSunlight::getDescription);

	private final double mAbsorption;
	private final int mDuration;

	public BottledSunlight(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mAbsorption = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.BOTTLED_SUNLIGHT_ABSORPTION_HEALTH.mEffectName, ABSORPTION[mRarity - 1]);
		mDuration = CharmManager.getDuration(mPlayer, CharmEffects.BOTTLED_SUNLIGHT_ABSORPTION_DURATION.mEffectName, BOTTLE_ABSORPTION_DURATION);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getEyeLocation();
		double velocity = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.BOTTLED_SUNLIGHT_BOTTLE_VELOCITY.mEffectName, BOTTLE_VELOCITY);
		Item bottle = AbilityUtils.spawnAbilityItem(world, loc, Material.HONEY_BOTTLE, "Bottled Sunlight", false, velocity, true, true);
		world.playSound(loc, Sound.ENTITY_SNOWBALL_THROW, SoundCategory.PLAYERS, 1, 0.15f);

		putOnCooldown();

		new BukkitRunnable() {
			int mTinctureDecay = 0;

			@Override
			public void run() {
				Location l = bottle.getLocation();
				new PartialParticle(Particle.SPELL, l, 3, 0, 0, 0, 0.1).spawnAsPlayerActive(mPlayer);

				for (Player p : PlayerUtils.playersInRange(l, 1, true)) {
					// Prevent players from picking up their own tincture instantly
					if (p == mPlayer && bottle.getTicksLived() < 12) {
						continue;
					}

					world.playSound(l, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1, 0.85f);
					new PartialParticle(Particle.BLOCK_DUST, l, 50, 0.1, 0.1, 0.1, 0.1, Material.GLASS.createBlockData()).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.FIREWORKS_SPARK, l, 30, 0.1, 0.1, 0.1, 0.2).spawnAsPlayerActive(mPlayer);
					bottle.remove();

					execute(mPlayer);
					if (p != mPlayer) {
						execute(p);
					}

					mPlugin.mTimers.removeCooldown(mPlayer, ClassAbility.BOTTLED_SUNLIGHT);
					putOnCooldown(false);

					this.cancel();
					return;
				}

				mTinctureDecay += BOTTLE_TICK_PERIOD;
				if (mTinctureDecay >= BOTTLE_THROW_COOLDOWN || !bottle.isValid() || bottle.isDead()) {
					bottle.remove();
					this.cancel();

					// Take the skill off cooldown (by setting to 0)
					mPlugin.mTimers.addCooldown(mPlayer, ClassAbility.BOTTLED_SUNLIGHT, 0);
				}
			}

		}.runTaskTimer(mPlugin, 0, BOTTLE_TICK_PERIOD);

		return true;
	}

	private void execute(Player player) {
		AbsorptionUtils.addAbsorption(player, mAbsorption, mAbsorption, mDuration);

		//Cleanse debuffs
		for (PotionEffectType effectType : PotionUtils.getNegativeEffects(mPlugin, player)) {
			PotionEffect effect = player.getPotionEffect(effectType);
			if (effect != null) {
				player.removePotionEffect(effectType);
				// No chance of overwriting and we don't want to trigger PotionApplyEvent for "upgrading" effects, so don't use PotionUtils here
				player.addPotionEffect(new PotionEffect(effectType, Math.max(effect.getDuration() - EFFECT_DURATION_REDUCTION, 0), effect.getAmplifier()));
			}
		}

		World world = player.getWorld();
		world.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 1.2f, 1.0f);
		new PartialParticle(Particle.FLAME, player.getLocation(), 30, 0.25, 0.1, 0.25, 0.125).spawnAsPlayerActive(mPlayer);
		new BukkitRunnable() {
			double mRotation = 0;
			double mY = 0.15;
			final double mRadius = 1.15;

			@Override
			public void run() {
				Location loc = player.getLocation();
				mRotation += 15;
				mY += 0.175;
				for (int i = 0; i < 3; i++) {
					double degree = Math.toRadians(mRotation + (i * 120));
					loc.add(FastUtils.cos(degree) * mRadius, mY, FastUtils.sin(degree) * mRadius);
					new PartialParticle(Particle.FLAME, loc, 1, 0.05, 0.05, 0.05, 0.05).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.SPELL_INSTANT, loc, 2, 0.05, 0.05, 0.05, 0).spawnAsPlayerActive(mPlayer);
					loc.subtract(FastUtils.cos(degree) * mRadius, mY, FastUtils.sin(degree) * mRadius);
				}

				if (mY >= 1.8) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}


	private static Description<BottledSunlight> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<BottledSunlight>(color)
			.add("Right click while sneaking to throw a luminescent bottle. If you or an ally walk over it, you both gain ")
			.add(a -> a.mAbsorption, ABSORPTION[rarity - 1], false, null, true)
			.add(" absorption health for ")
			.addDuration(a -> a.mDuration, BOTTLE_ABSORPTION_DURATION)
			.add(" seconds and the durations of negative potion effects get reduced by " + StringUtils.ticksToSeconds(EFFECT_DURATION_REDUCTION) + " seconds. If the bottle is destroyed or not grabbed, it quickly comes off cooldown.")
			.addCooldown(COOLDOWN);
	}


}

