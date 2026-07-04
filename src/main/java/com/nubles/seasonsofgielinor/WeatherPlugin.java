package com.nubles.seasonsofgielinor;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Dynamic Weather",
	description = "Adds immersive dynamic weather particles and day/night screen tint filters based on biome and local time",
	tags = {"weather", "ambient", "overlay", "immersion", "graphics", "skybox"},
	enabledByDefault = true
)
public class WeatherPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private WeatherConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private WeatherOverlay overlay;

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
	}

	@Provides
	WeatherConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WeatherConfig.class);
	}

	public WeatherType getCurrentWeather()
	{
		if (config.enableIndoorHiding() && isIndoors())
		{
			return WeatherType.CLEAR;
		}

		if (config.weatherType() != WeatherType.AUTO)
		{
			return config.weatherType();
		}

		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return WeatherType.CLEAR;
		}

		WeatherBiome biome = WeatherBiome.fromWorldPoint(localPlayer.getWorldLocation());
		return WeatherType.fromBiome(biome);
	}

	public boolean isIndoors()
	{
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return false;
		}

		WorldPoint location = localPlayer.getWorldLocation();

		// Underground check (OSRS coordinates map almost all underground areas to Y >= 9000)
		if (location.getY() >= 9000)
		{
			return true;
		}

		// Upper floors of buildings
		if (client.getPlane() > 0)
		{
			return true;
		}

		// Instanced dungeons/caves/boss rooms
		if (client.isInInstancedRegion())
		{
			return true;
		}

		return false;
	}
}
