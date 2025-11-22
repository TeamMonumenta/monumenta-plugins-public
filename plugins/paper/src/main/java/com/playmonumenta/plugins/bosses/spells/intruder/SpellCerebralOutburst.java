package com.playmonumenta.plugins.bosses.spells.intruder;

import com.playmonumenta.plugins.bosses.bosses.OmenBoss;
import com.playmonumenta.plugins.bosses.bosses.intruder.IntruderBoss;
import com.playmonumenta.plugins.bosses.events.SpellCastEvent;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.SpellCooldownManager;
import com.playmonumenta.plugins.bosses.spells.SpellOmen;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentSpeed;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellCerebralOutburst extends SpellOmen {
	private final IntruderBoss.Dialogue mDialogue;
	private boolean mFirstCast = true;
	private final SpellCooldownManager mSpellCooldownManager;

	public SpellCerebralOutburst(Plugin plugin, LivingEntity launcher, IntruderBoss.Dialogue dialogue, boolean enhanced) {
		super(plugin, launcher, new OmenBoss.Parameters());
		mDialogue = dialogue;

		mP.SOUND_HIT = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_ENDER_DRAGON_HURT, 1.2f, 0.4f))
			.build();
		mP.SOUND_WARN = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_WARDEN_DEATH, 0.3f, 0.1f))
			.add(new SoundsList.CSound(Sound.ENTITY_WARDEN_HEARTBEAT, 4.0f, 0.5f))
			.build();
		mP.SOUND_LAUNCH = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_ELDER_GUARDIAN_HURT, 2.0f, 0.1f))
			.add(new SoundsList.CSound(Sound.ENTITY_WARDEN_SONIC_BOOM, 0.7f, 0.5f))
			.add(new SoundsList.CSound(Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.8f, 0.8f))
			.build();
		mP.SOUND_TEL = SoundsList.EMPTY;
		mP.PARTICLE_TEL = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.DUST_COLOR_TRANSITION, 1, 0, 0, 0, 0,
				new Particle.DustTransition(Color.WHITE, Color.FUCHSIA, 2.5f)))
			.build();
		mP.PARTICLE_OMEN = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.DUST_COLOR_TRANSITION, 1, 0, 0.4, 0, 0,
				new Particle.DustTransition(Color.FUCHSIA, Color.RED, 1.2f)))
			.build();
		mP.PARTICLE_TEL_SWIRL = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.DUST_COLOR_TRANSITION, 1, 0, 0, 0, 0,
				new Particle.DustTransition(Color.RED, Color.PURPLE, 1.0f)))
			.add(new ParticlesList.CParticle(Particle.END_ROD, 1, 0.0, 0.0, 0.0, 9999))
			.build();
		mP.SAFESPOT_SIZE = 0;
		mP.MAX_RANGE = 40;
		mP.DETECTION = 100;
		mP.KB_X = 1.2f;
		mP.VELOCITY = 35;
		mP.KB_Y = 0.75f;
		mP.PARTICLE_GAP = 0.5;
		mP.PARTICLE_GAP_TEL = 0.1;
		mP.WIDTH = 10;
		mP.DEGREE_OFFSET = 0;
		mP.TARGETS = new EntityTargets(EntityTargets.TARGETS.PLAYER, 100.0, new EntityTargets.Limit(EntityTargets.Limit.LIMITSENUM.ALL, EntityTargets.Limit.SORTING.CLOSER));
		mP.DAMAGE = enhanced ? 75 : 65;
		mP.TEL_DURATION = 2 * 20;
		mP.COOLDOWN = enhanced ? 5 * 20 : 10 * 20;
		mP.SPLITS = enhanced ? 6 : 4;
		mP.SPLIT_ANGLE = enhanced ? 60 : 90;
		mP.HEIGHT_OFFSET = 0;
		mP.HITBOX_HEIGHT = 2.0;
		mP.DO_TARGETING = true;
		mP.SPELL_NAME = "Cerebral Outburst";

		mSpellCooldownManager = new SpellCooldownManager(7 * 20, 4 * 20, launcher::isValid, launcher::hasAI);
	}

	@Override
	public void run() {
		if (!canRun()) {
			return;
		}
		mSpellCooldownManager.setOnCooldown();
		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.AMBIENT_CAVE, SoundCategory.HOSTILE, 2.0f, 2.0f, 43);
		EffectManager.getInstance().addEffect(mBoss, "CerebralOutburst", new PercentSpeed(mP.TEL_DURATION, -0.65, "CerebralOutburst"));
		if (mFirstCast) {
			mFirstCast = false;
			mDialogue.dialogue("I ROAMED THESE WALLS. I CAN BREAK THEM.");
		}
		super.run();
		mActiveTasks.add(new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, SoundCategory.HOSTILE, 3.0f, 1.0f);
				mTicks += 20;
				if (mTicks > mP.TEL_DURATION) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 20));
	}

	public void setLastStand() {
		mP.WIDTH = 6;
	}

	@Override
	public boolean canRun() {
		return !mSpellCooldownManager.onCooldown() && mBoss.hasAI();
	}

	@Override
	public void bossCastAbility(SpellCastEvent event) {
		if (event.getSpell() instanceof SpellCerebralOnslaught) {
			mSpellCooldownManager.setOnCooldown(SpellCerebralOnslaught.COOLDOWN_TICKS);
		} else if (event.getSpell() instanceof SpellScreamroom) {
			cancel();
		}
	}

	@Override
	public int cooldownTicks() {
		return mP.TEL_DURATION;
	}
}
