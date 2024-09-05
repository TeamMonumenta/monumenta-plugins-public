package com.playmonumenta.plugins.cosmetics.finishers;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.Set;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;

public class EliteFinishers {

	private static final String FINISHER_GLOW_TAG = "finisherGlow";
	private static final String FINISHER_SHOW_TAG = "finisherShow";

	private static final ImmutableMap<String, EliteFinisher> FINISHERS =
		ImmutableMap.<String, EliteFinisher>builder()
			.put(BirthdayThemeFinisher.NAME, new BirthdayThemeFinisher())
			.put(CakeifyFinisher.NAME, new CakeifyFinisher())
			.put(ChainedFinisher.NAME, new ChainedFinisher())
			.put(CoolFireworkFinisher.NAME, new CoolFireworkFinisher())
			.put(CornucopiaFinisher.NAME, new CornucopiaFinisher())
			.put(DefaultDanceFinisher.NAME, new DefaultDanceFinisher())
			.put(DragonsBreathFinisher.NAME, new DragonsBreathFinisher())
			.put(EntombFinisher.NAME, new EntombFinisher())
			.put(ExcaliburFinisher.NAME, new ExcaliburFinisher())
			.put(FalseLichFinisher.NAME, new FalseLichFinisher())
			.put(FishedUpFinisher.NAME, new FishedUpFinisher())
			.put(FrozenSolidFinisher.NAME, new FrozenSolidFinisher())
			.put(GongFinisher.NAME, new GongFinisher())
			.put(ImplosionFinisher.NAME, new ImplosionFinisher())
			.put(LightningFinisher.NAME, new LightningFinisher())
			.put(LocustSwarmFinisher.NAME, new LocustSwarmFinisher())
			.put(MegalovaniaFinisher.NAME, new MegalovaniaFinisher())
			.put(PaintSplashFinisher.NAME, new PaintSplashFinisher())
			.put(PoultryficationFinisher.NAME, new PoultryficationFinisher())
			.put(Promenade.NAME, new Promenade())
			.put(ShootingStarFinisher.NAME, new ShootingStarFinisher())
			.put(SplishSplashFinisher.NAME, new SplishSplashFinisher())
			.put(SupernovaFinisher.NAME, new SupernovaFinisher())
			.put(SwordRainFinisher.NAME, new SwordRainFinisher())
			.put(TwinkleTwinkleLittleStar.NAME, new TwinkleTwinkleLittleStar())
			.put(USAFireworkFinisher.NAME, new USAFireworkFinisher())
			.put(VictoryThemeFinisher.NAME, new VictoryThemeFinisher())
			.put(WarmFireworkFinisher.NAME, new WarmFireworkFinisher())
			.put(Whirlpool.NAME, new Whirlpool())
			.put(Woolerman.NAME, new Woolerman())
			.build();

	// Delegate based on elite finisher name
	public static void activateFinisher(Player p, Entity killedMob, Location loc, String finisherName) {
		EliteFinisher finisher = FINISHERS.get(finisherName);
		if (finisher != null) {
			finisher.run(p, killedMob, loc);
		}
	}

	public static Material getDisplayItem(String finisherName) {
		EliteFinisher finisher = FINISHERS.get(finisherName);
		if (finisher != null) {
			return finisher.getDisplayItem();
		} else {
			return Material.FIREWORK_ROCKET;
		}
	}

	public static LivingEntity createClonedMob(LivingEntity killedMob, Player p, NamedTextColor color) {
		LivingEntity mClonedKilledMob = EntityUtils.copyMob(killedMob);
		if (p.getScoreboardTags().contains(FINISHER_GLOW_TAG)) {
			GlowingManager.startGlowing(mClonedKilledMob, color, 200, GlowingManager.PLAYER_ABILITY_PRIORITY);
			p.showEntity(Plugin.getInstance(), mClonedKilledMob);
			mClonedKilledMob.setInvisible(true);
			EntityEquipment equipment = mClonedKilledMob.getEquipment();
			if (equipment != null) {
				equipment.clear();
			}
		} else if (p.getScoreboardTags().contains(FINISHER_SHOW_TAG)) {
			GlowingManager.clearAll(mClonedKilledMob);
			p.showEntity(Plugin.getInstance(), mClonedKilledMob);
		} else if (p.getScoreboardTags().contains(FINISHER_GLOW_TAG) && p.getScoreboardTags().contains(FINISHER_SHOW_TAG)) {
			// both tags hide everything and no tags hide nothing (reversed) because i didn't want to create another tag and wanted
			// show all by default (no tags)
			GlowingManager.clearAll(mClonedKilledMob);
			p.hideEntity(Plugin.getInstance(), mClonedKilledMob);
			EntityEquipment equipment = mClonedKilledMob.getEquipment();
			if (equipment != null) {
				equipment.clear();
			}
		} else {
			GlowingManager.startGlowing(mClonedKilledMob, color, 200, GlowingManager.PLAYER_ABILITY_PRIORITY);
			p.showEntity(Plugin.getInstance(), mClonedKilledMob);
		}
		mClonedKilledMob.setHealth(1);
		mClonedKilledMob.setInvulnerable(true);
		mClonedKilledMob.setGravity(false);
		mClonedKilledMob.setCollidable(false);
		mClonedKilledMob.setAI(false);
		mClonedKilledMob.setSilent(true);
		mClonedKilledMob.addScoreboardTag("SkillImmune");
		return mClonedKilledMob;
	}


	public static String[] getNames() {
		return FINISHERS.keySet().toArray(String[]::new);
	}

	public static Set<String> getNameSet() {
		return Set.copyOf(FINISHERS.keySet());
	}

}
