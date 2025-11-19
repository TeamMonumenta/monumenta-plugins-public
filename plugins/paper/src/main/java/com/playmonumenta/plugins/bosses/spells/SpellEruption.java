package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.bosses.bosses.EruptionBoss;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class SpellEruption extends Spell {
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final EruptionBoss.Parameters mParameters;

	public SpellEruption(Plugin plugin, LivingEntity boss, EruptionBoss.Parameters parameters) {
		mPlugin = plugin;
		mBoss = boss;
		mParameters = parameters;
	}

	@Override
	public void run() {
		for (LivingEntity target : mParameters.TARGETS.getTargetsList(mBoss)) {
			mParameters.SOUND_WARNING.play(target.getLocation());
			performEruption(target);
		}
	}

	@Override
	public int cooldownTicks() {
		return mParameters.COOLDOWN;
	}

	protected void performEruption(LivingEntity target) {
		if (target == null) {
			return;
		}

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid() || EntityUtils.isStunned(mBoss) || EntityUtils.isSilenced(mBoss)) {
					this.cancel();
					return;
				}
				mTicks++;
				chargeActions(mBoss.getLocation(), mTicks);
				if (mTicks >= mParameters.CHARGE_DURATION) {
					mParameters.SOUND_ERUPTION.play(mBoss.getLocation());
					this.cancel();
					erupt(mBoss.getEyeLocation());
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	protected void chargeActions(Location loc, int ticks) {
		if (ticks <= mParameters.CHARGE_DURATION) {
			mParameters.PARTICLES_CHARGE_BOSS.spawn(mBoss, LocationUtils.getHalfHeightLocation(mBoss));

			if (ticks % 12 == 0) {
				new PPCircle(mParameters.PARTICLE_CHARGE_GROUND, loc.clone().add(0, 0.2, 0), 1)
					// RotateDelta originates from positive X
					.delta(1, 0, 0).rotateDelta(true)
					// 1 particle per 2 degrees; 90 particles per pi radians.
					// 1 radian per radius meters circumference
					// 90/(pi * radius) particles per meter
					.countPerMeter(90.0 / Math.PI).extra(0.25).directionalMode(true).distanceFalloff(15)
					.spawnAsBoss();
			}
			mParameters.SOUND_CHARGE_BOSS.play(loc, 1, 1.5f * (((float) ticks + 1) / (float) mParameters.CHARGE_DURATION));
		}
	}

	private @Nullable Player nearestHitPlayer(Item physicsItem) {
		Hitbox hitbox = new Hitbox.AABBHitbox(physicsItem.getWorld(), BoundingBox.of(physicsItem.getLocation(), 0.75, 0.75, 0.75));
		return EntityUtils.getNearestPlayer(physicsItem.getLocation(), hitbox.getHitPlayers(true));
	}


	protected void erupt(Location loc) {
		mParameters.PARTICLES_EXPLOSION_BOSS.spawn(mBoss, mBoss.getEyeLocation());
		for (int i = 0; i < mParameters.PROJECTILE_COUNT; i++) {
			int mDegreeRand = FastUtils.randomIntInRange(-15, 30);
			Item proj = launch((360 / mParameters.PROJECTILE_COUNT) * i + mDegreeRand, loc, new ItemStack(mParameters.MATERIAL), mParameters.XZ_VELOCITY, mParameters.Y_VELOCITY);
			proj.setFireTicks(80);
			EntityUtils.makeItemInvulnereable(proj);

			new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					Location itemLoc = proj.getLocation().clone().add(0, 0.2, 0);
					itemLoc.add(proj.getVelocity().multiply(0.5));
					mParameters.PARTICLES_PROJECTILE.spawn(mBoss, itemLoc);
					Player hitPlayer = nearestHitPlayer(proj);

					if (mTicks > 80 || proj.isOnGround() || hitPlayer != null || proj.isInLava()) {
						Location explodeLoc = proj.getLocation();
						mParameters.PARTICLES_EXPLOSION.spawn(mBoss, explodeLoc);
						mParameters.SOUND_EXPLOSIONS.play(explodeLoc);

						if (hitPlayer != null) {
							damage(hitPlayer);
						}
						for (int i = 0; i < 360; i += 30) {
							mParameters.PARTICLES_EXPLOSION_BORDER.spawn(mBoss, explodeLoc.clone().add(FastUtils.cos(Math.toRadians(i)) * mParameters.EXPLOSION_RADIUS, 0.2, FastUtils.sin(Math.toRadians(i)) * mParameters.EXPLOSION_RADIUS));
						}
						for (Player p : PlayerUtils.playersInRange(explodeLoc, mParameters.EXPLOSION_RADIUS, true)) {
							damage(p);
						}
						proj.remove();
						this.cancel();
					}
					mTicks += 1;
				}

			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	protected void damage(Player player) {
		DamageUtils.damage(mBoss, player, DamageEvent.DamageType.BLAST, mParameters.DAMAGE);
		mParameters.EFFECTS.apply(player, mBoss);
	}

	public static Item launch(int degrees, Location loc, ItemStack concrete, double xz, double y) {
		ItemMeta meta = concrete.getItemMeta();
		meta.displayName(Component.text("Fireball " + degrees, NamedTextColor.WHITE)
			.decoration(TextDecoration.ITALIC, false));
		concrete.setItemMeta(meta);
		Item concreteItem = loc.getWorld().dropItem(loc, concrete);
		concreteItem.setPickupDelay(Integer.MAX_VALUE);
		concreteItem.setVelocity(
			new Vector(xz * FastUtils.sinDeg(degrees), y + FastUtils.randomDoubleInRange(-0.05, 0.05), xz * FastUtils.cosDeg(degrees))
		);

		return concreteItem;
	}

}
