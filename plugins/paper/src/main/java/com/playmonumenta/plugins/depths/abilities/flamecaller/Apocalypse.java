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
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class Apocalypse extends DepthsAbility {
	public static final String ABILITY_NAME = "Apocalypse";
	public static final int COOLDOWN = 75 * 20;
	private static final double TRIGGER_HEALTH = 0.25;
	public static final int[] DAMAGE = {40, 50, 60, 70, 80, 100};
	public static final int RADIUS = 5;
	public static final double HEALING = 0.3; //percent health per kill
	public static final double MAX_ABSORPTION = 0.25;
	public static final int ABSORPTION_DURATION = 30 * 20;

	public static final String CHARM_COOLDOWN = "Apocalypse Cooldown";

	public static final DepthsAbilityInfo<Apocalypse> INFO =
		new DepthsAbilityInfo<>(Apocalypse.class, ABILITY_NAME, Apocalypse::new, DepthsTree.FLAMECALLER, DepthsTrigger.LIFELINE)
			.linkedSpell(ClassAbility.APOCALYPSE)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.ORANGE_DYE)
			.descriptions(Apocalypse::getDescription)
			.priorityAmount(10000);

	private final double mDamage;
	private final double mRadius;
	private final double mHealPercent;
	private final double mMaxAbsorption;

	public Apocalypse(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.APOCALYPSE_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.APOCALYPSE_RADIUS.mEffectName, RADIUS);
		mHealPercent = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.APOCALYPSE_HEALING.mEffectName, HEALING);
		mMaxAbsorption = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.APOCALYPSE_MAX_ABSORPTION.mEffectName, MAX_ABSORPTION);
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (event.isBlocked() || isOnCooldown() || event.getType() == DamageType.TRUE) {
			return;
		}

		// Calculate whether this effect should not be run based on player health.
		double healthRemaining = mPlayer.getHealth() - event.getFinalDamage(true);

		double maxHealth = EntityUtils.getMaxHealth(mPlayer);
		if (healthRemaining > maxHealth * TRIGGER_HEALTH) {
			return;
		}

		putOnCooldown();

		Location loc = mPlayer.getLocation();
		List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(loc, mRadius);
		int count = 0;

		for (LivingEntity mob : nearbyMobs) {
			DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, mDamage, mInfo.getLinkedSpell(), true);
			if (mob == null || mob.isDead() || mob.getHealth() <= 0) {
				count++;
			}
		}

		double totalHealing = maxHealth * count * mHealPercent;
		double healed = PlayerUtils.healPlayer(mPlugin, mPlayer, totalHealing);

		double absorption = totalHealing - healed;
		AbsorptionUtils.addAbsorption(mPlayer, absorption, maxHealth * mMaxAbsorption, ABSORPTION_DURATION);

		World world = mPlayer.getWorld();
		double mult = mRadius / RADIUS;
		new PartialParticle(Particle.EXPLOSION_HUGE, loc, (int) (10 * mult), 2 * mult, 2 * mult, 2 * mult).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.FLAME, loc, 100, 3.5, 3.5, 3.5, 0).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.HEART, loc.clone().add(0, 1, 0), count * 7, 0.5, 0.5, 0.5).spawnAsPlayerActive(mPlayer);

		world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 1.0f, 2.0f);
		world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 0.5f, 1.0f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 2.0f, 0.1f);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 2.0f, 0.1f);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 0.6f);
		world.playSound(loc, Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 1.0f, 0.1f);

		sendActionBarMessage("Apocalypse has been activated!");
		event.setCancelled(true);
	}

	@Override
	public void onHurtFatal(DamageEvent event) {
		onHurt(event, null, null);
	}

	private static Description<Apocalypse> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<Apocalypse>(color)
			.add("When your health drops below ")
			.addPercent(TRIGGER_HEALTH)
			.add(", ignore the hit and instead deal ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" magic damage in a ")
			.add(a -> a.mRadius, RADIUS)
			.add(" block radius. For each mob that is killed, heal ")
			.addPercent(a -> a.mHealPercent, HEALING)
			.add(" of your max health. Healing above your max health is converted into absorption, up to ")
			.addPercent(a -> a.mMaxAbsorption, MAX_ABSORPTION)
			.add(" of your max health that lasts ")
			.addDuration(ABSORPTION_DURATION)
			.add(" seconds.")
			.addCooldown(COOLDOWN);
	}


}
