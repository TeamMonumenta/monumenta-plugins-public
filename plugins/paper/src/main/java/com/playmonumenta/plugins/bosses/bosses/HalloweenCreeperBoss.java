package com.playmonumenta.plugins.bosses.bosses;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.utils.CommandUtils;

public class HalloweenCreeperBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_halloween_creeper";

	private final BukkitRunnable mRunnable;
	private final Creeper mCreeper;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new HalloweenCreeperBoss(plugin, boss);
	}

	public HalloweenCreeperBoss(Plugin plugin, LivingEntity boss) throws Exception {
		if (!(boss instanceof Creeper)) {
			throw new Exception(identityTag + " only works on mobs!");
		}

		super.constructBoss(plugin, identityTag, boss, null, null, 100, null);

		mCreeper = (Creeper)boss;
		if (mCreeper.isIgnited()) {
			// Nothing to do - creeper already exploding
			mRunnable = null;
		} else {
			mCreeper.setCustomName(ChatColor.GOLD + "Tricky Creeper");
			mCreeper.setGlowing(true);
			mCreeper.setExplosionRadius(20);
			mCreeper.setMaxFuseTicks(100);
			mCreeper.setIgnited(true);

			mRunnable = new BukkitRunnable() {
				private int mTicks = 0;

				@Override
				public void run() {
					Location loc = boss.getLocation().add(0, 1, 0);
					String baseCmd = "summon minecraft:firework_rocket " + Double.toString(loc.getX()) + " " + Double.toString(loc.getY()) + " " + Double.toString(loc.getZ());
					loc.getWorld().playSound(loc, Sound.ENTITY_CREEPER_HURT, SoundCategory.HOSTILE, 1.0f, 0.9f);
					switch (mTicks) {
						case 0:
						case 1:
						case 3:
						case 4:
						case 5:
						case 7:
						case 8:
							// Sorta jank - do nothing during intermediate steps
							break;
						case 2:
							CommandUtils.runCommandViaConsole(baseCmd + " {Silent:1b,FireworksItem:{id:firework_rocket,Count:1,tag:{Fireworks:{Explosions:[{Type:0,Flicker:0b,Colors:[I;16738847],FadeColors:[I;0]}]}}}}");
							break;
						case 6:
							CommandUtils.runCommandViaConsole(baseCmd + " {Silent:1b,FireworksItem:{id:firework_rocket,Count:1,tag:{Fireworks:{Explosions:[{Type:1,Flicker:0b,Colors:[I;16738847],FadeColors:[I;0]}]}}}}");
							break;
						default:
							CommandUtils.runCommandViaConsole(baseCmd + " {Silent:1b,FireworksItem:{id:firework_rocket,Count:1,tag:{Fireworks:{Explosions:[{Type:3,Flicker:1b,Colors:[I;16738847],FadeColors:[I;0]}]}}}}");
							this.cancel();
							break;
					}
					mTicks++;
				}
			};

			mRunnable.runTaskTimer(plugin, 0, 10);
		}
	}

	@Override
	public void bossDamagedByEntity(EntityDamageByEntityEvent event) {
		// Reduce the damage but don't cancel the hit
		event.setDamage(0);
	}

	@Override
	public void unload() {
		super.unload();

		if (mRunnable != null) {
			mRunnable.cancel();
		}

		if (mCreeper.isDead() || !mCreeper.isValid()) {
			Location loc = mCreeper.getLocation().add(0, 1, 0);
			CommandUtils.runCommandViaConsole("setblock " + Integer.toString(loc.getBlockX()) + " " + Integer.toString(loc.getBlockY()) + " " + Integer.toString(loc.getBlockZ()) + " minecraft:chest{LootTable:\"epic:event/halloween2019/tricked_creeper\",CustomName:\"{\\\"text\\\":\\\"§6§lCreeperween Chest\\\"}\"}");
		}
	}
}
