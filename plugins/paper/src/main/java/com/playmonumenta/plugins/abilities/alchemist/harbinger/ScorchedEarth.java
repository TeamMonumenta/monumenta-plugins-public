package com.playmonumenta.plugins.abilities.alchemist.harbinger;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.ScorchedEarthDamage;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;



public class ScorchedEarth extends MultipleChargeAbility {

	private static final String SCORCHED_EARTH_POTION_METAKEY = "ScorchedEarthPotion";

	private static final int SCORCHED_EARTH_1_COOLDOWN = 20 * 30;
	private static final int SCORCHED_EARTH_2_COOLDOWN = 20 * 25;
	private static final int SCORCHED_EARTH_1_CHARGES = 1;
	private static final int SCORCHED_EARTH_2_CHARGES = 2;
	private static final int SCORCHED_EARTH_DURATION = 20 * 15;
	public static final double SCORCHED_EARTH_DAMAGE_FRACTION = 0.25;
	private static final double SCORCHED_EARTH_RADIUS = 5;
	public static final Color SCORCHED_EARTH_COLOR_LIGHT = Color.fromRGB(230, 134, 0);
	public static final Color SCORCHED_EARTH_COLOR_DARK = Color.fromRGB(140, 63, 0);
	private static final String SCORCHED_EARTH_EFFECT_NAME = "ScorchedEarthDamageEffect";

	private Map<Location, Integer> mCenters;
	private int mLastCastTicks = 0;
	private @Nullable AlchemistPotions mAlchemistPotions;

	public ScorchedEarth(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Scorched Earth");
		mInfo.mLinkedSpell = ClassAbility.SCORCHED_EARTH;
		mInfo.mScoreboardId = "ScorchedEarth";
		mInfo.mShorthandName = "SE";
		mInfo.mDescriptions.add("Shift right click while holding an Alchemist's Bag to deploy a 5 block radius zone that lasts 15 seconds where the potion lands. Mobs in this zone are dealt 25% of your potion's damage extra whenever taking damage of types other than ailment or fire. Cooldown: 30s.");
		mInfo.mDescriptions.add("Cooldown reduced to 25s, and two charges of this ability can be stored at once.");
		mInfo.mCooldown = getAbilityScore() == 1 ? SCORCHED_EARTH_1_COOLDOWN : SCORCHED_EARTH_2_COOLDOWN;
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = new ItemStack(Material.BROWN_DYE, 1);
		mMaxCharges = getAbilityScore() == 1 ? SCORCHED_EARTH_1_CHARGES : SCORCHED_EARTH_2_CHARGES;
		mCharges = getTrackedCharges();
		mCenters = new HashMap<>();
		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
			mAlchemistPotions = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
		});
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		manageChargeCooldowns();
        // Copy list to avoid ConcurrentModificationException
		for (Location loc : new ArrayList<Location>(mCenters.keySet())) {
			Integer time = mCenters.get(loc);
			if (time == null) {
				continue;
			}
			int timeRemaining = time.intValue();
			if (timeRemaining <= 0) {
				mCenters.remove(loc);
			} else {
				mCenters.replace(loc, timeRemaining - 5);

				World world = loc.getWorld();
				world.spawnParticle(Particle.SMOKE_LARGE, loc, 3, 2.1, 0.3, 2.1, 0);
				world.spawnParticle(Particle.FLAME, loc, 3, 2, 0.1, 2, 0.1f);
				world.spawnParticle(Particle.REDSTONE, loc, 5, 2.1, 0.3, 2.1, new Particle.DustOptions(SCORCHED_EARTH_COLOR_LIGHT, 1.5f));
				world.spawnParticle(Particle.REDSTONE, loc, 5, 2.1, 0.3, 2.1, new Particle.DustOptions(SCORCHED_EARTH_COLOR_DARK, 1.5f));
				world.spawnParticle(Particle.LAVA, loc, 1, 2.1, 0.1, 2.1, 0);

				world.spawnParticle(Particle.REDSTONE, loc.clone().add(5 * FastUtils.sin((timeRemaining % 40 / 20.0 - 1) * Math.PI), 0, 5 * FastUtils.cos((timeRemaining % 40 / 20.0 - 1) * Math.PI)), 1, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1.25f));

				if (timeRemaining % 120 == 60 && timeRemaining < SCORCHED_EARTH_DURATION) {
					world.playSound(loc, Sound.BLOCK_FIRE_AMBIENT, 1f, 0.5f);
				}

				double damage = mAlchemistPotions.getDamage() * SCORCHED_EARTH_DAMAGE_FRACTION;
				for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, SCORCHED_EARTH_RADIUS)) {
					EffectManager.getInstance().addEffect(mob, SCORCHED_EARTH_EFFECT_NAME, new ScorchedEarthDamage(10, damage));
				}
			}
		}
	}

	@Override
	public boolean playerThrewSplashPotionEvent(ThrownPotion potion) {
		if (mPlayer != null && mPlayer.isSneaking() && ItemUtils.isAlchemistItem(mPlayer.getInventory().getItemInMainHand())) {
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
		if (mPlayer != null && potion.hasMetadata(SCORCHED_EARTH_POTION_METAKEY)) {
			Location loc = potion.getLocation();
			World world = mPlayer.getWorld();
			world.spawnParticle(Particle.SMOKE_NORMAL, loc, 50, 2.1, 0.5, 2.1, 0.1);
			world.spawnParticle(Particle.SMOKE_LARGE, loc, 15, 2.1, 0.5, 2.1, 0);
			world.spawnParticle(Particle.REDSTONE, loc, 20, 2.1, 0.5, 2.1, new Particle.DustOptions(SCORCHED_EARTH_COLOR_DARK, 2.0f));
			world.spawnParticle(Particle.FLAME, loc, 30, 2.1, 0.5, 2.1, 0.1);
			world.spawnParticle(Particle.LAVA, loc, 25, 1.5, 0.5, 1.5, 0);

			world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 1f, 0.5f);
			world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1f, 0.5f);
			world.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.5f, 1.5f);

			mCenters.put(loc, SCORCHED_EARTH_DURATION);
		}

		return true;
	}

}
