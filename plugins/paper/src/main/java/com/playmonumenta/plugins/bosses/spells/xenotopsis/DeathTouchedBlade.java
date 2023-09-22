package com.playmonumenta.plugins.bosses.spells.xenotopsis;

import com.playmonumenta.plugins.bosses.bosses.Xenotopsis;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellSlashAttack;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class DeathTouchedBlade extends Spell {
	// the duration of the telegraph of the attack, in ticks
	private static final int WINDUP_DURATION = 20 * 2;

	// the base cooldown of the attack, in ticks
	private static final int BASE_COOLDOWN = 20 * 2;

	// the amount of slashes for each group of slashes, this really just governs the visual effect and particle count
	private static final int SLASHES_PER_GROUP = 2;

	// the attack and death damage of each slash
	private static final int ATTACK_DAMAGE = 55;
	private static final int DEATH_DAMAGE = 6;

	// the speed of the boss during the attack
	private static final double BOSS_SPEED = 0.15;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;
	private final Xenotopsis mXenotopsis;
	private final int mCooldownTicks;

	private final double mBladeRadius;

	// so basically this is just waterfowl dance
	public DeathTouchedBlade(Plugin plugin, LivingEntity boss, Xenotopsis xenotopsis, int cooldownTicks, double bladeRadius) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = boss.getWorld();
		mCooldownTicks = cooldownTicks;
		mXenotopsis = xenotopsis;

		mBladeRadius = bladeRadius;
	}

	@Override
	public boolean canRun() {
		return mXenotopsis.canRunSpell(this);
	}

	@Override
	public void run() {
		final SpellSlashAttack mVisualSlash = new SpellSlashAttack(mPlugin, mBoss, 0, mXenotopsis.scaleDamage(ATTACK_DAMAGE), 0, mBladeRadius, -50, 50, "Death Touched Blade", 8, -40, 540, 0.12, "505a63", "323947", "09091B", "false", "false", new Vector(0.2, 0.15, 0.2), "true", 0.65, "false", 0.2, 0.8, DamageEvent.DamageType.MELEE, SoundsList.EMPTY, SoundsList.fromString("[(ENTITY_GLOW_SQUID_SQUIRT,0.8,1.7)]"), SoundsList.EMPTY, SoundsList.EMPTY);

		BukkitRunnable runnable = new BukkitRunnable() {
		    int mTicks = 0;

		    @Override
		    public void run() {
				if (mTicks == 0) {
					mXenotopsis.setMeleeDeathDamageOverride(DEATH_DAMAGE);
					EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MOVEMENT_SPEED, BOSS_SPEED);

					// telegraph part 1
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_NODAMAGE, SoundCategory.HOSTILE, 2f, 1.3f);
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_DROWNED_SHOOT, SoundCategory.HOSTILE, 4f, 1.7f);
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.HOSTILE, 0.2f, 0.9f);
					mWorld.playSound(mBoss.getLocation(), Sound.ITEM_AXE_SCRAPE, SoundCategory.HOSTILE, 2.6f, 1.4f);
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_SQUID_SQUIRT, SoundCategory.HOSTILE, 1.3f, 1.1f);
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.HOSTILE, 1.5f, 0.8f);
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 4.5f, 1.8f);

					new PPParametric(Particle.SQUID_INK, mBoss.getLocation().clone().add(0, 1.5, 0), (parameter, builder) -> {
						double r = FastUtils.randomDoubleInRange(0, Math.PI * 2);
						builder.offset(FastUtils.cos(r), 0, FastUtils.sin(r));
					})
						.directionalMode(true)
						.count(40)
						.extra(0.3)
						.spawnAsBoss();
				}

				if (mTicks == 8) {
					// telegraph part 2
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_NODAMAGE, SoundCategory.HOSTILE, 2f, 1.5f);
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_DROWNED_SHOOT, SoundCategory.HOSTILE, 4f, 1.9f);
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.HOSTILE, 0.2f, 1.2f);
					mWorld.playSound(mBoss.getLocation(), Sound.ITEM_AXE_SCRAPE, SoundCategory.HOSTILE, 2.6f, 1.5f);
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 0.9f, 0.8f);
				}

				if (mTicks == 12) {
					// telegraph part 3
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_NODAMAGE, SoundCategory.HOSTILE, 2f, 1.5f);
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_DROWNED_SHOOT, SoundCategory.HOSTILE, 4f, 1.9f);
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.HOSTILE, 0.2f, 1.2f);
					mWorld.playSound(mBoss.getLocation(), Sound.ITEM_AXE_SCRAPE, SoundCategory.HOSTILE, 2.6f, 1.5f);
				}

				if (mTicks % 4 == 0) {
					if ((mTicks >= WINDUP_DURATION && mTicks <= 20 + WINDUP_DURATION)
						|| (mTicks >= 36 + WINDUP_DURATION && mTicks <= 48 + WINDUP_DURATION)
						|| (mTicks >= 64 + WINDUP_DURATION && mTicks <= 72 + WINDUP_DURATION)
						|| (mTicks >= 84 + WINDUP_DURATION && mTicks <= 88 + WINDUP_DURATION)) {

						for (int i = 0; i < SLASHES_PER_GROUP; i++) {
							mVisualSlash.run();
						}
					}
				}

				if (mTicks == WINDUP_DURATION
					|| mTicks == 36 + WINDUP_DURATION
					|| mTicks == 64 + WINDUP_DURATION
					|| mTicks == 84 + WINDUP_DURATION) {


					Player nearestPlayer = null;
					for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), Xenotopsis.DETECTION_RANGE, true)) {
						if (player.getGameMode() != GameMode.SPECTATOR && (nearestPlayer == null || mBoss.getLocation().distance(player.getLocation()) < mBoss.getLocation().distance(nearestPlayer.getLocation()))) {
							nearestPlayer = player;
						}
					}

					if (nearestPlayer != null) {
						Vector dashVelocity = nearestPlayer.getLocation().clone().toVector().subtract(mBoss.getLocation().clone().toVector()).normalize().multiply(2.8).setY(0.35);
						mBoss.setVelocity(dashVelocity);
						mBoss.setAI(false);
						Bukkit.getScheduler().runTaskLater(mPlugin, () -> mBoss.setAI(true), 7);
					}
				}

		        mTicks++;
				if (mTicks > 88 + WINDUP_DURATION || mBoss.isDead()) {
					mXenotopsis.removeMeleeDeathDamageOverride();
					EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MOVEMENT_SPEED, mXenotopsis.getMovementSpeed());
					this.cancel();
				}
		    }
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int cooldownTicks() {
		return BASE_COOLDOWN + mCooldownTicks;
	}
}
