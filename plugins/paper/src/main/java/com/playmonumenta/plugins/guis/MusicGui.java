package com.playmonumenta.plugins.guis;

import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.depths.bosses.Broodmother;
import com.playmonumenta.plugins.depths.bosses.Davey;
import com.playmonumenta.plugins.depths.bosses.Hedera;
import com.playmonumenta.plugins.depths.bosses.Nucleus;
import com.playmonumenta.plugins.depths.bosses.Vesperidys;
import com.playmonumenta.plugins.itemstats.enums.Location;
import com.playmonumenta.plugins.utils.AdvancementUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.managers.SongManager;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class MusicGui extends Gui {
	public enum MusicPage {
		VANILLA("vanilla", 36, "Minecraft Music Discs"),
		VALLEY("valley", 36, "Monumenta Soundtrack: King's Valley"),
		ISLES("isles", 54, "Monuementa Soundtrack: Celsian Isles"),
		RING("ring", 36, "Monuementa Soundtrack: Architect's Ring"),
		DUNGEONS("dungeons", 27, "Monumenta Soundtrack: Dungeons"),
		LABS("labs", 27, "Monumenta Soundtrack: Alchemy Labs"),
		WHITE("white", 27, "Monumenta Soundtrack: Halls of Wind and Blood"),
		ORANGE("orange", 27, "Monumenta Soundtrack: Fallen Menagerie"),
		MAGENTA("magenta", 27, "Monumenta Soundtrack: Plagueroot Temple"),
		LIGHTBLUE("lightblue", 27, "Monumenta Soundtrack: Arcane Rivalry"),
		YELLOW("yellow", 27, "Monumenta Soundtrack: Vernal Nightmare"),
		WILLOWS("willows", 27, "Monumenta Soundtrack: Black Willows"),
		REVERIE("reverie", 27, "Monumenta Soundtrack: Malevolent Reverie"),
		BLUE("blue", 27, "Monumenta Soundtrack: Coven's Gambit"),
		BROWN("brown", 27, "Monumenta Soundtrack: Cradle of the Broken God"),
		PLOTS("plots", 27, "Monumenta Soundtrack: Miscellaneous");

		public final String mLabel;
		public final int mSize;
		public final String mTitle;

		MusicPage(String label, int size, String title) {
			mLabel = label;
			mSize = size;
			mTitle = title;
		}
	}

	public MusicPage mPage;
	public final boolean mReturnToRecordPlayer;
	public final boolean mPlayToOthers;

	public MusicGui(Player player, MusicPage page, boolean returnToRecordPlayer, boolean playToOthers) {
		super(player, page.mSize, page.mTitle);
		mPage = page;
		mReturnToRecordPlayer = returnToRecordPlayer;
		mPlayToOthers = playToOthers;
	}

	@Override
	public void setup() {
		switch (mPage) {
			case VANILLA -> {
				addVanillaMusicItem(1, 1, Material.MUSIC_DISC_13, "13", Sound.MUSIC_DISC_13, 178);
				addVanillaMusicItem(1, 2, Material.MUSIC_DISC_CAT, "Cat", Sound.MUSIC_DISC_CAT, 185);
				addVanillaMusicItem(1, 3, Material.MUSIC_DISC_BLOCKS, "Blocks", Sound.MUSIC_DISC_BLOCKS, 345);
				addVanillaMusicItem(1, 4, Material.MUSIC_DISC_CHIRP, "Chirp", Sound.MUSIC_DISC_CHIRP, 185);
				addVanillaMusicItem(1, 5, Material.MUSIC_DISC_FAR, "Far", Sound.MUSIC_DISC_FAR, 174);
				addVanillaMusicItem(1, 6, Material.MUSIC_DISC_MALL, "Mall", Sound.MUSIC_DISC_MALL, 197);
				addVanillaMusicItem(1, 7, Material.MUSIC_DISC_MELLOHI, "Mellohi", Sound.MUSIC_DISC_MELLOHI, 96);

				addVanillaMusicItem(2, 1, Material.MUSIC_DISC_STAL, "Stal", Sound.MUSIC_DISC_STAL, 150);
				addVanillaMusicItem(2, 2, Material.MUSIC_DISC_STRAD, "Strad", Sound.MUSIC_DISC_STRAD, 251);
				addVanillaMusicItem(2, 3, Material.MUSIC_DISC_WARD, "Ward", Sound.MUSIC_DISC_WARD, 251);
				addVanillaMusicItem(2, 4, Material.MUSIC_DISC_WAIT, "Wait", Sound.MUSIC_DISC_WAIT, 238);
				addVanillaMusicItem(2, 5, Material.MUSIC_DISC_PIGSTEP, "Pigstep", Sound.MUSIC_DISC_PIGSTEP, 148);
				addVanillaMusicItem(2, 6, Material.MUSIC_DISC_OTHERSIDE, "Otherside", Sound.MUSIC_DISC_OTHERSIDE, 195);
				addVanillaMusicItem(2, 7, Material.MUSIC_DISC_11, "11", Sound.MUSIC_DISC_11, 71);

				addVanillaMusicItem(3, 1, Material.MUSIC_DISC_5, "5", Sound.MUSIC_DISC_5, 178);
				//addVanillaMusicItem(3, 2, Material.MUSIC_DISC_RELIC, "Relic", Sound.MUSIC_DISC_RELIC, 218);
			}

			case VALLEY -> {
				addMusicItem(1, 1, Material.MUSIC_DISC_WAIT, "Storm's Eye", Location.LIGHTBLUE, "Sierhaven", "Casiel368", "epic:music.sierhaven", 222, checkAdvance("monumenta:quests/r1/sierhaven"));
				addMusicItem(1, 2, Material.MUSIC_DISC_BLOCKS, "Locus Amoenus", Location.RUSH, "Nyr", "Casiel368", "epic:music.nyr", 176, checkAdvance("monumenta:quests/r1/nyr"));
				addMusicItem(1, 3, Material.MUSIC_DISC_FAR, "In Memoriam", Location.KAUL, "Farr", "Whitebeard_OP", "epic:music.farr", 156, checkAdvance("monumenta:quests/r1/farr"));
				addMusicItem(1, 4, Material.MUSIC_DISC_MALL, "Silent Shanty", Location.MIDBLUE, "Lowtide", "TinyBoundler", "epic:music.lowtide", 85, checkAdvance("monumenta:quests/r1/lowtide"));
				addMusicItem(1, 5, Material.MUSIC_DISC_CHIRP, "Eastern Wardens", Location.RUSH, "Highwatch", "Whitebeard_OP", "epic:music.highwatch", 209, checkAdvance("monumenta:quests/r1/highwatch"));
				addMusicItem(1, 6, Material.MUSIC_DISC_WAIT, "Trading Waves", Location.LIGHTBLUE, "Oceangate", "Michael228p", "epic:music.oceangate", 207, checkAdvance("monumenta:quests/r1/oceangate"));
				addMusicItem(1, 7, Material.MUSIC_DISC_STRAD, "A Haven in Stone", Location.WHITE, "Ta'Eldim", "Whitebeard_OP", "epic:music.eldim", 146, checkAdvance("monumenta:quests/r1/taeldim"));

				addMusicItem(2, 1, Material.MUSIC_DISC_WARD, "Brothers of the Jungle", Location.VERDANT, "Verdant Remnants", "CmdrGod", "epic:music.vrsanctum", 366, checkScore("Quest21", 20));
				addMusicItem(2, 2, Material.MUSIC_DISC_WARD, "Cry of the Remnants", Location.VERDANT, "R'Kitxet", "CmdrGod", "epic:music.vrboss", 239, checkScore("Verdant"));
				addMusicItem(2, 3, Material.MUSIC_DISC_FAR, "Soulbinder", Location.SANCTUM, "C'Shura", "Whitebeard_OP", "epic:music.cshura", 230, checkScore("Sanctum"));
				addMusicItem(2, 5, Material.MUSIC_DISC_CHIRP, "Soulspeaker", Location.REVERIE, "C'Axtal", "Whitebeard_OP", "epic:music.caxtal", 151, checkScore("Corrupted"));
				addMusicItem(2, 6, Material.MUSIC_DISC_BLOCKS, "Tenebrae", Location.AZACOR, "Azacor", "Whitebeard_OP", "epic:music.azacor", 251, checkScore("AzacorNormalWin") || checkScore("AzacorHardWin"));
				addMusicItem(2, 7, Material.MUSIC_DISC_CAT, "Heart of the Jungle", Location.KAUL, "Kaul", "Whitebeard_OP", "epic:music.kaul", 220, checkScore("KaulWins"));

				addMusicItem(3, 1, Material.MUSIC_DISC_PIGSTEP, "Realm of Fate", Location.AMBER, "King's Valley", "Corpe_", "epic:music.valley", 174, true);
				addMusicItem(3, 2, Material.MUSIC_DISC_FAR, "Jovial Jaunt", TextColor.fromHexString("d1e05c"), "Racing Theme", "Whitebeard_OP", "epic:music.racegeneric", 113, true);
				addMusicItem(3, 3, Material.MUSIC_DISC_MELLOHI, "Endless Tactics", Location.BLITZ, "Blitz", "Whitebeard_OP", "epic:music.blitz", 118, checkScore("Blitz", 10));

				addMusicItem(3, 5, Material.MUSIC_DISC_13, "Sacred Temple", Location.AMBER, "The Monument", "Whitebeard_OP", "epic:music.monument", 244, checkAdvance("monumenta:handbook/important_sites/r1/monument"));
				addMusicItem(3, 6, Material.MUSIC_DISC_5, "Unknown Anomaly", Location.STARPOINT, "All That Remains", "CmdrGod", "epic:music.unknownatr", 180, checkScore("Quest54", 36));
			}

			case ISLES -> {
				addMusicItem(1, 1, Material.MUSIC_DISC_WAIT, "For Queen and Country", Location.FROSTGIANT, "Frostgate", "BobbyJonesSr", "epic:music.frostgate", 243, checkAdvance("monumenta:quests/r2/frostgate"));
				addMusicItem(1, 2, Material.MUSIC_DISC_MALL, "Frigid and Forlorn", Location.FROSTGIANT, "Nightroost", "BobbyJonesSr", "epic:music.nightroost", 215, checkAdvance("monumenta:quests/r2/nightroost"));
				addMusicItem(1, 3, Material.MUSIC_DISC_WAIT, "Snowflake's Refuge", Location.FROSTGIANT, "Wispervale", "Casiel368", "epic:music.wispervale", 223, checkAdvance("monumenta:quests/r2/wispervale"));

				addMusicItem(1, 5, Material.MUSIC_DISC_13, "For Gold and Glory", Location.AMBER, "Alnera", "BobbyJonesSr", "epic:music.alnera", 265, checkAdvance("monumenta:quests/r2/alnera"));
				addMusicItem(1, 6, Material.MUSIC_DISC_BLOCKS, "Arid and Adrift", Location.RUSH, "Rahkeri", "BobbyJonesSr", "epic:music.rahkeri", 222, checkAdvance("monumenta:quests/r2/rahkeri"));
				addMusicItem(1, 7, Material.MUSIC_DISC_CHIRP, "Ember's Refuge", Location.RUSH, "Molta", "Whitebeard_OP", "epic:music.molta", 162, checkAdvance("monumenta:quests/r2/molta"));

				addMusicItem(2, 1, Material.MUSIC_DISC_STRAD, "Through the Fog", Location.WHITE, "Mistport", "Casiel368", "epic:music.mistport", 231, checkAdvance("monumenta:quests/r2/mistport"));
				addMusicItem(2, 2, Material.MUSIC_DISC_MELLOHI, "Aquatic Antiquity", NamedTextColor.DARK_PURPLE, "Steelmeld", "BobbyJonesSr", "epic:music.steelmeld", 264, checkAdvance("monumenta:quests/r2/steelmeld"));
				addMusicItem(2, 3, Material.MUSIC_DISC_STAL, "Requiem of the Abyss", Location.DEPTHS, "Breachpoint", "Whitebeard_OP", "epic:music.breachpoint", 159, checkAdvance("monumenta:quests/r2/breachpoint"));

				addMusicItem(2, 5, Material.MUSIC_DISC_PIGSTEP, "The Celsian Isles", Location.AMBER, "Celsian Isles", "Corpe_", "epic:music.isles", 90, true);
				addMusicItem(2, 6, Material.MUSIC_DISC_13, "Sacred Temple - Steelmeld", Location.AMBER, "Celsian Monument", "Whitebeard_OP", "epic:music.monumentisles", 246, checkAdvance("monumenta:handbook/important_sites/r2/monument"));

				addMusicItem(3, 1, Material.MUSIC_DISC_BLOCKS, "Horsey", Location.HORSEMAN, "Headless Horseman", "Whitebeard_OP", "epic:music.horseman", 187, checkScore("HorsemanWins"));
				addMusicItem(3, 2, Material.MUSIC_DISC_STRAD, "Colossus' Song", Location.FROSTGIANT, "Eldrask", "BobbyJonesSr", "epic:music.eldrask", 246, checkScore("FGWins"));
				addMusicItem(3, 3, Material.MUSIC_DISC_13, "Dies Irae", Location.LICH, "Hekawt", "Whitebeard_OP", Lich.MUSIC_TITLE, Lich.MUSIC_DURATION, checkScore("LichWins"));
				addMusicItem(3, 4, Material.MUSIC_DISC_13, "Growth and Decay", Location.LICH, "Hekawt", "Michael228p", Lich.MUSIC_TITLE_P4, Lich.MUSIC_DURATION_P4, checkScore("LichWins"));
				addMusicItem(3, 5, Material.MUSIC_DISC_MELLOHI, "Lethal Collapse", Location.TEAL, "Echoes of Oblivion", "Casiel368", "epic:music.echorun", 131, checkScore("Teal"));
				addMusicItem(3, 6, Material.MUSIC_DISC_FAR, "Endless Emptiness", Location.FORUM, "False Spirit", "Whitebeard_OP", "epic:music.falsespirit", 210, checkScore("Forum"));
				addMusicItem(3, 7, Material.MUSIC_DISC_STAL, "Scourge of the Mist", Location.MIST, "Varcosa", "BobbyJonesSr", "epic:music.varcosa", 197, checkScore("MistClears"));

				addMusicItem(4, 2, Material.MUSIC_DISC_STRAD, "In the City of Bones...", Location.REMORSE, "Sealed Remorse", "CmdrGod", "epic:music.srbones", 151.6, checkScore("SealedRemorse"));
				addMusicItem(4, 3, Material.MUSIC_DISC_STRAD, "In the Pit of Ashes...", Location.REMORSE, "Sealed Remorse", "CmdrGod", "epic:music.srash", 66, checkScore("SealedRemorse"));
				addMusicItem(4, 4, Material.MUSIC_DISC_BLOCKS, "There Stood Two Broers...", Location.REMORSE, "Svalgot and Ghalkor", "CmdrGod", "epic:music.srbroers", 137.5, checkScore("SealedRemorse"));
				addMusicItem(4, 5, Material.MUSIC_DISC_STAL, "There it Slumbered... Waiting. The Beast.", Location.REMORSE, "Beast of the Blackflame", "CmdrGod", "epic:music.srbeast", 152.8, checkScore("SealedRemorse"));
				addMusicItem(4, 6, Material.MUSIC_DISC_STRAD, "Silver Prayer", Location.VIGIL, "Vigil", "Caecillius", "epic:music.vigil", 128, checkAdvance("monumenta:challenges/r2/sr/vigil"));

				addMusicItem(5, 3, Material.MUSIC_DISC_CAT, "Herbal Charmer", Location.DEPTHS, "Hedera", "Casiel368", Hedera.MUSIC_TITLE, Hedera.MUSIC_DURATION, checkScore("DepthsEndless", 11));
				addMusicItem(5, 4, Material.MUSIC_DISC_MALL, "Duel in the Depths", Location.DEPTHS, "Davey", "Xernial", Davey.MUSIC_TITLE, Davey.MUSIC_DURATION, checkScore("DepthsEndless", 21));
				addMusicItem(5, 5, Material.MUSIC_DISC_STAL, "Core Tenebris", Location.DEPTHS, "Gyrhaeddant", "Whitebeard_OP", Nucleus.MUSIC_TITLE, Nucleus.MUSIC_DURATION, checkScore("Depths"));
			}

			case RING -> {
				addMusicItem(1, 1, Material.MUSIC_DISC_FAR, "Those Who Survived", Location.FOREST, "Galengarde", "Whitebeard_OP", "epic:music.galengarde", 189, checkAdvance("monumenta:quests/r3/galengarde"));
				addMusicItem(1, 2, Material.MUSIC_DISC_STAL, "Argentum Lament", Location.KEEP, "New Antium", "Whitebeard_OP", "epic:music.newantium", 205, checkAdvance("monumenta:quests/r3/newantium"));
				addMusicItem(1, 3, Material.MUSIC_DISC_MALL, "Resting Among the Stars", Location.STARPOINT, "Chantry of Repentance", "Xernial", "epic:music.chantry", 159, checkAdvance("monumenta:quests/r3/chantryofrepentance"));

				addMusicItem(1, 5, Material.MUSIC_DISC_BLOCKS, "The Architect's Ring", TextColor.fromHexString("ff8f26"), "The Architect's Ring", "Corpe_", "epic:music.ring", 187, checkScore("R3Access"));
				addMusicItem(1, 6, Material.MUSIC_DISC_MALL, "Into the Star Verse", Location.STARPOINT, "Star Point", "CmdrGod", "epic:music.starpoint", 215, checkScore("R3Access"));

				addMusicItem(2, 2, Material.MUSIC_DISC_WAIT, "Reckoning", Location.BLUESTRIKE, "Samwell", "CmdrGod", "epic:music.samwell", 267, checkScore("MasqueradersRuin"));
				addMusicItem(2, 3, Material.MUSIC_DISC_STRAD, "Misanthropic Circuitry", Location.SCIENCE, "Iota", "Okaye", "epic:music.misanthropic_circuitry", 234, checkScore("Portal"));
				addMusicItem(2, 5, Material.MUSIC_DISC_MALL, "Cataclysm From the Stars", Location.SIRIUS, "Sirius", "CmdrGod", "epic:music.sirius", 319, checkScore("SiriusWins"));
				addMusicItem(2, 6, Material.MUSIC_DISC_CHIRP, "Emofobia", Location.SANGUINEHALLS, "Sanguine Halls", "Whitebeard_OP", "epic:music.gallerysanguineee", 195, checkScore("GallerySanguineHallsEasterEgg"));

				addMusicItem(3, 1, Material.MUSIC_DISC_CHIRP, "Into the Hive", Location.ZENITH, "Broodmother", "CmdrGod", Broodmother.MUSIC_TITLE_AMBIENT, Broodmother.MUSIC_DURATION_AMBIENT, checkScore("Zenith"));
				addMusicItem(3, 2, Material.MUSIC_DISC_CHIRP, "The Broodmother's Glory", Location.ZENITH, "Broodmother", "CmdrGod", Broodmother.MUSIC_TITLE, Broodmother.MUSIC_DURATION, checkScore("Zenith"));
				addMusicItem(3, 3, Material.MUSIC_DISC_CHIRP, "Death of a Hivemind", Location.ZENITH, "Broodmother", "CmdrGod", Broodmother.MUSIC_TITLE_2, Broodmother.MUSIC_DURATION_2, checkScore("Zenith"));
				addMusicItem(3, 5, Material.MUSIC_DISC_5, "Sermon of the Cosmos", Location.ZENITH, "Vesperidys", "CmdrGod", Vesperidys.MUSIC_TITLE_AMBIENT, Broodmother.MUSIC_DURATION_AMBIENT, checkScore("Zenith"));
				addMusicItem(3, 6, Material.MUSIC_DISC_5, "Subjugation of the Stars", Location.ZENITH, "Vesperidys", "CmdrGod", Vesperidys.MUSIC_TITLE, Broodmother.MUSIC_DURATION, checkScore("Zenith"));
				addMusicItem(3, 7, Material.MUSIC_DISC_5, "Supernova Slayer", Location.ZENITH, "Vesperidys", "CmdrGod", Vesperidys.MUSIC_TITLE_2, Broodmother.MUSIC_DURATION_2, checkScore("Zenith"));
			}

			case DUNGEONS -> {
				addDungeonItem(1, 2, Material.WHITE_WOOL, Location.WHITE, MusicPage.WHITE, "White");
				addDungeonItem(1, 3, Material.ORANGE_WOOL, Location.ORANGE, MusicPage.ORANGE, "Orange");
				addDungeonItem(1, 4, Material.MAGENTA_WOOL, Location.MAGENTA, MusicPage.MAGENTA, "Magenta");
				addDungeonItem(1, 5, Material.LIGHT_BLUE_WOOL, Location.LIGHTBLUE, MusicPage.LIGHTBLUE, "LightBlue");
				addDungeonItem(1, 6, Material.YELLOW_WOOL, Location.YELLOW, MusicPage.YELLOW, "Yellow");

				addDungeonItem(2, 2, Material.GLASS_BOTTLE, Location.LABS, MusicPage.LABS, "Labs");
				addDungeonItem(2, 3, Material.JUNGLE_LEAVES, Location.WILLOWS, MusicPage.WILLOWS, "R1Bonus");
				addDungeonItem(2, 4, Material.FIRE_CORAL, Location.REVERIE, MusicPage.REVERIE, "Corrupted");
				addDungeonItem(2, 5, Material.BLUE_WOOL, Location.BLUE, MusicPage.BLUE, "Blue");
				addDungeonItem(2, 6, Material.BROWN_WOOL, Location.BROWN, MusicPage.BROWN, "Brown");
			}

			case LABS -> {
				addMusicItem(1, 3, Material.MUSIC_DISC_CAT, "Caustic Cantor 1", Location.LABS, "Okaye", "epic:music.labs1", 134);
				addMusicItem(1, 5, Material.MUSIC_DISC_CAT, "Caustic Cantor 2", Location.LABS, "Okaye", "epic:music.labs2", 142);
			}

			case WHITE -> {
				addMusicItem(1, 3, Material.MUSIC_DISC_STAL, "Halls", Location.WHITE, "Okaye", "epic:music.white1", 75);
				addMusicItem(1, 4, Material.MUSIC_DISC_STRAD, "Wind", Location.WHITE, "Okaye", "epic:music.white2", 50);
				addMusicItem(1, 5, Material.MUSIC_DISC_CHIRP, "Blood", Location.WHITE, "Okaye", "epic:music.white3", 50);
			}

			case ORANGE -> {
				addMusicItem(1, 2, Material.MUSIC_DISC_BLOCKS, "Beasts of the Jungle 1", Location.ORANGE, "Okaye", "epic:music.orange1", 115);
				addMusicItem(1, 3, Material.MUSIC_DISC_BLOCKS, "Beasts of the Jungle 2", Location.ORANGE, "Okaye", "epic:music.orange2", 94);
				addMusicItem(1, 4, Material.MUSIC_DISC_BLOCKS, "Beasts of the Jungle 3", Location.ORANGE, "Okaye", "epic:music.orange3", 56);
				addMusicItem(1, 6, Material.MUSIC_DISC_CHIRP, "The Final Stand", Location.ORANGE, "Okaye", "epic:music.orangefinal", 52);
			}

			case MAGENTA -> {
				addMusicItem(1, 3, Material.MUSIC_DISC_MELLOHI, "The Nine Totems 1", Location.MAGENTA, "Okaye", "epic:music.magenta1", 81);
				addMusicItem(1, 4, Material.MUSIC_DISC_MELLOHI, "The Nine Totems 2", Location.MAGENTA, "Okaye", "epic:music.magenta2", 43);
				addMusicItem(1, 5, Material.MUSIC_DISC_MELLOHI, "The Nine Totems 3", Location.MAGENTA, "Okaye", "epic:music.magenta3", 73);
			}

			case LIGHTBLUE -> {
				addMusicItem(1, 2, Material.MUSIC_DISC_WAIT, "Arcane Robbery 1", Location.LIGHTBLUE, "Okaye", "epic:music.lightblue1", 61);
				addMusicItem(1, 3, Material.MUSIC_DISC_WAIT, "Arcane Robbery 2", Location.LIGHTBLUE, "Okaye", "epic:music.lightblue2", 44);
				addMusicItem(1, 4, Material.MUSIC_DISC_WAIT, "Arcane Robbery 3", Location.LIGHTBLUE, "Okaye", "epic:music.lightblue3", 63);
				addMusicItem(1, 6, Material.MUSIC_DISC_OTHERSIDE, "Enter the Bastion", Location.LIGHTBLUE, "Okaye", "epic:music.lightbluebastion", 49);
			}

			case YELLOW -> {
				addMusicItem(1, 3, Material.MUSIC_DISC_13, "Far Below 1", Location.YELLOW, "Okaye", "epic:music.yellow1", 113);
				addMusicItem(1, 5, Material.MUSIC_DISC_13, "Far Below 2", Location.YELLOW, "Okaye", "epic:music.yellow2", 113);
			}

			case WILLOWS -> {
				addMusicItem(1, 3, Material.MUSIC_DISC_WARD, "The Wind in the Trees 1", Location.WILLOWS, "Okaye", "epic:music.willows1", 53);
				addMusicItem(1, 4, Material.MUSIC_DISC_WARD, "The Wind in the Trees 2", Location.WILLOWS, "Okaye", "epic:music.willows2", 70);
				addMusicItem(1, 5, Material.MUSIC_DISC_WARD, "Exploring the Veil", Location.WILLOWS, "Okaye", "epic:music.willows3", 89);
			}

			case REVERIE -> {
				addMusicItem(1, 3, Material.MUSIC_DISC_CHIRP, "He is Awake 1", Location.REVERIE, "Okaye", "epic:music.reverie1", 103);
				addMusicItem(1, 5, Material.MUSIC_DISC_CHIRP, "He is Awake 2", Location.REVERIE, "Okaye", "epic:music.reverie2", 97);
			}

			case BLUE -> {
				addMusicItem(1, 2, Material.MUSIC_DISC_STRAD, "Nalatia", Location.BLUE, "Okaye", "epic:music.blueair", 213);
				addMusicItem(1, 3, Material.MUSIC_DISC_BLOCKS, "Molldyer", Location.BLUE, "Okaye", "epic:music.bluefire", 212);
				addMusicItem(1, 5, Material.MUSIC_DISC_WAIT, "The Vedha", Location.BLUE, "Okaye", "epic:music.bluewater", 315);
				addMusicItem(1, 6, Material.MUSIC_DISC_WARD, "Grandmother Laurey", Location.BLUE, "Okaye", "epic:music.blueearth", 295);
			}

			case BROWN -> {
				addMusicItem(1, 3, Material.MUSIC_DISC_BLOCKS, "Pelias' Echo", Location.BROWN, "Okaye", "epic:music.brownfactory", 210);
				addMusicItem(1, 4, Material.MUSIC_DISC_MALL, "The Empty Cradle", Location.BROWN, "Okaye x Whitebeard_OP", "epic:music.brownmall", 157);
				addMusicItem(1, 5, Material.MUSIC_DISC_PIGSTEP, "Advanced Annihilation Mechanism", Location.BROWN, "Xernial x CmdrGod", "epic:music.mechapelias", 151);
			}

			case PLOTS -> {
				addMusicItem(1, 3, Material.MUSIC_DISC_OTHERSIDE, "Well-Deserved Rest", Location.AMBER, "Plots", "Casiel368", "epic:music.plots", 195, true);
				addMusicItem(1, 5, Material.MUSIC_DISC_PIGSTEP, "Custom Plots Music", Location.AMBER, "Plots", null, "epic:music.plotscustom", 240, true);
			}

			default -> close();
		}

		if (mReturnToRecordPlayer) {
			ItemStack back = GUIUtils.createBasicItem(Material.OBSERVER, "Back to Record Player menu", NamedTextColor.WHITE);
			setItem(0, back).onClick(event -> {
				close();
				try {
					Plugin.getInstance().mGuiManager.showGui("recordplayer", mPlayer, "main");
				} catch (WrapperCommandSyntaxException e) {
					MMLog.warning("Caught exception attempting to open recordplayer GUI:");
					e.printStackTrace();
				}
			});
		}
	}

	public void addVanillaMusicItem(int row, int col, Material material, String name, Sound track, double duration) {
		addMusicItem(row, col, material, name, NamedTextColor.WHITE, false, List.of(), track.key().asString(), duration, true);
	}

	public void addMusicItem(int row, int col, Material material, String name, Location location, @Nullable String composer, String track, double duration) {
		addMusicItem(row, col, material, name, location, location.getDisplayName(), composer, track, duration, true);
	}

	public void addMusicItem(int row, int col, Material material, String name, Location color, String location, @Nullable String composer, String track, double duration, boolean unlocked) {
		addMusicItem(row, col, material, name, color.getColor(), location, composer, track, duration, unlocked);
	}

	public void addMusicItem(int row, int col, Material material, String name, TextColor color, String location, @Nullable String composer, String track, double duration, boolean unlocked) {
		List<Component> lore = new ArrayList<>();
		lore.add(Component.text(location));
		if (composer != null) {
			lore.add(Component.text("Artist: " + composer, NamedTextColor.GRAY, TextDecoration.ITALIC));
		}

		addMusicItem(row, col, material, name, color, true, lore, track, duration, unlocked);
	}

	public void addMusicItem(int row, int col, Material material, String name, TextColor color, boolean bold, List<Component> lore, String track, double duration, boolean unlocked) {
		if (!unlocked) {
			setItem(row, col, GUIUtils.createBasicItem(Material.MUSIC_DISC_11, "Undiscovered Track", NamedTextColor.GRAY));
			return;
		}

		ItemStack item = GUIUtils.createBasicItem(material, 1, Component.text(name, color).decoration(TextDecoration.BOLD, bold).decoration(TextDecoration.ITALIC, false), lore, true);
		setItem(row, col, item).onClick(event -> {
			List<Player> players = mPlayToOthers ? PlayerUtils.playersInRange(mPlayer.getLocation(), 48, true) : List.of(mPlayer);
			SongManager.playSong(players, new SongManager.Song(track, SoundCategory.RECORDS, duration, true, 1.0f, 1.0f), true);
			close();
		});
	}

	public void addDungeonItem(int row, int col, Material material, Location location, MusicPage page, @Nullable String objective) {
		if (!checkScore(objective)) {
			setItem(row, col, GUIUtils.createBasicItem(Material.CRACKED_STONE_BRICKS, "Undiscovered Dungeon", NamedTextColor.GRAY));
			return;
		}
		ItemStack item = GUIUtils.createBasicItem(material, location.getDisplay());
		setItem(row, col, item).onClick(event -> setPage(page));
	}

	public void setPage(MusicPage page) {
		mPage = page;
		setSize(page.mSize);
		setTitle(Component.text(page.mTitle));
		update();
	}

	public boolean checkScore(@Nullable String objective) {
		return objective == null || checkScore(objective, 1);
	}

	public boolean checkScore(String objective, int min) {
		return ScoreboardUtils.getScoreboardValue(mPlayer, objective).orElse(0) >= min;
	}

	public boolean checkAdvance(String advancement) {
		return AdvancementUtils.checkAdvancement(mPlayer, advancement);
	}
}
