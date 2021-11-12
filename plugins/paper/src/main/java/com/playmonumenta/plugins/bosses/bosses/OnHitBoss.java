package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;

public class OnHitBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_onhit";

	public static class Parameters extends BossParameters {
		@BossParam(help = "not written")
		public int DETECTION = 20;

		@BossParam(help = "not written")
		public boolean CAN_BLOCK = true;

		@BossParam(help = "Effects apply to the player when got hit by the boss")
		public EffectsList EFFECTS = EffectsList.EMPTY;

		//Particle & Sounds!
		@BossParam(help = "Particle summoned when the player got hit by the boss")
		public ParticlesList PARTICLE = ParticlesList.fromString("[(REDSTONE,20,0,0,0,1,#ffffff,2)]");

		@BossParam(help = "Sound played when the player got hit by the boss")
		public SoundsList SOUND = SoundsList.fromString("[(BLOCK_PORTAL_TRIGGER,0.25,2)]");

	}

	private Parameters mParams;

	public OnHitBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		//this boss has no ability
		mParams = BossParameters.getParameters(boss, identityTag, new Parameters());
		super.constructBoss(null, null, mParams.DETECTION, null);
	}

	@Override
	public void bossDamagedEntity(EntityDamageByEntityEvent event) {
		if (mParams.CAN_BLOCK) {
			if (event.getFinalDamage() == 0) {
				// Attack was blocked
				return;
			}
		}

		LivingEntity target = (LivingEntity) event.getEntity();
		if (target instanceof Player) {
			Location loc = target.getLocation().add(0, 1, 0);
			Player player = (Player) target;

			mParams.EFFECTS.apply(player, mBoss);

			//Particle & Sound
			mParams.SOUND.play(loc);
			mParams.PARTICLE.spawn(loc, 0d, 0d, 0d);
		}


	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new OnHitBoss(plugin, boss);
	}

}
