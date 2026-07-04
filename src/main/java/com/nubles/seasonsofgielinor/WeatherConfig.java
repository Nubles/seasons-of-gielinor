package com.nubles.seasonsofgielinor;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("weatheroverlay")
public interface WeatherConfig extends Config
{
	@ConfigItem(
		keyName = "weatherType",
		name = "Weather Type",
		description = "Configure the visual weather overlay",
		position = 1
	)
	default WeatherType weatherType()
	{
		return WeatherType.AUTO;
	}

	@Range(min = 10, max = 500)
	@ConfigItem(
		keyName = "particleCount",
		name = "Particle Count",
		description = "Max number of active weather particles on screen",
		position = 2
	)
	default int particleCount()
	{
		return 150;
	}

	@ConfigItem(
		keyName = "enableDayNight",
		name = "Day/Night Cycle",
		description = "Apply screen color tint adjustments based on real-world local time",
		position = 3
	)
	default boolean enableDayNight()
	{
		return true;
	}

	@Range(min = -12, max = 12)
	@ConfigItem(
		keyName = "timeHourOffset",
		name = "Time Offset (Hours)",
		description = "Adjust the day/night cycle timing offset (useful to manually shift sunset/sunrise)",
		position = 4
	)
	default int timeHourOffset()
	{
		return 0;
	}

	@Range(min = 1, max = 5)
	@ConfigItem(
		keyName = "particleSpeed",
		name = "Wind/Particle Speed",
		description = "Speed multiplier for active particles",
		position = 5
	)
	default int particleSpeed()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "enableIndoorHiding",
		name = "Hide Weather Indoors",
		description = "Stop rendering weather effects when inside dungeons, caves, or building upper floors",
		position = 6
	)
	default boolean enableIndoorHiding()
	{
		return true;
	}
}
