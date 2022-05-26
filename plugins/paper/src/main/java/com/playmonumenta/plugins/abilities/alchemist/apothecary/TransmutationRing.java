package com.playmonumenta.plugins.abilities.alchemist.apothecary;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.abilities.alchemist.PotionAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class TransmutationRing extends PotionAbility {
	private static final Particle.DustOptions GOLD_COLOR = new Particle.DustOptions(Color.fromRGB(255, 200, 0), 1.2f);
	private static final int TRANSMUTATION_RING_1_COOLDOWN = 25 * 20;
	private static final int TRANSMUTATION_RING_2_COOLDOWN = 20 * 20;
	private static final int TRANSMUTATION_RING_RADIUS = 5;
	private static final int TRANSMUTATION_RING_DURATION = 10 * 20;
	private static final String TRANSMUTATION_RING_DAMAGE_EFFECT_NAME = "TransmutationRingDamageEffect";
	private static final double DAMAGE_AMPLIFIER = 0.15;
	private static final int MAX_KILLS = 15;
	private static final int DURATION_INCREASE = 7; // was 0.333333 seconds but minecraft tick system is bad
	private static final int MAX_DURATION_INCREASE = 5 * 20;

	public static final String TRANSMUTATION_POTION_METAKEY = "TransmutationRingPotion";

	private @Nullable Location mCenter;
	private @Nullable AlchemistPotions mAlchemistPotions;
	private int mKills = 0;

	public TransmutationRing(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Transmutation Ring", 0, 0);
		mInfo.mLinkedSpell = ClassAbility.TRANSMUTATION_RING;
		mInfo.mScoreboardId = "Transmutation";
		mInfo.mShorthandName = "TR";
		mInfo.mDescriptions.add("Right click while sneaking and holding an Alchemist's Bag to create a Transmutation Ring at the potion's landing location that lasts for 10 seconds. The ring has a radius of 5 blocks. Other players within this ring deal 15% extra damage on all attacks. Mobs that die within this ring increase the damage bonus by 1% per mob, up to 30% total extra damage. Cooldown: 25s.");
		mInfo.mDescriptions.add("Mobs that die within this ring also increase the duration of the ring by 0.35 seconds per mob, up to 5 extra seconds. Cooldown: 20s.");
		mDisplayItem = new ItemStack(Material.GOLD_NUGGET, 1);
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mInfo.mCooldown = getAbilityScore() == 1 ? TRANSMUTATION_RING_1_COOLDOWN : TRANSMUTATION_RING_2_COOLDOWN;
		mInfo.mIgnoreCooldown = true;
		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
			mAlchemistPotions = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
		});
	}

	@Override
	public boolean playerThrewSplashPotionEvent(ThrownPotion potion) {
		if (mPlayer != null && mPlayer.isSneaking() && ItemUtils.isAlchemistItem(mPlayer.getInventory().getItemInMainHand()) && !isTimerActive()
			&& mAlchemistPotions.isAlchemistPotion(potion)) {
			putOnCooldown();
			potion.setMetadata(TRANSMUTATION_POTION_METAKEY, new FixedMetadataValue(mPlugin, null));
		}
		return true;
	}

	@Override
	public boolean playerSplashPotionEvent(Collection<LivingEntity> affectedEntities, ThrownPotion potion, PotionSplashEvent event) {
		if (mPlayer != null && potion.hasMetadata(TRANSMUTATION_POTION_METAKEY)) {
			mCenter = potion.getLocation();
			World world = mPlayer.getWorld();

			world.playSound(mCenter, Sound.ENTITY_PHANTOM_FLAP, 3f, 0.35f);

			new BukkitRunnable() {
				int mTicks = 0;
				int mMaxTicks = TRANSMUTATION_RING_DURATION;

				List<Integer> mDegrees1 = new ArrayList<>();
				List<Integer> mDegrees2 = new ArrayList<>();
				List<Integer> mDegrees3 = new ArrayList<>();

				@Override
				public void run() {
					if (getAbilityScore() == 2) {
						mMaxTicks = TRANSMUTATION_RING_DURATION + Math.min(mKills * DURATION_INCREASE, MAX_DURATION_INCREASE);
					}

					if (mTicks >= mMaxTicks || mCenter == null) {
						mCenter = null;
						mKills = 0;
						this.cancel();
						return;
					}

					double damageBoost = DAMAGE_AMPLIFIER + Math.min(mKills, MAX_KILLS) * 0.01;
					List<Player> players = PlayerUtils.playersInRange(mCenter, TRANSMUTATION_RING_RADIUS, true);
					players.remove(mPlayer);
					for (Player player : players) {
						mPlugin.mEffectManager.addEffect(player, TRANSMUTATION_RING_DAMAGE_EFFECT_NAME, new PercentDamageDealt(1 * 20, damageBoost));
					}

					List<Integer> degreesToKeep = new ArrayList<>();
					for (int deg = 0; deg < 360; deg += 3) {
						world.spawnParticle(Particle.REDSTONE, mCenter.clone().add(TRANSMUTATION_RING_RADIUS * FastUtils.cosDeg(deg), 0, TRANSMUTATION_RING_RADIUS * FastUtils.sinDeg(deg)), 1, GOLD_COLOR);

						if (mDegrees1.contains(deg)) {
							world.spawnParticle(Particle.REDSTONE, mCenter.clone().add(TRANSMUTATION_RING_RADIUS * FastUtils.cosDeg(deg), 0.5, TRANSMUTATION_RING_RADIUS * FastUtils.sinDeg(deg)), 1, GOLD_COLOR);
							if (FastUtils.randomDoubleInRange(0, 1) < 0.5) {
								mDegrees1.remove((Integer) deg);
							}
						}

						if (mDegrees2.contains(deg)) {
							world.spawnParticle(Particle.REDSTONE, mCenter.clone().add(TRANSMUTATION_RING_RADIUS * FastUtils.cosDeg(deg), 1, TRANSMUTATION_RING_RADIUS * FastUtils.sinDeg(deg)), 1, GOLD_COLOR);
							if (FastUtils.randomDoubleInRange(0, 1) < 0.5) {
								mDegrees2.remove((Integer) deg);
							}
						}

						if (mDegrees3.contains(deg)) {
							world.spawnParticle(Particle.REDSTONE, mCenter.clone().add(TRANSMUTATION_RING_RADIUS * FastUtils.cosDeg(deg), 1.75, TRANSMUTATION_RING_RADIUS * FastUtils.sinDeg(deg)), 1, GOLD_COLOR);
						}

						if (FastUtils.randomDoubleInRange(0, 1) < 0.25) {
							degreesToKeep.add(deg);
						}
					}

					mDegrees3 = new ArrayList<>(mDegrees2);
					mDegrees2 = new ArrayList<>(mDegrees1);
					mDegrees1 = new ArrayList<>(degreesToKeep);

					mTicks += 5;
				}
			}.runTaskTimer(mPlugin, 0, 5);
		}

		return true;
	}

	@Override
	public @Nullable Location entityDeathRadiusCenterLocation() {
		return mCenter;
	}

	@Override
	public double entityDeathRadius() {
		return TRANSMUTATION_RING_RADIUS;
	}

	@Override
	public void entityDeathRadiusEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		mKills++;
		mPlayer.getWorld().playSound(event.getEntity().getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1, 2);
	}

}
