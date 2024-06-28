package com.playmonumenta.plugins.bosses.spells.salieriswordsage;

import com.playmonumenta.plugins.bosses.bosses.SalieriTheSwordsage;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseCharge;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class CounterHit extends Spell {
	private final Plugin mPlugin;
	private final SalieriTheSwordsage mBossClass;
	private final LivingEntity mBoss;
	private final SpellBaseCharge mDashSweep;
	private static final int RANGE = 7;
	private static final int ATTACK_DELAY = 12;
	private static final int ATTACK_EXTRA_WINDOW = 5;
	private static final int DASH_DAMAGE = 32;
	private final int mDamage;
	private int mTicks = 0;
	private static final Particle.DustOptions SWORD_COLOR = new Particle.DustOptions(Color.fromRGB(200, 200, 200), 1.0f);


	public CounterHit(Plugin plugin, LivingEntity boss, int damage, SalieriTheSwordsage bossClass) {
		mPlugin = plugin;
		mBoss = boss;
		mDamage = damage;
		mBossClass = bossClass;

		mDashSweep = new SpellBaseCharge(plugin, boss, 40, 0, 15, false,
			// Warning sound/particles at boss location and slow boss
			(LivingEntity player) -> {
				new PartialParticle(Particle.VILLAGER_ANGRY, boss.getLocation(), 50).delta(2, 2, 2).spawnAsBoss();
				boss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 4));
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1.75f);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.15f);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_VINDICATOR_AMBIENT, 1f, 0.85f);
			},
			// Warning particles
			(Location loc) -> {
				new PartialParticle(Particle.CRIT, loc, 1).delta(0.65, 0.65, 0.65).spawnAsBoss();
			},
			// Charge attack sound/particles at boss location
			(LivingEntity player) -> {
				new PartialParticle(Particle.SMOKE_NORMAL, boss.getLocation(), 100).delta(0.4, 0.4, 0.4).extra(0.25).spawnAsBoss();
				new PartialParticle(Particle.CLOUD, boss.getLocation(), 45).delta(0.15, 0.4, 0.15).extra(0.15).spawnAsBoss();
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.75f);
				boss.getWorld().playSound(boss.getLocation(), Sound.ITEM_TRIDENT_THROW, 1f, 0.9f);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1.4f);
			},
			// Attack hit a player
			(LivingEntity player) -> {
				new PartialParticle(Particle.BLOCK_CRACK, player.getLocation().add(0, 1, 0), 5).delta(0.4, 0.4, 0.4)
					.extra(0.4).data(Bukkit.createBlockData(Material.REDSTONE_BLOCK)).spawnAsBoss();
				new PartialParticle(Particle.BLOCK_CRACK, player.getLocation().add(0, 1, 0), 12).delta(0.4, 0.4, 0.4)
					.extra(0.4).data(Bukkit.createBlockData(Material.REDSTONE_WIRE)).spawnAsBoss();
				BossUtils.blockableDamage(boss, player, DamageEvent.DamageType.MELEE_SKILL, damage);
			},
			// Attack particles
			(Location loc) -> {
				new PartialParticle(Particle.SWEEP_ATTACK, loc, 1).delta(0.5, 0.5, 0.5).extra(0.2).spawnAsBoss();
				new PartialParticle(Particle.CRIT, loc, 4).delta(0.5, 0.5, 0.5).extra(0.75).spawnAsBoss();
				new PartialParticle(Particle.SMOKE_NORMAL, loc, 3).delta(0.5, 0.5, 0.5).extra(0.2).spawnAsBoss();
			},
			// Ending particles on boss
			() -> {
				new PartialParticle(Particle.SMOKE_NORMAL, boss.getLocation(), 100).delta(0.4, 0.4, 0.4).extra(0.25).spawnAsBoss();
				new PartialParticle(Particle.CLOUD, boss.getLocation(), 45).delta(0.15, 0.4, 0.15).extra(0.15).spawnAsBoss();
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.1f, 1.5f);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.75f);
				boss.getWorld().playSound(boss.getLocation(), Sound.ITEM_TRIDENT_THROW, 1f, 0.9f);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1.4f);

				World world = boss.getWorld();

				Vector vec;
				List<BoundingBox> boxes = new ArrayList<>();
				Location loc = mBoss.getLocation();

				world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 3, 0.5f);

				//Rather inefficient, but was not able to optimize without breaking it
				//Final particle show
				for (double r = 0; r < 5; r++) {
					for (double degree = 0; degree < 360; degree += 10) {
						double radian1 = Math.toRadians(degree);
						vec = new Vector(FastUtils.cos(radian1) * r, 0, FastUtils.sin(radian1) * r);
						vec = VectorUtils.rotateYAxis(vec, loc.getYaw());
						Location l = loc.clone().add(vec);
						//1.5 -> 15
						BoundingBox box = BoundingBox.of(l, 0.65, 3, 0.65);
						boxes.add(box);

						new PartialParticle(Particle.SWEEP_ATTACK, l, 1).delta(0.1, 0.1, 0.1).extra(0.1).spawnAsBoss();
					}
				}

				for (Player player : PlayerUtils.playersInRange(loc, 40, true)) {
					for (BoundingBox box : boxes) {
						if (player.getBoundingBox().overlaps(box)) {
							BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.MELEE_SKILL, DASH_DAMAGE);
							player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 8, 1));
						}
					}
				}
			});
	}

	@Override
	public void run() {
		mTicks += 5;
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	@Override
	public void onHurtByEntity(DamageEvent event, Entity damager) {
		// If the damage type is possibly a Damage Over Time effect, its damage must be higher than a certain value in order to trigger the counter
		final EnumSet<DamageEvent.DamageType> POSSIBLE_DOT = EnumSet.of(
			DamageEvent.DamageType.MAGIC,
			DamageEvent.DamageType.OTHER,
			DamageEvent.DamageType.FIRE
		);
		// Return if the damage instance isn't a player, if it hasn't been long enough since the last counter, if a spell is currently active, if the boss would be dead after the hit, or if it's a low-damage DOT
		double healthRemaining = mBoss.getHealth() - event.getFinalDamage(true);
		if (!(event.getDamager() instanceof Player player) || mTicks <= ATTACK_DELAY + ATTACK_EXTRA_WINDOW || mBossClass.mSpellActive || healthRemaining <= 0 || mBoss.isDead() || !mBoss.isValid()
			|| event.getType() == DamageEvent.DamageType.AILMENT || event.getType() == DamageEvent.DamageType.POISON || (POSSIBLE_DOT.contains(event.getType()) && event.getFinalDamage(true) <= 15)) {
			return;
		}

		//If player is close enough to melee, use sweep attack
		if (player.getLocation().distance(mBoss.getLocation()) <= 4) {
			playMeleeAttack(player);
		} else {
			//If too far, leap towards player to close distance and sweep circularly
			mDashSweep.run();
		}

		mTicks = 0;
	}

	private void playMeleeAttack(Player player) {
		World world = mBoss.getWorld();
		Location loc = mBoss.getLocation();

		world.playSound(mBoss.getLocation(), Sound.ENTITY_VINDICATOR_HURT, 1f, 0.5f);
		int random = FastUtils.RANDOM.nextInt(3);

		EntityUtils.selfRoot(mBoss, ATTACK_DELAY + ATTACK_EXTRA_WINDOW);

		if (random == 0) {
			//Attacks upwards in a wide half-circle sweep
			sweepHigh(world, loc);
		} else if (random == 1) {
			//Attacks vertically in front of the boss
			verticalSlash(world, player);
		} else {
			//Attacks downward in a half-circle arc
			sweepLow(world, loc);
		}

	}

	private void sweepHigh(World world, Location loc) {
		Vector direction = mBoss.getLocation().getDirection().setY(0).normalize();
		Vector sideways = new Vector(direction.getZ() / 2, 0, -direction.getX() / 2);
		Location locParticle = mBoss.getLocation().add(0, 2, 0).subtract(sideways.clone().multiply(10));
		for (int i = 0; i <= 28; i++) {
			new PartialParticle(Particle.REDSTONE, locParticle, 10).delta(0.2, 0.2, 0.2).data(SWORD_COLOR).spawnAsBoss();
			locParticle.add(sideways);
		}

		BukkitRunnable warning = new BukkitRunnable() {
			int mTime = 0;

			@Override
			public void run() {
				Vector forwards = mBoss.getLocation().getDirection().setY(0).normalize();
				Vector sideways = new Vector(forwards.getZ(), 0, -forwards.getX());

				Vector shift1 = new Vector(0, 0, 0).add(forwards.clone().multiply(Math.sin(Math.PI * mTime / 8))).add(sideways.clone().multiply(Math.cos(Math.PI * mTime / 8)));
				Location loc1 = mBoss.getLocation().add(0, 2.0, 0);
				Vector shift2 = new Vector(0, 0, 0).add(forwards.clone().multiply(Math.sin(Math.PI * mTime / 8))).add(sideways.clone().multiply(-Math.cos(Math.PI * mTime / 8)));
				Location loc2 = mBoss.getLocation().add(0, 2.0, 0);
				BoundingBox hitbox1 = new BoundingBox().shift(loc1).expand(1, 0.2, 1);
				BoundingBox hitbox2 = new BoundingBox().shift(loc2).expand(1, 0.2, 1);

				for (int i = 0; i < RANGE; i++) {
					loc1.add(shift1);
					hitbox1.shift(shift1);
					loc2.add(shift2);
					hitbox2.shift(shift2);
					new PartialParticle(Particle.REDSTONE, loc1, 3).delta(0.2, 0.2, 0.2).data(SWORD_COLOR).spawnAsBoss();
					new PartialParticle(Particle.REDSTONE, loc2, 3).delta(0.2, 0.2, 0.2).data(SWORD_COLOR).spawnAsBoss();

				}

				mTime++;
				if (mTime >= 5) {
					this.cancel();
				}
			}
		};

		warning.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(warning);

		BukkitRunnable attack = new BukkitRunnable() {
			final List<Player> mPlayers = PlayerUtils.playersInRange(mBoss.getLocation(), RANGE * 4, true);
			int mTime = 0;

			@Override
			public void run() {
				Vector forwards = mBoss.getLocation().getDirection().setY(0).normalize();
				Vector sideways = new Vector(forwards.getZ(), 0, -forwards.getX());

				Vector shift1 = new Vector(0, 0, 0).add(forwards.clone().multiply(Math.sin(Math.PI * mTime / 8))).add(sideways.clone().multiply(Math.cos(Math.PI * mTime / 8)));
				Location loc1 = mBoss.getLocation().add(0, 1.9, 0);
				Vector shift2 = new Vector(0, 0, 0).add(forwards.clone().multiply(Math.sin(Math.PI * mTime / 8))).add(sideways.clone().multiply(-Math.cos(Math.PI * mTime / 8)));
				Location loc2 = mBoss.getLocation().add(0, 1.9, 0);
				BoundingBox hitbox1 = new BoundingBox().shift(loc1).expand(1, 0.2, 1);
				BoundingBox hitbox2 = new BoundingBox().shift(loc2).expand(1, 0.2, 1);
				world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.3f, 1f);

				for (int i = 0; i < RANGE; i++) {
					loc1.add(shift1);
					hitbox1.shift(shift1);
					loc2.add(shift2);
					hitbox2.shift(shift2);
					new PartialParticle(Particle.SWEEP_ATTACK, loc1, 1).spawnAsBoss();
					new PartialParticle(Particle.SWEEP_ATTACK, loc2, 1).spawnAsBoss();

					Iterator<Player> iter = mPlayers.iterator();
					while (iter.hasNext()) {
						Player player = iter.next();
						BoundingBox box = player.getBoundingBox();
						//Player can crouch to avoid upwards slash
						if ((box.overlaps(hitbox1) || box.overlaps(hitbox2)) && !player.isSneaking()) {
							BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.MELEE_SKILL, mDamage);
							iter.remove();
						}
					}
				}

				mTime++;
				if (mTime >= 5) {
					this.cancel();
				}
			}
		};

		attack.runTaskTimer(mPlugin, ATTACK_DELAY, 1);
		mActiveRunnables.add(attack);
	}

	public void verticalSlash(World world, Player player) {
		Vector direction = player.getLocation().subtract(mBoss.getLocation()).toVector().setY(0).normalize();
		Location locParticle = mBoss.getEyeLocation();
		for (int i = 0; i < 14; i++) {
			new PartialParticle(Particle.REDSTONE, locParticle, 5).delta(0.2, 0.2, 0.2).data(SWORD_COLOR).spawnAsBoss();
			locParticle.add(0, 0.5, 0);
		}

		BukkitRunnable warning = new BukkitRunnable() {
			int mTime = 0;
			final Vector mDirection = direction;

			@Override
			public void run() {
				Vector upwards = new Vector(0, 1, 0);
				Vector shift = new Vector(0, 0, 0).add(mDirection.clone().multiply(Math.sin(Math.PI * mTime / 8))).add(upwards.clone().multiply(Math.cos(Math.PI * mTime / 8)));
				Location loc = mBoss.getLocation();
				BoundingBox hitbox = new BoundingBox().shift(loc).expand(0.8);
				for (int i = 0; i < RANGE; i++) {
					loc.add(shift);
					hitbox.shift(shift);
					new PartialParticle(Particle.REDSTONE, loc, 3).delta(0.2, 0.2, 0.2).data(SWORD_COLOR).spawnAsBoss();
				}

				mTime++;
				if (mTime >= 5) {
					this.cancel();
				}
			}
		};

		warning.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(warning);

		BukkitRunnable attack = new BukkitRunnable() {
			final List<Player> mPlayers = PlayerUtils.playersInRange(mBoss.getLocation(), RANGE * 4, true);
			int mTime = 0;
			final Vector mDirection = direction;

			@Override
			public void run() {
				Vector upwards = new Vector(0, 1, 0);
				Vector shift = new Vector(0, 0, 0).add(mDirection.clone().multiply(Math.sin(Math.PI * mTime / 8))).add(upwards.clone().multiply(Math.cos(Math.PI * mTime / 8)));
				Location loc = mBoss.getLocation();
				BoundingBox hitbox = new BoundingBox().shift(loc).expand(0.8);
				world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.3f, 1f);
				for (int i = 0; i < RANGE; i++) {
					loc.add(shift);
					hitbox.shift(shift);
					new PartialParticle(Particle.SWEEP_ATTACK, loc, 1).spawnAsBoss();

					Iterator<Player> iter = mPlayers.iterator();
					while (iter.hasNext()) {
						Player player = iter.next();
						if (player.getBoundingBox().overlaps(hitbox)) {
							BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.MELEE_SKILL, mDamage);
							iter.remove();
						}
					}
				}

				mTime++;
				if (mTime >= 5) {
					this.cancel();
				}
			}
		};

		attack.runTaskTimer(mPlugin, ATTACK_DELAY, 1);
		mActiveRunnables.add(attack);
	}

	public void sweepLow(World world, Location loc) {
		Vector direction = mBoss.getLocation().getDirection().setY(0).normalize();
		Vector sideways = new Vector(direction.getZ() / 2, 0, -direction.getX() / 2);
		Location locParticle = mBoss.getLocation().add(0, 0.1, 0).subtract(sideways.clone().multiply(10));
		for (int i = 0; i <= 28; i++) {
			new PartialParticle(Particle.REDSTONE, locParticle, 10).delta(0.2, 0.2, 0.2).data(SWORD_COLOR).spawnAsBoss();
			locParticle.add(sideways);
		}

		BukkitRunnable warning = new BukkitRunnable() {
			int mTime = 0;

			@Override
			public void run() {
				Vector forwards = mBoss.getLocation().getDirection().setY(0).normalize();
				Vector sideways = new Vector(forwards.getZ(), 0, -forwards.getX());

				Vector shift1 = new Vector(0, 0, 0).add(forwards.clone().multiply(Math.sin(Math.PI * mTime / 8))).add(sideways.clone().multiply(Math.cos(Math.PI * mTime / 8)));
				Location loc1 = mBoss.getLocation();
				Vector shift2 = new Vector(0, 0, 0).add(forwards.clone().multiply(Math.sin(Math.PI * mTime / 8))).add(sideways.clone().multiply(-Math.cos(Math.PI * mTime / 8)));
				Location loc2 = mBoss.getLocation();
				BoundingBox hitbox1 = new BoundingBox().shift(loc1).expand(1, 0.2, 1);
				BoundingBox hitbox2 = new BoundingBox().shift(loc2).expand(1, 0.2, 1);

				for (int i = 0; i < RANGE; i++) {
					loc1.add(shift1);
					hitbox1.shift(shift1);
					loc2.add(shift2);
					hitbox2.shift(shift2);
					new PartialParticle(Particle.REDSTONE, loc1, 3).delta(0.2, 0.2, 0.2).data(SWORD_COLOR).spawnAsBoss();
					new PartialParticle(Particle.REDSTONE, loc2, 3).delta(0.2, 0.2, 0.2).data(SWORD_COLOR).spawnAsBoss();
				}

				mTime++;
				if (mTime >= 5) {
					this.cancel();
				}
			}
		};

		warning.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(warning);

		BukkitRunnable attack = new BukkitRunnable() {
			final List<Player> mPlayers = PlayerUtils.playersInRange(mBoss.getLocation(), RANGE * 4, true);
			int mTime = 0;

			@Override
			public void run() {
				Vector forwards = mBoss.getLocation().getDirection().setY(0).normalize();
				Vector sideways = new Vector(forwards.getZ(), 0, -forwards.getX());

				Vector shift1 = new Vector(0, 0, 0).add(forwards.clone().multiply(Math.sin(Math.PI * mTime / 8))).add(sideways.clone().multiply(Math.cos(Math.PI * mTime / 8)));
				Location loc1 = mBoss.getLocation();
				Vector shift2 = new Vector(0, 0, 0).add(forwards.clone().multiply(Math.sin(Math.PI * mTime / 8))).add(sideways.clone().multiply(-Math.cos(Math.PI * mTime / 8)));
				Location loc2 = mBoss.getLocation();
				BoundingBox hitbox1 = new BoundingBox().shift(loc1).expand(1, 0.2, 1);
				BoundingBox hitbox2 = new BoundingBox().shift(loc2).expand(1, 0.2, 1);
				world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.3f, 1f);

				for (int i = 0; i < RANGE; i++) {
					loc1.add(shift1);
					hitbox1.shift(shift1);
					loc2.add(shift2);
					hitbox2.shift(shift2);
					new PartialParticle(Particle.SWEEP_ATTACK, loc1, 1).spawnAsBoss();
					new PartialParticle(Particle.SWEEP_ATTACK, loc2, 1).spawnAsBoss();

					Iterator<Player> iter = mPlayers.iterator();
					while (iter.hasNext()) {
						Player player = iter.next();
						BoundingBox box = player.getBoundingBox();
						if (box.overlaps(hitbox1) || box.overlaps(hitbox2)) {
							BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.MELEE_SKILL, mDamage);
							iter.remove();
						}
					}
				}

				mTime++;
				if (mTime >= 5) {
					this.cancel();
				}
			}
		};

		attack.runTaskTimer(mPlugin, ATTACK_DELAY, 1);
		mActiveRunnables.add(attack);
	}
}
