package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.events.SpellCastEvent;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.CoreElemental;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class PassiveCoreInstability extends Spell {
	private static final Particle.DustOptions RED_PARTICLE = new Particle.DustOptions(Color.RED, 1);
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final CoreElemental mQuarry;
	private final int mOuterRadius;
	public int mUnstable = 0;
	private final List<ChargeUpManager> mChargeUpList = new ArrayList<>();

	public PassiveCoreInstability(Plugin plugin, LivingEntity boss, CoreElemental quarry, int outerRadius) {
		mPlugin = plugin;
		mBoss = boss;
		mQuarry = quarry;
		mOuterRadius = outerRadius;
	}

	@Override
	public void run() {

	}

	// Spoil
	@Override
	public void onHurt(DamageEvent event) {
		if (
			mUnstable > 0 && event.getSource() instanceof Player player
				&& !AbilityUtils.isIndirectDamage(event)
		) {
			// The player has spoiled the loot
			player.playSound(mBoss, Sound.ENTITY_WITHER_HURT, SoundCategory.HOSTILE, 1f, 1.5f);
			if (mQuarry.spoil(player)) {
				player.sendMessage(Component.text("You exacerbated the Core's instability, spoiling your loot.", NamedTextColor.DARK_RED));
			}
		}
	}

	// Ability chargers and core instability
	@Override
	public void bossCastAbility(SpellCastEvent event) {
		if (event.getSpell() instanceof SpellPyroclasticSlam) {
			cancelChargeUps();
		}
		if (event.getSpell() instanceof CoreElemental.CoreElementalBase spell) {
			castAbility(spell);
		}
	}

	public void castAbility(CoreElemental.CoreElementalBase spell) {
		ChargeUpManager chargeUp = new ChargeUpManager(mBoss, spell.getChargeDuration(), spell.getTitle(), BossBar.Color.RED, BossBar.Overlay.PROGRESS, mOuterRadius);
		mChargeUpList.add(chargeUp);
		// Charging boss bar
		if (spell.getChargeDuration() > 0) {
			BukkitRunnable runnable = new BukkitRunnable() {
				@Override
				public void run() {
					if (chargeUp.nextTick()) {
						castSpell(spell, chargeUp);
						this.cancel();
						mActiveRunnables.remove(this);
					}
				}
			};
			runnable.runTaskTimer(mPlugin, 0, 1);
			mActiveRunnables.add(runnable);
		}
	}

	private void castSpell(CoreElemental.CoreElementalBase spell, ChargeUpManager chargeUp) {
		// Particles
		BukkitRunnable particleRunnable = new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				mT++;
				Location pendingLocation = mBoss.getEyeLocation().clone();
				double yaw = mT * 10;
				double pitch = FastUtils.sinDeg(mT) * 20;
				Vector direction = VectorUtils.rotationToVector(yaw, pitch).multiply(1.5);
				// Effects
				new PartialParticle(Particle.REDSTONE, pendingLocation.add(direction))
					.count(15)
					.delta(0.2)
					.data(RED_PARTICLE)
					.spawnAsBoss();
				new PartialParticle(Particle.REDSTONE, pendingLocation.add(direction.rotateAroundY(Math.PI)))
					.count(15)
					.delta(0.2)
					.data(RED_PARTICLE)
					.spawnAsBoss();
				if (mT > spell.getSpellDuration()) {
					this.cancel();
					mActiveRunnables.remove(this);
				}
			}
		};
		particleRunnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(particleRunnable);

		// Casting boss bar
		if (spell.getSpellDuration() > 0) {
			mUnstable++;
			chargeUp.setTitle(
				Component.text()
					.append(Component.text("------", NamedTextColor.RED, TextDecoration.OBFUSCATED))
					.append(Component.text("CORE INSTABILITY DETECTED", NamedTextColor.RED))
					.append(Component.text("------", NamedTextColor.RED, TextDecoration.OBFUSCATED)).build());
			chargeUp.setChargeTime(spell.getSpellDuration());
			chargeUp.setTime(spell.getSpellDuration());
			BukkitRunnable runnable = new BukkitRunnable() {
				@Override
				public void run() {
					if (mQuarry.mIsCastingBanish) {
						this.cancel();
						return;
					}
					if (chargeUp.previousTick()) {
						this.cancel();
					}
				}

				@Override
				public synchronized void cancel() {
					super.cancel();
					chargeUp.remove();
					mUnstable--;
				}
			};
			runnable.runTaskTimer(mPlugin, 4, 1);
			mActiveRunnables.add(runnable);
		} else {
			chargeUp.remove();
		}
	}

	private void cancelChargeUps() {
		for (ChargeUpManager chargeUp : mChargeUpList) {
			chargeUp.remove();
		}
		mChargeUpList.clear();
		for (BukkitRunnable runnable : mActiveRunnables) {
			runnable.cancel();
		}
		mActiveRunnables.clear();
		mUnstable = 0;
	}

	public @Nullable ChargeUpManager getChargeUp(CoreElemental.CoreElementalBase spell) {
		for (ChargeUpManager chargeUp : mChargeUpList) {
			if (chargeUp.getTitle().equals(spell.getTitle())) {
				return chargeUp;
			}
		}
		return null;
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
