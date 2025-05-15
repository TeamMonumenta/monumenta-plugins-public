package com.playmonumenta.plugins.bosses.spells.intruder;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.intruder.AbhorrentHallucinationBoss;
import com.playmonumenta.plugins.bosses.bosses.intruder.IntruderBoss;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class SpellAbhorrentHallucination extends Spell {
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	@Nullable
	private LivingEntity mAbhorrenthallucination;
	private final Location mCenter;
	private final IntruderBoss.Dialogue mDialogue;
	private final Runnable mOnFinished;
	private final ChargeUpManager mChargeUpManager;

	private static final String SPELL_NAME = "Abhorrent Hallucination";
	private static final int CHARGE_TIME = 2 * 20;

	private static final List<String> mBosses = List.of(
		"RelentlessHallucination",
		"ManipulatingHallucination",
		"EtherealHallucination",

		"IrritatingHallucination",
		"AgitatedHallucination",
		"BrutalHallucination",

		"DepravedHallucination",
		"PanickingHallucination",
		"InescapableHallucination"
	);

	private int mCastCount = 0;
	private boolean mHallucinationActive;

	public SpellAbhorrentHallucination(Plugin plugin, LivingEntity boss, Location spawnLocation, IntruderBoss.Dialogue dialogue, Runnable onFinished) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = spawnLocation;
		mDialogue = dialogue;
		mOnFinished = onFinished;
		mChargeUpManager = new ChargeUpManager(boss, CHARGE_TIME, Component.text(SPELL_NAME, NamedTextColor.DARK_RED, TextDecoration.BOLD), BossBar.Color.PURPLE, BossBar.Overlay.NOTCHED_12, IntruderBoss.DETECTION_RANGE);
	}

	@Override
	public void run() {
		mChargeUpManager.setTime(0);
		mHallucinationActive = true;
		mDialogue.dialogue("YOU ARE NOT. ALONE HERE.");
		EffectManager.getInstance().addEffect(mBoss, "AbhorrentHallucinationResistance", new PercentDamageReceived(60 * 60 * 20, -1.0));
		EffectManager.getInstance().addEffect(mBoss, "AbhorrentHallucinationSpeed", new PercentSpeed(60 * 60 * 20, -0.6, "AbhorrentHallucinationSpeed"));

		Location spawnLocation = LocationUtils.randomSafeLocationInCircle(mBoss.getLocation(), 5, location ->
			!location.getBlock().isSolid());
		mAbhorrenthallucination = (LivingEntity) Objects.requireNonNull(LibraryOfSoulsIntegration.summon(spawnLocation, "AbhorrentHallucination"));

		List<Player> viewers = IntruderBoss.playersInRange(spawnLocation);
		viewers.forEach(player -> player.hideEntity(mPlugin, mAbhorrenthallucination));
		mAbhorrenthallucination.setInvulnerable(true);

		new BukkitRunnable() {

			@Override
			public void run() {
				if (mChargeUpManager.nextTick(2)) {
					mChargeUpManager.remove();

					new PartialParticle(Particle.FLASH, spawnLocation).minimumCount(1).spawnAsBoss();

					if (mAbhorrenthallucination != null) {
						EntityUtils.setRemoveEntityOnUnload(mAbhorrenthallucination);
						viewers.forEach(player -> player.showEntity(mPlugin, mAbhorrenthallucination));
						mAbhorrenthallucination.setInvulnerable(false);
						BossManager.getInstance().manuallyRegisterBoss(mAbhorrenthallucination, new AbhorrentHallucinationBoss(mPlugin, mAbhorrenthallucination, mCenter, mBosses.subList(mCastCount * 3, mCastCount * 3 + 3)));
					}
					mCastCount++;
					this.cancel();
				} else {
					float progress = (float) mChargeUpManager.getTime() / mChargeUpManager.getChargeTime();

					for (int degree = 0; degree < 360; degree += 30) {
						double radian = Math.toRadians(degree + progress * 180);
						Vector vec = new Vector(1, 0, 0).rotateAroundY(radian).multiply(10 * (1 - progress));
						Location loc = spawnLocation.clone().add(vec);
						new PartialParticle(Particle.SMOKE_LARGE, loc).spawnAsBoss();
						new PartialParticle(Particle.DUST_COLOR_TRANSITION, loc)
							.data(new Particle.DustTransition(Color.RED, Color.BLACK, 1.3f))
							.spawnAsBoss();
					}
					new PartialParticle(Particle.SQUID_INK, spawnLocation.clone().add(0, 5 * (1 - progress), 0)).spawnAsBoss();

					spawnLocation.getWorld().playSound(LocationUtils.randomLocationInCircle(spawnLocation, 5), Sound.ENTITY_ENDERMAN_SCREAM, SoundCategory.HOSTILE, 0.4f * progress, FastUtils.randomFloatInRange(0.1f, 1.5f));
					spawnLocation.getWorld().playSound(LocationUtils.randomLocationInCircle(spawnLocation, 5), Sound.ENTITY_WARDEN_ROAR, SoundCategory.HOSTILE, 0.4f * progress, FastUtils.randomFloatInRange(0.1f, 1.5f));
				}
			}
		}.runTaskTimer(mPlugin, 0, 2);
	}

	public void bossEntityDeathEvent(EntityDeathEvent event) {
		if (mHallucinationActive && event.getEntity() == mAbhorrenthallucination) {
			mHallucinationActive = false;
			mOnFinished.run();
			EffectManager.getInstance().clearEffects(mBoss, "AbhorrentHallucinationSpeed");
			EffectManager.getInstance().clearEffects(mBoss, "AbhorrentHallucinationResistance");
		}
	}

	public boolean getHallucinationActive() {
		return mHallucinationActive;
	}

	@Override
	public int cooldownTicks() {
		return 10;
	}

	@Override
	public boolean onlyForceCasted() {
		return true;
	}

	public void killHallucination() {
		mHallucinationActive = false;
		if (mAbhorrenthallucination != null) {
			mAbhorrenthallucination.setHealth(0);
		}
		EntityUtils.getNearbyMobs(mCenter, IntruderBoss.DETECTION_RANGE, IntruderBoss.DETECTION_RANGE, IntruderBoss.DETECTION_RANGE, entity ->
			entity.getScoreboardTags().contains(AbhorrentHallucinationBoss.SUMMON_TAG)).forEach(Entity::remove);
	}
}
