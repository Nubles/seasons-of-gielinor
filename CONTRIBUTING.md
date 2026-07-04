# Contributing to Seasons of Gielinor

Thank you for your interest in contributing to **Seasons of Gielinor**! Here are the guidelines and instructions to get you started.

## Developer Setup

### Prerequisites
* Java 11 JDK
* An IDE (IntelliJ IDEA is highly recommended)

### Running in Development Mode
1. Clone this repository:
   ```bash
   git clone https://github.com/Nubles/seasons-of-gielinor.git
   ```
2. Open the project in IntelliJ IDEA.
3. Gradle will automatically sync and fetch the RuneLite client dependencies.
4. Locate the bootstrap file `src/test/java/com/weather/WeatherPluginTest.java`.
5. Right-click the file and select **Run 'WeatherPluginTest.main()'**.
6. The RuneLite client will launch in developer mode with the **Dynamic Weather** plugin enabled.

## Contribution Guidelines
1. Fork the repository and create a branch for your feature or bug fix.
2. Ensure your code conforms to standard Java code styling guidelines.
3. Keep dependency footprint minimal (only use standard RuneLite dependencies).
4. Open a Pull Request detailing the changes and how you verified them.
