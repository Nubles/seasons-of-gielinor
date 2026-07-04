package com.nubles.seasonsofgielinor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class WeatherOverlay extends Overlay
{
	private final Client client;
	private final WeatherPlugin plugin;
	private final WeatherConfig config;
	private final Random random = new Random();

	private final List<Particle> particles = new ArrayList<>();
	private long lastFrameTime = System.currentTimeMillis();

	// Transition tracking variables
	private WeatherType lastWeather = WeatherType.CLEAR;
	private WeatherType targetWeather = WeatherType.CLEAR;
	private float transitionProgress = 1.0f; // 0.0 to 1.0
	private static final float TRANSITION_DURATION = 3.0f; // 3 seconds transition

	// Lightning variables
	private float lightningIntensity = 0.0f;
	private float nextLightningTime = 5.0f; // Seconds until next lightning strike
	private int lightningFlickerStage = 0; // 0 = idle, 1 = first peak decay, 2 = second peak decay

	private static class Particle
	{
		float x, y;
		float vx, vy;
		float size;
		float alpha;
		Color color;
		float life;
		float maxLife;
		float swayOffset;
		WeatherType weatherType;
	}

	@Inject
	public WeatherOverlay(Client client, WeatherPlugin plugin, WeatherConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		int width = client.getCanvasWidth();
		int height = client.getCanvasHeight();
		if (width <= 0 || height <= 0)
		{
			return null;
		}

		// Calculate frame delta-time
		long nowTime = System.currentTimeMillis();
		float dt = (nowTime - lastFrameTime) / 1000.0f;
		lastFrameTime = nowTime;

		// Protect against large freezes/frame drops skewing physics
		if (dt > 0.1f)
		{
			dt = 0.1f;
		}

		WeatherType newWeather = plugin.getCurrentWeather();

		// Handle transition triggering
		if (newWeather != targetWeather)
		{
			lastWeather = targetWeather;
			targetWeather = newWeather;
			transitionProgress = 0.0f;
		}

		// Advance transition
		if (transitionProgress < 1.0f)
		{
			transitionProgress += dt / TRANSITION_DURATION;
			if (transitionProgress >= 1.0f)
			{
				transitionProgress = 1.0f;
				lastWeather = targetWeather;
			}
		}

		// Update Lightning state machine (only during RAIN)
		if (targetWeather == WeatherType.RAIN)
		{
			updateLightning(dt);
		}
		else
		{
			lightningIntensity = 0.0f;
			lightningFlickerStage = 0;
		}

		// Enable anti-aliasing for nice particle rendering
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// 1. Draw Full Screen Color Tint (Interpolated)
		Color finalTint = getFinalTint();
		if (finalTint.getAlpha() > 0)
		{
			graphics.setColor(finalTint);
			graphics.fillRect(0, 0, width, height);
		}

		// 2. Draw Lightning Flash
		if (lightningIntensity > 0.0f)
		{
			int alphaInt = (int) (lightningIntensity * 180); // cap alpha at 180 to avoid blinding
			graphics.setColor(new Color(245, 245, 255, alphaInt));
			graphics.fillRect(0, 0, width, height);
		}

		// 3. Draw Weather Particles
		if (targetWeather != WeatherType.CLEAR || lastWeather != WeatherType.CLEAR)
		{
			updateParticles(lastWeather, targetWeather, transitionProgress, width, height, dt);
			for (Particle p : particles)
			{
				drawParticle(graphics, p);
			}
		}
		else
		{
			particles.clear();
		}

		return null;
	}

	private void updateLightning(float dt)
	{
		switch (lightningFlickerStage)
		{
			case 0: // Idle, counting down to next flash
				nextLightningTime -= dt;
				if (nextLightningTime <= 0.0f)
				{
					lightningIntensity = 0.5f + random.nextFloat() * 0.4f;
					lightningFlickerStage = 1;
				}
				break;
			case 1: // First Peak decaying
				lightningIntensity -= dt * 6.0f;
				if (lightningIntensity <= 0.2f)
				{
					lightningIntensity = 0.3f + random.nextFloat() * 0.3f;
					lightningFlickerStage = 2;
				}
				break;
			case 2: // Second Peak decaying
				lightningIntensity -= dt * 4.0f;
				if (lightningIntensity <= 0.0f)
				{
					lightningIntensity = 0.0f;
					lightningFlickerStage = 0;
					nextLightningTime = 8.0f + random.nextFloat() * 16.0f; // 8-24 seconds delay
				}
				break;
		}
	}

	private Color getFinalTint()
	{
		Color lastTint = getWeatherTint(lastWeather);
		Color nextTint = getWeatherTint(targetWeather);
		Color weatherTint = blendColor(lastTint, nextTint, transitionProgress);

		Color timeTint = config.enableDayNight() ? getDayNightTint(config.timeHourOffset()) : new Color(0, 0, 0, 0);

		// Blend time tint and weather tint
		float weatherAlphaRatio = weatherTint.getAlpha() / 255.0f;
		return blendColor(timeTint, weatherTint, weatherAlphaRatio);
	}

	private Color getWeatherTint(WeatherType weather)
	{
		switch (weather)
		{
			case RAIN:
				return new Color(30, 40, 60, 40); // Soft dark gray-blue
			case SNOW:
				return new Color(200, 220, 245, 30); // Cool frost tint
			case SANDSTORM:
				return new Color(195, 130, 50, 55); // Desert gold/sand tint
			case FOG:
				return new Color(40, 60, 45, 60); // Spooky green-gray swamp fog
			case ASHFALL:
				return new Color(80, 20, 10, 50); // Volcanic red/orange tint
			case MAGICAL:
				return new Color(130, 40, 150, 30); // Mystical violet tint
			case UNDERWATER:
				return new Color(10, 80, 130, 75); // Deep aquamarine/blue tint
			case AUTUMN:
				return new Color(230, 160, 50, 20); // Warm golden afternoon tint
			case ARCEUUS:
				return new Color(75, 20, 110, 45); // Mysterious dark purple/necromantic tint
			case COSMIC:
				return new Color(5, 5, 25, 60); // Deep space indigo/black tint
			case CLEAR:
			default:
				return new Color(0, 0, 0, 0);
		}
	}

	private Color getDayNightTint(int offsetHours)
	{
		LocalTime now = LocalTime.now().plusHours(offsetHours);
		int hour = now.getHour();
		int minute = now.getMinute();
		float timeFraction = (hour + minute / 60.0f) / 24.0f;

		// 00:00 - 05:00 Night
		if (timeFraction < 0.2083f)
		{
			return new Color(10, 15, 45, 50);
		}
		// 05:00 - 08:00 Sunrise
		else if (timeFraction < 0.3333f)
		{
			float t = (timeFraction - 0.2083f) / (0.3333f - 0.2083f);
			return blendColor(new Color(10, 15, 45, 50), new Color(220, 100, 50, 30), t);
		}
		// 08:00 - 17:00 Day
		else if (timeFraction < 0.7083f)
		{
			float t = (timeFraction - 0.3333f) / (0.7083f - 0.3333f);
			return blendColor(new Color(220, 100, 50, 30), new Color(255, 230, 200, 5), t);
		}
		// 17:00 - 20:00 Sunset
		else if (timeFraction < 0.8333f)
		{
			float t = (timeFraction - 0.7083f) / (0.8333f - 0.7083f);
			return blendColor(new Color(255, 230, 200, 5), new Color(150, 50, 100, 40), t);
		}
		// 20:00 - 24:00 transition to Night
		else
		{
			float t = (timeFraction - 0.8333f) / (1.0f - 0.8333f);
			return blendColor(new Color(150, 50, 100, 40), new Color(10, 15, 45, 50), t);
		}
	}

	private Color blendColor(Color c1, Color c2, float ratio)
	{
		int r = (int) (c1.getRed() * (1.0f - ratio) + c2.getRed() * ratio);
		int g = (int) (c1.getGreen() * (1.0f - ratio) + c2.getGreen() * ratio);
		int b = (int) (c1.getBlue() * (1.0f - ratio) + c2.getBlue() * ratio);
		int a = (int) (c1.getAlpha() * (1.0f - ratio) + c2.getAlpha() * ratio);
		return new Color(r, g, b, a);
	}

	private void updateParticles(WeatherType lastWeather, WeatherType targetWeather, float transitionProgress, int width, int height, float dt)
	{
		int maxParticles = config.particleCount();
		int speedMult = config.particleSpeed();

		int targetMax = (int) (maxParticles * transitionProgress);
		int lastMax = maxParticles - targetMax;

		int targetCount = 0;
		int lastCount = 0;

		for (Particle p : particles)
		{
			if (p.weatherType == targetWeather)
			{
				targetCount++;
			}
			else
			{
				lastCount++;
			}
		}

		for (int i = 0; i < particles.size(); i++)
		{
			Particle p = particles.get(i);
			p.life -= dt;

			if (p.life <= 0 || isOffScreen(p, width, height))
			{
				WeatherType newType;
				if (targetCount < targetMax)
				{
					newType = targetWeather;
					if (p.weatherType != targetWeather)
					{
						lastCount--;
						targetCount++;
					}
				}
				else if (lastCount < lastMax && lastWeather != WeatherType.CLEAR)
				{
					newType = lastWeather;
					if (p.weatherType != lastWeather)
					{
						targetCount--;
						lastCount++;
					}
				}
				else
				{
					newType = targetWeather;
					if (p.weatherType != targetWeather)
					{
						lastCount--;
						targetCount++;
					}
				}

				resetParticle(p, newType, width, height, false);
			}
			else
			{
				double yawRad = (client.getCameraYaw() / 16384.0) * 2.0 * Math.PI;
				float cameraWindOffset = (float) (Math.sin(yawRad) * getBaseWindStrength(p.weatherType));

				if (p.weatherType == WeatherType.SNOW || p.weatherType == WeatherType.MAGICAL || p.weatherType == WeatherType.UNDERWATER || p.weatherType == WeatherType.AUTUMN || p.weatherType == WeatherType.ARCEUUS || p.weatherType == WeatherType.COSMIC)
				{
					p.swayOffset += dt * 2.0f;
					p.x += (p.vx + cameraWindOffset + (float) Math.sin(p.swayOffset) * 15.0f) * dt * speedMult;
				}
				else
				{
					p.x += (p.vx + cameraWindOffset) * dt * speedMult;
				}
				p.y += p.vy * dt * speedMult;
			}
		}

		particles.removeIf(p -> p.weatherType != targetWeather && p.weatherType != lastWeather);

		if (particles.size() > maxParticles)
		{
			particles.subList(maxParticles, particles.size()).clear();
		}

		while (particles.size() < maxParticles)
		{
			WeatherType newType = (targetCount < targetMax) ? targetWeather : lastWeather;
			if (newType == WeatherType.CLEAR)
			{
				newType = targetWeather;
			}

			if (newType != WeatherType.CLEAR)
			{
				Particle p = new Particle();
				resetParticle(p, newType, width, height, true);
				particles.add(p);
				if (newType == targetWeather)
				{
					targetCount++;
				}
				else
				{
					lastCount++;
				}
			}
			else
			{
				break;
			}
		}
	}

	private float getBaseWindStrength(WeatherType weather)
	{
		switch (weather)
		{
			case RAIN:
				return 80.0f;
			case SNOW:
				return 50.0f;
			case SANDSTORM:
				return 200.0f;
			case FOG:
				return 15.0f;
			case ASHFALL:
				return 30.0f;
			case MAGICAL:
				return 10.0f;
			case UNDERWATER:
				return 5.0f;
			case AUTUMN:
				return 20.0f;
			case ARCEUUS:
				return 8.0f;
			case COSMIC:
				return 15.0f;
			case CLEAR:
			default:
				return 0.0f;
		}
	}

	private boolean isOffScreen(Particle p, int width, int height)
	{
		return p.x < -150 || p.x > width + 150 || p.y < -50 || p.y > height + 50;
	}

	private void resetParticle(Particle p, WeatherType weather, int width, int height, boolean fullScreenStart)
	{
		p.weatherType = weather;
		p.maxLife = 5.0f + random.nextFloat() * 5.0f;
		p.life = fullScreenStart ? random.nextFloat() * p.maxLife : p.maxLife;

		if (fullScreenStart)
		{
			p.x = random.nextFloat() * width;
			p.y = random.nextFloat() * height;
		}
		else
		{
			if (weather == WeatherType.SANDSTORM)
			{
				p.x = width + 10;
				p.y = random.nextFloat() * height;
			}
			else if (weather == WeatherType.FOG)
			{
				p.x = -150 - random.nextFloat() * 100;
				p.y = random.nextFloat() * height;
			}
			else if (weather == WeatherType.UNDERWATER || weather == WeatherType.ARCEUUS)
			{
				p.x = random.nextFloat() * width;
				p.y = height + 10;
			}
			else
			{
				p.x = random.nextFloat() * width;
				p.y = -10;
			}
		}

		switch (weather)
		{
			case RAIN:
				p.vx = -20 - random.nextFloat() * 20;
				p.vy = 250 + random.nextFloat() * 100;
				p.size = 1.0f + random.nextFloat() * 1.5f;
				p.alpha = 0.3f + random.nextFloat() * 0.4f;
				p.color = new Color(130, 170, 220);
				p.swayOffset = 0;
				break;
			case SNOW:
				p.vx = -10 - random.nextFloat() * 15;
				p.vy = 40 + random.nextFloat() * 40;
				p.size = 2.0f + random.nextFloat() * 3.0f;
				p.alpha = 0.5f + random.nextFloat() * 0.5f;
				p.color = Color.WHITE;
				p.swayOffset = random.nextFloat() * 100.0f;
				break;
			case SANDSTORM:
				p.vx = -300 - random.nextFloat() * 200;
				p.vy = 10 + random.nextFloat() * 30;
				p.size = 1.0f + random.nextFloat() * 2.0f;
				p.alpha = 0.2f + random.nextFloat() * 0.4f;
				p.color = new Color(210, 160, 90);
				p.swayOffset = 0;
				break;
			case FOG:
				p.vx = 5 + random.nextFloat() * 15;
				p.vy = -2 - random.nextFloat() * 3;
				p.size = 80.0f + random.nextFloat() * 120.0f;
				p.alpha = 0.03f + random.nextFloat() * 0.05f;
				p.color = new Color(180, 195, 185);
				p.swayOffset = 0;
				break;
			case ASHFALL:
				p.vx = -10 - random.nextFloat() * 20;
				p.vy = 20 + random.nextFloat() * 30;
				p.size = 1.5f + random.nextFloat() * 2.0f;
				p.alpha = 0.6f + random.nextFloat() * 0.4f;
				p.color = random.nextBoolean() ? new Color(255, 90, 30) : new Color(200, 50, 20);
				p.swayOffset = 0;
				break;
			case MAGICAL:
				p.vx = -5 - random.nextFloat() * 10;
				p.vy = 20 + random.nextFloat() * 15;
				p.size = 2.0f + random.nextFloat() * 2.5f;
				p.alpha = 0.4f + random.nextFloat() * 0.4f;
				p.color = Color.getHSBColor(random.nextFloat(), 0.5f, 0.95f);
				p.swayOffset = random.nextFloat() * 100.0f;
				break;
			case UNDERWATER:
				p.vx = -10 + random.nextFloat() * 20;
				p.vy = -60 - random.nextFloat() * 40;
				p.size = 4.0f + random.nextFloat() * 5.0f;
				p.alpha = 0.3f + random.nextFloat() * 0.4f;
				p.color = new Color(180, 225, 255);
				p.swayOffset = random.nextFloat() * 100.0f;
				break;
			case AUTUMN:
				p.vx = -30 - random.nextFloat() * 40;
				p.vy = 40 + random.nextFloat() * 30;
				p.size = 5.0f + random.nextFloat() * 5.0f;
				p.alpha = 0.5f + random.nextFloat() * 0.4f;
				int choice = random.nextInt(3);
				if (choice == 0)
				{
					p.color = new Color(210, 105, 30);
				}
				else if (choice == 1)
				{
					p.color = new Color(218, 165, 32);
				}
				else
				{
					p.color = new Color(178, 34, 34);
				}
				p.swayOffset = random.nextFloat() * 100.0f;
				break;
			case ARCEUUS:
				p.vx = -15 - random.nextFloat() * 15;
				p.vy = -30 - random.nextFloat() * 20;
				p.size = 2.0f + random.nextFloat() * 4.0f;
				p.alpha = 0.4f + random.nextFloat() * 0.4f;
				p.color = random.nextBoolean() ? new Color(180, 50, 255) : new Color(50, 230, 255);
				p.swayOffset = random.nextFloat() * 100.0f;
				break;
			case COSMIC:
				p.vx = -40 + random.nextFloat() * 80;
				p.vy = -20 + random.nextFloat() * 60;
				p.size = 1.5f + random.nextFloat() * 2.0f;
				p.alpha = 0.5f + random.nextFloat() * 0.5f;
				int starColor = random.nextInt(3);
				if (starColor == 0)
				{
					p.color = new Color(255, 255, 255); // white
				}
				else if (starColor == 1)
				{
					p.color = new Color(130, 230, 255); // cyan
				}
				else
				{
					p.color = new Color(255, 215, 0); // gold
				}
				p.swayOffset = random.nextFloat() * 100.0f;
				break;
			case CLEAR:
			default:
				p.vx = 0;
				p.vy = 0;
				p.size = 0;
				p.alpha = 0;
				p.color = new Color(0, 0, 0, 0);
				p.swayOffset = 0;
				break;
		}
	}

	private void drawParticle(Graphics2D g, Particle p)
	{
		WeatherType weather = p.weatherType;
		int alphaInt = (int) (p.alpha * 255);
		if (weather == WeatherType.FOG || weather == WeatherType.ASHFALL || weather == WeatherType.ARCEUUS)
		{
			float ratio = p.life / p.maxLife;
			float fade = ratio < 0.2f ? (ratio / 0.2f) : (ratio > 0.8f ? ((1.0f - ratio) / 0.2f) : 1.0f);
			alphaInt = (int) (p.alpha * fade * 255);
		}

		if (alphaInt <= 0) return;
		alphaInt = Math.max(0, Math.min(255, alphaInt));

		Color drawColor = new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), alphaInt);
		g.setColor(drawColor);

		double yawRad = (client.getCameraYaw() / 16384.0) * 2.0 * Math.PI;
		float cameraWindOffset = (float) (Math.sin(yawRad) * getBaseWindStrength(weather));

		if (weather == WeatherType.RAIN)
		{
			int x2 = (int) (p.x + (p.vx + cameraWindOffset) * 0.05f);
			int y2 = (int) (p.y + p.vy * 0.05f);
			g.drawLine((int) p.x, (int) p.y, x2, y2);
		}
		else if (weather == WeatherType.SANDSTORM)
		{
			int x2 = (int) (p.x + (p.vx + cameraWindOffset) * 0.02f);
			int y2 = (int) (p.y + p.vy * 0.02f);
			g.drawLine((int) p.x, (int) p.y, x2, y2);
		}
		else if (weather == WeatherType.FOG)
		{
			g.fillOval((int) (p.x - p.size / 2), (int) (p.y - p.size / 2), (int) p.size, (int) p.size);
		}
		else if (weather == WeatherType.MAGICAL)
		{
			int x = (int) p.x;
			int y = (int) p.y;
			int s = (int) p.size;
			g.drawLine(x - s, y, x + s, y);
			g.drawLine(x, y - s, x, y + s);
		}
		else if (weather == WeatherType.UNDERWATER)
		{
			// Bubble: semi-transparent fill + solid outline
			g.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), alphaInt / 3));
			g.fillOval((int) p.x, (int) p.y, (int) p.size, (int) p.size);
			g.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), alphaInt));
			g.drawOval((int) p.x, (int) p.y, (int) p.size, (int) p.size);
		}
		else if (weather == WeatherType.AUTUMN)
		{
			// Draw diamond leaf shape
			int x = (int) p.x;
			int y = (int) p.y;
			int s = (int) p.size;
			int[] xPoints = { x, x + s / 2, x, x - s / 2 };
			int[] yPoints = { y - s, y, y + s, y };
			g.fillPolygon(xPoints, yPoints, 4);
		}
		else if (weather == WeatherType.ARCEUUS)
		{
			// Soft glowing wisp circle
			g.fillOval((int) p.x, (int) p.y, (int) p.size, (int) p.size);
		}
		else if (weather == WeatherType.COSMIC)
		{
			int x = (int) p.x;
			int y = (int) p.y;
			int s = (int) p.size;
			// Draw small starry cross
			g.drawLine(x - s, y, x + s, y);
			g.drawLine(x, y - s, x, y + s);
		}
		else
		{
			g.fillOval((int) p.x, (int) p.y, (int) p.size, (int) p.size);
		}
	}
}
