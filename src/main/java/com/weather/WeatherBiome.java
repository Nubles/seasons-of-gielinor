package com.weather;

import net.runelite.api.coords.WorldPoint;

public enum WeatherBiome
{
	TEMPERATE,
	DESERT,
	TUNDRA,
	SWAMP,
	VOLCANIC,
	JUNGLE,
	MAGICAL,
	UNDERWATER,
	SAVANNAH,
	ARCEUUS,
	COSMIC;

	public static WeatherBiome fromWorldPoint(WorldPoint point)
	{
		if (point == null)
		{
			return TEMPERATE;
		}

		int x = point.getX();
		int y = point.getY();
		int regionID = point.getRegionID();

		// Cosmic / Abyss zones (The Abyss, Cosmic Altar, runecrafting planes)
		if (regionID == 12107 || regionID == 8523 || (x >= 3000 && x <= 3070 && y >= 4800 && y <= 4865))
		{
			return COSMIC;
		}

		// Arceuus / Necromantic zone (Arceuus House in Great Kourend)
		if (x >= 1600 && x <= 1850 && y >= 3700 && y <= 3900)
		{
			return ARCEUUS;
		}

		// Savannah / Autumn zones (Hosidius, Varlamore Savannah)
		if ((x >= 1600 && x <= 1850 && y >= 3450 && y <= 3650) || // Hosidius
			(x >= 1600 && x <= 1850 && y >= 2900 && y <= 3200))   // Varlamore Savannah
		{
			return SAVANNAH;
		}

		// Underwater (Fossil Island Underwater coordinates)
		if (x >= 3700 && x <= 3850 && y >= 10200 && y <= 10380)
		{
			return UNDERWATER;
		}

		// Magical / Crystal zones (Zanaris, Prifddinas)
		if (regionID == 12127 || 
			regionID == 12894 || regionID == 12895 || regionID == 13150 || regionID == 13151 ||
			(x >= 2100 && x <= 2300 && y >= 3200 && y <= 3450))
		{
			return MAGICAL;
		}

		// Desert area (Kharidian Desert)
		if (x >= 3150 && x <= 3500 && y >= 2700 && y <= 3130)
		{
			return DESERT;
		}

		// Tundra / Snow area (Rellekka, Troll Weiss, North Wilderness)
		if (y >= 3600 || regionID == 11158 || regionID == 11414)
		{
			return TUNDRA;
		}

		// Swamp area (Morytania)
		if (x >= 3400 && x <= 3600 && y >= 3150 && y <= 3500)
		{
			return SWAMP;
		}

		// Volcanic area (Mount Karuulm, Lava Maze, Wilderness Volcanic area)
		if ((x >= 1200 && x <= 1350 && y >= 3750 && y <= 3900) || // Karuulm
			(x >= 3150 && x <= 3300 && y >= 3800 && y <= 3950))   // Wilderness Volcano
		{
			return VOLCANIC;
		}

		// Jungle area (Karamja, Ape Atoll)
		if (x >= 2700 && x <= 2950 && y >= 2700 && y <= 3150)
		{
			return JUNGLE;
		}

		return TEMPERATE;
	}
}
