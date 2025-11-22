package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.intruder.SpellLucidRendSlash;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class LucidRendBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_floatingsword_slashattack";
	public static final String DISABLE_TAG = "LucidRendDisable";
	private final SpellLucidRendSlash mSpell;

	public static class Parameters extends BossParameters {
		@BossParam(help = "The base cooldown between each swing. Default: 100")
		public int COOLDOWN = 100;
		@BossParam(help = "The damage of the attack. Default: 20")
		public double DAMAGE = 20;
		@BossParam(help = "The range within which there must be at least one player to cast the spell. Default: 30")
		public int DETECTION_RANGE = 30;
		@BossParam(help = "The delay between the telegraph and the actual attack. Default: 0")
		public int TELEGRAPH_DURATION = 0;
		@BossParam(help = "The recovery time after the attack and targeting players again. Default: 20")
		public int RECOVER_DURATION = 20;
		@BossParam(help = "The delay between spawn and first attack. Default: 10")
		public int DELAY = 10;
		@BossParam(help = "The radius of the slash attack. Default: 2.5")
		public double RADIUS = 2.5;
		@BossParam(help = "The name of the attack, shown when a player dies to it. Default: \"Slash\"")
		public String ATTACK_NAME = "Slash";
		@BossParam(help = "The width of the attack. Default: 8")
		public int RINGS = 8;
		@BossParam(help = "The starting angle of the slash. Default: -40")
		public double START_ANGLE = -40;
		@BossParam(help = "The ending angle of the slash. Default: 140")
		public double END_ANGLE = 140;
		@BossParam(help = "The spacing between each ring of the slash. Default: 0.2")
		public double SPACING = 0.2;
		@BossParam(help = "The starting color of the color transition. Default: 498f72")
		public String START_HEX_COLOR = "498f72";
		@BossParam(help = "The middle color of the color transition. Only applicable if horizontalcolor=true. Default: 81fcc9")
		public String MID_HEX_COLOR = "81fcc9";
		@BossParam(help = "The ending color of the color transition. Default: 20bd35")
		public String END_HEX_COLOR = "20bd35";
		@BossParam(help = "Whether the slash is drawn using a Full Arc (true) or Half Arc (false).")
		public boolean FULL_ARC = false;
		@BossParam(help = "Whether to transition color horizontally or not. Default: false")
		public boolean HORIZONTAL_COLOR = false;
		@BossParam(help = "The x component of the knockback. Default: 0")
		public double KB_X = 0;
		@BossParam(help = "The y component of the knockback. Default: 0")
		public double KB_Y = 0;
		@BossParam(help = "The z component of the knockback. Default: 0")
		public double KB_Z = 0;
		@BossParam(help = "Whether or not to apply the knockback away from the boss. Will use the x component for horizontal, and y for vertical. Default: false")
		public boolean KNOCK_AWAY = false;
		@BossParam(help = "The effectiveness of KBR. Default: 1.0")
		public double KBR_EFFECTIVENESS = 1;
		@BossParam(help = "Whether or not the attack should be repositioned at the caster. Default: false")
		public boolean FOLLOW_CASTER = false;
		@BossParam(help = "The size of each particle's hitbox, in all three directions. Default: 0.2")
		public double HITBOX_SIZE = 0.2;
		@BossParam(help = "Force the size of each dust particle to this one, if a positive value. Default: -1")
		public double FORCED_PARTICLE_SIZE = -1;
		@BossParam(help = "Whether or not the slash respects iframes")
		public boolean RESPECT_IFRAMES = true;
		@BossParam(help = "Whether or not the slash can hit people again")
		public boolean MULTI_HIT = false;
		@BossParam(help = "Minimum interval to when slashattack can hit again")
		public int MULTIHIT_INTERVAL = 8;
		@BossParam(help = "The particles spawned in a cross shape when the sword slashes.")
		public ParticlesList CROSS_PARTICLE = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.END_ROD, 30))
			.build();
		@BossParam(help = "The type of the damage dealt by the attack. Default: MELEE")
		public DamageEvent.DamageType DAMAGE_TYPE = DamageEvent.DamageType.MELEE;
		@BossParam(help = "The sound at the start of telegraph. Default: ENTITY_PLAYER_BREATH and ENTITY_IRON_GOLEM_DEATH")
		public SoundsList SOUND_TELEGRAPH = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_PLAYER_BREATH, 2.0f, 0.7f))
			.add(new SoundsList.CSound(Sound.ENTITY_IRON_GOLEM_DEATH, 2.0f, 2.0f))
			.build();
		@BossParam(help = "The sound at the start of the slash. Default: ENTITY_PLAYER_ATTACK_SWEEP")
		public SoundsList SOUND_SLASH_START = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f))
			.build();
		@BossParam(help = "The sound at every tick of the slash. Default: EMPTY")
		public SoundsList SOUND_SLASH_TICK = SoundsList.EMPTY;
		@BossParam(help = "The sound at the end of the slash. Default: EMPTY")
		public SoundsList SOUND_SLASH_END = SoundsList.EMPTY;
	}

	Parameters mParams = new Parameters();
	@Nullable
	private BukkitTask mTask;

	public LucidRendBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		Parameters.getParameters(boss, identityTag, mParams);

		mSpell = new SpellLucidRendSlash(plugin, boss, mParams);
		super.constructBoss(new SpellManager(List.of(mSpell)), Collections.emptyList(), mParams.DETECTION_RANGE, null, mParams.DELAY);

		if (boss instanceof Mob mBossMob) {
			mTask = new BukkitRunnable() {
				int mCurrentPauseTicks = 0;
				int mCurrentCooldown = mParams.DELAY;

				@Override
				public void run() {
					if (!mBoss.isValid() || mBoss.isDead()) {
						this.cancel();
					}
					if (mBoss.getScoreboardTags().contains(DISABLE_TAG)) {
						return;
					}

					if (mCurrentCooldown > 0) {
						mCurrentCooldown--;
					}
					if (mCurrentPauseTicks > 0) {
						mCurrentPauseTicks--;
						return;
					}

					if (mBossMob.getTarget() != null) {
						Location loc = mBoss.getLocation();
						Vector targetDir = mBossMob.getTarget().getLocation().toVector().subtract(loc.toVector());
						double[] targetYawPitch = VectorUtils.vectorToRotation(targetDir);
						mBoss.setRotation((float) targetYawPitch[0], (float) targetYawPitch[1]);
						mSpell.swordMatchRotation();
						if (mCurrentCooldown <= 0 && Objects.requireNonNull(mBossMob.getTarget()).getLocation().distance(mBoss.getLocation()) <= 3.5) {
							forceCastSpell(mSpell.getClass());
							// Jank
							mCurrentPauseTicks = mSpell.mTelegraphDuration + mSpell.mRecoverDuration;
							mCurrentCooldown = mParams.COOLDOWN;
						}
					}
				}
			}.runTaskTimer(plugin, 0, 1);
		}
	}

	@Override
	public void unload() {
		super.unload();
		onRemovedOrDeath();
	}


	@Override
	public void death(@Nullable EntityDeathEvent event) {
		super.death(event);
		mActiveSpells.cancelAll();
		onRemovedOrDeath();
	}

	private void onRemovedOrDeath() {
		if (mTask != null && !mTask.isCancelled()) {
			mTask.cancel();
		}
		mSpell.removeSword();
	}
}