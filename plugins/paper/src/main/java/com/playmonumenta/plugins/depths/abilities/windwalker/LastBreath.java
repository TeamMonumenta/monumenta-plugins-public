package com.playmonumenta.plugins.depths.abilities.windwalker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.bosses.bosses.CrowdControlImmunityBoss;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class LastBreath extends DepthsAbility {
	public static final String ABILITY_NAME = "Last Breath";
	public static final int COOLDOWN = 60 * 20;
	private static final double TRIGGER_HEALTH = 0.4;
	public static final double[] COOLDOWN_REDUCTION = {0.4, 0.5, 0.6, 0.7, 0.8, 1.0};
	private static final double[] SPEED = {0.1, 0.125, 0.15, 0.175, 0.2, 0.35};
	private static final int[] RESISTANCE_TICKS = {30, 35, 40, 45, 50, 70};
	private static final int SPEED_DURATION = 6 * 20;
	private static final String SPEED_EFFECT_NAME = "LastBreathSpeedEffect";
	public static final int RADIUS = 5;
	public static final double KNOCKBACK_SPEED = 2;

	public static final DepthsAbilityInfo<LastBreath> INFO =
		new DepthsAbilityInfo<>(LastBreath.class, ABILITY_NAME, LastBreath::new, DepthsTree.WINDWALKER, DepthsTrigger.LIFELINE)
			.linkedSpell(ClassAbility.LAST_BREATH)
			.cooldown(COOLDOWN)
			.displayItem(new ItemStack(Material.DRAGON_BREATH))
			.descriptions(LastBreath::getDescription)
			.priorityAmount(10000);

	public LastBreath(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (event.isBlocked() || isOnCooldown() || event.getType() == DamageEvent.DamageType.TRUE) {
			return;
		}

		// Calculate whether this effect should not be run based on player health.
		double healthRemaining = mPlayer.getHealth() - event.getFinalDamage(true);

		if (healthRemaining > EntityUtils.getMaxHealth(mPlayer) * TRIGGER_HEALTH) {
			return;
		}

		putOnCooldown();

		for (Ability abil : mPlugin.mAbilityManager.getPlayerAbilities(mPlayer).getAbilities()) {
			ClassAbility linkedSpell = abil.getInfo().getLinkedSpell();
			if (abil == this || linkedSpell == null) {
				continue;
			}
			int totalCD = abil.getModifiedCooldown();
			int reducedCD;
			if (abil instanceof DepthsAbility da && da.getInfo().getDepthsTree() == DepthsTree.WINDWALKER) {
				reducedCD = totalCD;
			} else {
				reducedCD = (int) (totalCD * COOLDOWN_REDUCTION[mRarity - 1]);
			}
			mPlugin.mTimers.updateCooldown(mPlayer, linkedSpell, reducedCD);
		}

		Location loc = mPlayer.getLocation();
		World world = mPlayer.getWorld();

		mPlugin.mEffectManager.addEffect(mPlayer, SPEED_EFFECT_NAME, new PercentSpeed(SPEED_DURATION, SPEED[mRarity - 1], SPEED_EFFECT_NAME));
		mPlayer.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, RESISTANCE_TICKS[mRarity - 1], 4));
		for (LivingEntity e : EntityUtils.getNearbyMobs(loc, RADIUS)) {
			if (!ScoreboardUtils.checkTag(e, CrowdControlImmunityBoss.identityTag)) {
				Vector knockback = e.getVelocity().add(e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(KNOCKBACK_SPEED));
				knockback.setY(knockback.getY() * 2);
				e.setVelocity(knockback.add(new Vector(0, 0.25, 0)));
				new PartialParticle(Particle.EXPLOSION_NORMAL, e.getLocation(), 5, 0, 0, 0, 0.35).spawnAsPlayerActive(mPlayer);
			}
		}

		new PartialParticle(Particle.END_ROD, loc.clone().add(0, 1, 0), 20, 1.25, 1.25, 1.25).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CLOUD, loc.clone().add(0, 1, 0), 20, 1.25, 1.25, 1.25).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.VILLAGER_HAPPY, loc.clone().add(0, 1, 0), 20, 1.25, 1.25, 1.25).spawnAsPlayerActive(mPlayer);

		world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 2.0f, 1.2f);
		world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 2.0f, 0.4f);

		MessagingUtils.sendActionBarMessage(mPlayer, "Last Breath has been activated!");

	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("When your health drops below " + StringUtils.multiplierToPercentage(TRIGGER_HEALTH) + "%, all your other Windwalker abilities' cooldowns are reset, and abilities from other trees have their cooldowns reduced by ")
			.append(Component.text(StringUtils.multiplierToPercentage(COOLDOWN_REDUCTION[rarity - 1]) + "%", color))
			.append(Component.text(". You gain "))
			.append(Component.text(StringUtils.multiplierToPercentage(SPEED[rarity - 1]) + "%", color))
			.append(Component.text(" speed for " + SPEED_DURATION / 20 + " seconds, Resistance V for "))
			.append(Component.text(RESISTANCE_TICKS[rarity - 1] / 20, color))
			.append(Component.text(" seconds, and mobs within " + RADIUS + " blocks are knocked away. Cooldown: " + COOLDOWN / 20 + "s."));
	}


}
