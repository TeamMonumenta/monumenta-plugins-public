package com.playmonumenta.plugins.bosses.spells.frostgiant;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.FrostGiant;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class RingOfFrost extends Spell {
	private static final String SPELL_NAME = "Ring of Frost";
	private static final String SLOWNESS_SRC = "RingOfFrostSlowness";
	private static final int CHARGE_DURATION = Constants.TICKS_PER_SECOND * 4;
	private static final Particle.DustOptions GREEN_COLOR = new Particle.DustOptions(Color.fromRGB(0, 255, 0), 1.0f);

	private final Plugin mPlugin;
	private final FrostGiant mFrostGiant;
	private final LivingEntity mBoss;
	private final World mWorld;
	private final double mRadius;
	private final ChargeUpManager mChargeManager;
	private final PPCircle mInnerCircle;
	private final PPCircle mOuterCircle1;
	private final PPCircle mOuterCircle2;
	private final PPCircle mExplodeCircle;

	public RingOfFrost(final Plugin plugin, final FrostGiant frostGiant, final double radius, final Location loc) {
		mPlugin = plugin;
		mFrostGiant = frostGiant;
		mBoss = mFrostGiant.mBoss;
		mWorld = mBoss.getWorld();
		mRadius = radius;
		mChargeManager = FrostGiant.defaultChargeUp(mBoss, CHARGE_DURATION, "Charging " + SPELL_NAME + "...");

		mInnerCircle = new PPCircle(Particle.REDSTONE, loc, radius).count(40).delta(0.05).extra(0.05).data(GREEN_COLOR).distanceFalloff(FrostGiant.ARENA_RADIUS * 2);
		mOuterCircle1 = new PPCircle(Particle.DAMAGE_INDICATOR, loc, radius).count(20).delta(1, 0.1, 1).extra(0.05).distanceFalloff(FrostGiant.ARENA_RADIUS * 2);
		mOuterCircle2 = new PPCircle(Particle.DRAGON_BREATH, loc, radius).count(20).delta(1, 0.1, 1).extra(0.05).distanceFalloff(FrostGiant.ARENA_RADIUS * 2);
		mExplodeCircle = new PPCircle(Particle.EXPLOSION_NORMAL, loc, radius).count(150).delta(1, 3, 1).extra(0.45).distanceFalloff(FrostGiant.ARENA_RADIUS * 2);
	}

	@Override
	public void run() {
		if (mFrostGiant.getArenaParticipants().isEmpty()) {
			return;
		}

		mFrostGiant.freezeGolems();
		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 3, 1.5f);
		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_POLAR_BEAR_WARNING, SoundCategory.HOSTILE, 5, 0.5f);

		mFrostGiant.getArenaParticipants().forEach(player ->
			player.sendMessage(Component.text("The air away from the giant starts to freeze!", NamedTextColor.AQUA)));

		final int runnablePeriod = 2;
		final BukkitRunnable runnable = new BukkitRunnable() {
			float mPitch = 0.5f;

			@Override
			public void run() {
				if (!mBoss.isValid() || mBoss.isDead()) {
					cancel();
					return;
				}

				final Location loc = mBoss.getLocation().add(0, 0.1, 0);

				mInnerCircle.location(loc);
				mOuterCircle1.location(loc);
				mOuterCircle2.location(loc);

				mInnerCircle.spawnAsEntityActive(mBoss);
				for (int r = 24; r > mRadius + 1; r -= 4) {
					mOuterCircle1.radius(r).spawnAsEntityActive(mBoss);
					mOuterCircle2.radius(r).spawnAsEntityActive(mBoss);
				}

				mWorld.playSound(loc, Sound.ENTITY_SNOW_GOLEM_SHOOT, SoundCategory.HOSTILE, 3, mPitch);
				mPitch += 0.025f;

				if (mChargeManager.nextTick(runnablePeriod)) {
					mChargeManager.reset();
					mFrostGiant.unfreezeGolems();
					mExplodeCircle.location(mBoss.getLocation().add(0, 2, 0)).spawnAsEntityActive(mBoss);
					mWorld.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 3, 1.5f);
					mWorld.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 3, 0.5f);

					final List<Player> hitPlayers = (List<Player>) mFrostGiant.getArenaParticipants();
					hitPlayers.removeAll(new Hitbox.UprightCylinderHitbox(loc.clone().add(0, -5, 0), 25, mRadius).getHitPlayers(true));
					hitPlayers.forEach(player -> {
						DamageUtils.damage(mBoss, player, DamageType.MAGIC, 40, null, false, false, SPELL_NAME);
						MovementUtils.knockAway(loc, player, -2.75f, 0.5f, false);
						com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(player, SLOWNESS_SRC,
							new PercentSpeed(Constants.TICKS_PER_SECOND * 10, -0.6, SLOWNESS_SRC));
						new PartialParticle(Particle.EXPLOSION_NORMAL, player.getLocation().add(0, 1, 0), 30, 0.25, 0.45, 0.25, 0.2).spawnAsEntityActive(mBoss);
					});

					this.cancel();
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int cooldownTicks() {
		return Constants.TICKS_PER_SECOND * 6;
	}
}
