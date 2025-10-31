package com.playmonumenta.plugins.hunts.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.HuntsManager;
import com.playmonumenta.plugins.hunts.bosses.spells.BanishFallDistance;
import com.playmonumenta.plugins.hunts.bosses.spells.PassivePhantomControl;
import com.playmonumenta.plugins.hunts.bosses.spells.SpellFanOfFeathers;
import com.playmonumenta.plugins.hunts.bosses.spells.SpellFeatherBomb;
import com.playmonumenta.plugins.hunts.bosses.spells.SpellFeatherStorm;
import com.playmonumenta.plugins.hunts.bosses.spells.SpellHawkSpoil;
import com.playmonumenta.plugins.hunts.bosses.spells.SpellImpactfulSwoop;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SteelWingHawk extends Quarry {
	public static final String identityTag = "boss_steel_wing_hawk";
	private static final int HEALTH = 8500;
	public static final int INNER_RADIUS = 50;
	public static final int OUTER_RADIUS = 70;
	public static final TextColor COLOR = TextColor.color(166, 161, 172);

	private static final int EFFECT_DURATION = 30 * 20;
	private static final double PERCENT_PROJ_DAMAGE = -0.25;
	private static final String PROJ_DAMAGE_EFFECT = "SteelWingHawkFeatherProjectileDamage";
	private static final double PERCENT_SLOW = -0.15;
	private static final String SLOW_EFFECT = "SteelWingHawkFeatherSlow";

	public int mFeathers;

	public SteelWingHawk(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc, INNER_RADIUS, OUTER_RADIUS, HuntsManager.QuarryType.STEEL_WING_HAWK);
		mBoss.setRemoveWhenFarAway(false);
		mBoss.addScoreboardTag("Boss");
		mBoss.customName(Component.text("Steel Wing Hawk", NamedTextColor.DARK_RED, TextDecoration.BOLD));
		mBoss.setCustomNameVisible(false);
		EntityUtils.setMaxHealthAndHealth(mBoss, HEALTH);
		mBoss.setAI(false);
		GlowingManager.startGlowing(mBoss, NamedTextColor.GRAY, -1, 1);
		PassivePhantomControl passivePhantomControl = new PassivePhantomControl(mBoss, mPlugin, this);
		passivePhantomControl.run();
		((Phantom) mBoss).setSize(5);
		mBanishSpell = new BanishFallDistance(plugin, boss, this);
		List<Spell> passives = List.of(
			new SpellHawkSpoil(boss, this),
			new SpellBlockBreak(mBoss, true, false)
		);
		List<Spell> actives = List.of(
			new SpellFeatherBomb(plugin, boss, this, passivePhantomControl),
			new SpellFanOfFeathers(plugin, boss, this),
			new SpellFeatherStorm(plugin, boss, this),
			new SpellImpactfulSwoop(plugin, boss, this, passivePhantomControl)
		);
		SpellManager spells = new SpellManager(actives);
		BossBarManager bossBar = new BossBarManager(mBoss, OUTER_RADIUS * 2, BossBar.Color.PURPLE, BossBar.Overlay.NOTCHED_10, getBaseHealthEvents(), true, true, mSpawnLoc);
		super.constructBoss(spells, passives, OUTER_RADIUS, bossBar);

		int playerCount = mPlayers.size();
		mFeathers = Math.max(50, 15 + FastUtils.randomIntInRange((int) (4 * Math.sqrt(playerCount)), (int) (4 * Math.pow(playerCount, 0.65))));
	}

	@Override
	public boolean bossIsOutOfRange() {
		// We need the horizontal range to be small without the boss naturally leaving while flying
		return LocationUtils.xzDistance(mBoss.getLocation(), mSpawnLoc) > mRadiusOuter;
	}

	@Override
	public void onHurt(DamageEvent event) {
		super.onHurt(event);

		Location loc = mBoss.getLocation();
		if (loc.getY() > mSpawnLoc.getY() + 10 && mFeathers > 0 && event.getSource() instanceof Player player && event.getType() == DamageEvent.DamageType.PROJECTILE && MetadataUtils.checkOnceInRecentTicks(mPlugin, player, "SteelWingHawkFeather", 2 * 20)) {
			mFeathers -= 1;
			new PartialParticle(Particle.CLOUD, loc.clone().add(0, -0.5, 0), 1).delta(0.5).spawnAsBoss();
			player.playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, SoundCategory.HOSTILE, 0.8f, 2.0f);

			World world = loc.getWorld();
			Item feather = AbilityUtils.spawnAbilityItem(world, loc.clone().add(0, -1, 0), Material.FEATHER, "SteelWingHawkFeather", false, 0, false, true);

			new BukkitRunnable() {
				int mT = 0;

				@Override
				public void run() {
					if (mT >= 60 * 20 || !mBoss.isValid()) {
						feather.remove();
						this.cancel();
					}

					Location l = feather.getLocation();
					new PartialParticle(Particle.FALLING_DUST, l, 2, 0.2, 0.2, 0.2, Material.COBWEB.createBlockData()).spawnAsBoss();
					new PPCircle(Particle.CLOUD, l, 1.5).countPerMeter(0.65).delta(0.01, 0.15, 0.01).ringMode(true).spawnAsBoss();
					new PPCircle(Particle.SNOWFLAKE, l, 1.35).countPerMeter(1.1).ringMode(false).spawnAsBoss();

					for (Player p : new Hitbox.UprightCylinderHitbox(l, 1, 1.5).getHitPlayers(true)) {
						com.playmonumenta.plugins.Plugin plugin = com.playmonumenta.plugins.Plugin.getInstance();
						plugin.mEffectManager.addEffect(p, PROJ_DAMAGE_EFFECT, new PercentDamageDealt(EFFECT_DURATION, PERCENT_PROJ_DAMAGE).damageTypes(DamageEvent.DamageType.getAllProjectileTypes()));
						plugin.mEffectManager.addEffect(p, SLOW_EFFECT, new PercentSpeed(EFFECT_DURATION, PERCENT_SLOW, SLOW_EFFECT));

						Location pLoc = p.getLocation();
						world.playSound(pLoc, Sound.BLOCK_STONE_BREAK, SoundCategory.PLAYERS, 0.85f, 1.5f);
						world.playSound(pLoc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.9f, 1.85f);
						world.playSound(pLoc, Sound.ENTITY_PARROT_IMITATE_SPIDER, SoundCategory.PLAYERS, 1.1f, 0.8f);
						new PartialParticle(Particle.BLOCK_CRACK, l, 15, 0.15, 0.15, 0.15, 0.75F, Material.COBWEB.createBlockData()).spawnAsBoss();
						new PartialParticle(Particle.SMOKE_NORMAL, pLoc, 12, 0.05, 0.2, 0.05, 0.25F).spawnAsBoss();

						feather.remove();
						this.cancel();
						break;
					}

					mT += 2;
				}
			}.runTaskTimer(mPlugin, 0, 2);
		}
	}

	@Override
	public String getUnspoiledLootTable() {
		return "epic:r3/hunts/loot/steel_wing_hawk_unspoiled";
	}

	@Override
	public String getSpoiledLootTable() {
		return "epic:r3/hunts/loot/steel_wing_hawk_spoiled";
	}

	@Override
	public String getAdvancement() {
		return "monumenta:challenges/r3/hunts/steel_wing_hawk";
	}

	@Override
	public String getQuestTag() {
		return "HuntPhantom";
	}
}
