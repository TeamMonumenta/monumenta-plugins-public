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
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.ParticleUtils.SpawnParticleAction;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class TotemOfSalvation extends DepthsAbility {

	public static final String ABILITY_NAME = "Totem of Salvation";
	public static final int COOLDOWN = 20 * 40;
	public static final int[] TICK_FREQUENCY = {40, 35, 30, 25, 20, 10};
	private static final double VELOCITY = 0.5;
	public static final int DURATION = 15 * 20;
	private static final int EFFECT_RADIUS = 5;
	private static final double PARTICLE_RING_HEIGHT = 1.0;
	private static final double PERCENT_HEALING = 0.08;
	private static final int MAX_ABSORPTION = 4;
	private static final int ABSORPTION_DURATION = 5 * 20;
	private static final Particle.DustOptions PARTICLE_COLOR = new Particle.DustOptions(Color.fromRGB(254, 212, 38), 1.0f);

	public static final String CHARM_COOLDOWN = "Totem of Salvation Cooldown";

	public static final DepthsAbilityInfo<TotemOfSalvation> INFO =
		new DepthsAbilityInfo<>(TotemOfSalvation.class, ABILITY_NAME, TotemOfSalvation::new, DepthsTree.DAWNBRINGER, DepthsTrigger.SWAP)
			.linkedSpell(ClassAbility.TOTEM_OF_SALVATION)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.actionBarColor(TextColor.color(254, 212, 38))
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", TotemOfSalvation::cast, DepthsTrigger.SWAP))
			.displayItem(Material.TOTEM_OF_UNDYING)
			.descriptions(TotemOfSalvation::getDescription);

	private static final Collection<Map.Entry<Double, SpawnParticleAction>> PARTICLES =
		List.of(new AbstractMap.SimpleEntry<Double, SpawnParticleAction>(0.4, (Location loc) -> new PartialParticle(Particle.REDSTONE, loc, 1, 0.1, 0.1, 0.1, PARTICLE_COLOR).spawnAsOtherPlayerActive()));

	private final double mRadius;
	private final int mDuration;
	private final double mPercentHeal;
	private final int mTickFrequency;
	private final int mAbsorptionDuration;
	private final double mMaxAbsorption;

	public TotemOfSalvation(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.TOTEM_OF_SALVATION_RADIUS.mEffectName, EFFECT_RADIUS);
		mDuration = CharmManager.getDuration(mPlayer, CharmEffects.TOTEM_OF_SALVATION_DURATION.mEffectName, DURATION);
		mPercentHeal = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.TOTEM_OF_SALVATION_HEALING.mEffectName, PERCENT_HEALING);
		mTickFrequency = TICK_FREQUENCY[mRarity - 1];
		mAbsorptionDuration = CharmManager.getDuration(mPlayer, CharmEffects.TOTEM_OF_SALVATION_ABSORPTION_DURATION.mEffectName, ABSORPTION_DURATION);
		mMaxAbsorption = MAX_ABSORPTION + CharmManager.getLevel(mPlayer, CharmEffects.TOTEM_OF_SALVATION_MAX_ABSORPTION.mEffectName);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getEyeLocation();
		Item totem = AbilityUtils.spawnAbilityItem(world, loc, Material.TOTEM_OF_UNDYING, "Totem of Salvation", false, VELOCITY, true, true);
		world.playSound(loc, Sound.ENTITY_SNOWBALL_THROW, SoundCategory.PLAYERS, 1.0f, 2.5f);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				Location l = totem.getLocation();
				if (!l.isChunkLoaded() || totem.isDead() || !totem.isValid()) {
					this.cancel();
					return;
				}

				//Particles once per second
				if (mTicks % 20 == 0) {
					ParticleUtils.explodingRingEffect(mPlugin, l.clone().add(0, 0.5, 0), mRadius, PARTICLE_RING_HEIGHT, 20, PARTICLES);
				}

				//Heal nearby players once per rarity frequency
				if (mTicks % TICK_FREQUENCY[mRarity - 1] == 0) {
					for (Player p : PlayerUtils.playersInRange(l, mRadius, true)) {
						double maxHealth = EntityUtils.getMaxHealth(p);
						double healthToHeal = maxHealth * mPercentHeal;
						if (p != mPlayer) {
							healthToHeal *= 1.5;
						}
						double healed = PlayerUtils.healPlayer(mPlugin, p, healthToHeal);

						double remainingHealing = healthToHeal - healed;
						if (remainingHealing > 0) {
							AbsorptionUtils.addAbsorption(p, remainingHealing, mMaxAbsorption, mAbsorptionDuration);
						}
					}
				}

				mTicks += 5;

				if (mTicks >= mDuration) {
					totem.remove();
					this.cancel();
				}

				// Very infrequently check if the item is still actually there
				if (mTicks % 100 == 0) {
					if (!EntityUtils.isStillLoaded(totem)) {
						this.cancel();
					}
				}
			}
		}.runTaskTimer(mPlugin, 0, 5);

		return true;
	}

	private static Description<TotemOfSalvation> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<TotemOfSalvation>(color)
			.add("Swap hands while holding a weapon to summon a totem that lasts ")
			.addDuration(a -> a.mDuration, DURATION)
			.add(" seconds. The totem heals all players within ")
			.add(a -> a.mRadius, EFFECT_RADIUS)
			.add(" blocks by ")
			.addPercent(a -> a.mPercentHeal, PERCENT_HEALING)
			.add(" of their max health every ")
			.addDuration(a -> a.mTickFrequency, TICK_FREQUENCY[rarity - 1], true, true)
			.add(" second")
			.add(TICK_FREQUENCY[rarity - 1] == 20 ? "s" : "")
			.add(". If a player has full health, the healing will be converted into absorption that lasts ")
			.addDuration(a -> a.mAbsorptionDuration, ABSORPTION_DURATION)
			.add(" seconds and caps at ")
			.add(a -> a.mMaxAbsorption, MAX_ABSORPTION)
			.add(" health. Healing from the totem is 50% more effective on allies.")
			.addCooldown(COOLDOWN);
	}

}

