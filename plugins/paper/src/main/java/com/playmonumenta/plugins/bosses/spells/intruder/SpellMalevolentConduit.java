package com.playmonumenta.plugins.bosses.spells.intruder;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.intruder.IntruderBoss;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.SpellCooldownManager;
import com.playmonumenta.plugins.bosses.spells.SpellNova;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellMalevolentConduit extends SpellNova {
	private final LivingEntity mBoss;
	private final IntruderBoss.Dialogue mDialogue;
	private final IntruderBoss.Narration mNarration;
	private final ChargeUpManager mChargeUpManager;

	private final SpellCooldownManager mSpellCooldownManager;

	public SpellMalevolentConduit(Plugin plugin, LivingEntity boss, IntruderBoss.Dialogue dialogue, IntruderBoss.Narration narration) {
		super(plugin, boss, 14, "Malevolent Conduit", 4 * 20, 30 * 20, false, false, Sound.ENTITY_WITHER_SPAWN, 0.25f, 2,
			ParticlesList.EMPTY, ParticlesList.builder()
				.add(new ParticlesList.CParticle(Particle.DUST_COLOR_TRANSITION, 5, 0, 0, 0, 0,
					new Particle.DustTransition(Color.PURPLE, Color.BLACK, 2.0f)))
				.build(),
			ParticlesList.builder()
				.add(new ParticlesList.CParticle(Particle.SPELL_WITCH, 20, 0, 0, 0))
				.build(),
			new EntityTargets(EntityTargets.TARGETS.PLAYER, 14, EntityTargets.Limit.DEFAULT, List.of()),
			SoundsList.builder()
				.add(new SoundsList.CSound(Sound.ENTITY_WARDEN_SONIC_CHARGE, 1f, 0.1f))
				.build(),
			1);
		mBoss = boss;
		mDialogue = dialogue;
		mNarration = narration;
		mChargeUpManager = new ChargeUpManager(mBoss, mDuration, Component.text("Channeling ", NamedTextColor.GOLD).append(Component.text(mSpellName, NamedTextColor.RED)), BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS, IntruderBoss.DETECTION_RANGE);
		mSpellCooldownManager = new SpellCooldownManager(40 * 20, boss::isValid, boss::hasAI);
	}

	@Override
	public void run() {
		mSpellCooldownManager.setOnCooldown();
		mDialogue.dialogue("YOUR POWER IS NOTHING.");
		mNarration.narration("The <obfuscated>lxxxxxxx</obfuscated> prepares to weaken your willpower...");
		EntityUtils.selfRoot(mBoss, 4 * 20);
		super.run();

		mChargeUpManager.setTime(0);

		mActiveTasks.add(new BukkitRunnable() {
			@Override
			public void run() {
				if (mChargeUpManager.getTime() % 10 == 0) {
					if (mChargeUpManager.getTime() < mDuration - 20) {
						new PPCircle(Particle.REVERSE_PORTAL, mLauncher.getLocation().add(new Vector(0, 0.2, 0)), mRadius)
							.count(200)
							.directionalMode(true)
							.delta(-0.45, 0, 0)
							.extra(1)
							.rotateDelta(true)
							.spawnAsBoss();
					}

					new PPCircle(Particle.DUST_COLOR_TRANSITION, mLauncher.getLocation().add(new Vector(0, 0.2, 0)), mRadius)
						.data(new Particle.DustTransition(Color.PURPLE, Color.FUCHSIA, 2.0f))
						.count(80)
						.delta(0.1)
						.spawnAsBoss();

					new PPCircle(Particle.SPELL_WITCH, mLauncher.getLocation().add(new Vector(0, 0.2, 0)), mRadius)
						.count(50)
						.delta(0.4)
						.spawnAsBoss();
				}
				if (mChargeUpManager.nextTick(2)) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 2));
	}

	@Override
	protected void chargeCircleAction(Location loc, double radius) {
		double timeStart = radius / mRadius * mDuration;
		double timeEnd = (radius - (double) mRadius / mDuration) / mRadius * mDuration;
		Color color = ParticleUtils.getTransition(Color.FUCHSIA, Color.RED, radius / mRadius);

		for (int i = 0; i < 10; i++) {
			double time = timeStart + (timeEnd - timeStart) * i / 10;
			double x = FastUtils.sin(time);
			double z = FastUtils.cos(time);
			double y = FastUtils.sin(time / 1.34) * 2;
			double mult = FastUtils.sin(y * Math.PI / 2) * 2;
			Location pLoc = loc.clone().add(new Vector(0, 5, 0));

			pLoc.setX(pLoc.getX() + x * mult);
			pLoc.setZ(pLoc.getZ() + z * mult);
			pLoc.setY(pLoc.getY() + y);
			new PartialParticle(Particle.DUST_COLOR_TRANSITION, pLoc)
				.data(new Particle.DustTransition(color, Color.RED, 1.45f))
				.delta(0.075)
				.spawnAsBoss();
		}

		new PartialParticle(Particle.REDSTONE, loc.clone().add(new Vector(0, 5, 0)))
			.data(new Particle.DustOptions(color, 2.0f))
			.spawnAsBoss();
		super.chargeCircleAction(loc, radius);
	}

	@Override
	public int cooldownTicks() {
		return 5 * 20;
	}

	@Override
	public boolean canRun() {
		return !mSpellCooldownManager.onCooldown() && mBoss.hasAI();
	}

	@Override
	protected void dealDamageAction(Location loc) {
		new PartialParticle(Particle.FLASH, loc.clone().add(new Vector(0, 5, 0))).minimumCount(1).spawnAsBoss();
		for (LivingEntity target : mTargets.getTargetsList(mLauncher)) {
			if (target instanceof Player player) {
				PotionUtils.clearPositives(Plugin.getInstance(), player);
				List<EffectManager.EffectPair> pairs = EffectManager.getInstance().getAllEffectPairs(player);
				if (pairs != null) {
					for (EffectManager.EffectPair pair : pairs) {
						// Delete (hopefully) all positive effects;
						if (pair.mEffect().isBuff() && !pair.mSource().startsWith("PatronShrine")) {
							pair.mEffect().setDuration(0);
						}
					}
				}
				player.sendMessage(Component.text("YOU WILL NOT BE. NEEDING THAT.", TextColor.color(0xcc11a8), TextDecoration.BOLD));
				DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, DAMAGE);
				EffectManager.getInstance().addEffect(player, "MalevolentConduit", new PercentHeal(30 * 20, -.30));
			}
		}
		cancel();
	}
}
