# Quickstart: Candy Rush Development

**Date**: 2025-10-20
**Feature**: Candy Rush - チーム対戦型ポイント収集ゲーム

このドキュメントは、開発環境のセットアップから最初の機能実装までの手順を記載します。

---

## Prerequisites

### Required Software

1. **Java Development Kit (JDK) 17+**
   ```bash
   java -version
   # java version "17.0.x" or later
   ```

2. **Maven or Gradle** (推奨: Maven 3.8+)
   ```bash
   mvn -version
   # Apache Maven 3.8.x or later
   ```

3. **IDE** (推奨: IntelliJ IDEA Community Edition)
   - JetBrains Toolbox: https://www.jetbrains.com/toolbox-app/
   - Eclipse with Buildship plugin も可

4. **Git**
   ```bash
   git --version
   ```

### Optional but Recommended

- **Paper Test Server**: ローカルテスト用
- **SQLite Browser**: DB確認用 (https://sqlitebrowser.org/)

---

## Project Setup

### 1. Create Maven Project

```bash
cd /Users/masafumi_t/Develop/hacklab/kikaku2/candy_rush2
```

`pom.xml` を作成:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.candyrush</groupId>
    <artifactId>candy-rush</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <name>CandyRush</name>
    <description>Team-based point collection game plugin for Minecraft</description>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
    </properties>

    <repositories>
        <!-- Paper API Repository -->
        <repository>
            <id>papermc</id>
            <url>https://repo.papermc.io/repository/maven-public/</url>
        </repository>
        <!-- MythicMobs Repository -->
        <repository>
            <id>mythiccraft</id>
            <url>https://mvn.lumine.io/repository/maven-public/</url>
        </repository>
    </repositories>

    <dependencies>
        <!-- Paper API -->
        <dependency>
            <groupId>io.papermc.paper</groupId>
            <artifactId>paper-api</artifactId>
            <version>1.19.4-R0.1-SNAPSHOT</version>
            <scope>provided</scope>
        </dependency>

        <!-- MythicMobs API -->
        <dependency>
            <groupId>io.lumine</groupId>
            <artifactId>Mythic-Dist</artifactId>
            <version>5.3.0</version>
            <scope>provided</scope>
        </dependency>

        <!-- SQLite JDBC -->
        <dependency>
            <groupId>org.xerial</groupId>
            <artifactId>sqlite-jdbc</artifactId>
            <version>3.44.1.0</version>
        </dependency>

        <!-- HikariCP (Connection Pooling) -->
        <dependency>
            <groupId>com.zaxxer</groupId>
            <artifactId>HikariCP</artifactId>
            <version>5.1.0</version>
        </dependency>

        <!-- JUnit 5 (Testing) -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
            <version>5.10.1</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-engine</artifactId>
            <version>5.10.1</version>
            <scope>test</scope>
        </dependency>

        <!-- MockBukkit (Testing) -->
        <dependency>
            <groupId>com.github.seeseemelk</groupId>
            <artifactId>MockBukkit-v1.19</artifactId>
            <version>3.9.0</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <finalName>${project.name}-${project.version}</finalName>
        <resources>
            <resource>
                <directory>src/main/resources</directory>
                <filtering>true</filtering>
            </resource>
        </resources>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>17</source>
                    <target>17</target>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.5.1</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <relocations>
                                <!-- Relocate HikariCP to avoid conflicts -->
                                <relocation>
                                    <pattern>com.zaxxer.hikari</pattern>
                                    <shadedPattern>com.candyrush.lib.hikari</shadedPattern>
                                </relocation>
                                <!-- Relocate SQLite JDBC -->
                                <relocation>
                                    <pattern>org.sqlite</pattern>
                                    <shadedPattern>com.candyrush.lib.sqlite</shadedPattern>
                                </relocation>
                            </relocations>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

### 2. Create Directory Structure

```bash
mkdir -p src/main/java/com/candyrush/{models,managers,listeners,commands,storage,integration,utils,tasks}
mkdir -p src/main/resources/mythicmobs/{Mobs,Skills,Items}
mkdir -p src/test/java/com/candyrush/{unit,integration}
```

### 3. Create plugin.yml

`src/main/resources/plugin.yml`:

```yaml
name: CandyRush
version: '${project.version}'
main: com.candyrush.CandyRushPlugin
api-version: '1.19'
description: Team-based point collection game
author: YourName
website: https://github.com/yourusername/candy-rush

depend:
  - MythicMobs

commands:
  stats:
    description: Show game statistics and rankings
    usage: /stats
    permission: candyrush.stats

permissions:
  candyrush.stats:
    description: Allows use of /stats command
    default: true
  candyrush.admin:
    description: Admin permissions
    default: op
```

### 4. Create config.yml

`src/main/resources/config.yml`:

```yaml
# ゲーム設定
game:
  min-players: 2
  countdown-seconds: 10
  cooldown-minutes: 5
  duration-minutes: 20
  map-radius: 250

# チーム設定
teams:
  colors:
    - RED
    - BLUE
    - GREEN
    - YELLOW

# 宝箱設定
treasure:
  per-chunk: 1
  trapped-chest-damage: 4.0
  trapped-chest-equipment-chance: 0.7
  respawn-delay-seconds: 60

# イベント設定
event:
  npc-per-chunks: 3
  raid-duration-seconds: 300
  escape-distance-chunks: 2
  boss-spawn-threshold: 3

# Murderer設定
murderer:
  duration-seconds: 600

# 天候・時間設定
world:
  weather: CLEAR
  auto-morning: true

# データベース設定
database:
  type: sqlite
  sqlite:
    file: "data.db"

# デバッグ設定
debug:
  enabled: false
  verbose-logging: false
```

---

## Development Workflow

### Step 1: Create Main Plugin Class

`src/main/java/com/candyrush/CandyRushPlugin.java`:

```java
package com.candyrush;

import org.bukkit.plugin.java.JavaPlugin;

public class CandyRushPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("CandyRush plugin enabled!");

        // Load configuration
        saveDefaultConfig();

        // Initialize managers
        // TODO: Implement manager initialization

        // Register listeners
        // TODO: Implement listener registration

        // Register commands
        // TODO: Implement command registration

        getLogger().info("CandyRush plugin initialization complete.");
    }

    @Override
    public void onDisable() {
        getLogger().info("CandyRush plugin disabled!");

        // Save all data
        // TODO: Implement data saving

        // Cleanup resources
        // TODO: Implement cleanup
    }
}
```

### Step 2: Build Project

```bash
mvn clean package
```

成功すると `target/CandyRush-1.0.0-SNAPSHOT.jar` が生成されます。

---

## Testing Setup

### Local Test Server Setup

1. **Paper サーバーをダウンロード**:
   ```bash
   mkdir test-server
   cd test-server
   wget https://api.papermc.io/v2/projects/paper/versions/1.19.4/builds/latest/downloads/paper-1.19.4-xxx.jar
   ```

2. **初回起動** (eula.txtを生成):
   ```bash
   java -Xmx2G -Xms1G -jar paper-1.19.4-xxx.jar --nogui
   # eula.txtを編集: eula=true に変更
   ```

3. **MythicMobs をインストール**:
   ```bash
   cd plugins
   wget https://mythiccraft.io/downloads/MythicMobs-5.3.0.jar
   cd ..
   ```

4. **CandyRush プラグインをコピー**:
   ```bash
   cp ../target/CandyRush-1.0.0-SNAPSHOT.jar plugins/
   ```

5. **サーバー起動**:
   ```bash
   java -Xmx2G -Xms1G -jar paper-1.19.4-xxx.jar --nogui
   ```

### Unit Test Example

`src/test/java/com/candyrush/unit/TeamManagerTest.java`:

```java
package com.candyrush.unit;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TeamManagerTest {

    @Test
    public void testTeamAssignment() {
        // TODO: Implement test
        assertTrue(true, "Placeholder test");
    }

    @Test
    public void testPointCalculation() {
        // TODO: Implement test
        assertEquals(10, 5 + 5, "Points should add correctly");
    }
}
```

Run tests:
```bash
mvn test
```

---

## IDE Setup (IntelliJ IDEA)

1. **プロジェクトをインポート**:
   - File → Open → `pom.xml` を選択
   - "Open as Project" を選択

2. **JDK設定**:
   - File → Project Structure → Project SDK: Java 17

3. **Maven自動リロード**:
   - View → Tool Windows → Maven
   - 右クリック → "Reload All Maven Projects"

4. **Run Configuration**:
   - Run → Edit Configurations → + → Maven
   - Name: "Package Plugin"
   - Command line: `clean package`

---

## First Feature Implementation

### Implement TeamManager (例)

1. **Enum定義**: `src/main/java/com/candyrush/models/TeamColor.java`

```java
package com.candyrush.models;

import net.kyori.adventure.text.format.NamedTextColor;

public enum TeamColor {
    RED(NamedTextColor.RED),
    BLUE(NamedTextColor.BLUE),
    GREEN(NamedTextColor.GREEN),
    YELLOW(NamedTextColor.YELLOW);

    private final NamedTextColor textColor;

    TeamColor(NamedTextColor textColor) {
        this.textColor = textColor;
    }

    public NamedTextColor getTextColor() {
        return textColor;
    }
}
```

2. **Manager実装**: `src/main/java/com/candyrush/managers/TeamManager.java`

```java
package com.candyrush.managers;

import com.candyrush.models.TeamColor;
import org.bukkit.entity.Player;

import java.util.*;

public class TeamManager {
    private final Map<UUID, TeamColor> playerTeams = new HashMap<>();
    private final Map<TeamColor, Integer> teamPoints = new EnumMap<>(TeamColor.class);
    private final Map<UUID, Integer> personalPoints = new HashMap<>();

    public TeamManager() {
        // Initialize team points
        for (TeamColor color : TeamColor.values()) {
            teamPoints.put(color, 0);
        }
    }

    public TeamColor assignTeam(Player player) {
        // Find team with least players
        TeamColor assigned = Arrays.stream(TeamColor.values())
            .min(Comparator.comparingInt(this::getTeamSize))
            .orElse(TeamColor.RED);

        playerTeams.put(player.getUniqueId(), assigned);
        return assigned;
    }

    private int getTeamSize(TeamColor team) {
        return (int) playerTeams.values().stream()
            .filter(t -> t == team)
            .count();
    }

    public void addPersonalPoints(Player player, int points) {
        if (points < 0) throw new IllegalArgumentException("Points must be positive");
        personalPoints.merge(player.getUniqueId(), points, Integer::sum);
    }

    public void addTeamPoints(TeamColor team, int points) {
        if (points < 0) throw new IllegalArgumentException("Points must be positive");
        teamPoints.merge(team, points, Integer::sum);
    }

    // ... 他のメソッド実装
}
```

3. **テスト実装**: `src/test/java/com/candyrush/unit/TeamManagerTest.java`

```java
package com.candyrush.unit;

import com.candyrush.managers.TeamManager;
import com.candyrush.models.TeamColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TeamManagerTest {
    private TeamManager teamManager;

    @BeforeEach
    public void setup() {
        teamManager = new TeamManager();
    }

    @Test
    public void testAddPoints() {
        teamManager.addTeamPoints(TeamColor.RED, 10);
        assertEquals(10, teamManager.getTeamPoints(TeamColor.RED));
    }

    @Test
    public void testNegativePointsThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            teamManager.addTeamPoints(TeamColor.BLUE, -5);
        });
    }
}
```

---

## Development Best Practices

1. **TDD (Test-Driven Development)**:
   - テストを先に書く
   - テストが失敗することを確認
   - 最小限の実装でテストをパス
   - リファクタリング

2. **Commit頻度**:
   - 機能単位で細かくコミット
   - 意味のあるコミットメッセージ

3. **Code Review**:
   - Pull Request作成前に自己レビュー
   - 契約（contracts/）に準拠しているか確認

4. **Performance**:
   - メインスレッドをブロックしない
   - DB I/Oは非同期実行

---

## Next Steps

1. **Phase 2: タスク生成**:
   ```bash
   /speckit.tasks
   ```

2. **実装開始**:
   - `tasks.md` の Task 1 から順に実装
   - 各タスク完了後、テスト実行とコミット

3. **定期的なビルド確認**:
   ```bash
   mvn clean test package
   ```

---

## Troubleshooting

### Maven依存関係の問題

```bash
mvn dependency:tree
mvn clean install -U
```

### プラグインがロードされない

- `plugin.yml`の`main`クラスパスを確認
- `api-version`が正しいか確認
- Paper サーバーログで詳細エラーを確認

### MythicMobs連携エラー

- MythicMobs APIが`provided` scopeになっているか確認
- サーバーにMythicMobsプラグインがインストールされているか確認

---

**Quickstart Complete**: 開発環境セットアップ完了。実装フェーズに進めます。
