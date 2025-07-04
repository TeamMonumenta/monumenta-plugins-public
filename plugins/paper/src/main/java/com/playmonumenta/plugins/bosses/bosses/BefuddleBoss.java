package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.InventoryUtils;
import java.util.Collections;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class BefuddleBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_befuddle";

	public static class Parameters extends BossParameters {
		@BossParam(help = "NOTE: This bosstag may be prone to exploit. Use with caution, and keep note of monumenta.bosstag.canbefuddle perm!")
		public int DETECTION = 20;

		@BossParam(help = "not written")
		public boolean CAN_BLOCK = true;

		//Particle & Sounds!
		@BossParam(help = "Particle summoned when the player got hit by the boss")
		public ParticlesList PARTICLE = ParticlesList.EMPTY;

		@BossParam(help = "Sound played when the player got hit by the boss")
		public SoundsList SOUND = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.1f, 0.8f))
			.build();

		@BossParam(help = "if set, makes boss_befuddle only trigger when the spell with this name deals damage")
		public String SPELL_NAME = "";

		@BossParam(help = "if set, only triggers on this type of damage")
		public @Nullable DamageType DAMAGE_TYPE = null;
	}

	private final Parameters mParams;

	public BefuddleBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		//this boss has no ability
		mParams = BossParameters.getParameters(boss, identityTag, new Parameters());
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), mParams.DETECTION, null);
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (mParams.CAN_BLOCK && event.isBlockedByShield()) {
			// Attack was blocked
			return;
		}

		if (!mParams.SPELL_NAME.equals("") && !mParams.SPELL_NAME.equals(event.getBossSpellName())) {
			// If it isn't the spell that we want, don't trigger anything
			return;
		}

		if (mParams.DAMAGE_TYPE != null && event.getType() != mParams.DAMAGE_TYPE) {
			// if it isn't the right damage type, don't trigger
			return;
		}

		new BukkitRunnable() {
			@Override
			public void run() {
				if (!event.isCancelled()) {
					execute(damagee);
				}
			}
		}.runTask(mPlugin);
	}

	private void execute(LivingEntity damagee) {
		Location loc = damagee.getLocation().add(0, 1, 0);

		mParams.SOUND.play(loc);
		mParams.PARTICLE.spawn(mBoss, loc, 0d, 0d, 0d);

		if (damagee instanceof Player player) {
			player.showElderGuardian(true);
			for (int i = 0; i < 9; i++) {
				InventoryUtils.swapTwoInventoryItems(player, i, 17 - i);
				InventoryUtils.swapTwoInventoryItems(player, 18 + i, 35 - i);
			}
		}
	}
}