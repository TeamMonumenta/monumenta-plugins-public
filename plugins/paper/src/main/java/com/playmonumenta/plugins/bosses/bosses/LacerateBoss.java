package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellLacerate;
import com.playmonumenta.plugins.events.DamageEvent;
import java.util.Collections;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class LacerateBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_lacerate";

	public static class Parameters extends BossParameters {
		@BossParam(help = "Radius of the spell")
		public int RADIUS = 4;

		@BossParam(help = "Range at which the spell can be cast")
		public int RANGE = 12;

		@BossParam(help = "how long before first cast of the spell")
		public int DELAY = 100;

		@BossParam(help = "Damage that this spell deals to players per hit")
		public int DAMAGE = 7;

		@BossParam(help = "Amounts of hits dealt to players (this includes the finisher, so if you want 5 rapid hits before it then put 6, etc.)")
		public int HIT_AMOUNT = 5;

		@BossParam(help = "Finishing hit damage")
		public int FINISHER_DAMAGE = 20;

		@BossParam(help = "The type of the damage dealt by the attack. Default: MELEE")
		public DamageEvent.DamageType DAMAGE_TYPE = DamageEvent.DamageType.MELEE;

		@BossParam(help = "time between casts of the spell")
		public int COOLDOWN = 160;

		@BossParam(help = "range for targets to be in for spell to start casting")
		public int DETECTION = 20;

		@BossParam(help = "Time between casting the spell and the resulting flurry")
		public int TELEGRAPH_DURATION = 50;


		@BossParam(help = "You should not use this. use TARGETS instead.", deprecated = true)
		public boolean LINE_OF_SIGHT = true;

		@BossParam(help = "target of this spell")
		public EntityTargets TARGETS = EntityTargets.GENERIC_PLAYER_TARGET_LINE_OF_SIGHT;

		@BossParam(help = "Knock away from the mob")
		public double KNOCK_AWAY = 3.0;

		@BossParam(help = "Knock up")
		public double KNOCK_UP = 0.4;

		@BossParam(help = "The RGB values of the random straight lines in the flurry. (Default: #ffc4c4)")
		public Color LINE_COLOR = Color.fromRGB(255, 196, 196);

		@BossParam(help = "The color of the inner part of the curved lines in the flurry. (Default: #d99898")
		public Color SLASH_COLOR_INNER = Color.fromRGB(217, 152, 152);

		@BossParam(help = "The color of the outer part of the curved lines in the flurry. (Default: #bd3535")
		public Color SLASH_COLOR_OUTER = Color.fromRGB(189, 53, 53);

		@BossParam(help = "Amount of rings in the curved slashes of the flurry.")
		public int RINGS = 8;

		@BossParam(help = "Space between rings in the curved slashes of the flurry")
		public double SPACING = 0.2;

		@BossParam(help = "Particle Size on Slashes")
		public double FORCED_PARTICLE_SIZE = -1;

		@BossParam(help = "Density of line particles")
		public int LINE_PARTICLE_COUNT = 3;
		@BossParam(help = "interval at which the lines show up (also how often the player is ticked for damage")
		public int LINE_INTERVAL = 2;
		@BossParam(help = "whether attack respects iframes")
		public boolean RESPECT_IFRAMES = false;

		@BossParam(help = "Number of points for the explosion's circle")
		public int EXPLOSION_POINTS = 60;

		@BossParam(help = "Length of the straight lines")
		public double LINE_LENGTH = 2.25;

		@BossParam(help = "Velocity of the Circle Explosion particles")
		public float EXPLOSION_SPEED = 1.75f;

		@BossParam(help = "Whether or not to draw the random straight flurry lines")
		public boolean SHOW_LINES = true;

		@BossParam(help = "Whether or not to draw the side curved slashes")
		public boolean SHOW_SLASHES = true;

		@BossParam(help = "Whether or not to draw circle explosions")
		public boolean SHOW_EXPLOSIONS = true;

		@BossParam(help = "Whether or not to draw the end line")
		public boolean SHOW_END_LINE = true;

		@BossParam(help = "end line redstone dust color")
		public Color END_LINE_COLOR = Color.fromRGB(245, 233, 233);

		@BossParam(help = "Sound played at the targeted player when the boss starts charging the ability ability")
		public SoundsList SOUND_WARNING = SoundsList.fromString("[(ITEM_TRIDENT_RETURN,5,0.75)]");

		@BossParam(help = "Sound played every tick at the caster while the spell is charging. Pitch is automatically increased by 0.01 every tick")
		public SoundsList SOUND_CHARGE_BOSS = SoundsList.fromString("[]");

		@BossParam(help = "Sound played at the start of when the flurry happens.")
		public SoundsList SOUND_EXPLOSION = SoundsList.fromString("[(BLOCK_BEACON_POWER_SELECT,1,1.65)]");

		@BossParam(help = "Sound played at the start of when the flurry happens.")
		public SoundsList SOUND_FINISHER = SoundsList.fromString("[(ITEM_TRIDENT_RIPTIDE_3, 1.25, 1),(BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.25, 0.8)]");

		@BossParam(help = "Sound of the flurries. increases by 0.125 every time")
		public SoundsList SOUND_FLURRY_INCREMENT = SoundsList.fromString("[(ITEM_TRIDENT_THROW, 1.25, 0.85),(ENTITY_PUFFER_FISH_BLOW_OUT, 1.25, 0.75)]");

		@BossParam(help = "background sound of the flurries. does not increment")
		public SoundsList SOUND_FLURRY_BACKGROUND = SoundsList.fromString("[(ITEM_TRIDENT_HIT, 1.1, 0.75),(ITEM_TRIDENT_RETURN, 1.1, 1.25)]");

		@BossParam(help = "Particles used in telegraph beam")
		public ParticlesList TELEGRAPH_BEAM = ParticlesList.fromString("[(ELECTRIC_SPARK)]");

		@BossParam(help = "Particles used in telegraph circle")
		public ParticlesList TELEGRAPH_CIRCLE = ParticlesList.fromString("[(ELECTRIC_SPARK)]");

		@BossParam(help = "Particles used when hit")
		public Particle ON_HIT_PARTICLE = Particle.WAX_OFF;

		@BossParam(help = "Particles used on the last hit")
		public Particle FINISHER_PARTICLE = Particle.SWEEP_ATTACK;

		@BossParam(help = "Particle of the Circular Explosions")
		public Particle EXPLOSION_PARTICLES = Particle.ELECTRIC_SPARK;

		@BossParam(help = "end line particle 2")
		public Particle END_LINE_PARTICLE = Particle.ELECTRIC_SPARK;

	}

	public LacerateBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());
		if (p.TARGETS == EntityTargets.GENERIC_PLAYER_TARGET_LINE_OF_SIGHT) {
			p.TARGETS = new EntityTargets(EntityTargets.TARGETS.PLAYER, p.RANGE, false, EntityTargets.Limit.DEFAULT_ONE, p.LINE_OF_SIGHT ? List.of(EntityTargets.PLAYERFILTER.HAS_LINEOFSIGHT) : Collections.emptyList());
		}

		Spell spell = new SpellLacerate(plugin, boss, p);
		super.constructBoss(spell, p.DETECTION, null, p.DELAY);
	}
}
