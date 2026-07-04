package com.weather;

public enum WeatherType
{
	AUTO("Automatic (Based on Biome)"),
	RAIN("Rain"),
	SNOW("Snow/Blizzard"),
	SANDSTORM("Sandstorm"),
	FOG("Swamp Fog"),
	ASHFALL("Volcanic Ashfall"),
	MAGICAL("Crystal Dust"),
	UNDERWATER("Bubbles"),
	AUTUMN("Falling Leaves"),
	ARCEUUS("Soul Dust"),
	COSMIC("Stardust"),
	CLEAR("Clear");

	private final String name;

	WeatherType(String name)
	{
		this.name = name;
	}

	@Override
	public String toString()
	{
		return name;
	}

	public static WeatherType fromBiome(WeatherBiome biome)
	{
		switch (biome)
		{
			case DESERT:
				return SANDSTORM;
			case TUNDRA:
				return SNOW;
			case SWAMP:
				return FOG;
			case VOLCANIC:
				return ASHFALL;
			case MAGICAL:
				return MAGICAL;
			case UNDERWATER:
				return UNDERWATER;
			case SAVANNAH:
				return AUTUMN;
			case ARCEUUS:
				return ARCEUUS;
			case COSMIC:
				return COSMIC;
			case JUNGLE:
			case TEMPERATE:
			default:
				return RAIN;
		}
	}
}
