package com.nubles.seasonsofgielinor;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class WeatherPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(WeatherPlugin.class);
		RuneLite.main(args);
	}
}
