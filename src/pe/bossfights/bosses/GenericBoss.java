package pe.bossfights.bosses;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import pe.bossfights.BossBarManager;

public class GenericBoss extends Boss
{
	public static final String identityTag = "boss_generic";
	public static final int detectionRange = 30;

	LivingEntity mBoss;

	public static Boss deserialize(Plugin plugin, LivingEntity boss) throws Exception
	{
		return new GenericBoss(plugin, boss);
	}

	public GenericBoss(Plugin plugin, LivingEntity boss)
	{
		mBoss = boss;

		BossBarManager bossBar = new BossBarManager(boss, detectionRange, BarColor.WHITE, BarStyle.SOLID, null);

		super.constructBoss(plugin, identityTag, mBoss, null, null, detectionRange, bossBar);
	}
}
