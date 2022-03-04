package com.playmonumenta.plugins.abilities.alchemist.apothecary;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.abilities.alchemist.PotionAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
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
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class TransmutationRing extends PotionAbility {
	private static final Particle.DustOptions GOLD_COLOR = new Particle.DustOptions(Color.fromRGB(255, 200, 0), 1.2f);
	private static final int TRANSMUTATION_RING_1_COOLDOWN = 25 * 20;
	private static final int TRANSMUTATION_RING_2_COOLDOWN = 20 * 20;
	private static final int TRANSMUTATION_RING_RADIUS = 5;
	private static final int TRANSMUTATION_RING_DURATION = 10 * 20;
	private static final double TRANSMUTATION_RING_RESISTANCE = 0.1;
	private static final String TRANSMUTATION_RING_RESISTANCE_EFFECT_NAME = "TransmutationRingResistanceEffect";
	private static final double TRANSMUTATION_RING_1_DAMAGE_FRACTION = 0.3;
	private static final double TRANSMUTATION_RING_2_DAMAGE_FRACTION = 0.5;

	public static final String TRANSMUTATION_POTION_TAG = "TransmutationRingPotion";

	private @Nullable Location mCenter;
	private double mDamageFraction;
	private @Nullable AlchemistPotions mAlchemistPotions;

	public TransmutationRing(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Transmutation Ring", 0, 0);
		mInfo.mLinkedSpell = ClassAbility.TRANSMUTATION_RING;
		mInfo.mScoreboardId = "Transmutation";
		mInfo.mShorthandName = "TR";
		mInfo.mDescriptions.add("Right click while sneaking and holding an Alchemist's Bag to create a Transmutation Ring at your location that lasts for 10 seconds. The ring has a radius of 5 blocks. Players within this ring receive 10% damage reduction. Mobs that die within this ring spawn an Alchemist's Potion that deals 30% of your base potion damage, with no extra effects. Cooldown: 25s.");
		mInfo.mDescriptions.add("Potions dropped from mob deaths within the ring now deal 50% of your base potion damage. Cooldown: 20s.");
		mDisplayItem = new ItemStack(Material.GOLD_NUGGET, 1);
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mInfo.mCooldown = getAbilityScore() == 1 ? TRANSMUTATION_RING_1_COOLDOWN : TRANSMUTATION_RING_2_COOLDOWN;
		mInfo.mIgnoreCooldown = true;
		mDamageFraction = getAbilityScore() == 1 ? TRANSMUTATION_RING_1_DAMAGE_FRACTION : TRANSMUTATION_RING_2_DAMAGE_FRACTION;
		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
			mAlchemistPotions = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
		});
	}

	@Override
	public void cast(Action action) {
		if (mPlayer != null && mPlayer.isSneaking() && !isTimerActive() && !mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell) && ItemUtils.isAlchemistItem(mPlayer.getInventory().getItemInMainHand())) {
			putOnCooldown();
			mCenter = mPlayer.getLocation();
			World world = mCenter.getWorld();

			world.playSound(mCenter, Sound.ENTITY_PHANTOM_FLAP, 3f, 0.35f);

			new BukkitRunnable() {
				int mTicks = 0;

				List<Integer> mDegrees1 = new ArrayList<>();
				List<Integer> mDegrees2 = new ArrayList<>();
				List<Integer> mDegrees3 = new ArrayList<>();

				@Override
				public void run() {
					if (mTicks >= TRANSMUTATION_RING_DURATION || mCenter == null) {
						mCenter = null;
						this.cancel();
						return;
					}

					for (Player player : PlayerUtils.playersInRange(mCenter, TRANSMUTATION_RING_RADIUS, true)) {
						mPlugin.mEffectManager.addEffect(player, TRANSMUTATION_RING_RESISTANCE_EFFECT_NAME, new PercentDamageReceived(6, -TRANSMUTATION_RING_RESISTANCE));
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
		if (mPlayer == null || mAlchemistPotions == null) {
			return;
		}

		ThrownPotion pot = mPlayer.getWorld().spawn(event.getEntity().getEyeLocation(), ThrownPotion.class);
		pot.setShooter(mPlayer);
		mAlchemistPotions.setPotionToAlchemistPotion(pot);
		pot.setMetadata(TRANSMUTATION_POTION_TAG, new FixedMetadataValue(mPlugin, 0));
		mPlayer.getWorld().playSound(event.getEntity().getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1, 2);
	}

	// Called in AlchemistPotions
	public void applyTransmutationPotion(Collection<LivingEntity> affectedEntities) {
		if (mPlayer == null || mAlchemistPotions == null) {
			return;
		}

		if (affectedEntities != null && !affectedEntities.isEmpty()) {
			for (LivingEntity entity : affectedEntities) {
				if (EntityUtils.isHostileMob(entity)) {
					DamageUtils.damage(mPlayer, entity, DamageType.AILMENT, mDamageFraction * mAlchemistPotions.getDamage(), ClassAbility.ALCHEMIST_POTION, false, true);
				}

			}
		}
	}
}
