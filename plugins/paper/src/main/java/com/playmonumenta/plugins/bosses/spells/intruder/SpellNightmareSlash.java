package com.playmonumenta.plugins.bosses.spells.intruder;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.SpellCooldownManager;
import com.playmonumenta.plugins.bosses.spells.SpellSlashAttack;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.ParticleUtils;
import org.bukkit.Color;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;


public class SpellNightmareSlash extends SpellSlashAttack {
	private final SpellCooldownManager mSpellCooldownManager;

	public SpellNightmareSlash(LivingEntity boss) {
		super(Plugin.getInstance(), boss, 0, 50, 30,
			2, 12, 12, "Nightmare Slash", 16, -50,
			180, 0.25,
			Color.BLACK,
			ParticleUtils.getTransition(Color.BLACK, Color.PURPLE, 0.35),
			Color.fromRGB(0xff3333),
			false, true, false, true, new Vector(1, 0.5, 1), true, 1,
			false, true, 0.5, -1, DamageEvent.DamageType.MELEE,
			SoundsList.builder()
				.add(new SoundsList.CSound(Sound.ENTITY_WARDEN_DEATH, 2.1f, 0.01f))
				.add(new SoundsList.CSound(Sound.ENTITY_WARDEN_SONIC_CHARGE, 2.5f, 0.1f))
				.build(),
			SoundsList.builder()
				.add(new SoundsList.CSound(Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.5f, 0.1f))
				.add(new SoundsList.CSound(Sound.ENTITY_WITHER_SKELETON_HURT, 1.5f, 0.1f))
				.add(new SoundsList.CSound(Sound.ENTITY_WITHER_HURT, 0.4f, 0.5f))
				.build(),
			SoundsList.EMPTY,
			SoundsList.EMPTY,
			true, 2, 20, false,
			false, 1, false
		);
		mSpellCooldownManager = new SpellCooldownManager(8 * 20, boss::isValid, boss::hasAI);
	}

	@Override
	public void run() {
		if (!canRun()) {
			return;
		}
		mSpellCooldownManager.setOnCooldown();
		super.run();
	}

	@Override
	public boolean canRun() {
		return !mSpellCooldownManager.onCooldown() && mBoss.hasAI();
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
