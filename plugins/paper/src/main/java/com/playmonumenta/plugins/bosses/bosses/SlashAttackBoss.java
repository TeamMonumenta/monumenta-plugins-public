package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.SpellSlashAttack;
import com.playmonumenta.plugins.events.DamageEvent;
import java.util.Collections;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public class SlashAttackBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_slashattack";

	public static class Parameters extends BossParameters {
		@BossParam(help = "The base cooldown between each swing. Default: 100")
		public int COOLDOWN = 100;
		@BossParam(help = "The damage of the attack. Default: 20")
		public double DAMAGE = 20;
		@BossParam(help = "The range within which there must be at least one player to cast the spell. Default: 30")
		public int DETECTION_RANGE = 30;
		@BossParam(help = "The delay between the telegraph and the actual attack. Default: 0")
		public int TELEGRAPH_DURATION = 0;
		@BossParam(help = "The delay between spawn and first attack. Default: 10")
		public int DELAY = 10;
		@BossParam(help = "The radius of the slash attack. Default: 2.5")
		public double RADIUS = 2.5;
		@BossParam(help = "The minimum angle of the slash. 180 is horizontal. Default: 150")
		public double MIN_ANGLE = 150;
		@BossParam(help = "The maximum angle of the slash. 180 is horizontal. Default: 210")
		public double MAX_ANGLE = 210;
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
		@BossParam(help = "Whether there should or not be a second, perpendicular slash, forming an X. Default: false")
		public String X_SLASH = "false";
		@BossParam(help = "Whether to transition color horizontally or not. Default: false")
		public String HORIZONTAL_COLOR = "false";
		@BossParam(help = "The x component of the knockback. Default: 0")
		public double KB_X = 0;
		@BossParam(help = "The y component of the knockback. Default: 0")
		public double KB_Y = 0;
		@BossParam(help = "The z component of the knockback. Default: 0")
		public double KB_Z = 0;
		@BossParam(help = "Whether or not to apply the knockback away from the boss. Will use the x component for horizontal, and y for vertical. Default: false")
		public String KNOCK_AWAY = "false";
		@BossParam(help = "The effectiveness of KBR. Default: 1.0")
		public double KBR_EFFECTIVENESS = 1;
		@BossParam(help = "Whether or not the attack should be repositioned at the caster. Default: false")
		public String FOLLOW_CASTER = "false";
		@BossParam(help = "The size of each particle's hitbox, in all three directions. Default: 0.2")
		public double HITBOX_SIZE = 0.2;
		@BossParam(help = "Force the size of each dust particle to this one, if a positive value. Default: -1")
		public double FORCED_PARTICLE_SIZE = -1;
		@BossParam(help = "The type of the damage dealt by the attack. Default: MELEE")
		public DamageEvent.DamageType DAMAGE_TYPE = DamageEvent.DamageType.MELEE;
	}

	public final Parameters mParams;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new SlashAttackBoss(plugin, boss);
	}

	public SlashAttackBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		mParams = Parameters.getParameters(boss, identityTag, new Parameters());

		SpellManager activeSpells = new SpellManager(List.of(
				new SpellSlashAttack(plugin, boss,
						mParams.COOLDOWN, mParams.DAMAGE, mParams.TELEGRAPH_DURATION, mParams.RADIUS, mParams.MIN_ANGLE,
						mParams.MAX_ANGLE, mParams.ATTACK_NAME, mParams.RINGS, mParams.START_ANGLE, mParams.END_ANGLE,
						mParams.SPACING, mParams.START_HEX_COLOR, mParams.MID_HEX_COLOR, mParams.END_HEX_COLOR,
						mParams.X_SLASH, mParams.HORIZONTAL_COLOR, new Vector(mParams.KB_X, mParams.KB_Y, mParams.KB_Z),
						mParams.KNOCK_AWAY, mParams.KBR_EFFECTIVENESS, mParams.FOLLOW_CASTER, mParams.HITBOX_SIZE,
						mParams.FORCED_PARTICLE_SIZE, mParams.DAMAGE_TYPE
				)
		));

		super.constructBoss(activeSpells, Collections.emptyList(), mParams.DETECTION_RANGE, null, mParams.DELAY);
	}
}
