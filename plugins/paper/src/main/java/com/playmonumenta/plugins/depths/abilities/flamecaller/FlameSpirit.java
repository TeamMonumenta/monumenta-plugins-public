package com.playmonumenta.plugins.depths.abilities.flamecaller;

import com.playmonumenta.plugins.Plugin;
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
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class FlameSpirit extends DepthsAbility {

	public static final String ABILITY_NAME = "Flame Spirit";

	public static final int[] DAMAGE = {5, 6, 7, 8, 9, 13};
	public static final int DURATION = 3 * 20;
	public static final int FIRE_TICKS = 2 * 20;
	public static final int RADIUS = 4;

	public static final DepthsAbilityInfo<FlameSpirit> INFO =
		new DepthsAbilityInfo<>(FlameSpirit.class, ABILITY_NAME, FlameSpirit::new, DepthsTree.FLAMECALLER, DepthsTrigger.SPAWNER)
			.linkedSpell(ClassAbility.FLAME_SPIRIT)
			.displayItem(Material.SOUL_CAMPFIRE)
			.descriptions(FlameSpirit::getDescription)
			.singleCharm(false);

	private final double mDamage;
	private final double mRadius;
	private final int mFireDuration;
	private final int mDuration;

	public FlameSpirit(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(player, CharmEffects.FLAME_SPIRIT_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mRadius = CharmManager.getRadius(player, CharmEffects.FLAME_SPIRIT_RADIUS.mEffectName, RADIUS);
		mFireDuration = CharmManager.getDuration(player, CharmEffects.FLAME_SPIRIT_FIRE_DURATION.mEffectName, FIRE_TICKS);
		mDuration = CharmManager.getDuration(player, CharmEffects.FLAME_SPIRIT_DURATION.mEffectName, DURATION);
	}

	public static void onSpawnerBreak(Plugin plugin, Player player, int rarity, Location loc) {
		double damage = CharmManager.calculateFlatAndPercentValue(player, CharmEffects.FLAME_SPIRIT_DAMAGE.mEffectName, DAMAGE[rarity - 1]);
		double radius = CharmManager.getRadius(player, CharmEffects.FLAME_SPIRIT_RADIUS.mEffectName, RADIUS);
		int fireDuration = CharmManager.getDuration(player, CharmEffects.FLAME_SPIRIT_FIRE_DURATION.mEffectName, FIRE_TICKS);
		int duration = CharmManager.getDuration(player, CharmEffects.FLAME_SPIRIT_DURATION.mEffectName, DURATION);
		onSpawnerBreak(plugin, player, loc, damage, radius, fireDuration, duration);
	}

	public static void onSpawnerBreak(Plugin plugin, Player player, Location loc, double damage, double radius, int fireDuration, int duration) {

		ItemStatManager.PlayerItemStats playerItemStats = plugin.mItemStatManager.getPlayerItemStatsCopy(player);

		new BukkitRunnable() {
			int mTickCount = 0;

			@Override
			public void run() {
				if (mTickCount >= duration) {
					this.cancel();
				}

				for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, radius)) {
					EntityUtils.applyFire(plugin, fireDuration, mob, player, playerItemStats);
					DamageUtils.damage(player, mob, new DamageEvent.Metadata(DamageType.MAGIC, ClassAbility.FLAME_SPIRIT, playerItemStats), damage, true, false, false);
				}

				new BukkitRunnable() {
					double mVerticalAngle = 0;
					double mRotationAngle = 0;
					int mTicksElapsed = 0;

					@Override
					public void run() {
						if (mTicksElapsed >= 20) {
							this.cancel();
						}

						mVerticalAngle += 5.5;
						mRotationAngle += 20;
						mVerticalAngle %= 360;
						mRotationAngle %= 360;

						new PartialParticle(
							Particle.FLAME,
							loc
								.add(
									FastUtils.cos(Math.toRadians(mRotationAngle)) * 2,
									FastUtils.sin(Math.toRadians(mVerticalAngle)) * 0.02,
									FastUtils.sin(Math.toRadians(mRotationAngle)) * 2
								), 1, 0, 0.01
						).spawnAsPlayerActive(player);

						new PartialParticle(
							Particle.FLAME,
							loc
								.add(
									FastUtils.cos(Math.toRadians(mRotationAngle)) * -2,
									FastUtils.sin(Math.toRadians(mVerticalAngle)) * 0.02,
									FastUtils.sin(Math.toRadians(mRotationAngle)) * -2
								), 1, 0, 0.01
						).spawnAsPlayerActive(player);

						mTicksElapsed++;
					}
				}.runTaskTimer(plugin, 0, 1);
				mTickCount += 20;
			}
		}.runTaskTimer(plugin, 0, 20);
	}

	@Override
	public boolean blockBreakEvent(BlockBreakEvent event) {
		if (event.isCancelled()) {
			return true;
		}
		Block block = event.getBlock();
		if (ItemUtils.isPickaxe(event.getPlayer().getInventory().getItemInMainHand()) && block.getType() == Material.SPAWNER) {
			onSpawnerBreak(mPlugin, mPlayer, block.getLocation().add(0.5, 0, 0.5), mDamage, mRadius, mFireDuration, mDuration);
		}
		return true;
	}

	private static Description<FlameSpirit> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<FlameSpirit>(color)
			.add("Breaking a spawner summons a spirit that deals ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" magic damage in a ")
			.add(a -> a.mRadius, RADIUS)
			.add(" block radius every second for ")
			.addDuration(a -> a.mDuration, DURATION)
			.add(" seconds and sets affected mobs on fire for ")
			.addDuration(a -> a.mFireDuration, FIRE_TICKS)
			.add(" seconds.");
	}


}
