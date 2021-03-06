package com.playmonumenta.plugins.bosses.bosses;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;

/**
 * @deprecated use boss_onhit instead, like this:
 *<blockquote><pre>
 * /boss var Tags add boss_onhit
 * /boss var Tags add boss_onhit[SilenceTicks=60,COLOR=XXXXX]
 * we have yet to find a way to look at color
 * </pre></blockquote>
 * @G3m1n1Boy
 *
*/
public class AbilitySilenceBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_abilitysilence";

	public static class Parameters {
		public int DETECTION = 32;
		public int DURATION = 20 * 3;
		public Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(127, 0, 0), 1.0f);
	}

	private final Parameters mParams;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new AbilitySilenceBoss(plugin, boss);
	}

	public AbilitySilenceBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, identityTag, boss);

		mParams = BossUtils.getParameters(boss, identityTag, new Parameters());

		super.constructBoss(null, null, mParams.DETECTION, null);
	}

	@Override
	public void bossDamagedEntity(EntityDamageByEntityEvent event) {
		// Attack was blocked
		if (event.getFinalDamage() == 0) {
			return;
		}

		LivingEntity target = (LivingEntity) event.getEntity();
		if (target instanceof Player) {
			World world = target.getWorld();
			Location loc = target.getLocation().add(0, 1, 0);
			world.playSound(loc, Sound.BLOCK_PORTAL_TRIGGER, 0.25f, 2f);
			world.spawnParticle(Particle.REDSTONE, loc, 100, 0, 0, 0, 0.5, mParams.COLOR);

			AbilityUtils.silencePlayer((Player) target, mParams.DURATION);
		}
	}
}
