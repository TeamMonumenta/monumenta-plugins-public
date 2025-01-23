package com.playmonumenta.plugins.bosses.spells.salieriswordsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.SalieriTheSwordsage;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.EnumSet;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class Taunt extends Spell {
	private final Plugin mPlugin;
	private final SalieriTheSwordsage mBossClass;
	private final LivingEntity mBoss;
	private final int mDamage;
	private final int mDuration;
	private @Nullable BukkitRunnable mTauntRunnable = null;

	//If taunting for getting hit
	private boolean mTaunting = false;


	public Taunt(Plugin plugin, LivingEntity boss, SalieriTheSwordsage bossClass, int damage, int duration) {
		mPlugin = plugin;
		mBoss = boss;
		mBossClass = bossClass;
		mDamage = damage;
		mDuration = duration;
	}

	@Override
	public void run() {
		//Do not run normally
	}

	public void activate() {
		mBossClass.mSpellActive = true;

		World world = mBoss.getWorld();
		Location loc = mBoss.getLocation();

		world.playSound(loc, Sound.ENTITY_VILLAGER_NO, SoundCategory.HOSTILE, 3, 0);
		world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 3, 0);
		world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, SoundCategory.HOSTILE, 3, 0);
		new PartialParticle(Particle.SPELL_WITCH, loc.clone().add(0, 1, 0), 30).delta(0.25, 1, 0.25).spawnAsBoss();
		new PartialParticle(Particle.VILLAGER_ANGRY, loc.clone().add(0, 1, 0), 6).delta(0.45, 0.5, 0.45).spawnAsBoss();

		EntityUtils.selfRoot(mBoss, mDuration);

		mTauntRunnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				new PartialParticle(Particle.SPELL_WITCH, loc.clone().add(0, 1, 0), 30).delta(0.25, 1, 0.25).spawnAsBoss();
				new PartialParticle(Particle.VILLAGER_ANGRY, loc.clone().add(0, 1, 0), 6).delta(0.45, 0.5, 0.45).spawnAsBoss();

				if (mTicks == 20) {
					mTaunting = true;
				}

				if (mTicks >= mDuration) {
					this.cancel();
					mBossClass.mSpellActive = false;
					mTaunting = false;
					EntityUtils.cancelSelfRoot(mBoss);
				}
				mTicks += 2;
			}
		};
		mTauntRunnable.runTaskTimer(mPlugin, 5, 2);
		mActiveRunnables.add(mTauntRunnable);
	}

	@Override
	public int cooldownTicks() {
		return 20 * 6;
	}

	@Override
	public void onHurtByEntity(DamageEvent event, Entity damager) {
		// If the damage type is possibly a Damage Over Time effect, its damage must be higher than a certain value in order to trigger the parry
		final EnumSet<DamageEvent.DamageType> POSSIBLE_DOT = EnumSet.of(
			DamageEvent.DamageType.MAGIC,
			DamageEvent.DamageType.OTHER
		);

		//Return if not a player or if damage type is invalid
		if (!(event.getDamager() instanceof Player player) || !mTaunting || event.getType() == DamageEvent.DamageType.AILMENT || event.getType() == DamageEvent.DamageType.FIRE || event.getType() == DamageEvent.DamageType.POISON
			|| (POSSIBLE_DOT.contains(event.getType()) && event.getFinalDamage(true) <= 15)) {
			return;
		}

		mBossClass.mSpellActive = false;
		mTaunting = false;
		if (mTauntRunnable != null && !mTauntRunnable.isCancelled()) {
			mTauntRunnable.cancel();
		}

		World world = mBoss.getWorld();
		world.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.HOSTILE, 3, 0);
		new PartialParticle(Particle.CRIT_MAGIC, player.getLocation().add(0, 1, 0), 20).delta(0.3, 0.3, 0.3).extra(0.5).spawnAsBoss();
		new PartialParticle(Particle.DAMAGE_INDICATOR, player.getLocation().add(0, 1, 0), 20).delta(0.3, 0.3, 0.3).extra(0.5).spawnAsBoss();
		new PartialParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1, 0), 10).delta(1, 1, 1).spawnAsBoss();
		new PartialParticle(Particle.VILLAGER_ANGRY, player.getLocation().add(0, 1, 0), 20).delta(0.3, 0.3, 0.3).extra(0.5).spawnAsBoss();

		mBoss.setVelocity(mBoss.getLocation().subtract(player.getLocation()).toVector().setY(0).normalize().multiply(2));
		BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.MELEE_SKILL, mDamage);
		EntityUtils.selfRoot(mBoss, 20);
		event.setFlatDamage(0);
	}
}
