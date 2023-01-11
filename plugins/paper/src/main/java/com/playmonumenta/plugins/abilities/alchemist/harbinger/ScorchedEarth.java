package com.playmonumenta.plugins.abilities.alchemist.harbinger;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.ScorchedEarthDamage;
import com.playmonumenta.plugins.itemstats.ItemStatManager.PlayerItemStats;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.Nullable;

public class ScorchedEarth extends MultipleChargeAbility {

	private static final String SCORCHED_EARTH_POTION_METAKEY = "ScorchedEarthPotion";

	private static final int SCORCHED_EARTH_1_COOLDOWN = 20 * 30;
	private static final int SCORCHED_EARTH_2_COOLDOWN = 20 * 25;
	private static final int SCORCHED_EARTH_1_CHARGES = 1;
	private static final int SCORCHED_EARTH_2_CHARGES = 2;
	private static final int SCORCHED_EARTH_DURATION = 20 * 15;
	private static final int SCORCHED_EARTH_FIRE_DURATION = 20 * 4;
	public static final double SCORCHED_EARTH_DAMAGE_FRACTION = 0.25;
	private static final double SCORCHED_EARTH_RADIUS = 5;
	public static final Color SCORCHED_EARTH_COLOR_LIGHT = Color.fromRGB(230, 134, 0);
	public static final Color SCORCHED_EARTH_COLOR_DARK = Color.fromRGB(140, 63, 0);
	private static final String SCORCHED_EARTH_EFFECT_NAME = "ScorchedEarthDamageEffect";

	public static final String CHARM_COOLDOWN = "Scorched Earth Cooldown";
	public static final String CHARM_CHARGES = "Scorched Earth Charge";
	public static final String CHARM_DURATION = "Scorched Earth Duration";
	public static final String CHARM_RADIUS = "Scorched Earth Radius";
	public static final String CHARM_DAMAGE = "Scorched Earth Damage";
	public static final String CHARM_FIRE_DURATION = "Scorched Earth Fire Duration";

	public static final AbilityInfo<ScorchedEarth> INFO =
		new AbilityInfo<>(ScorchedEarth.class, "Scorched Earth", ScorchedEarth::new)
			.linkedSpell(ClassAbility.SCORCHED_EARTH)
			.scoreboardId("ScorchedEarth")
			.shorthandName("SE")
			.descriptions(
				"Sneak while throwing an Alchemist's Potion to deploy a 5 block radius zone that lasts 15 seconds where the potion lands. " +
					"Mobs in this zone are dealt 25% of your potion's damage and set on fire for 4s whenever taking damage of types other than ailment or fire. Cooldown: 30s.",
				"Cooldown reduced to 25s, and two charges of this ability can be stored at once.")
			.cooldown(SCORCHED_EARTH_1_COOLDOWN, SCORCHED_EARTH_2_COOLDOWN, CHARM_COOLDOWN)
			.displayItem(new ItemStack(Material.BROWN_DYE, 1));

	private final int mDuration;
	private final double mRadius;

	private final Map<Location, Integer> mCenters;
	private int mLastCastTicks = 0;
	private @Nullable AlchemistPotions mAlchemistPotions;

	public ScorchedEarth(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mMaxCharges = (isLevelOne() ? SCORCHED_EARTH_1_CHARGES : SCORCHED_EARTH_2_CHARGES) + (int) CharmManager.getLevel(mPlayer, CHARM_CHARGES);
		mCharges = getTrackedCharges();
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, SCORCHED_EARTH_DURATION);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, SCORCHED_EARTH_RADIUS);
		mCenters = new HashMap<>();
		Bukkit.getScheduler().runTask(mPlugin, () -> {
			mAlchemistPotions = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
		});
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mAlchemistPotions == null) {
			mCenters.clear();
			return;
		}
		manageChargeCooldowns();
		double damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, mAlchemistPotions.getDamage() * SCORCHED_EARTH_DAMAGE_FRACTION);
		for (Iterator<Map.Entry<Location, Integer>> iterator = mCenters.entrySet().iterator(); iterator.hasNext(); ) {
			Map.Entry<Location, Integer> center = iterator.next();
			Location loc = center.getKey();
			int timeRemaining = center.getValue();
			if (timeRemaining <= 0) {
				iterator.remove();
			} else {
				center.setValue(timeRemaining - 5);

				World world = loc.getWorld();
				new PartialParticle(Particle.SMOKE_LARGE, loc, 3, 2.1, 0.3, 2.1, 0).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.FLAME, loc, 3, 2, 0.1, 2, 0.1f).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.REDSTONE, loc, 5, 2.1, 0.3, 2.1, new Particle.DustOptions(SCORCHED_EARTH_COLOR_LIGHT, 1.5f)).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.REDSTONE, loc, 5, 2.1, 0.3, 2.1, new Particle.DustOptions(SCORCHED_EARTH_COLOR_DARK, 1.5f)).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.LAVA, loc, 1, 2.1, 0.1, 2.1, 0).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);

				new PartialParticle(Particle.REDSTONE, loc.clone().add(5 * FastUtils.sin((timeRemaining % 40 / 20.0 - 1) * Math.PI), 0, 5 * FastUtils.cos((timeRemaining % 40 / 20.0 - 1) * Math.PI)), 1, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1.25f)).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);

				if (timeRemaining % 120 == 60 && timeRemaining < mDuration) {
					world.playSound(loc, Sound.BLOCK_FIRE_AMBIENT, SoundCategory.PLAYERS, 1f, 0.5f);
				}

				PlayerItemStats stats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);
				int fireDuration = CharmManager.getDuration(mPlayer, CHARM_FIRE_DURATION, SCORCHED_EARTH_FIRE_DURATION);
				Hitbox hitbox = new Hitbox.SphereHitbox(loc, mRadius);
				for (LivingEntity mob : hitbox.getHitMobs()) {
					mPlugin.mEffectManager.addEffect(mob, SCORCHED_EARTH_EFFECT_NAME, new ScorchedEarthDamage(10, damage, mPlayer, stats, fireDuration));
				}
			}
		}
	}

	@Override
	public boolean playerThrewSplashPotionEvent(ThrownPotion potion) {
		if (mPlayer.isSneaking()
			    && ItemUtils.isAlchemistItem(mPlayer.getInventory().getItemInMainHand())
			    && mAlchemistPotions != null
			    && mAlchemistPotions.isAlchemistPotion(potion)) {

			int ticks = mPlayer.getTicksLived();
			// Prevent double casting on accident
			if (ticks - mLastCastTicks <= 5 || !consumeCharge()) {
				return true;
			}
			mLastCastTicks = ticks;
			potion.setMetadata(SCORCHED_EARTH_POTION_METAKEY, new FixedMetadataValue(mPlugin, null));
		}
		return true;
	}

	@Override
	public boolean playerSplashPotionEvent(Collection<LivingEntity> affectedEntities, ThrownPotion potion, PotionSplashEvent event) {
		if (potion.hasMetadata(SCORCHED_EARTH_POTION_METAKEY)) {
			Location loc = potion.getLocation();
			World world = mPlayer.getWorld();
			new PartialParticle(Particle.SMOKE_NORMAL, loc, 50, 2.1, 0.5, 2.1, 0.1).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.SMOKE_LARGE, loc, 15, 2.1, 0.5, 2.1, 0).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.REDSTONE, loc, 20, 2.1, 0.5, 2.1, new Particle.DustOptions(SCORCHED_EARTH_COLOR_DARK, 2.0f)).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.FLAME, loc, 30, 2.1, 0.5, 2.1, 0.1).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.LAVA, loc, 25, 1.5, 0.5, 1.5, 0).spawnAsPlayerActive(mPlayer);

			world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 1f, 0.5f);
			world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1f, 0.5f);
			world.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.PLAYERS, 0.5f, 1.5f);

			mCenters.put(loc, mDuration);
		}

		return true;
	}

}
