package com.playmonumenta.plugins.abilities.snowperks;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.guis.SnowPerkGui;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;

public class SnowyOwlFeather extends Ability {
	private static final String SCOREBOARD = "SnowyOwlFeather";
	private static final int POINT_COST = 4;
	private static final int COOLDOWN = 2 * 20;
	private static final double JUMP_STRENGTH = 1f;

	public static final AbilityInfo<SnowyOwlFeather> INFO =
		new SnowPerkGui.SnowPerkInfo<>(SnowyOwlFeather.class, "Snowy Owl Feather", SnowyOwlFeather::new)
			.snowPointCost(POINT_COST)
			.scoreboardId(SCOREBOARD)
			.linkedSpell(ClassAbility.SNOWY_OWL_FEATHER)
			.displayItem(Material.FEATHER)
			.description(getDescription())
			.shorthandName("\uD83D\uDC26")
			.actionBarColor(TextColor.color(0x74D2D2))
			.cooldown(COOLDOWN);

	private final BukkitRunnable mDoubleJumpRunnable;

	public SnowyOwlFeather(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		mDoubleJumpRunnable = new BukkitRunnable() {
			boolean mOnGround = true;

			@Override
			public void run() {
				if (mPlayer.getGameMode() == GameMode.CREATIVE || mPlayer.getGameMode() == GameMode.SPECTATOR) {
					mPlayer.setAllowFlight(true);
					mPlayer.setFlySpeed(0.1f);
				} else {
					mPlayer.setFlySpeed(0);
				}
				if (!mOnGround) {
					new PartialParticle(Particle.SNOWFLAKE, mPlayer.getLocation(), 1)
						.directionalMode(true).delta(0.2).deltaVariance(true).spawnAsPlayerPassive(mPlayer);
				}
				if (PlayerUtils.isOnGround(mPlayer) && !mOnGround) {
					Location particleLoc = mPlayer.getLocation();
					particleLoc.setDirection(particleLoc.getDirection().setY(0));
					ParticleUtils.drawParticleCircleExplosion(mPlayer, particleLoc, 0, 0.3, 0, 0, 12, 0.16f, true, 0, 0.25, Particle.SNOWFLAKE);
					mOnGround = true;
				}
				if (!isOnCooldown() && mOnGround && !mPlayer.getAllowFlight()) {
					new PPCircle(Particle.CLOUD, LocationUtils.getEntityCenter(mPlayer), 0.5).count(6).spawnAsPlayerPassive(mPlayer);
					mPlayer.setAllowFlight(true);
				}
				if (mPlayer.isFlying() && !AbilityUtils.isSilenced(mPlayer) && !(mPlayer.getGameMode() == GameMode.CREATIVE || mPlayer.getGameMode() == GameMode.SPECTATOR)) {
					putOnCooldown();

					mPlayer.setAllowFlight(false);
					mPlayer.setFlying(false);

					Vector dir = NmsUtils.getVersionAdapter().getActualDirection(mPlayer);
					Vector velocity = dir.clone().setY(Math.max(0.35, Math.min(1.1, dir.getY() + 0.55))).multiply(JUMP_STRENGTH);
					mPlayer.setVelocity(velocity);
					mPlayer.setFallDistance(0);
					mOnGround = false;

					Location particleLoc = mPlayer.getLocation().setDirection(velocity);
					ParticleUtils.drawParticleCircleExplosion(mPlayer, particleLoc, 0, 0.3, 0, -90, 30, 0.3f, true, 0, 0, Particle.SNOWFLAKE);
					particleLoc.getWorld().playSound(particleLoc, Sound.ENTITY_ZOMBIE_INFECT, SoundCategory.PLAYERS, 1f, 1.2f);
				}
			}

			@Override
			public synchronized void cancel() {
				super.cancel();
				if (mPlayer.getGameMode() == GameMode.CREATIVE || mPlayer.getGameMode() == GameMode.SPECTATOR) {
					mPlayer.setAllowFlight(true);
				} else {
					mPlayer.setAllowFlight(false);
					mPlayer.setFlying(false);
				}
				mPlayer.setFlySpeed(0.1f);
			}
		};
		cancelOnDeath(mDoubleJumpRunnable.runTaskTimer(plugin, 0, 1));
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (event.getType() == DamageEvent.DamageType.FALL) {
			event.setCancelled(true);
		}
	}

	@Override
	public void invalidate() {
		if (mDoubleJumpRunnable != null) {
			mDoubleJumpRunnable.cancel();
		}
	}

	@Override
	public void playerQuitEvent(PlayerQuitEvent event) {
		event.getPlayer().setFlySpeed(0.1f);
	}

	@Override
	public void showOffCooldownMessage() {

	}

	public static Description<SnowyOwlFeather> getDescription() {
		return new FormattedDescriptionBuilder<>(() -> INFO).arrowColor(SnowPerkGui.SNOW_ARROW_COLOR)
			.addDashedLine()
			.addLine("You can double jump to leap forward")
			.addLine("with a %t cooldown.").statValues(stat(COOLDOWN))
			.addLine("(Can only leap once while midair)")
			.addLine()
			.addLine("You no longer take fall damage.")
			.addLine()
			.addStat("Cost: %d Snow Points").statValues(stat(POINT_COST))
			.addDashedLine();
	}
}
