package com.playmonumenta.plugins.depths.abilities.flamecaller;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.shadow.DummyDecoy;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

public class RingOfFlames extends DepthsAbility {

	public static final String ABILITY_NAME = "Ring of Flames";
	private static final int COOLDOWN = 18 * 20;
	private static final int[] DAMAGE = {4, 5, 6, 7, 8, 10};
	private static final int[] DURATION = {8 * 20, 9 * 20, 10 * 20, 11 * 20, 12 * 20, 14 * 20};
	private static final int EFFECT_DURATION = 4 * 20;
	private static final double BLEED_AMOUNT = 0.2;

	public static final DepthsAbilityInfo<RingOfFlames> INFO =
		new DepthsAbilityInfo<>(RingOfFlames.class, ABILITY_NAME, RingOfFlames::new, DepthsTree.FLAMECALLER, DepthsTrigger.SHIFT_LEFT_CLICK)
			.linkedSpell(ClassAbility.RING_OF_FLAMES)
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", RingOfFlames::cast,
				new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true).keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE), HOLDING_WEAPON_RESTRICTION))
			.displayItem(new ItemStack(Material.BLAZE_POWDER))
			.descriptions(RingOfFlames::getDescription);

	public RingOfFlames(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		putOnCooldown();

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();

		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);

		new PartialParticle(Particle.SOUL_FIRE_FLAME, mPlayer.getLocation(), 50, 0.25f, 0.1f, 0.25f, 0.15f).spawnAsPlayerActive(mPlayer);

		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1, 0);
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1, 0), 5);

		Location tempLoc = loc.clone();
		List<BoundingBox> boxes = new ArrayList<>();

		for (int deg = 0; deg < 360; deg += 5) {
			tempLoc.set(loc.getX() + 4 * FastUtils.cos(deg), loc.getY() + 2, loc.getZ() + 4 * FastUtils.sin(deg));
			boxes.add(BoundingBox.of(tempLoc, 0.5, 3.5, 0.5));
		}

		new BukkitRunnable() {
			private int mTicks = 0;
			private int mDeg = 0;
			@Override
			public void run() {
				if (mTicks >= DURATION[mRarity - 1]) {
					this.cancel();
					world.playSound(loc, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.PLAYERS, 1, 0.75f);
					return;
				}

				for (int y = -1; y < 4; y++) {
					tempLoc.set(loc.getX() + 4 * FastUtils.cos(mDeg), loc.getY() + y, loc.getZ() + 4 * FastUtils.sin(mDeg));
					new PartialParticle(Particle.SOUL_FIRE_FLAME, tempLoc, 1, 0, 0, 0, 0).spawnAsPlayerActive(mPlayer);

					tempLoc.set(loc.getX() + 4 * FastUtils.cos(mDeg + 180), loc.getY() + y, loc.getZ() + 4 * FastUtils.sin(mDeg + 180));
					new PartialParticle(Particle.FLAME, tempLoc, 1, 0, 0, 0, 0).spawnAsPlayerActive(mPlayer);
				}
				mDeg++;
				if (mDeg >= 360) {
					mDeg = 0;
				}


				if (mTicks % 20 == 0) {
					List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, 6);
					mobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG) && !mob.getName().equals(DummyDecoy.DUMMY_NAME));

					int mobsHitThisTick = 0;
					for (BoundingBox box : boxes) {
						for (LivingEntity e : mobs) {
							if (box.overlaps(e.getBoundingBox())) {
								if (mobsHitThisTick <= 10) {
									world.playSound(e.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.PLAYERS, 0.8f, 1f);
								}
								new PartialParticle(Particle.SOUL_FIRE_FLAME, e.getLocation(), 30, 0.25f, 0.1f, 0.25f, 0.15f).spawnAsPlayerActive(mPlayer);
								EntityUtils.applyFire(mPlugin, EFFECT_DURATION, e, mPlayer, playerItemStats);
								EntityUtils.applyBleed(mPlugin, EFFECT_DURATION, BLEED_AMOUNT, e);

								DamageUtils.damage(mPlayer, e, new DamageEvent.Metadata(DamageType.MAGIC, mInfo.getLinkedSpell(), playerItemStats), DAMAGE[mRarity - 1], false, true, false);

								mobsHitThisTick++;
							}
						}
					}
				}

				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}


	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("Left click while sneaking and holding a weapon to summon a ring of flames around you that lasts for ")
			.append(Component.text(DURATION[rarity - 1] / 20, color))
			.append(Component.text(" seconds. Enemies on the flame perimeter are dealt "))
			.append(Component.text(DAMAGE[rarity - 1], color))
			.append(Component.text(" magic damage every second, and they are inflicted with " + StringUtils.multiplierToPercentage(BLEED_AMOUNT) + "% Bleed and set on fire for " + EFFECT_DURATION / 20 + " seconds. Cooldown: " + COOLDOWN / 20 + "s."));
	}
}
