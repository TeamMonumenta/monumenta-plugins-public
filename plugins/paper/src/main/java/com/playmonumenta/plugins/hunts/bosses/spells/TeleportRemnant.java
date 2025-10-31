package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.TheImpenetrable;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class TeleportRemnant extends Spell {
	private static final int ATTACK_DAMAGE = 150;
	private static final double ATTACK_RANGE = 10;

	private static final double REMNANT_HEALTH = 750;

	private static final int LIFESPAN = 200;

	private static final String REMNANT_RESISTANCE_TAG = "ImpenetrableRemnantResistance";

	private boolean mIsExisting = false;

	private final Plugin mPlugin;
	private final com.playmonumenta.plugins.Plugin mMonumentaPlugin;
	private final World mWorld;
	private final LivingEntity mBoss;

	private final ChargeUpManager mChargeUp;

	public TeleportRemnant(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mMonumentaPlugin = com.playmonumenta.plugins.Plugin.getInstance();
		mBoss = boss;
		mWorld = boss.getWorld();

		mChargeUp = new ChargeUpManager(mBoss, LIFESPAN, Component.text("Decaying Remnant", NamedTextColor.RED), BossBar.Color.RED, BossBar.Overlay.PROGRESS, TheImpenetrable.OUTER_RADIUS);
	}

	@Override
	public void run() {
		if (mIsExisting) {
			return;
		}

		MagmaCube remnant = mWorld.spawn(mBoss.getLocation(), MagmaCube.class);
		remnant.setAI(false);
		remnant.setInvisible(true);
		remnant.setGravity(false);
		remnant.setGlowing(true);
		remnant.setSize(2);
		remnant.setLootTable(null);
		remnant.addScoreboardTag("TheImpenetrable");
		remnant.addScoreboardTag("boss_nosplit");
		remnant.addScoreboardTag("Boss");
		ScoreboardUtils.addEntityToTeam(remnant, "dark_purple");

		EntityUtils.setMaxHealthAndHealth(remnant, REMNANT_HEALTH);
		Collection<Player> nearbyPlayers = PlayerUtils.playersInRange(mBoss.getLocation(), TheImpenetrable.OUTER_RADIUS, true);
		int players = nearbyPlayers.size();
		double resistanceStrength = 1 / BossUtils.healthScalingCoef(players, 0.5, 0.45);
		mMonumentaPlugin.mEffectManager.addEffect(remnant, REMNANT_RESISTANCE_TAG, new PercentDamageReceived(Integer.MAX_VALUE, 1 - resistanceStrength));

		mIsExisting = true;

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;
			int mTicksToTelegraph = 0;

			@Override
			public void run() {
				if (mTicks % 10 == 0) {
					telegraphParticles(mBoss.getLocation());
					telegraphParticles(remnant.getLocation());
				}

				if (mTicksToTelegraph <= 0 && mTicks < LIFESPAN - 20) {
					mTicksToTelegraph = (int) (20 - 12 * ((double) mTicks / LIFESPAN));

					movingOrb(remnant.getLocation().clone().add(0, 0.5, 0));

					mWorld.playSound(remnant.getLocation(), Sound.ENTITY_WARDEN_TENDRIL_CLICKS, SoundCategory.HOSTILE, 2, (float) (0.5 + 1.5 * ((double) mTicks / LIFESPAN)));
				}

				mTicksToTelegraph--;

				// Warning
				if (mTicks == LIFESPAN * 3 / 4) {
					ScoreboardUtils.addEntityToTeam(remnant, "dark_red");

					mWorld.playSound(remnant.getLocation(), Sound.BLOCK_BELL_RESONATE, SoundCategory.HOSTILE, 2, 1.3f);
					mWorld.playSound(remnant.getLocation(), Sound.BLOCK_BELL_USE, SoundCategory.HOSTILE, 2, 0.6f);
				}

				// Explosion
				if (mTicks == LIFESPAN) {
					mWorld.playSound(remnant.getLocation(), Sound.ITEM_TOTEM_USE, SoundCategory.HOSTILE, 2, 1.4f);
					mWorld.playSound(remnant.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 2, 0.7f);

					implosionParticles(mBoss.getLocation());
					implosionParticles(remnant.getLocation());

					Hitbox remnantHitbox = new Hitbox.SphereHitbox(remnant.getLocation(), ATTACK_RANGE);
					Hitbox bossHitbox = new Hitbox.SphereHitbox(mBoss.getLocation(), ATTACK_RANGE);
					List<Player> hitPlayers = new ArrayList<>();
					hitPlayers.addAll(remnantHitbox.getHitPlayers(true));
					hitPlayers.addAll(bossHitbox.getHitPlayers(true));

					for (Player player : hitPlayers) {
						DamageUtils.damage(mBoss, player, DamageEvent.DamageType.BLAST, ATTACK_DAMAGE, null, true, true, "Remnant Implosion");
					}

					this.cancel();
					return;
				}

				double remnantPercentHealth = remnant.getHealth() / EntityUtils.getMaxHealth(remnant);

				mChargeUp.update();
				mChargeUp.setProgress(1 - ((float) mTicks / LIFESPAN));
				mChargeUp.setTitle(Component.text(String.format("Decaying Remnant - %s%%", (int) (remnantPercentHealth * 100)), NamedTextColor.RED));

				mTicks++;
				if (remnant.isDead() && mTicks < LIFESPAN - 1) {
					mWorld.playSound(remnant.getLocation(), Sound.ENTITY_WARDEN_DIG, SoundCategory.HOSTILE, 4f, 1f);

					this.cancel();
				}
				if (mBoss.isDead()) {
					this.cancel();
				}
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				remnant.remove();
				mIsExisting = false;
				super.cancel();
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	private void telegraphParticles(Location location) {
		PPCircle circle = new PPCircle(Particle.SPELL_WITCH, location, ATTACK_RANGE)
			.countPerMeter(1)
			.delta(0.02)
			.ringMode(true);
		circle.axes(new Vector(1, 0, 0), new Vector(0, 1, 0)).spawnAsBoss();
		circle.axes(new Vector(1, 0, 0), new Vector(0, 0, 1)).spawnAsBoss();
		circle.axes(new Vector(0, 1, 0), new Vector(0, 0, 1)).spawnAsBoss();
	}

	private void implosionParticles(Location location) {
		new PartialParticle(Particle.EXPLOSION_HUGE, location)
			.count(10)
			.delta(2)
			.spawnAsBoss();

		new PPParametric(Particle.ELECTRIC_SPARK, location, (parameter, builder) -> {
			Vector vector = VectorUtils.randomUnitVector();
			builder.location(location.clone().add(vector.clone().multiply(ATTACK_RANGE)));
			builder.offset(-vector.getX(), -vector.getY(), -vector.getZ());
		})
			.directionalMode(true)
			.count(1000)
			.extra(1)
			.spawnAsBoss();
	}

	private void movingOrb(Location location) {
		Vector dir = VectorUtils.randomUnitVector();
		dir.setY(-Math.abs(dir.getY()));
		Location currentLocation = location.clone().subtract(dir.clone().multiply(3));

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				new PartialParticle(Particle.REDSTONE, currentLocation)
					.count(5)
					.data(new Particle.DustOptions(Color.fromRGB(105, 20, 135), 0.75f))
					.delta(0.05)
					.spawnAsBoss();
				new PartialParticle(Particle.SPELL_WITCH, currentLocation)
					.count(1)
					.delta(0.15)
					.spawnAsBoss();

				currentLocation.add(dir.clone().multiply(0.065));

				mTicks++;
				if (mTicks > 40 || currentLocation.distance(location) < 0.3) {
					this.cancel();
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	public boolean hasExistingRemnant() {
		return mIsExisting;
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
