package com.playmonumenta.plugins.cosmetics.finishers;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.utils.CommandUtils;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;



public class EliteFinishers {

	private static final ImmutableMap<String, EliteFinisher> FINISHERS =
		ImmutableMap.<String, EliteFinisher>builder()
			.put(BirthdayThemeFinisher.NAME, new BirthdayThemeFinisher())
			.put(CakeifyFinisher.NAME, new CakeifyFinisher())
			.put(CoolFireworkFinisher.NAME, new CoolFireworkFinisher())
			.put(CornucopiaFinisher.NAME, new CornucopiaFinisher())
			.put(DefaultDanceFinisher.NAME, new DefaultDanceFinisher())
			.put(DragonsBreathFinisher.NAME, new DragonsBreathFinisher())
			.put(EntombFinisher.NAME, new EntombFinisher())
			.put(ExcaliburFinisher.NAME, new ExcaliburFinisher())
			.put(FalseLich.NAME, new FalseLich())
			.put(FishedUpFinisher.NAME, new FishedUpFinisher())
			.put(FrozenSolidFinisher.NAME, new FrozenSolidFinisher())
			.put(ImplosionFinisher.NAME, new ImplosionFinisher())
			.put(LightningFinisher.NAME, new LightningFinisher())
			.put(LocustSwarmFinisher.NAME, new LocustSwarmFinisher())
			.put(MegalovaniaFinisher.NAME, new MegalovaniaFinisher())
			.put(PaintSplashFinisher.NAME, new PaintSplashFinisher())
			.put(PoultryficationFinisher.NAME, new PoultryficationFinisher())
			.put(Promenade.NAME, new Promenade())
			.put(SplishSplashFinisher.NAME, new SplishSplashFinisher())
			.put(SwordRainFinisher.NAME, new SwordRainFinisher())
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

	public static String[] getNames() {
		return FINISHERS.keySet().stream().map(CommandUtils::quoteIfNeeded).toArray(String[]::new);
	}

	public static Set<String> getNameSet() {
		return Set.copyOf(FINISHERS.keySet());
	}

}
