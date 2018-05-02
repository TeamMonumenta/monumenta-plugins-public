package pe.bossfights.bosses;

import org.bukkit.entity.LivingEntity;

public abstract class Boss
{
	LivingEntity mBoss;

	public Boss(String identityTag, LivingEntity boss) {
		mBoss = boss;
		mBoss.setRemoveWhenFarAway(false);
		boss.addScoreboardTag(identityTag);
	}

	/* Called only the first time the boss is summoned into the world */
	public abstract void init();

	/* Called when the boss dies */
	public abstract void death();

	/* Called when the chunk the boss is in unloads. Also called after death() */
	public abstract void unload();
}
