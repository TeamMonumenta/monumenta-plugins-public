package com.playmonumenta.plugins.cosmetics.skills;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.GruesomeEchoesCS;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.SunriseBrewCS;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.apothecary.PrestigiousRemedyCS;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.apothecary.RitualRingCS;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.harbinger.PrestigiousEsotericCS;
import com.playmonumenta.plugins.cosmetics.skills.cleric.BloodyRetaliationCS;
import com.playmonumenta.plugins.cosmetics.skills.cleric.DarkPunishmentCS;
import com.playmonumenta.plugins.cosmetics.skills.cleric.TouchOfEntropyCS;
import com.playmonumenta.plugins.cosmetics.skills.cleric.hierophant.PrestigiousBeamCS;
import com.playmonumenta.plugins.cosmetics.skills.cleric.paladin.PrestigiousInfusionCS;
import com.playmonumenta.plugins.cosmetics.skills.mage.DarkLanceCS;
import com.playmonumenta.plugins.cosmetics.skills.mage.HallowedLanceCS;
import com.playmonumenta.plugins.cosmetics.skills.mage.SanguineAegisCS;
import com.playmonumenta.plugins.cosmetics.skills.mage.TwistedLanceCS;
import com.playmonumenta.plugins.cosmetics.skills.mage.VolcanicBurstCS;
import com.playmonumenta.plugins.cosmetics.skills.mage.arcanist.PrestigiousMoonbladeCS;
import com.playmonumenta.plugins.cosmetics.skills.mage.elementalist.PrestigiousStarfallCS;
import com.playmonumenta.plugins.cosmetics.skills.rogue.DaggerOfNothingCS;
import com.playmonumenta.plugins.cosmetics.skills.rogue.DecapitationCS;
import com.playmonumenta.plugins.cosmetics.skills.rogue.TranscCombosCS;
import com.playmonumenta.plugins.cosmetics.skills.rogue.WindStepCS;
import com.playmonumenta.plugins.cosmetics.skills.rogue.assassin.PrestigiousBlitzCS;
import com.playmonumenta.plugins.cosmetics.skills.rogue.swordsage.PrestigiousRondeCS;
import com.playmonumenta.plugins.cosmetics.skills.scout.EverseeingEyeCS;
import com.playmonumenta.plugins.cosmetics.skills.scout.TwistedCompanionCS;
import com.playmonumenta.plugins.cosmetics.skills.scout.hunter.FireworkStrikeCS;
import com.playmonumenta.plugins.cosmetics.skills.scout.hunter.PrestigiousPinningShotCS;
import com.playmonumenta.plugins.cosmetics.skills.scout.ranger.PrestigiousManeuverCS;
import com.playmonumenta.plugins.cosmetics.skills.warlock.AvalanchexCS;
import com.playmonumenta.plugins.cosmetics.skills.warlock.InfernalFlamesCS;
import com.playmonumenta.plugins.cosmetics.skills.warlock.VampiricDrainCS;
import com.playmonumenta.plugins.cosmetics.skills.warlock.reaper.PrestigiousBondsCS;
import com.playmonumenta.plugins.cosmetics.skills.warlock.tenebrist.PrestigiousShadesCS;
import com.playmonumenta.plugins.cosmetics.skills.warrior.BrambleShellCS;
import com.playmonumenta.plugins.cosmetics.skills.warrior.ColossalBruteCS;
import com.playmonumenta.plugins.cosmetics.skills.warrior.berserker.GloryExecutionCS;
import com.playmonumenta.plugins.cosmetics.skills.warrior.berserker.PrestigiousSlamCS;
import com.playmonumenta.plugins.cosmetics.skills.warrior.guardian.PrestigiousShieldCS;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

public class CosmeticSkills {

	/** For each class:
	 * 1 = Depths theme
	 * 2 = Delves theme
	 * 3 = Gallery-Sanguine theme
	 * 4 & 5 = Prestige theme
	 */
	private static final ImmutableMap<String, CosmeticSkill> COSMETIC_SKILLS =
		ImmutableMap.<String, CosmeticSkill>builder()
			//Alchemist
			.put(SunriseBrewCS.NAME, new SunriseBrewCS())
			.put(GruesomeEchoesCS.NAME, new GruesomeEchoesCS())
			.put(RitualRingCS.NAME, new RitualRingCS())
			.put(PrestigiousEsotericCS.NAME, new PrestigiousEsotericCS())
			.put(PrestigiousRemedyCS.NAME, new PrestigiousRemedyCS())
			//Cleric
			.put(DarkPunishmentCS.NAME, new DarkPunishmentCS())
			.put(TouchOfEntropyCS.NAME, new TouchOfEntropyCS())
			.put(BloodyRetaliationCS.NAME, new BloodyRetaliationCS())
			.put(PrestigiousInfusionCS.NAME, new PrestigiousInfusionCS())
			.put(PrestigiousBeamCS.NAME, new PrestigiousBeamCS())
			//Mage
			.put(VolcanicBurstCS.NAME, new VolcanicBurstCS())
			.put(TwistedLanceCS.NAME, new TwistedLanceCS())
			.put(SanguineAegisCS.NAME, new SanguineAegisCS())
			.put(PrestigiousMoonbladeCS.NAME, new PrestigiousMoonbladeCS())
			.put(PrestigiousStarfallCS.NAME, new PrestigiousStarfallCS())
			//Rogue
			.put(WindStepCS.NAME, new WindStepCS())
			.put(TranscCombosCS.NAME, new TranscCombosCS())
			.put(DecapitationCS.NAME, new DecapitationCS())
			.put(PrestigiousRondeCS.NAME, new PrestigiousRondeCS())
			.put(PrestigiousBlitzCS.NAME, new PrestigiousBlitzCS())
			//Scout
			.put(FireworkStrikeCS.NAME, new FireworkStrikeCS())
			.put(TwistedCompanionCS.NAME, new TwistedCompanionCS())
			.put(EverseeingEyeCS.NAME, new EverseeingEyeCS())
			.put(PrestigiousManeuverCS.NAME, new PrestigiousManeuverCS())
			.put(PrestigiousPinningShotCS.NAME, new PrestigiousPinningShotCS())
			//Warlock
			.put(AvalanchexCS.NAME, new AvalanchexCS())
			.put(InfernalFlamesCS.NAME, new InfernalFlamesCS())
			.put(VampiricDrainCS.NAME, new VampiricDrainCS())
			.put(PrestigiousBondsCS.NAME, new PrestigiousBondsCS())
			.put(PrestigiousShadesCS.NAME, new PrestigiousShadesCS())
			//Warrior
			.put(BrambleShellCS.NAME, new BrambleShellCS())
			.put(ColossalBruteCS.NAME, new ColossalBruteCS())
			.put(GloryExecutionCS.NAME, new GloryExecutionCS())
			.put(PrestigiousSlamCS.NAME, new PrestigiousSlamCS())
			.put(PrestigiousShieldCS.NAME, new PrestigiousShieldCS())
			//Deprecated or test only
			.put(DarkLanceCS.NAME, new DarkLanceCS())
			.put(HallowedLanceCS.NAME, new HallowedLanceCS())
			.put(DaggerOfNothingCS.NAME, new DaggerOfNothingCS())
			.build();

	/* Notes for implementing more Cosmetic Skills
	 * 0. 基础 If the ability is not skinned, add a default CS class!
	 * 0.a 名单 A default CS should have an ImmutableList to store all skins of this ability.
	 * 0.b 效果 A default CS should store all effects to be skinned.
	 * 0.c 变量 Add skin checker and mCosmetic field in the ability
	 * 1. 新类 Create new class in correct package, extending the base CS.
	 * 2. 重载 Override effect methods for new skin.
	 * 3. 属性 Override getCosmetic() and getDisplayItem() for correct cosmetic display.
	 * 4. 注册 Put a new entry here.
	 */

	private static final ImmutableList<DepthsCS> DEPTHS_CS;
	private static final ImmutableList<GalleryCS> GALLERY_CS;
	private static final ImmutableList<PrestigeCS> PRESTIGE_CS;

	static {
		ImmutableList.Builder<DepthsCS> depthsBuilder = ImmutableList.<DepthsCS>builder();
		ImmutableList.Builder<GalleryCS> galleryBuilder = ImmutableList.<GalleryCS>builder();
		for (CosmeticSkill cs : COSMETIC_SKILLS.values()) {
			if (cs instanceof DepthsCS) {
				depthsBuilder.add((DepthsCS) cs);
				continue;
			}
			if (cs instanceof GalleryCS) {
				galleryBuilder.add((GalleryCS) cs);
				continue;
			}
		}
		DEPTHS_CS = depthsBuilder.build();
		GALLERY_CS = galleryBuilder.build();
		// Prestige skins need a special order in shop GUI
		PRESTIGE_CS = ImmutableList.<PrestigeCS>builder()
			.add(new PrestigiousEsotericCS())
			.add(new PrestigiousInfusionCS())
			.add(new PrestigiousMoonbladeCS())
			.add(new PrestigiousRondeCS())
			.add(new PrestigiousManeuverCS())
			.add(new PrestigiousBondsCS())
			.add(new PrestigiousSlamCS())
			.add(new PrestigiousRemedyCS())
			.add(new PrestigiousBeamCS())
			.add(new PrestigiousStarfallCS())
			.add(new PrestigiousBlitzCS())
			.add(new PrestigiousPinningShotCS())
			.add(new PrestigiousShadesCS())
			.add(new PrestigiousShieldCS())
			.build();
	}

	public static Cosmetic getCosmeticByName(String name) {
		CosmeticSkill cs = COSMETIC_SKILLS.getOrDefault(name, null);
		return cs != null ? cs.getCosmetic() : null;
	}

	public static CosmeticSkill getCosmeticSkill(String name) {
		return COSMETIC_SKILLS.getOrDefault(name, null);
	}

	public static <T extends CosmeticSkill> T getPlayerCosmeticSkill(@Nullable Player player, T baseCosmetic, ImmutableMap<String, T> skinList) {
		String name = CosmeticsManager.getInstance().getSkillCosmeticName(player, baseCosmetic.getAbilityName());
		return skinList.getOrDefault(name, baseCosmetic);
	}

	public static Material getDisplayItem(String cosmeticSkillName) {
		CosmeticSkill cosmeticSkill = COSMETIC_SKILLS.get(cosmeticSkillName);
		if (cosmeticSkill != null) {
			return cosmeticSkill.getDisplayItem();
		} else {
			return Material.BLAZE_POWDER;
		}
	}

	public static String[] getNames() {
		return COSMETIC_SKILLS.keySet().toArray(new String[0]);
	}

	public static ImmutableList<DepthsCS> getDepthsSkins() {
		return DEPTHS_CS;
	}

	public static ImmutableList<String> getDepthsNames() {
		ImmutableList.Builder<String> builder = ImmutableList.<String>builder();
		for (DepthsCS cs : getDepthsSkins()) {
			builder.add(((CosmeticSkill) cs).getName());
		}
		return builder.build();
	}

	public static ImmutableList<String> getDelvesNames() {
		return ImmutableList.<String>builder()
			.add(GruesomeEchoesCS.NAME)
			.add(TouchOfEntropyCS.NAME)
			.add(TwistedLanceCS.NAME)
			.add(TranscCombosCS.NAME)
			.add(TwistedCompanionCS.NAME)
			.add(InfernalFlamesCS.NAME)
			.add(ColossalBruteCS.NAME)
			.build();
	}

	public static ImmutableList<GalleryCS> getGallerySkins() {
		return GALLERY_CS;
	}

	public static ImmutableList<String> getGalleryNames() {
		ImmutableList.Builder<String> builder = ImmutableList.<String>builder();
		for (GalleryCS cs : getGallerySkins()) {
			builder.add(((CosmeticSkill) cs).getName());
		}
		return builder.build();
	}

	public static ImmutableList<PrestigeCS> getPrestigeSkins() {
		return PRESTIGE_CS;
	}

	public static ImmutableList<String> getPrestigeNames() {
		ImmutableList.Builder<String> builder = ImmutableList.<String>builder();
		for (PrestigeCS cs : getPrestigeSkins()) {
			builder.add(((CosmeticSkill) cs).getName());
		}
		return builder.build();
	}
}
