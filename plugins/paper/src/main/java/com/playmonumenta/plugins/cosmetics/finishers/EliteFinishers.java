package com.playmonumenta.plugins.cosmetics.finishers;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.scoreboard.Team;

public class EliteFinishers {

	public static final String FINISHER_GLOW_TAG = "finisherGlow";
	public static final String FINISHER_SHOW_TAG = "finisherShow";
	public static final String FINISHER_HIDE_OTHER_TAG = "finisherHideOthers";

	private static final ImmutableMap<String, EliteFinisher> FINISHERS =
		ImmutableMap.<String, EliteFinisher>builder()
			.put(AdvancedAudioMechanism.NAME, new AdvancedAudioMechanism())
			.put(BirthdayThemeFinisher.NAME, new BirthdayThemeFinisher())
			.put(BlastOffFinisher.NAME, new BlastOffFinisher())
			.put(BuzzedFinisher.NAME, new BuzzedFinisher())
			.put(CakeifyFinisher.NAME, new CakeifyFinisher())
			.put(ChainedFinisher.NAME, new ChainedFinisher())
			.put(CoolFireworkFinisher.NAME, new CoolFireworkFinisher())
			.put(CornucopiaFinisher.NAME, new CornucopiaFinisher())
			.put(DefaultDanceFinisher.NAME, new DefaultDanceFinisher())
			.put(DoofFinisher.NAME, new DoofFinisher())
			.put(DragonsBreathFinisher.NAME, new DragonsBreathFinisher())
			.put(EntombFinisher.NAME, new EntombFinisher())
			.put(ExcaliburFinisher.NAME, new ExcaliburFinisher())
			.put(FalseLichFinisher.NAME, new FalseLichFinisher())
			.put(FishedUpFinisher.NAME, new FishedUpFinisher())
			.put(FrozenSolidFinisher.NAME, new FrozenSolidFinisher())
			.put(GatekeeperFinisher.NAME, new GatekeeperFinisher())
			.put(GongFinisher.NAME, new GongFinisher())
			.put(GrindsMyGearsFinisher.NAME, new GrindsMyGearsFinisher())
			.put(HarmonicDissonanceFinisher.NAME, new HarmonicDissonanceFinisher())
			.put(ImplosionFinisher.NAME, new ImplosionFinisher())
			.put(LightningFinisher.NAME, new LightningFinisher())
			.put(LocustSwarmFinisher.NAME, new LocustSwarmFinisher())
			.put(MaledictioRanae.NAME, new MaledictioRanae())
			.put(MegalovaniaFinisher.NAME, new MegalovaniaFinisher())
			.put(MoneyRainFinisher.NAME, new MoneyRainFinisher())
			.put(PaintSplashFinisher.NAME, new PaintSplashFinisher())
			.put(PoultryficationFinisher.NAME, new PoultryficationFinisher())
			.put(PrideFinisher.NAME, new PrideFinisher())
			.put(Promenade.NAME, new Promenade())
			.put(SeasonalFireworks.NAME, new SeasonalFireworks())
			.put(ShootingStarFinisher.NAME, new ShootingStarFinisher())
			.put(SinkholeFinisher.NAME, new SinkholeFinisher())
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

	public static LivingEntity createClonedMob(LivingEntity killedMob, Player p, NamedTextColor color, boolean gravity, boolean ai, boolean silent) {
		LivingEntity mClonedKilledMob = EntityUtils.copyMob(killedMob);
		mClonedKilledMob.setGravity(gravity);
		mClonedKilledMob.setAI(ai);
		mClonedKilledMob.setSilent(silent);

		return modifyFinisherMob(mClonedKilledMob, p, color);
	}

	public static LivingEntity modifyFinisherMob(LivingEntity killedMob, Player p, NamedTextColor color) {
		killedMob.setHealth(1);
		killedMob.setInvulnerable(true);
		ScoreboardUtils.addEntityToTeam(killedMob, "finisher", NamedTextColor.WHITE).setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
		killedMob.addScoreboardTag("SkillImmune");
		boolean hasGlowTag = p.getScoreboardTags().contains(FINISHER_GLOW_TAG);
		boolean hasShowTag = p.getScoreboardTags().contains(FINISHER_SHOW_TAG);
		List<Player> otherPlayers = PlayerUtils.playersInRange(killedMob.getLocation(), 8 * 16, true, true);
		otherPlayers.remove(p);
		otherPlayers.forEach(player -> {
			if (player.getScoreboardTags().contains(FINISHER_HIDE_OTHER_TAG)) {
				player.hideEntity(Plugin.getInstance(), killedMob);
			}
		});
		if (hasGlowTag && hasShowTag) {
			// Both tags present: hide everything
			killedMob.setVisibleByDefault(false);
		} else if (hasGlowTag) {
			// Only Glow Tag present: show glowing and hide mob
			GlowingManager.startGlowing(killedMob, color, 200, GlowingManager.PLAYER_ABILITY_PRIORITY);
			killedMob.setInvisible(true);
			EntityEquipment equipment = killedMob.getEquipment();
			if (equipment != null) {
				equipment.clear();
			}
		} else if (hasShowTag) {
			// Only Show Tag present: remove glowing, show mob
			GlowingManager.clearAll(killedMob);
		} else {
			// Neither tag present: show everything by default (glowing)
			GlowingManager.startGlowing(killedMob, color, 200, GlowingManager.PLAYER_ABILITY_PRIORITY);
		}

		return killedMob;
	}

	public static boolean canAccess(Player player) {
		return (ScoreboardUtils.getScoreboardValue(player, Constants.Objectives.PATREON_DOLLARS).orElse(0) >= Constants.PATREON_TIER_2);
	}

	public static void handleLogin(Player player) {
		if (canAccess(player)) {
			CosmeticsManager.getInstance().addCosmetic(player, CosmeticType.ELITE_FINISHER, MoneyRainFinisher.NAME);
		} else {
			CosmeticsManager.getInstance().removeCosmetic(player, CosmeticType.ELITE_FINISHER, MoneyRainFinisher.NAME);
		}
	}

	public static String[] getNames() {
		return FINISHERS.keySet().toArray(String[]::new);
	}

	public static Set<String> getNameSet() {
		return Set.copyOf(FINISHERS.keySet());
	}

}
