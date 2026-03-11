package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.hunts.bosses.SporousAmalgam;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.util.function.Predicate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SporeInfection extends Spell {
	private static final double CURE_ZONE_RADIUS = 4.2;
	private static final int TICKS_TO_CURE = 20;
	private static final String CURE_MOB_SOUL_NAME = "CleansingBulbholder";
	private static final TextColor CURE_COLOR = NamedTextColor.AQUA;
	private static final String INFECTED_SYMBOL = "✽";

	private static final int FAST_CURE_TIME = (int) (20 * 3.5) + TICKS_TO_CURE;
	private static final float FAST_CURE_REDUCTION = -2.0f;
	private static final float SPORE_AMOUNT_PER_TIME = 0.05f;

	private static final int COOLDOWN = FAST_CURE_TIME + 20 * 4;

	private boolean mHasInfected;
	private final Plugin mPlugin;
	private final SporousAmalgam mSporeBeast;
	private final LivingEntity mBoss;

	public SporeInfection(Plugin plugin, SporousAmalgam sporeBeast) {
		mHasInfected = false;
		mPlugin = plugin;
		mSporeBeast = sporeBeast;
		mBoss = sporeBeast.mBoss;
	}

	@Override
	public void run() {
		Player targetPlayer = mSporeBeast.getPlayerForInfection();
		LivingEntity cureEntity = (LivingEntity) LibraryOfSoulsIntegration.summon(getCureLocation(targetPlayer), CURE_MOB_SOUL_NAME);
		sendInfectionNotification(targetPlayer);
		Predicate<Player> showGlowing = p -> p == targetPlayer;
		if (cureEntity == null) {
			return;
		}
		GlowingManager.ActiveGlowingEffect mobGlowing = GlowingManager.startGlowing(cureEntity, NamedTextColor.YELLOW, 20 * 120, GlowingManager.BOSS_SPELL_PRIORITY, showGlowing, "BulbCarrierGlow");
		GlowingManager.ActiveGlowingEffect bulbGlowing = GlowingManager.startGlowing(cureEntity.getPassengers().getFirst(), NamedTextColor.AQUA, 20 * 120, GlowingManager.BOSS_SPELL_PRIORITY, showGlowing, "BulbCarrierGlow");

		mHasInfected = true;
		new BukkitRunnable() {
			float mSpores = SPORE_AMOUNT_PER_TIME;
			int mTicks = 0;
			int mTicksInCureZone = 0;

			@Override
			public void run() {
				Location cureLocation = cureEntity.getLocation();
				updateAndSpawnParticles(cureLocation, targetPlayer, mTicks);
				double playerDistance = cureLocation.distance(targetPlayer.getLocation());

				if (cureLocation.distanceSquared(mBoss.getLocation()) >= SporousAmalgam.SPELL_INNER_RADIUS * SporousAmalgam.SPELL_INNER_RADIUS) {
					cureEntity.setVelocity(LocationUtils.getDirectionTo(mBoss.getLocation(), cureLocation));
				}

				if (mTicks % 5 == 0) {
					targetPlayer.playSound(targetPlayer, Sound.ENTITY_WITCH_DRINK, 0.7f, .85f);
				}
				if (mTicks % 10 == 0) {
					if (playerDistance > CURE_ZONE_RADIUS) {
						mSporeBeast.addSpores(targetPlayer, mSpores);
						mSpores = Math.min(SPORE_AMOUNT_PER_TIME * 5, mSpores + SPORE_AMOUNT_PER_TIME);
					}
				}
				if (playerDistance < CURE_ZONE_RADIUS) {
					mSpores = SPORE_AMOUNT_PER_TIME;
					mTicksInCureZone++;
				} else {
					mTicksInCureZone = Math.max(0, mTicksInCureZone - 1);
				}
				if (mBoss.isDead() || targetPlayer.isDead() || mTicksInCureZone >= TICKS_TO_CURE || targetPlayer.getLocation().distance(mBoss.getLocation()) >= SporousAmalgam.OUTER_RADIUS || cureEntity.isDead()) {
					if (mobGlowing != null) {
						mobGlowing.clear();
					}
					if (bulbGlowing != null) {
						bulbGlowing.clear();
					}
					cureEntity.remove();
					mHasInfected = false;
					if (mTicks <= FAST_CURE_TIME) {
						mSporeBeast.addSpores(targetPlayer, FAST_CURE_REDUCTION);
					}
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void sendInfectionNotification(Player targetPlayer) {
		MessagingUtils.sendTitle(targetPlayer, Component.text(""), Component.text(INFECTED_SYMBOL, CURE_COLOR));
		targetPlayer.sendMessage(Component.text("Spores begin to infect you, the blue mushroom might be able to wash them off...", CURE_COLOR));
	}

	private void updateAndSpawnParticles(Location cureLocation, Player targetPlayer, int mTicks) {
		double particlePosition = 0.05 * mTicks;
		while (particlePosition > 1) {
			particlePosition -= 1;
		}

		double multiplier = targetPlayer.getLocation().distance(cureLocation) * particlePosition;
		Vector pLineLoc = LocationUtils.getDirectionTo(cureLocation, targetPlayer.getLocation()).multiply(multiplier);
		new PartialParticle(Particle.GLOW, targetPlayer.getLocation().add(pLineLoc).add(0, 0.2, 0)).count(4).spawnForPlayer(ParticleCategory.BOSS, targetPlayer);
	}

	private Location getCureLocation(Player targetPlayer) {
		Location location;
		do {
			location = mSporeBeast.getRandomLocationInArena(CURE_ZONE_RADIUS * 2, CURE_ZONE_RADIUS + 1, 0);
		} while (location.distance(targetPlayer.getLocation()) <= 10);
		return location;
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN;
	}

	@Override
	public boolean canRun() {
		if (!mSporeBeast.canRunUproot() && !mHasInfected) {
			return mSporeBeast.canRunSpell(this);
		}
		return false;
	}
}
