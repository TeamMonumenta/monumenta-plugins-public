package com.playmonumenta.plugins.depths.abilities.flamecaller;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.shadow.DummyDecoy;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import java.util.List;
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
import org.bukkit.scheduler.BukkitRunnable;

public class RingOfFlames extends DepthsAbility {

	public static final String ABILITY_NAME = "Ring of Flames";
	private static final int COOLDOWN = 18 * 20;
	private static final int[] DAMAGE = {6, 7, 8, 9, 10, 12};
	private static final int[] DURATION = {8 * 20, 9 * 20, 10 * 20, 11 * 20, 12 * 20, 14 * 20};
	private static final int EFFECT_DURATION = 4 * 20;
	private static final double BLEED_AMOUNT = 0.2;

	public static final String CHARM_COOLDOWN = "Ring of Flames Cooldown";

	public static final DepthsAbilityInfo<RingOfFlames> INFO =
		new DepthsAbilityInfo<>(RingOfFlames.class, ABILITY_NAME, RingOfFlames::new, DepthsTree.FLAMECALLER, DepthsTrigger.SHIFT_LEFT_CLICK)
			.linkedSpell(ClassAbility.RING_OF_FLAMES)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", RingOfFlames::cast, DepthsTrigger.SHIFT_LEFT_CLICK))
			.displayItem(Material.BLAZE_POWDER)
			.descriptions(RingOfFlames::getDescription);

	private final double mDamage;
	private final int mEffectDuration;
	private final double mBleedAmount;
	private final int mRingDuration;

	public RingOfFlames(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.RING_OF_FLAMES_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mEffectDuration = CharmManager.getDuration(mPlayer, CharmEffects.RING_OF_FLAMES_FIRE_DURATION.mEffectName, EFFECT_DURATION);
		mBleedAmount = BLEED_AMOUNT + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.RING_OF_FLAMES_BLEED_AMPLIFIER.mEffectName);
		mRingDuration = CharmManager.getDuration(mPlayer, CharmEffects.RING_OF_FLAMES_DURATION.mEffectName, DURATION[mRarity - 1]);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();

		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);

		new PartialParticle(Particle.SOUL_FIRE_FLAME, mPlayer.getLocation(), 50, 0.25f, 0.1f, 0.25f, 0.15f).spawnAsPlayerActive(mPlayer);

		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1, 0);
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1, 0), 5);

		new BukkitRunnable() {
			private int mTicks = 0;
			private int mDeg = 0;
			private final Hitbox mHitbox = Hitbox.approximateHollowCylinderSegment(loc, 4, 4 - 0.5, 4 + 0.5, Math.PI);
			@Override
			public void run() {
				if (mTicks >= mRingDuration) {
					this.cancel();
					world.playSound(loc, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.PLAYERS, 1, 0.75f);
					return;
				}

				Location tempLoc = loc.clone();
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
					List<LivingEntity> mobs = mHitbox.getHitMobs().stream().filter(mob -> !mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG) || mob.getName().equals(DummyDecoy.DUMMY_NAME)).toList();
					int mobsHitThisTick = 0;
					for (LivingEntity e : mobs) {
						if (mobsHitThisTick <= 10) {
							world.playSound(e.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.PLAYERS, 0.8f, 1f);
						}
						new PartialParticle(Particle.SOUL_FIRE_FLAME, e.getLocation(), 30, 0.25f, 0.1f, 0.25f, 0.15f).spawnAsPlayerActive(mPlayer);
						EntityUtils.applyFire(mPlugin, mEffectDuration, e, mPlayer, playerItemStats);
						EntityUtils.applyBleed(mPlugin, mEffectDuration, mBleedAmount, e);

						DamageUtils.damage(mPlayer, e, new DamageEvent.Metadata(DamageType.MAGIC, mInfo.getLinkedSpell(), playerItemStats), mDamage, false, true, false);

						mobsHitThisTick++;
					}
				}

				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
		return true;
	}


	private static Description<RingOfFlames> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<RingOfFlames>(color)
			.add("Left click while sneaking to summon a ring of flames around you that lasts for ")
			.addDuration(a -> a.mRingDuration, DURATION[rarity - 1], false, true)
			.add(" seconds. Enemies on the flame perimeter are dealt ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" magic damage every second, and they are inflicted with ")
			.addPercent(a -> a.mBleedAmount, BLEED_AMOUNT)
			.add(" Bleed and set on fire for ")
			.addDuration(a -> a.mEffectDuration, EFFECT_DURATION)
			.add(" seconds.")
			.addCooldown(COOLDOWN);
	}
}
