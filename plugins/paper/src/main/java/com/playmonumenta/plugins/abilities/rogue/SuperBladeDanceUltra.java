package com.playmonumenta.plugins.abilities.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public final class SuperBladeDanceUltra extends Ability {
	private static final int COOLDOWN = 80 * 20;
	private static final int DURATION = 10 * 20;

	public static final AbilityInfo<SuperBladeDanceUltra> INFO =
		new AbilityInfo<>(SuperBladeDanceUltra.class, "Super Blade Dance Ultra", SuperBladeDanceUltra::new)
			.linkedSpell(ClassAbility.SUPER_BLADE_DANCE_ULTRA)
			.scoreboardId("SuperBladeDanceUltra")
			.shorthandName("\uD83D\uDDE1\uD83D\uDCAC") // Rogue speech bubble
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Spin eternally.")
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", SuperBladeDanceUltra::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true).onGround(false),
				AbilityTriggerInfo.HOLDING_TWO_SWORDS_RESTRICTION))
			.displayItem(Material.SEA_LANTERN);

	public SuperBladeDanceUltra(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();
		world.playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 1.0f, 1.1f);
		world.playSound(loc, Sound.BLOCK_GRINDSTONE_USE, SoundCategory.PLAYERS, 0.5f, 1.2f);
		world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 0.6f, 0.1f);
		world.playSound(loc, Sound.ITEM_AXE_SCRAPE, SoundCategory.PLAYERS, 2.0f, 1.0f);
		new PartialParticle(Particle.GUST_EMITTER, loc, 5).delta(1).spawnAsPlayerActive(mPlayer);
		EntityUtils.getNearbyMobs(loc, 5).forEach(e ->
			DamageUtils.damage(mPlayer, e, DamageEvent.DamageType.MELEE_SKILL, 6.7, ClassAbility.SUPER_BLADE_DANCE_ULTRA, true));
		mPlugin.mEffectManager.addEffect(mPlayer, "SuperBladeDanceUltraSpeed", new PercentSpeed(DURATION, 0.5, "SuperBladeDanceUltraSpeed"));

		cancelOnDeath(new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mTicks += 1;
				mPlayer.setRotation(9 * mTicks, 0);
				Vector dir = mPlayer.getLocation().getDirection().multiply(3.5);
				new PartialParticle(Particle.SWEEP_ATTACK, mPlayer.getEyeLocation().add(dir).subtract(0, 0.5, 0)).spawnAsPlayerActive(mPlayer);
				world.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 2f);
				Hitbox.approximateCylinder(mPlayer.getEyeLocation(), mPlayer.getEyeLocation().add(dir), 0.75, false).getHitMobs().forEach(e ->
					DamageUtils.damage(mPlayer, e, DamageEvent.DamageType.MELEE_SKILL, 3, ClassAbility.SUPER_BLADE_DANCE_ULTRA, false));
				if (mTicks > DURATION) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1));

		putOnCooldown();
		return true;
	}

	private static Description<SuperBladeDanceUltra> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addDashedLine();
	}

	private static Description<SuperBladeDanceUltra> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addDashedLine();
	}

	private static Description<SuperBladeDanceUltra> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addTrigger()
			.addDashedLine()
			.addLine("Unleash a devastating attack, dealing such a large")
			.addLine("amount of damage I couldn't possibly write it here.")
			.addLine("It's so powerful. Like way above the integer limit.")
			.addLine("Then, you gain Super Speed and start Spinning Eternally")
			.addLine("while slashing and killing everything and also while")
			.addLine("also being invincible to enemy attacks and boss attacks.")
			.addDashedLine();
	}
}
