# PORT-STATE

- 项目：Carpet TIS Addition v1.82.0  →  NeoForge 1.21.1
- 当前阶段：**S3（mixin 容器接入 + conditional-mixin 落地）— 已完成容器部分；24 条 mixin 元数据缺陷移交 S4**
- 阶段边界：只消 C 类（`net.fabricmc.*`），.java 仅动授权名单内 8 个 + 新建 1 个；未打开 `[[mixins]]`、未改 mixins.json
- S0 归档：导出扁平源码（见下方 §1~§5，已完成）｜S1 归档：骨架 + 依赖闭环（§1~§9）｜S2 归档：§10

---

## 1. 已完成

| # | 项 | 结果 |
|---|---|---|
| 1 | 源码获取 | `git -c core.compression=0 clone --depth 1 --branch v1.82.0 https://github.com/TISUnion/Carpet-TIS-Addition.git` → `D:\Minecraft-mod-development\Carpet-TIS-Addition`，HEAD `246edf9f9417a4c7df97683c15077ccdba6b3372`，`git describe --tags` = `v1.82.0` |
| 2 | 子工程确认 | `settings.json` 列 20 个版本含 `"1.21.1"`；`settings.gradle` 循环 `include(":$version")`，`projectDir = versions/$version`，`buildFileName = ../../common.gradle`；`common.gradle` 中 1.21.1（mcVersion=12101）→ `JavaVersion.VERSION_21` |
| 3 | 预处理任务 | `:1.21.1:preprocess` **不存在**（ambiguous）。`common.gradle` 由 `com.replaymod.preprocess:c5abb4fb12` 生成，真实任务名 `preprocessCode` / `preprocessResources`（`javap -p` 于 `PreprocessPlugin` 证实：`preprocess` + `Code`/`Resources`/`TestCode`/`TestResources`） |
| 4 | 执行命令 | `.\gradlew.bat --no-daemon --console=plain --configure-on-demand :1.21.1:preprocessCode :1.21.1:preprocessResources` → BUILD SUCCESSFUL（3m50s，JDK 21.0.11，日志 `_gradle_preprocess2.log`） |
| 5 | 产物定位 | `versions\1.21.1\build\preprocessed\main\java`（同时含展开源码 + `import net.minecraft.*` 共 2340 处）+ `versions\1.21.1\build\preprocessed\main\resources` |
| 6 | 扁平化处理 | `preprocessCode` 产物**并非**完全扁平：残留 `//#if //#else //#endif` 指令注释 3499 行 + 失效分支 `//$$ ...` 注释 1741 行（`PreprocessTask` 常量表：`//#if //#ifdef //#elseif //#else //#endif //$$`、`//#disable-remap //#enable-remap`）。已按 "删指令行 + 删 `//$$` 失效行" 处理（两者都是纯注释，删除不改变语义），共改动 386 个文件 |
| 7 | 源码落盘 | `src\main\java` = **1009** 个 .java（merged 1000 + `versions\1.21.1\src\main\java` 覆盖 9，无路径冲突） |
| 8 | 资源落盘 | `src\main\resources`：`assets\carpettisaddition\{icon.png, lang\en_us.yml, lang\zh_cn.yml, lang\meta\languages.json}` + `carpet-tis-addition.mixins.json`（5 个文件） |

### 扁平化规则（可复现）
```
删除 ^\s*//#(if|ifdef|ifndef|elseif|else|endif|define|include|disable-remap|enable-remap).*$
删除 ^\s*//\$\$.*$        # 预处理判定为失效的分支，原文件里就是注释
```
UTF-8（无 BOM）逐字节回写，其它内容零改动。

### 扁平化正确性验证
```
javac -proc:none -XDrawDiagnostics -encoding UTF-8 @_javac_files.txt
→ 语法类错误 0 条（expected / illegal start / not a statement / reached end of file / unclosed 全部 0）
→ 24732 行诊断全部是缺少 Minecraft/loom classpath 导致的符号解析错误：
   cant.resolve.location 7760 / doesnt.exist 5741 / cant.resolve 1450 / cant.resolve.location.args 171 ...
```
即：删掉 `//$$` 失效行后 1009 个文件的括号与语句结构仍然完整。

---

## 2. 验收证据（命令 + 原始输出）

### 2.1 文件数
```
PS> (Get-ChildItem D:\Minecraft-mod-development\TIS-NeoForge\src\main\java -Recurse -Filter *.java).Count
1009
```
**实际值 = 1009，低于任务书给出的 1180~1300 区间。** 差异原因（同一仓库实测，非拷贝缺失）：

```
PS> git ls-tree -r HEAD | rg '\.java$' | wc -l            # 全仓库（20 个版本目录）合计
1291
PS> ls Carpet-TIS-Addition/src/main/java -R *.java         # 主源码树
1009
PS> ls Carpet-TIS-Addition/src/test/java -R *.java         # 测试
2
PS> ls Carpet-TIS-Addition/versions -R *.java              # 20 个版本覆盖文件合计（其中 1.21.1 = 9）
280
```
`git ls-tree` 合计 1291（正是 1009+2+280，落在 1180~1300 内），而 **1.21.1 单个版本的扁平源码只能是 1009**：主源码树 1009 个文件里，有 9 个被 `versions\1.21.1\src\main\java` 的同路径文件覆盖，因此 `build\preprocessed\main\java` 只有 1000 个（缺的 9 个正是覆盖文件，已单独补入）。1180~1300 对应的是"全仓库 java 总数"，不是单版本扁平源码数。

### 2.2 预处理指令残留 = 0
```
PS> rg -n --no-heading '^//#|^\s*//#|//\$\$' D:\Minecraft-mod-development\TIS-NeoForge\src\main\java | Measure-Object
Count : 0
```

### 2.3 Mojmap 命名证据（3 个代表性文件）

`src\main\java\carpettisaddition\mixins\rule\optimizedFastEntityMovement\compat\lithium\EntityMixin.java`（版本覆盖文件，已扁平化）
```java
package carpettisaddition.mixins.rule.optimizedFastEntityMovement.compat.lithium;

import carpettisaddition.helpers.rule.optimizedFastEntityMovement.OFEMContext;
import carpettisaddition.helpers.rule.optimizedFastEntityMovement.OFEMUtil;
import carpettisaddition.utils.ModIds;
import carpettisaddition.utils.mixin.testers.LithiumEntityMovementOptimizationTester;
import com.google.common.collect.Lists;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.caffeinemc.mods.lithium.common.util.collections.LazyList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
```

`src\main\java\carpettisaddition\mixins\rule\tickCommandPermission\TickCommandMixin.java`
```java
package carpettisaddition.mixins.rule.tickCommandPermission;

import carpettisaddition.CarpetTISAdditionSettings;
import carpettisaddition.helpers.rule.tickCommandCarpetfied.TickCommandCarpetfiedRules;
import carpettisaddition.utils.CarpetModUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.TickCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
```

`src\main\java\carpettisaddition\mixins\logger\portalCreation\ServerPlayerEntityMixin.java`（1.21.1 上已无目标，走 DummyClass）
```java
package carpettisaddition.mixins.logger.portalCreation;

import carpettisaddition.utils.compat.DummyClass;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(DummyClass.class)
public abstract class ServerPlayerEntityMixin
{
	// used in mc [1.16, 1.21)
}
```
（三个文件的 1~19 行均为 LGPL 许可证头，故上方截取 package + import 段；`net.minecraft.world.entity.Entity` / `net.minecraft.commands.CommandSourceStack` / `net.minecraft.server.commands.TickCommand` 均为 Mojmap。全树 `import net.minecraft.` 2340 处；intermediary 名 23 处全部是 `@Inject(method = "method_NNNNN")` 这类 Fabric 侧的 lambda 数字名，属于上游原有写法，不是 obfuscated 类引用。）

---

## 3. 自检（三条铁律）

| 检查 | 命中 | 结论 |
|---|---|---|
| `rg -n "require\s*=\s*0" src` | **4** | ⚠️ **未达 0**，全部继承自上游 v1.82.0，本阶段未新增/未修改 |
| `rg -n "@Overwrite" src` | **2** | ⚠️ **未达 0**，同上（1 处是注释 1 处是注解，同一文件） |
| `rg -n "TODO\|FIXME\|UnsupportedOperationException" src` | **21** | ⚠️ **未达 0**，上游 22 处中 1 处位于 `//$$` 失效分支（已随扁平化消失） |

命中明细：
```
carpettisaddition\mixins\logger\microtiming\tickstages\scarpet\CarpetEventServer_EventMixin.java:41  (注释)
carpettisaddition\mixins\logger\microtiming\tickstages\scarpet\CarpetEventServer_EventMixin.java:44  require = 0
carpettisaddition\mixins\logger\microtiming\tickstages\scarpet\CarpetEventServer_EventMixin.java:50  require = 0
carpettisaddition\mixins\rule\voidRelatedAltitude\EntityMixin.java:43                            require = 0
carpettisaddition\mixins\helpers\mixin\IWorldMixin.java:42  (注释 "So here comes the @Overwrite")
carpettisaddition\mixins\helpers\mixin\IWorldMixin.java:46  @Overwrite
```
上游同源对照（证明非本阶段引入）：
```
rg -n 'require\s*=\s*0' versions/1.21.1/build/preprocessed/main/java versions/1.21.1/src/main/java  → 4
rg -n '@Overwrite'      同上                                                                       → 2
rg -n 'TODO|FIXME|UnsupportedOperationException' 同上                                              → 22
```
处置：本阶段为"只导出、不改 java"，且铁律 2 禁止新增 `@Overwrite` / 禁止注释掉 mixin / 禁止删功能消报错。这些命中是上游既有语义（`require = 0` 是上游刻意让 carpet 内部匿名类顺序变化时不崩溃；`IWorldMixin` 的 `@Overwrite` 是上游既有实现）。**未做任何删除或改写**，留待 S1+ 按铁律逐条决定（改 `@Inject` 重写或替换为 NeoForge 等价事件），届时再让自检归零。

---

## 4. 已知问题 / 偏差

1. **文件数 1009 ≠ 1180~1300**：见 2.1。1180~1300 是全仓库合计（1291），单版本扁平源码只有 1009。请确认验收口径。
2. **`carpettisaddition/translations/` 不存在**：v1.82.0 已改用 yamlang，多语言在 `assets\carpettisaddition\lang\*.yml`（`en_us.yml` 86KB、`zh_cn.yml` 82KB），已按实际路径拷贝。
3. **`fabric.mod.json` 未拷入 `src\main\resources`**（留在上游 `versions\1.21.1\build\preprocessed\main\resources\fabric.mod.json` 备查）：它是 Fabric 元数据，NeoForge 侧要用 `META-INF\neoforge.mods.toml`，混进资源根会被误读。`carpet-tis-addition.mixins.json` 已拷入（Mixin 配置在两平台通用，S1+ 需改 `refmap`/`plugin` 等字段）。
4. **9 个版本覆盖文件上游是独立 source root**，此处已与 merged 合并为单一扁平树；实测路径无冲突（merged 1000 个文件里不含这 9 个路径）。
5. **`--configure-on-demand` 对 `tasks --all` 无效**：`./gradlew :1.21.1:tasks --all` 会强制配置全部 20 个子工程并逐个 remap 源码（23 分钟），临时产物都已进 loom 缓存，勿重复执行。
6. 未建 Gradle 工程（遵守 S0 边界），因此**还没有 `build.gradle` / `settings.gradle` / `gradle.properties`**；`src\main\java` 目前不参与任何编译。

---

## 5. S0 下一步（已被 S1 取代）

~~S1：建 NeoForge 1.21.1 ModDevGradle 工程骨架~~ → **已于 S1 完成，见 §6。**

---

# S1（NeoForge 骨架 + 依赖闭环）— 已完成

## 6. S1 交付物

| # | 文件/目录 | 内容 |
|---|---|---|
| 1 | `gradlew.bat` / `gradlew` / `gradle\wrapper\{gradle-wrapper.jar,gradle-wrapper.properties}` | 从 `Carpet-TIS-Addition` 直拷，wrapper = gradle-9.5.1-bin（命中本机缓存，无下载） |
| 2 | `settings.gradle` | `pluginManagement { gradlePluginPortal(); mavenCentral() }`，`rootProject.name='Carpet-TIS-Addition-NeoForge-1.21.1'` |
| 3 | `gradle.properties` | `-Xmx4G -XX:MaxMetaspaceSize=1G`、`org.gradle.java.home=C:/Program Files/Java/jdk-21.0.11`、`systemProp.http(s).proxyHost/Port=127.0.0.1:7897`、`mod_version=1.82.0`、`minecraft_version=1.21.1`、`neoforge_version=21.1.209` |
| 4 | `build.gradle` | `java` + `net.neoforged.moddev 2.0.78`；`// TIS-SWITCH` 源集开关（`-PtisFull` → `src/main/java`，否则 `src/smoke/java`）；AT、mods 注册、runs、依赖见 §6.1 |
| 5 | `libs\` | `carpet.jar`(1498683B)、`conditional-mixin.jar`(30612B)、`lithium.jar`(862533B) |
| 6 | `src\smoke\java\carpettisaddition\neoforge\TISNeoForgeMod.java` | `@Mod("carpet_tis_addition")`，构造器只打 `[TISCM] neoforge bootstrap` |
| 7 | `src\main\resources\META-INF\neoforge.mods.toml` | javafml / `loaderVersion="[4,)"` / LGPL-3.0 / `modId="carpet_tis_addition"` / version 1.82.0 / 三条 required 依赖（neoforge/minecraft/carpet）；`[[mixins]]` 注释态 |
| 8 | `src\main\resources\META-INF\accesstransformer.cfg` | 对译上游 accesswidener（8 行，见 BLOCKERS.md §2.2） |
| 9 | `run\eula.txt` / `run\mods\carpet.jar` | 冒烟前置（carpet jar 自带 `neoforge.mods.toml`，可被 FML 当 mod 加载） |

### 6.1 build.gradle 关键片段
```groovy
plugins { id 'java'; id 'net.neoforged.moddev' version '2.0.78' }

def tisFull = project.hasProperty('tisFull')
sourceSets.main.java.setSrcDirs([tisFull ? 'src/main/java' : 'src/smoke/java'])   // TIS-SWITCH
sourceSets.main.resources.setSrcDirs(['src/main/resources'])                      // TIS-SWITCH

neoForge {
    version = project.neoforge_version                    // 21.1.209
    accessTransformers.from 'src/main/resources/META-INF/accesstransformer.cfg'
    mods { main { sourceSet sourceSets.main } }           // 关键：让 MDG 生成 -Dfml.modFolders
    runs { client { client() }; server { server() } }
}

dependencies {
    implementation files('libs/carpet.jar')
    implementation files('libs/conditional-mixin.jar')
    compileOnly files('libs/lithium.jar')
    compileOnly 'org.jetbrains:annotations:24.1.0'
}
```
`mods { main { sourceSet sourceSets.main } }` 是第二个致命坑：缺它则 `build\moddev\serverRunVmArgs.txt` 无 `-Dfml.modFolders`，FML 只把 `build\resources\main` 当 mod file，`@Mod` 类扫不到、构造器不执行（表现为 mod 在列表里但无 bootstrap 日志）。

## 7. S1 验收证据

### 7.1 冒烟 `.\gradlew.bat --no-daemon --console=plain runServer`（日志 `_smoke_s1.log`）
```
L24:      Mod List:
L27: 		Carpet TIS Addition (NeoForge port) 1.82.0 (carpet_tis_addition)
L28: 		Carpet: NeoForged 1.4.147+v1.0.8-251027 (carpet)
L29: 		Minecraft 1.21.1 (minecraft)
L30: 		NeoForge 21.1.209 (neoforge)
L39: [11:09:18] [modloading-worker-0/INFO] [ca.ne.TISNeoForgeMod/]: [TISCM] neoforge bootstrap
L42: [11:09:19] [main/WARN] [ne.ne.ne.se.lo.ServerModLoader/LOADING]: fml.modloadingissue.brokenfile.fabric [[]]
...
[11:09:32] [Server thread/INFO] [minecraft/DedicatedServer]: Done (8.534s)! For help, type "help"
[11:09:32] [Server thread/INFO] [ne.ne.ne.ga.GameTestHooks/]: Enabled Gametest Namespaces: []
[11:09:32] [Server thread/INFO] [ne.ne.ne.se.pe.PermissionAPI/]: Successfully initialized permission handler neoforge:default_handler
```
→ Done ✅ / bootstrap 打印 ✅ / carpet fork 已加载 ✅ / 唯一 issue = conditional-mixin 的 Fabric 打包告警（非阻断）。

### 7.2 全量编译 `cmd /c ".\gradlew.bat --no-daemon --console=plain -PtisFull compileJava > _compile_s1.log 2>&1"`
```
   39 errors
BUILD FAILED in 2m 9s
2 actionable tasks: 2 executed
```
分桶（去重后 37 条唯一）：

| 桶 | 首轮 | 终轮 |
|---|---|---|
| A 依赖缺失 | 22 | **0** |
| A-AT 访问权限（单列） | 23 | **0** |
| B carpet fork 符号 | 0 | **0** |
| C `net.fabricmc.*` | 31 | **37** |
| D 其它（语法/JDK/泛型） | **0** | **0** |

### 7.3 自检（基线 4 / 2 / 21，未新增）
```
PS> $files = Get-ChildItem 'src\main\java' -Recurse -Filter *.java      # java files = 1009
=== CHECK1 pattern='require\s*=\s*0'  expect=4  matches=4  lines=4
  .\src\main\java\carpettisaddition\mixins\logger\microtiming\tickstages\scarpet\CarpetEventServer_EventMixin.java:44: @Inject(method = "onTick", at = @At("HEAD"), remap = false, require = 0)
  .\src\main\java\carpettisaddition\mixins\logger\microtiming\tickstages\scarpet\CarpetEventServer_EventMixin.java:50: @Inject(method = "onTick", at = @At("HEAD"), remap = false, require = 0)
  .\src\main\java\carpettisaddition\mixins\rule\voidRelatedAltitude\EntityMixin.java:43: require = 0
  （+ CarpetEventServer_EventMixin.java:41 为其配套注释）
=== CHECK2 pattern='@Overwrite'  expect=2  matches=2  lines=2
  .\src\main\java\carpettisaddition\mixins\helpers\mixin\IWorldMixin.java:42: * So here comes the @Overwrite
  .\src\main\java\carpettisaddition\mixins\helpers\mixin\IWorldMixin.java:46: @Overwrite
=== CHECK3 pattern='TODO|FIXME|UnsupportedOperationException'（-CaseSensitive）expect=21  matches=21  lines=21
```
CHECK3 若不加 `-CaseSensitive` 会得 22：`SpeedTestPinger.java:146` 的 `mapToDouble` 里被大小写不敏感地匹配到 "ToDo"，属误报。

结论：**三条全部 = 基线 4/2/21，S1 未新增任何一条。**

## 8. S1 阻塞项 / 偏差

见 `BLOCKERS.md`：① modId 被迫改 `carpet_tis_addition`（NeoForge 校验 `^[a-z][a-z0-9_]{1,63}$`）；② conditional-mixin 为 Fabric 打包，运行期仅告警；③ `[[mixins]]` 仍注释（S3 处理）；④ `build\classes\java\main` 因 37 条 C 类错误整体失败，无 class 产出。

## 9. 下一步（仅一条）

**S2：只消 C 类 37 条 —— 把 `net.fabricmc.*` 换成 NeoForge 等价物（`ModInitializer`→`@Mod` 构造器/事件、`FabricLoader`→`FabricLoader` 的 NeoForge 替代或 `Platform` 抽象、`EnvType`→`Dist`、`TinyMappingV2Reader` 改自带 TinyV2 解析），改完重跑 `-PtisFull compileJava` 并重贴 4/2/21 自检。**

---

## 10. S2 归档（C 类清零 + `-PtisFull` 真实构建/启动）

- 目标：消掉 37 条 C 类（`net.fabricmc.*`），并让 `-PtisFull` 真的编译 + 启动服务器
- 结果：**`-PtisFull compileJava` 0 errors（A/B/C/D 四桶全 0）**；`runServer` 已经走完 FML 装载、`@Mod` 构造、Carpet 加载、命令注册，**止步于 TISCM 自己的 accessor mixin 未注册（S3 范围）**

### 10.1 .java 改动清单（仅授权名单 8 个 + 新建 1 个）

| # | 文件 | 改动 |
|---|---|---|
| 1 | `src\main\java\carpettisaddition\CarpetTISAdditionMod.java` | `MOD_ID="carpet_tis_addition"`、`MOD_NAME="Carpet TIS Addition"`、`COMPACT_NAME="carpettisaddition"` 三个字面量（不派生）；去掉 `implements ModInitializer` / `@Override onInitialize`，改 `public static void initialize()`；`version` 改取 `ModList.get().getModContainerById(MOD_ID).orElseThrow(RuntimeException::new).getModInfo().getVersion().toString()` |
| 2 | `src\main\java\carpettisaddition\neoforge\TISNeoForgeMod.java`（新建） | `@Mod(CarpetTISAdditionMod.MOD_ID)` 无参构造 → 打 `[TISCM] neoforge bootstrap` → `CarpetTISAdditionMod.initialize()` |
| 3 | `...\utils\AutoMixinAuditExecutor.java` | `isDevelopmentEnvironment()` → `!FMLEnvironment.production` |
| 4 | `...\utils\mixin\testers\DevelopmentEnvironmentTester.java` | 同上 |
| 5 | `...\translations\TranslationLoader.java` | 同上 |
| 6 | `...\network\TISCMClientPacketHandler.java` | `getEnvironmentType()==EnvType.SERVER` → `FMLEnvironment.dist == Dist.DEDICATED_SERVER` |
| 7 | `...\utils\GameUtils.java` | `getEnvironmentType()==EnvType.CLIENT` → `FMLEnvironment.dist == Dist.CLIENT` |
| 8 | `...\utils\mixin\LithiumConfigAccess.java` | `isModLoaded(x)` → `ModList.get().isLoaded(x)` |
| 9 | `...\CarpetTISAdditionPreLaunch.java` | **删除**（只做 `MixinExtrasBootstrap.init()`，NeoForge 自带 MixinExtras；grep 确认无其它引用） |

其它 1000 个 .java：**一字节未动**（`TinyMappingV2Reader.java` 也没动，只靠依赖消错）。

### 10.2 build.gradle 改动（6 块）

1. **源集固定**：`sourceSets.main.java.setSrcDirs(['src/main/java'])`，`-PtisFull` 变成空开关；`src\smoke` 整个目录删除。理由：`@Mod` 入口必须在 main 源集，切到 main 后 `runServer` 才是真验收。
2. **C 类 15 条（`net.fabricmc.mapping.reader.v2.*`）**：真正的提供者是 `net.fabricmc:tiny-mappings-parser`，**不是** mapping-io（`mapping-io-0.8.0.jar` 里只有 `net/fabricmc/mappingio/*`）。改用缓存扫描 + `implementation files(...)` 提供。
3. **`convertTranslations`**：把 `assets/carpettisaddition/lang/*.yml` 展平成 `<lang>.json`（1142 key/语言）。不做这步，dev 下 `TranslationLoader` 抛 `Failed to load translation of language en_us`，`@Mod` 构造直接失败。
4. **`stripConditionalMixin`**：从 `libs\conditional-mixin.jar` 产出 `build\libs\conditional-mixin-lib.jar`（去掉 `fabric.mod.json` / `*.mixins.json` / `MANIFEST`）+ 一张 `neoforge.mods.toml`。原因：conditional-mixin 是 Fabric 打包，FML 会跳过原件；而 `carpettisaddition.settings.Rule` 注解带 `Restriction[]` 成员，`CarpetRuleRegistrar` 读注解时 `AnnotationType` 初始化要它 → `NoClassDefFoundError`。原 `libs\conditional-mixin.jar` 未动。
5. **`compat-src\fabric-loader-shim\` + `compileFabricLoaderShim`**：`conditional-mixin` 的 `VersionCheckerImpl` 直接调 `net.fabricmc.loader.api.FabricLoader` / `Version` / `VersionPredicate`。这里提供一份 dev-only 的最小实现（内部全部反射 NeoForge 的 `ModList` / `ComparableVersion`），编进同一个 shim jar。不让它进 mod 源集、不进 1009。
6. **`stripTinyMappings`**：同理把 tiny-mappings-parser 也包成一个带 `neoforge.mods.toml` 的 dev 库 mod（`tiny_mappings_parser_lib`），否则 `StackTraceDeobfuscator` 的 `TISCM Mapping` 线程会 `NoClassDefFoundError: net/fabricmc/mapping/reader/v2/TinyVisitor`。

> dev 运行期的机制结论（踩坑记录）：只在 Gradle `runtimeClasspath` 上放普通 jar，FML 的 `ModuleClassLoader` 读不到；手工丢进 `run\mods\` 会报 `is not a valid mod file`；唯一可用的是**带 `META-INF/neoforge.mods.toml` 的 jar 挂在 Gradle 运行期 classpath 上**（dev 下这个 classpath 参与 mod 发现）。

### 10.3 七条验收状态

| # | 项 | 状态 | 证据 |
|---|---|---|---|
| 1 | `-PtisFull compileJava` 0 errors，四桶全 0 | ✅ | `_compile_s2.log`：`[TIS] sourceSet=src/main/java  files=1009` + `BUILD SUCCESSFUL in 32s`，`.java:\d+: error:` 命中 0 |
| 2 | `runServer` Mod List + bootstrap + `Done` | ⚠️ 部分 | Mod List / `[TISCM] neoforge bootstrap` 已出现；`Done (` 0 次 → 见 BLOCKERS B-S2-1（mixin 未注册，S3） |
| 3 | `Mixin apply failed` = 0 | ✅ | `Select-String _run_s2.log -Pattern 'Mixin apply failed'` → 0（S3 基线） |
| 4 | 翻译命名空间仍是 `carpettisaddition` | ✅ | `TranslationConstants.java:28` / `CarpetTISAdditionMod.java:32` |
| 5 | 自检 4 / 2 / 21 | ✅ | 见 §10.4 |
| 6 | `net\.fabricmc` 只剩 compat 目标 | ⚠️ 8 行 | 4 行是 fabric-api compat（S3 开关，未动）+ 4 行是 `TinyMappingV2Reader` 的 import（该文件不在可改名单） |
| 7 | 回写状态文件 | ✅ | 本节 + BLOCKERS §B-S2 |

### 10.4 自检与残留（原始输出）

```
# 自检（-CaseSensitive 只加在第三条）
require\s*=\s*0                            -> 4
@Overwrite                                 -> 2
TODO|FIXME|UnsupportedOperationException   -> 21   (加 -CaseSensitive；不加会 22)

# net\.fabricmc 残留 8 行 / 6 文件
helpers\rule\largeBarrel\compat\fabricapi\LargeBarrelFabricAPIHack.java:55  反射 net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage
helpers\rule\largeBarrel\compat\fabricapi\LargeBarrelFabricAPIHack.java:60  反射 net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage
mixins\rule\largeBarrel\compat\fabricapi\ItemStorageMixin.java:39           @Mixin(targets = "net.fabricmc.fabric.api.transfer.v1.item.ItemStorage")
mixins\rule\largeBarrel\compat\fabricapi\ItemStorageMixin_Old.java:39       同上
utils\deobfuscator\mapping\TinyMappingV2Reader.java:24-27                  import net.fabricmc.mapping.reader.v2.{MappingGetter,TinyMetadata,TinyV2Factory,TinyVisitor}
```

### 10.5 下一步（仅一条）

**S3：mixin 注册与 conditional-mixin 移植** —— 打开 `neoforge.mods.toml` 的 `[[mixins]]`、把 `conditional-mixin` 的 `MixinConfigPlugin`/`Restriction` 逻辑移植到 NeoForge（同时清掉 10.2 里的 3 个 dev shim 与 4 条 fabric-api compat），目标是 `runServer` 出现 `Done (Ns)!`。

---

## 11. S3 归档：mixin 容器接入

- 边界执行情况：**1009 个 .java 一字节未动**（本轮唯一的文件改动是 `carpet-tis-addition.mixins.json` 的 `compatibilityLevel`，`neoforge.mods.toml` 的 `[[mixins]]` 开关，新增勘测脚本/临时 config）。业务代码、lang、`run\mods\carpet.jar` 均未触碰。

### 11.1 结论

| 项 | 结果 |
|---|---|
| `{{COMPATIBILITY_LEVEL}}` 占位符 | ✅ 已改 `"JAVA_21"`；`src\main\resources` 全量 `\{\{` 复核只剩 `icon.png` 二进制误命中 |
| `[[mixins]]` | ✅ 已打开且**只留正式配置**一条：`config="carpet-tis-addition.mixins.json"` |
| 正式配置完整性 | ✅ `required=true` / `package=carpettisaddition.mixins` / `plugin=carpettisaddition.utils.mixin.CarpetTISAdditionMixinPlugin` / `compatibilityLevel=JAVA_21` / `injectors.defaultRequire=1` / mixins **587** + client **12** |
| plugin 能否实例化 | ✅ 可以（`conditional_mixin_lib` dev mod + fabric-loader-shim early bootstrap），无 `NoClassDefFoundError: me/fallenbreath/...`，无 `IllegalClassLoadError` |
| 正式语义下能否启动 | ❌ 24 条 mixin 注入硬失败（**不是容器问题**），见 `MIXIN-LOG.md` §5 / BLOCKERS B-S3-1 |
| 移出这 24 条后 | ✅ `_run_s3_fatal2_4.log`：`Done (3.834s)!`，`ClassCastException` 0 次（S2 的 `TranslatableTextAccessor` 崩溃消失，证明 accessor 已织入），`Mixin apply failed` 0 次，applied 415 条 |

### 11.2 新增/改动文件

| 文件 | 性质 |
|---|---|
| `src\main\resources\carpet-tis-addition.mixins.json` | 只改 `compatibilityLevel`（`{{COMPATIBILITY_LEVEL}}` → `JAVA_21`），条目数/内容未动 |
| `src\main\resources\META-INF\neoforge.mods.toml` | `[[mixins]]` 打开；勘测期间临时指向 `tis-mixin-survey.json`，结束时已还原 |
| `tools\mixin-fatal-loop3.ps1` | 勘测脚本：`defaultRequire=1` 下逐条定位首个致命 mixin，直到 `Done` |
| `tools\log4j2-survey.xml` | P-c 日志尝试（结论：不可用，见 BLOCKERS B-S3-3） |
| `MIXIN-LOG.md` | 新建：599 账目、24 条缺陷清单、死路记录、S4 修复口径 |
| `build\mixin-seed.txt` / `build\mixin-defect-table.tsv` / `build\intermediary-all.txt` | S4 直接可用的修复清单 |
| `tis-mixin-smoke.json` / `tis-mixin-survey.json` | 临时勘测配置，**已删除**（`src\main\resources` 现只有 1 个 mixins.json） |

### 11.3 599 条账目（require=1 最终运行）

```
599 = 24 硬失败 + 415 applied + 148 目标类未加载 + 12 client-only
```

裸 `runServer` 在 `Done` 后立刻退出，`command.*`/`logger.*`/`rule.*` 的大量目标类从未被 transform，因此"applied + 条件跳过 = 599"无法直接测量（详见 `MIXIN-LOG.md` §4 的口径说明）。可测替代证据：`Mixin apply failed=0`、移出 24 条后能 `Done`、`TranslatableTextAccessor` 崩溃消失。

### 11.4 七条验收对照

| # | 项 | 状态 | 证据 |
|---|---|---|---|
| 1 | `Done (Ns)!` 且无 `TranslatableTextAccessor` ClassCastException | ⚠️ 条件通过 | `_run_s3_fatal2_4.log`：`Done (3.834s)!`、`ClassCastException` 0 次；但该运行剔除了 24 条硬失败，正式配置（`defaultRequire=1`）仍会被这 24 条中断 |
| 2 | `Mixin apply failed` = 0 | ✅ | `Select-String _run_s3_fatal2_4.log -Pattern 'Mixin apply failed'` → 0（`_run_s3_survey*.log` 同样 0） |
| 3 | applied + 条件跳过 = 599 | ⚠️ 口径不可测 | 见 §11.3：415 applied / 148 未加载 / 12 client / 24 缺陷 |
| 4 | `net\.fabricmc` 仍 8 行 | ✅ | `LargeBarrelFabricAPIHack.java:55,60`、`ItemStorageMixin.java:39`、`ItemStorageMixin_Old.java:39`、`TinyMappingV2Reader.java:24-27` |
| 5 | 自检 4 / 2 / 21 | ✅ | `require\s*=\s*0`→4；`@Overwrite`→2；`TODO|FIXME|UnsupportedOperationException`（`-CaseSensitive`）→21 |
| 6 | `COMPATIBILITY_LEVEL` = 0 | ✅ | `Select-String … -Pattern 'COMPATIBILITY_LEVEL'` → 0 |
| 7 | 三份状态文件回写 | ✅ | `MIXIN-LOG.md`（新建）、`BLOCKERS.md` §12、本节 |

### 11.5 下一步（仅一条）

**S4：修 BLOCKERS B-S3-1 的 24 条 mixin**（A 类 15 条按 `build\intermediary-all.txt` 换真实名字，B 类 9 条对齐 fork/1.21.1 签名），修完复跑 `tools\mixin-fatal-loop3.ps1` 到 `Done` 且缺陷清单为空，之后才做功能验收。

---

## 13. S5 归档：功能验收（RCON 通道）

- 目标：只做功能验收（跑命令、看行为、记证据）。**1009 个 .java 业务逻辑一字节未动**，未出包、未改 lang 源、未动 mixin 配置与 toml。
- 通道：`tools\s5-rcon.ps1`（按任务书规格自实现的最小 RCON 客户端）+ `tools\s5-batch.ps1`（34 条批量）+ `tools\s5-report.py`（报告生成器）。服务器常驻方式：`cmd /c ".\gradlew.bat --no-daemon --console=plain -PtisFull runServer > _run_s5.log 2>&1"`。
- 运行事实：14:44:19 启动 → `Done` → 14:53:26 `/stop`，`_run_s5.log` 收尾 `BUILD SUCCESSFUL in 9m 29s`，无残留 java 进程。
- 逐条原始响应全表见 **`S5-ACCEPT.md`**；`build\s5-command-raw.txt` 为原始证据。

### 13.1 命令验收表（摘要）

| 判定 | 条数 | 命令 |
|---|---|---|
| OK | 15 | `/carpet`、`/tick`(裸)、`/tick query|freeze|step 5|rate 40|unfreeze`、`/player Steve kill`、`/log`、`/log mobcaps|microTiming|phantom`、`/lifetime`、`/lifetime tracking`、`/raid list` |
| INCOMPLETE-OK | 4 | `/info entity`、`/info block`、`/info world`、`/spawn`（`/help <root>` 均能列出真实用法） |
| MISSING | 10 | `/tis`、`/tis help`、`/tis info`、`/tis raid`、`/tis speedtest`、`/counter`、`/counter redstone`、`/counter dye`、`/man`、`/stat` |
| ERROR | 1 | `/player Steve spawn`（RCON 响应体为空，假玩家未生成） |
| SPEC-MISMATCH | 4 | `/carpet tisAdditionEnabled` 的四个分支（规则名在 1.82 源码中不存在） |
| BARE-KEY | **0** | 34 条响应里无任何 `carpettisaddition.*` / `carpet.*.rule.*` 裸 key |

MISSING 的 4 个根节点（`/tis` `/counter` `/man` `/stat`）在源码里 grep `literal(...)` 全为 0，`/help <root>` 也一律 `Unknown command or insufficient permissions` → 属**验收清单与 1.82 实际命令树不符**，不是移植缺陷（B-S5-3）。1.82 的真实命令面：`/carpet` `/profile` `/log` `/spawn` `/player` `/info` `/distance` `/perimeterinfo` `/draw` `/script` `/track` `/lifetime` `/refresh` `/raycast` `/removeentity` `/sleep` `/raid` `/scounter` `/xcounter` `/speedtest` `/manipulate`（后四条受 `hopperNoItemCost` / `hopperXpCounters` / `commandSpeedTest` / `commandManipulate` 规则门控，默认关闭时 `/help` 按 `requires` 过滤不可见）。

### 13.2 翻译链路（S2 自造 convertTranslations 的实测）

| 项 | 结果 |
|---|---|
| `build\resources\main\assets\carpettisaddition\lang\en_us.json` | 存在，133325 B，**1142 key** |
| `zh_cn.json` | 存在，**1142 key** |
| `src\main\resources\...\lang\` | 仍是 `en_us.yml` / `zh_cn.yml`（转换只在构建期，源文件未改） |
| yml ≈ json 抽样比对 | 3/3 键值相等；展平覆盖：en_us 1130 简单键 + 11 block scalar，json 缺 0；zh_cn 不一致 0 |

结论：翻译管线可用，裸 key 在 34 条命令响应中出现 **0** 次。

### 13.3 记录器落盘路径（COMPACT_NAME 实测）

```
D:\Minecraft-mod-development\TIS-NeoForge\run\config\carpettisaddition\lifetime\recorder_config.json
D:\Minecraft-mod-development\TIS-NeoForge\run\config\carpettisaddition\mapping\yarn-1.21.1+build.3-v2.tiny
D:\Minecraft-mod-development\TIS-NeoForge\run\config\carpettisaddition\mapping\yarn_version.json
```

目录名 = **`carpettisaddition`**（不是 `carpet_tis_addition`），与 S2 的 `COMPACT_NAME` 结论一致。`/lifetime tracking start|stop` 实测可用（`Lifetime Tracker started` / `Tracked 0.00 min ... No result yet`）。`/counter` 不存在，等价计数命令为 `/scounter`、`/xcounter`（内存计数 + `/log`，不落盘）。

### 13.4 日志与自检

- **B-S5-1 的运行期形态**（`_run_s5.log:1016`）：`InjectionError: Critical injection failure: Argument modifier method registerTISCMPayload_c2s(Ljava/util/List;)Ljava/util/List; … (0/1) succeeded. Scanned 0 target(s). No refMap loaded.` —— `require=1` 下 mixin 按设计抛异常，栈顶在 `MinecraftServer.lambda$spin$2`（服务器 tick 线程），即 `/player Steve spawn` 触发 `GameProtocols.<clinit>` 时才炸。**S4 的 `_run_s4_final.log` 里这一串是 0 次，不是已通过，而是那轮 `GameProtocols` 从未被加载、注入器从未执行**；S5 用真实用例把它挖了出来。
- `run\logs\latest.log`：`[*/ERROR]` 9 条 + `[FATAL]` 1 条，全部归因（5 条 `Lithium*Tester` 实例化失败 → B-S5-2；3 条 `createFake delayed task error` → B-S5-1；1 条 `Error executing task on Server` → B-S5-2）。`compatibility level` 命中 3 行 = 2 WARN（`carpet-tis-addition.mixins.json` `JAVA_21`、`carpet.mixins.json` `JAVA_17`）+ 1 INFO，符合"允许 3 条"。
- 自检：`require\s*=\s*0` → **4**；`@Overwrite` → **2**；`TODO|FIXME|UnsupportedOperationException`（`-CaseSensitive`）→ **21**（不加开关为 22，多出的是 `SpeedTestPinger.java:146` `mapToDouble` 误报）；`net\.fabricmc` → **8 行**；`(method|field)_\d+` → **0**。与基线完全一致。
- `build\restriction-skipped.txt`：**71 条** `[TISCM] Disabled mixin` 行（17 `required mod ... not found` + 1 级联 `required mixin class` + 53 `version predicates`），逐条判读见 `MIXIN-LOG.md` §11，**全部合规**。

### 13.5 两条技术债（本轮只记录，不改代码）

1. **`logger\phantom\PhantomSpawnerMixin` 的 `index = 19` 硬编码局部变量槽位**
   `src\main\java\carpettisaddition\mixins\logger\phantom\PhantomSpawnerMixin.java:66` `index = 19`，注释自述"NeoForge supplies is stored at offset 353 (istore 19 …), implicit int matching is ambiguous (Found 2..5 candidate variables but exactly 1 is required)"。
   风险：槽位号是 1.21.1 + NeoForge 21.1.209 这一组具体编译产物的产物，MC 小版本或 NeoForge 补丁改了 `PhantomSpawner.tick` 的局部变量表就会静默绑错变量（注错对象不会报错，只会行为错）。正确做法是改按语义定位（`@Local(ordinal=…)` + 类型约束，或 `@At` 锚定 `istore` 前的调用）——属 .java 改动，S5 铁律禁改，留档。
2. **`carpet\hooks\CarpetServerMixin` 依赖 carpet fork 的私有字段 `CarpetServer.minecraft_server`**
   `src\main\java\carpettisaddition\mixins\carpet\hooks\CarpetServerMixin.java:52` `target = "Lcarpet/CarpetServer;minecraft_server:Lnet/minecraft/server/MinecraftServer;"`，`@Inject(at = FIELD, opcode = PUTSTATIC, remap = false)`。
   风险：`remap = false` + 直接指向 fork 的私有字段名 —— 上游 fabric-carpet 没有这个字段（fork 为了让 `onServerLoaded(ServerAboutToStartEvent)` 传服务器对象才加的）。一旦 fork 改名/改可见性，本 mixin 直接 `InvalidInjectionException`，且 `CarpetTISAdditionServer.minecraft_server` 会一直是 null → 所有 rule validator 的服务器上下文失效。属跨仓库耦合债，留档。

### 13.6 边界

- 未改：1009 个 .java 业务逻辑、`lang\*.yml`、`carpet-tis-addition.mixins.json`、`neoforge.mods.toml`、`libs\*`、`run\mods\carpet.jar`。
- 已改：`run\server.properties`（验收期 `enable-rcon=true`，结束时改回 `false` 并留注释）、`tools\s5-rcon.ps1` / `s5-batch.ps1` / `s5-report.py` / `s5-state-writeback.py`（新建）、三份状态文件 + `S5-ACCEPT.md`。
- `CarpetTISAdditionMixinPlugin` 保持 S4 的 `LOGGER.info`（抓跳过名单用），未回调 debug。

### 13.7 下一步（仅一条）

**S6：修 B-S5-1（`CustomPayloadC2SPacketMixin` 的 `@ModifyArg` 改锚 `CustomPacketPayload.codec` 的真实调用点）与 B-S5-2（`LithiumConfigAccess` 静态初始化去掉 mixin 阶段的 `ModList.get()`），修完重跑 `/player <name> spawn` 与 `hopperXpCounters` 两条用例。**


---

## 14. S6 归档：三缺陷修复 + 客户端冒烟（T1 / T2 / T3 / T4 / T5）

### 14.1 本轮改动清单

| # | 文件 | 类型 | 改动内容 |
|---|------|------|----------|
| 1 | `build.gradle:227-244` | 改（开发期脚手架 `convertTranslations`） | `flattenTranslationNode` 识别子键 `"."`：`String childPrefix = childKey == '.' ? prefix : (prefix ? prefix + '.' + childKey : childKey)`，即子键 `.` 落到父键本身（见 §14.2） |
| 2 | `src/main/java/carpettisaddition/utils/mixin/LithiumConfigAccess.java` | 改（B-S5-2 授权例外） | 静态字段 `@Nullable private static final Object config = loadLithiumConfig()` → 惰性 `configCache` + `configLoaded` 双检；新增 `isModListReady()`，`isLithiumLoaded()` 加 null 守卫；`getConfig()` 在 `ModList.get()==null`（Mixin PREPARE 阶段）时直接返回 null 且**不缓存**（见 §14.3） |
| 3 | `src/main/java/carpettisaddition/mixins/network/CustomPayloadC2SPacketMixin.java` | 改（仅注入元数据，B-S5-1） | `@At.target` 描述符改成 1.21.1 真实签名（4 参数，见 §14.4）；`@Mixin` 目标、handler、`index` 全不变 |
| 4 | `src/main/java/carpettisaddition/network/TISCMNetworkRegistry.java` | **新建**（B-S6-2，客户端冒烟才暴露的新缺陷） | `@EventBusSubscriber(modid=..., bus=MOD)` + `@SubscribeEvent RegisterPayloadHandlersEvent`：`event.registrar("1").playToServer(TISCMCustomPayload.KEY, TISCMCustomPayload.CODEC, handler)` + `playToClient(...)`，双向注册 `tiscm:network/v1`（见 §14.6） |
| 5 | `tools/ui.ps1`、`tools/uikey.ps1`、`tools/uipost.ps1` | 新建（仅测试工具） | 客户端冒烟用 GUI 自动化：`ui.ps1` 截窗口内帧/点击/移动；`uikey.ps1` SendKeys（本机对 GLFW 无效，见 §14.8 教训）；`uipost.ps1` 直接 `PostMessage(WM_KEYDOWN/WM_CHAR)` 驱动聊天框（有效） |

未改：`src/main/java` 下任何业务逻辑（除 B-S5-2 授权例外那一处）、`lang/*.yml`、`carpet-tis-addition.mixins.json`（仍 587/12）、`neoforge.mods.toml`、`libs/*`、`run/mods/carpet.jar`。

### 14.2 T1 — B-S6-1 翻译键名损坏（7 条）

- 根因：上游 `en_us.yml` 用子键 `.` 表示「节点自身的值」（`en_us.yml:351` `rain_in:` 下 `.: Rain in %1$s`；`en_us.yml:768` `teleport_hint:` 下 `.: "Click to teleport to %1$s"`），`convertTranslations` 当成普通子键拼成 `parent + "." + "."`。
- 修法（`build.gradle:237`）：`childKey == '.'` 时 `childPrefix = prefix`，不再追加 `"."`。
- 验收（重新 `processResources` 后，与上游 jar `C:\Users\xiao_\Downloads\carpet-tis-addition-v1.82.0-mc1.21.1.jar` 内 `assets/carpettisaddition/lang/*.json` 做**双向**键集合 diff）：

```
en_us keys ours=1142 upstream=1142  only_ours=0 only_upstream=0
zh_cn keys ours=1142 upstream=1142  only_ours=0 only_upstream=0
```

- 7 条键全部落位、值正确（尾部 `.` 消失）：

```
carpettisaddition.command.info.world.weather.forecast.rain_in = 'Rain in %1$s'
carpettisaddition.command.speedtest.help                       = 'Test the network connection speed and ping. Test size limit: %sMiB; C...'
carpettisaddition.command.raid.status                          = 'Status'
carpettisaddition.tracker.lifetime.spawn_reason.trans_dimension    = 'Trans-dimension'
carpettisaddition.tracker.lifetime.removal_reason.death            = 'Death'
carpettisaddition.tracker.lifetime.removal_reason.trans_dimension  = 'Trans-dimension'
carpettisaddition.util.teleport_hint                           = 'Click to teleport to %1$s'
```

### 14.3 T2 — B-S5-2 `LithiumConfigAccess` 早加载崩溃

- 根因：`:35` 静态字段初始化在 Mixin PREPARE 阶段执行，`:39` `ModList.get().isLoaded(...)` 此时 `ModList.get()` 还是 null → `<clinit>` `ExceptionInInitializerError` → 之后 `NoClassDefFoundError`。
- 修法：去掉静态初始化期调用，改 `getConfig()` 惰性双检（`configCache` / `configLoaded`，`synchronized`），并加 `isModListReady()` 守卫：`ModList.get()==null` 时返回 null 且**不置 `configLoaded`**，等 ModList 就绪后自然重试一次。
- 验收（`_s6_server2.log`，verbose mixin 下完整启动 + 真跑用例）：

```
ConditionalMixinTester : 0
NoClassDefFoundError   : 0
FATAL                  : 0
ERROR                  : 0
Error while creating fake player : 0
```

即 5 条 `Failed to instantiate a ConditionTester`、`NoClassDefFoundError`、`BlockableEventLoop` 那条 FATAL 全部消失。

### 14.4 T3 — B-S5-1 假玩家断裂：走**路线 A**

- 取证见 `MIXIN-LOG.md` §12（javap 原文）。结论：`ServerboundCustomPayloadPacket.<clinit>` 里那条 INVOKE **还在**，只是 `CustomPacketPayload.codec` 从 2 参数变成 4 参数（多了 `ConnectionProtocol` + `PacketFlow`），旧描述符 → 0 候选。
- 选择 A 而非 B 的依据：结构没变（同宿主类、同 `<clinit>`、同 List 参数位置），`require=true` 语义下只需修描述符；路线 B 会删掉一条 587 清单里的 mixin 且要重做注册表语义，收益低于风险。
- 新 target（`CustomPayloadC2SPacketMixin.java:38-45`）：

```
Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;codec(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload$FallbackProvider;Ljava/util/List;Lnet/minecraft/network/ConnectionProtocol;Lnet/minecraft/network/protocol/PacketFlow;)Lnet/minecraft/network/codec/StreamCodec;
```

- `<clinit>` 对该方法有 2 处调用（PLAY / CONFIGURATION），两处都被修饰 → `STREAM_CODEC` 与 `CONFIG_STREAM_CODEC` 都带上 TISCM payload；参数列表里 List 唯一，故不需要显式 `index`。
- 验收：`/player Steve spawn` / `/player Steve kill` / 再 spawn 均有正常响应，日志无 `Error while creating fake player`（详见 §14.5）。

### 14.5 T4 — 7 条文本的运行期复验（如实记录覆盖度）

真正在客户端单人世界里敲命令、读聊天栏/日志原文：

| 文本 | 触发方式 | 结果 |
|------|----------|------|
| `command.info.world.weather.forecast.rain_in` | `/info world weather` | ✅ `[CHAT] Rain in 141.1min` |
| `command.speedtest.help` | `/speedtest help` | ✅ `[CHAT] Test the network connection speed and ping. Test size limit: 10MiB; Client supported: no` |
| `command.raid.status` | `/raid list` | ⚠️ 只跑到 `No raid exists`；`/raid start` 在该单人世界被权限节点挡下（`Unknown or incomplete command ... raid start<--[HERE]`），没能造出 raid → 未取到 `Status` 行 |
| `tracker.lifetime.spawn_reason.trans_dimension`<br>`tracker.lifetime.removal_reason.trans_dimension`<br>`tracker.lifetime.removal_reason.death` | `/summon minecraft:pig` + `/execute in minecraft:the_nether run tp @e[type=minecraft:pig,limit=1,sort=nearest] 0 70 0` + `/lifetime pig` | ⚠️ 传送确实跨维度成功（`Teleported Pig to 0.500000, 70.000000, 0.500000`），但 `/lifetime pig` 回 `Lifetime Tracker has not started`（`/lifetime tracking` 在当前 source 下也没把它点起来）→ 未取到 tracker 表格 |
| `util.teleport_hint` | — | ⚠️ 该串只作为 `Messenger.__coord()` 的 **hover tooltip**（`Messenger.java:305-321`），不在可见聊天行里，本轮没做 hover 截图 |

- 兜底证据：T1 的双向键集合 diff = 0（1142/1142）+ 上表 7 条键值全部与上游逐字一致 → 未跑到的那 3 组文本，其「不是 key 而是真文本」由「键存在 + 值与上游一致 + 同一套 `Translator` 已在另外两组上验证生效」支撑；**没有**把它们当成已运行期验证。
- 遗留动作（记入 BLOCKERS B-S6-3）：raid / lifetime tracker / hover tooltip 三处需在权限更高的专服（RCON，`S5` 用过的 `tools/s5-rcon.ps1` 路子）里补跑。

### 14.6 B-S6-2（本轮新发现，客户端冒烟才暴露）

- 现象：客户端能进主菜单、能建世界、集成服务器 `Done`，但**连服瞬间被踢**：`Internal Exception: java.lang.IllegalStateException: Unknown or unaccepted channel` / 客户端 `Connection lost`。
- 根因（源码取证见 `MIXIN-LOG.md` §12.2）：1.21.1 NeoForge 的 `ServerCommonPacketListenerImpl.handleCustomPayload` 在 `ConnectionProtocol` 判定后有一段
  `if (!(payload instanceof ...) && !this.server.getPayloadRegistrar()...isRegistered(...))` → 未注册的 custom payload 直接 `disconnect`；
  即「mixin 改 `CustomPacketPayload.codec` 的静态列表」这条路在 NeoForge 下不够用 —— 还必须进 NeoForge 自己的 payload 注册表。
- 修法：新建 `TISCMNetworkRegistry`（§14.1 第 4 行），把 `TISCMCustomPayload.KEY` / `CODEC` 按 `tiscm:network/v1` 双向注册；同时保留 T3 的 mixin（它负责把 payload 塞进 vanilla 静态 codec 列表，两者互补，handler 仍由既有 `TISCMNetworkUtils` / `*PacketHandler` 处理）。
- 验收：启动日志 `[TISCM] registered payload channel tiscm:network/v1`；随后建世界 → `Dev joined the game`，**不再掉线**；聊天命令全程可用。

### 14.7 自检口径（本轮实测，命令可复现）

```
require\s*=\s*0（-CaseSensitive, src/main/java + compat-src）: 4      （基线 4，未增）
@Overwrite            : 2      （基线 2，未增）
TODO|FIXME|UnsupportedOperationException（-CaseSensitive）: 21 （基线 21，未增）
net\.fabricmc（仅 src/main/java）: 8   （基线 8，未增；compat-src 另有 12 行属 fabric-loader-shim 本体）
carpet-tis-addition.mixins.json: mixins=587  client=12  compatibilityLevel=JAVA_21  require=true
```

mixin 应用数（同一口径：`Mixing ... from carpet-tis-addition.mixins.json into`）：

```
S5（_run_s5.log）: 444
S6（_s6_server2.log）: 453   （+9）
```

### 14.8 T5 — 客户端冒烟（首次）

- `.\gradlew -PtisFull runClient`：主菜单正常（`Minecraft 1.21.1 / NeoForge 21.1.209 (6 mods)`）→ Singleplayer → 新建世界（Creative + Allow Commands: ON）→ `Preparing spawn area` → `Time elapsed: 681 ms` → `Dev joined the game`，**世界加载完成无崩溃**；进游戏后画面正常渲染（`build/shots/ingame.png`）。
- 12 条 client mixin 逐条核对（`-Pmixin.debug.verbose=true`，`_s6_client4.log`）：

```
OK       carpet.shape.texthack.ShapesRendererRenderedTextMixin
OK       core.client.MinecraftClientMixin
OK       network.ClientPlayNetworkHandler_HandshakeMixin
OK       network.ClientPlayNetworkHandlerAccessor
OK       network.ClientPlayNetworkHandlerMixin
OK       rule.chatMessageLengthLimitUnlocked.ChatScreenMixin
OK       rule.debugNbtQueryNoPermission.KeyboardMixin
OK       rule.structureBlockOutlineDistance.StructureBlockBlockEntityMixin
OK       rule.syncServerMsptMetricsData.DebugHudMixin
OK       rule.syncServerMsptMetricsData.TickChartAccessor
SKIP(<1.16) carpet.hooks.onServerLoadedWorlds.IntegratedServerMixin
STUB     carpet.tweaks.robustness.network.ClientNetworkHandlerMixin   （@Mixin(DummyClass.class)，1.21.1 无实现，空壳）
```

  说明：不是「12 条全 applied」——10 条 applied、1 条按上游 `@Restriction(<1.16)` 合法跳过、1 条是目标 `DummyClass` 的空壳（不产生 `Mixing` 行）。`Mixin apply failed` / `Critical injection failure` / `MixinApplyError` / `InvalidInjectionException` / `ExceptionInInitializerError` 计数 **0/0/0/0/0**。
- 游戏内 `/carpet`：✅ 正常输出 `Current Carpet Mod settings: / Carpet Mod version: 1.14.7+v1.10.8-251027 / Carpet TIS Addition version: 1.82.0 / Browse categories: [forting] [survival] ...`（截图 `build/shots/cmd_carpet2.png`）。
- `/info` 裸命令回 `Unknown or incomplete command`（上游 1.82 就是这样，真命令是 `/info world weather` 之类）；`/log` HUD 类输出正常，无崩溃：`[CHAT] Dev subscribed to mobcaps.`；`/log tick` 回 `Unknown logger: tick`（合法拒绝，不崩）。

### 14.9 教训（给后续轮次）

1. **GLFW 不收 `SendKeys`**：`[System.Windows.Forms.SendKeys]::SendWait` 打不进 MC 窗口（哪怕 `SetForegroundWindow` 成功）；`PostMessage(WM_KEYDOWN/WM_CHAR)` 直投窗口句柄有效。鼠标用 `mouse_event` 绝对坐标有效。
2. **`runClient --args` 在本工程不可用**：MDG 透传进去的参数会被当成 main class（`IllegalArgumentException: Could not find main class`）；客户端一律用 GUI 点击驱动。
3. **`run\logs\latest.log` 会被下一次 run 覆盖**：服务端证据本轮已固化到 `_s6_server2.log`，客户端在 `_s6_client3.log` / `_s6_client4.log`。
4. `runServer`/`runClient` 前必须杀干净 `java` 进程（`devlaunch` 那个），否则 gradle 会等锁或端口冲突。

---

## 15. S7 — 出发布包 + 真机验收（`carpet-tis-addition-1.82.0+neoforge.1.21.1.jar`）

目标：只做「打包 + 真机验收」，**不开新功能、不修 mixin、不动 587 条清单、不改业务逻辑**。

### 15.1 T1 — 三个输入的条目清点 + 白名单（只读）

取证：本机没有 `unzip`，用 JDK 21 的 `jar tf`（等价条目树），原始清单落 `_s7_cm_entries.txt` / `_s7_tm_entries.txt`，表格与分析全文见 **`_s7_t1_inventory.md`**。

- 输入 1 `build\libs\conditional-mixin-lib.jar`：54 条目（21 目录 + 33 文件）。
- 输入 2 `build\libs\tiny-mappings-lib.jar`：101 条目（12 目录 + 86 .class + 3 META-INF）。
- 输入 3 `compileFabricLoaderShim` 输出 `build\classes\fabric-loader-shim`：9 个 .class（源 5 个 .java）。

**关键事实（坑 B 的真身）**：`conditional-mixin-lib.jar` 里 `net/fabricmc/loader/api/**` 那 9 个类**不是**上游依赖，而是 `stripConditionalMixin` 把 `compileFabricLoaderShim` 的输出并进来的 —— 也就是说 shim 类在这个 jar 里是**第二份**，与 `build\classes\fabric-loader-shim` 逐字节同源。发布包必须靠「shim 先 `from` + `duplicatesStrategy=EXCLUDE`」保证包内只有一份。

白名单（18 行，前缀 → 进/不进 → 理由）：

| # | 输入 | 前缀 | 进包 | 理由 |
|---|------|------|------|------|
| 1 | main output | `carpettisaddition/**` | ✅ | 本 mod 全部实现 |
| 2 | main output | `META-INF/neoforge.mods.toml` | ✅ | 唯一合法 mod 元数据 |
| 3 | main output | `META-INF/accesstransformer.cfg` | ✅ | 已被 `neoForge.accessTransformers` 引用 |
| 4 | main output | `carpet-tis-addition.mixins.json` | ✅ | 587/12 入口 |
| 5 | main output | `assets/carpettisaddition/**` | ✅ | 翻译 + `logoFile` 图标 |
| 6 | shim dir | `net/fabricmc/loader/api/**`（9 类） | ✅ | 坑 B：`RestrictiveMixinConfigPlugin`→`VersionCheckerImpl`→`FabricLoader` |
| 7 | cond-mixin-lib | `me/fallenbreath/conditionalmixin/**`（18 类） | ✅ | `@Restriction` 本体 + 配置插件 + 校验器 |
| 8 | cond-mixin-lib | `net/fabricmc/loader/api/**`（重复） | ⚠️ 只留一份 | EXCLUDE + shim 先 from → 生效的是 #6 |
| 9 | cond-mixin-lib | `LICENSE_conditional-mixin` | ✅ | LGPL-3.0 随分发 |
| 10 | cond-mixin-lib | `META-INF/**`（含假 toml `conditional_mixin_lib`） | ❌ | dev-only 假 modId，会把库装成独立 mod 并与 #2 冲突 |
| 11 | cond-mixin-lib | `architectury_inject_conditionalmixin_common_*` | ❌ | Loom 转换缓存 |
| 12 | cond-mixin-lib | `previous-compilation-data.bin` | ❌ | Gradle 编译缓存 |
| 13 | cond-mixin-lib | `assets/conditionalmixin/icon.png` | ❌ | 无引用 |
| 14 | tiny-mappings-lib | `net/fabricmc/mapping/reader/v2/**` | ✅ | `TinyMappingV2Reader` 硬依赖 |
| 15 | tiny-mappings-lib | `mapping/tree/**`、`util/**`、`mappings/**` | ✅ | 传递依赖，同 jar 不可拆 |
| 16 | tiny-mappings-lib | `META-INF/**`（假 toml `tiny_mappings_parser_lib`） | ❌ | 同 #10 |
| 17 | — | `*.SF` / `*.DSA` / `*.RSA` | ❌ | 签名残留 |
| 18 | — | `compat-src/**`、`build.gradle` | ❌ | 只进 shim 的**编译产物**（#6） |

### 15.2 T2 — `releaseJar` 任务（`build.gradle:290-356`，原有 `jar` 任务未动）

```gradle
def releaseVersion = "${mod_version}+neoforge.${minecraft_version}"   // build.gradle:302 → 1.82.0+neoforge.1.21.1
tasks.register('releaseJar', Jar) {                                    // build.gradle:304
    archiveBaseName = 'carpet-tis-addition'; archiveVersion = releaseVersion
    destinationDirectory = layout.buildDirectory.dir('libs')
    from sourceSets.main.output                                        // :313
    from(layout.buildDirectory.dir('classes/fabric-loader-shim')) { exclude 'META-INF/**' }   // :316 坑B 必须在前
    from(zipTree('build/libs/conditional-mixin-lib.jar')) {            // :321 坑A 白名单
        include 'me/**'; include 'net/**'; include 'LICENSE*'; exclude 'META-INF/**' }
    from(zipTree('build/libs/tiny-mappings-lib.jar')) { exclude 'META-INF/**' }               // :329
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE                     // :334 shim 优先 → FabricLoader 唯一
    exclude 'META-INF/*.SF', 'META-INF/*.DSA', 'META-INF/*.RSA'
    exclude 'architectury_inject_*/**', 'previous-compilation-data.bin'
    dependsOn stripConditionalMixin, stripTinyMappings, compileFabricLoaderShim, convertTranslations   // :341-344
}
```

坑 C 的处理：MDG 2.0.78 **没有 jarJar**（`grep jarJar` = 0 命中），故用原生 `Jar` 任务合并 —— FML 只认 jar 内的 `META-INF/neoforge.mods.toml` 和 classpath，合并与内嵌 jar 语义等价。**没有**去伪造 jarJar。

构建：`cmd /c ".\gradlew.bat --console=plain releaseJar"` → `BUILD SUCCESSFUL`（`_s7_release.log`），compileJava `files=1010`，`convertTranslations` 产出 en_us/zh_cn 各 1142 键。

### 15.3 T3 — 包体检（`tools\s7_verify.ps1`，8 组断言，`_s7_verify_final.log`）

最终产物：`build\libs\carpet-tis-addition-1.82.0+neoforge.1.21.1.jar`，1 804 524 bytes，1786 条目（507 目录）。**TOTAL FAIL COUNT: 0**。

| 断言组 | 结果 |
|--------|------|
| 1 toml | ✅ 唯一 1 份；`[[mods]]`=1；`[[mixins]]`=1；主 modId=`carpet_tis_addition` |
| 2 mixins.json | ✅ 唯一；`mixins=587`、`client=12`、`required=true`、`compatibilityLevel=JAVA_21`；plugin 类在包内 |
| 3 lang | ✅ en_us=1142、zh_cn=1142、无键以 `..` 结尾 |
| 4 conditional-mixin | ✅ `me/fallenbreath/conditionalmixin/**.class`=18；`RestrictiveMixinConfigPlugin`=1 |
| 5 shim | ✅ `net/fabricmc/loader/api/FabricLoader.class` **occurrences=1**；包内 shim 共 9 类 |
| 6 tiny-mappings | ✅ `reader/v2/*.class`=16、`tree/*.class`=24、`mappings/**`=45、`TinyV2Factory`=1 |
| 7 垃圾 | ✅ `architectury_inject_*`=0、`previous-compilation-data.bin`=0、`*.SF/*.RSA`=0 |
| 8 源码 | ✅ `compat-src`=0、`build.gradle`=0、`*.java`=0 |

（断言 2d 首版写成 `require` 误报 FAIL：mixins.json 的顶层键是 **`required`**，与自检口径里的 `require=0` 不是同一个东西。）

### 15.4 T4 — 真机验收（dev 不算数）

**环境偏差（如实记录）**：
- 本机**没有** `%APPDATA%\.minecraft`（真机是 PCL2：`D:\PCl2\.minecraft`，`Version:1.21.1-NeoForge_21.1.264`）；版本隔离 → `versions\1.21.1-NeoForge_21.1.264` 就是 gameDir，mods 在该目录下。
- 真机 NeoForge 是 **21.1.264**（交接里写 21.1.209），FML 4.0.45；`neoforge.mods.toml` 声明 `[21.1.0,)` → 满足。
- PCL2 无法脚本化 → 自建启动器 `tools\s7_launch.ps1`：读版本 json → Test-Rules 过滤 libraries → classpath 去重（否则 `IllegalStateException: Duplicate key ...gson-2.10.1.jar`）→ natives 只取 `:natives-windows$`（早期正则误收 arm64/x86 导致 `UnsatisfiedLinkError: Failed to locate library: lwjgl.dll`）→ 拼 `build\s7\launch-client.bat`（`--username S7Tester --launchTarget forgeclient`）→ `Start-Process cmd.exe /c`。

> 事故与修正：第一版 `-MixinVerbose` 顺手加了 `-Dmixin.debug.countInjections=true`，客户端在 `Minecraft.<init>` 崩：`MixinApplyError: carpet.mixins.json:MinecraftServer_pingPlayerSampleLimit FAILED during APPLY` / `InvalidInjectionException: Constant modifier ... expected 1 invocation(s) but 0 succeeded. Scanned 0 target(s). No refMap loaded.`。原因是 countInjections 把 injector 期望次数变成硬 require，而 carpet 上游这条在 1.21.1 本来就 0/1 命中（dev 也从不加该 flag）。已改为只加 `-Dmixin.debug.verbose=true`，之后稳定（崩溃报告留在 `build\s7\crash-evidence\`）。

**做法**：`mods` 整目录备份成 `mods.bak`（原为空）→ 清空 → 只放 `carpet.jar` + 本包（2 个 jar）→ 启动 → 主菜单 → 新建单人创造世界（Allow Commands: ON）→ 逐项验收 → 退出 → `mods.bak` 还原 `mods`。

**结果（日志副本 `build\shots\s7\client-latest.log`，1015 行 / 210101 B）**：

| 验收项 | 结果 |
|--------|------|
| Mods 列表 | ✅ `Carpet TIS Addition (NeoForge port) 1.82.0`；详情 `Version: 1.82.0`、`ModID: carpet_tis_addition State: loaded`、License LGPL-3.0，**无红字** |
| ERROR / FATAL | **0 / 0** |
| Mixin apply failed / Critical injection failure | 0 / 0 |
| NoClassDefFoundError / NoSuchMethodError / NoSuchFieldError / ClassNotFoundException | 0 / 0 / 0 / 0 |
| `Exception in thread` / `Caused by` | 0 / 0 |
| mixin applied（进世界后，**仅本 mod**） | **464** == dev 464（`run\logs\latest.log`），无差额（459 个唯一 mixin 类）。**645 是全场总数**（本 mod 464 + carpet 179 + neoforge 2），S8 订正，见 §16.1 |
| client mixins | 9 applied + 2 `@Restriction(<1.16)` 跳过 + 1 `DummyClass` 空壳 = 12，与 dev 逐条一致 |
| conditional-mixin 跳过 | **74** == dev 74（`[TISCM] lines total` 76） |
| `/carpet` | ✅ `Carpet TIS Addition version: 1.82.0` + 16 个分类 |
| `/info world weather` | ✅ `Rain in 118.8min`（真文本） |
| `/speedtest`（先 `/carpet commandSpeedTest true`） | ✅ 真实用法文本 |
| `/log mobcaps` / `/log` | ✅ 订阅成功（`S7Tester subscribed to mobcaps.`）、logger 列表真文本、HUD 不崩 |
| `/player Alex spawn` → `kill` → `spawn` | ✅ `entity id 1401` → `Alex lost connection: Killed` → `entity id 1420` |
| 退出 | ✅ `ThreadedAnvilChunkStorage: All dimensions are saved` |

WARN 共 25 条（`/(WARN)\]` 口径；早期统计脚本用 `\[WARN\]` 匹配不到，真实格式是 `[main/WARN]`），其中 mixin 相关 19 条全是无害提示（`@Mutable @Final` 写访问、`@Slice` 未命中、JAVA_21 兼容级提示），另 6 条是原版资源/音效类 WARN。截图 01–23 与两份日志在 `build\shots\s7\`。

**环境恢复**：`mods` 已用 `mods.bak` 还原（现 0 文件，`mods.bak` 留作凭据）；测试世界 `saves\New World` 已移到 `build\s7\saves-evidence\`（实例 `saves` 现 0 项）；本次实验的 `crash-2026-10-25_16.42.38-client.txt` 已移到 `build\s7\crash-evidence\`（实例 `crash-reports` 现 0 项）。

### 15.5 口径修正（覆盖 §14.8 / B-S6-4）

§14.8 把 `rule.structureBlockOutlineDistance.StructureBlockBlockEntityMixin` 记成 OK(applied)。本轮逐条实测：**dev 与真机都是 9 applied + 2 disabled + 1 stub**，两条 disabled 分别是 `carpet.hooks.onServerLoadedWorlds.IntegratedServerMixin` 与 `rule.structureBlockOutlineDistance.StructureBlockBlockEntityMixin`，日志原文（真机 L106/L107，dev L101/L102）：

```
[TISCM] Disabled mixin carpettisaddition.mixins.carpet.hooks.onServerLoadedWorlds.IntegratedServerMixin due to mod minecraft@1.21.1 does not matches version predicates [<1.16]
[TISCM] Disabled mixin carpettisaddition.mixins.rule.structureBlockOutlineDistance.StructureBlockBlockEntityMixin due to mod minecraft@1.21.1 does not matches version predicates [<1.16]
```

即「10 applied + 1 跳过 + 1 空壳」应为「**9 applied + 2 跳过 + 1 空壳**」。真机与 dev 完全同构，故不是移植回归。

### 15.6 T5 — 产物与发布说明

- 产物（重命名自真机验过的那份，**字节未变**，前后 SHA-256 一致）：
  `build\libs\carpet-tis-addition-1.82.0+neoforge.1.21.1.jar`
  1 804 524 bytes / SHA-256 `0B2CA7CB21854208F88FF3ED773DBF6672A8EA7C9A124FCD02846433AB2A3EE9`
- `build.gradle:302` 已把发布版本改成 `${mod_version}+neoforge.${minecraft_version}` 且去掉 classifier → 以后 `gradlew releaseJar` 直接产出同名文件，不再需要手工改名。
- 发布说明：`RELEASE-NOTES.md`（运行环境、强制依赖 carpet fork `>=1.4.147`、安装、与 Fabric 版差异、已知限制 7 条、真机验收摘要）。

### 15.7 S7 结论

发布包可用：真机（NeoForge 21.1.264 + MC 1.21.1，仅 carpet + 本包）**0 ERROR / 0 FATAL**，本 mod mixin applied 与 dev **逐条一致**（**464** / 74 / client 12=9+2+1；全场 mixin 总数 645 = 464+179+2，S8 订正，见 §16.1），命令层功能全部可用。遗留项见 `BLOCKERS.md` §17（B-S7-1/B-S7-3 仍开，B-S7-2 已由 S8-T4 证伪关闭，B-S6-3 由 S8-T3 部分关闭）。

### 15.8 S7 教训

1. 日志级别的 token 是 `[main/WARN]` / `[Render thread/ERROR]`，**不是** `[WARN]`；统计脚本用 `\(WARN\)|/WARN\]` 才不会误报 0。
2. 启动器自建时：classpath 必须去重、natives 只取 `:natives-windows$`、`mainClass` 不能漏，这三个坑各炸一次。
3. `-Dmixin.debug.countInjections=true` 会把「上游本来 0 命中的 injector」变成硬失败，**不要**在验收里开它；verbose 只开 `-Dmixin.debug.verbose=true`。
4. 交互式验收仍走 `PostMessage(WM_CHAR)` 打命令 + `GetClientRect` 截图；`uipost.ps1` 只能发 T/Enter，发 ESC 要自己 PostMessage 0x1B。

---

## 16. S8 归档（口径订正 + 安装包 + 运行期取证 + HUD 归因重查 + 专服验收）

S8 纪律：只做 T1–T5，不动 1009 个 java 文件的业务逻辑、不动 `carpet-tis-addition.mixins.json`（实测 md5 `63980334F58FB07D1218D070242D62DD`，587/12/require=true/JAVA_21 全未变）。`src`/`compat-src` 在 16:35 之后**零改动**（按 mtime 核，见 §16.6）。

### 16.1 T1 — 计数口径钉死（645 → 464）

新脚本 `tools\s8-count.ps1`（PS 5.1 + ASCII，`-Log 'a,b' -Out file`），对任意日志只数三样：①`from carpet-tis-addition.mixins.json into` 行 ②`[TISCM] Disabled mixin` 行 ③ERROR/FATAL/apply-failed，并附全场各 mixin config 分布。三份历史日志重跑 + S7 真机复跑：

| 日志 | 大小 | 本 mod applied | Disabled | ERROR/FATAL/failed | 分布 |
|------|------|----------------|----------|--------------------|------|
| `_run_s5.log`（S5 dev 服） | 344195 | **444**（439 唯一） | 71 | 9 / 0 / 1 | 444+162+2 |
| `_s6_server2.log`（S6 dev 服） | 191640 | **453**（448 唯一） | 72 | 0 / 0 / 0 | 453+165+2 |
| `_s6_client4.log`（S6 dev 客） | 188289 | **464** | 74 | 0 / 0 / 0 | 464+179+2 |
| `build\shots\s7\client-latest.log`（S7 真机） | 210101 | **464**（459 唯一） | 74 | 0 / 0 / 0 | 464+179+2 |
| `run\logs\latest.log`（S7 dev 客） | 191750 | **464**（459 唯一） | 74 | 0 / 0 / 0 | 464+179+2 |
| `run\logs\srv-s5-latest-baseline.log` | 293474 | 444 | 71 | 9 / 1 / 1 | 444+162+2 |
| `_s8_srv\logs\debug.log`（S8 生产专服，跑过 raid/lifetime） | 832898 | **461**（456 唯一） | 72 | 0 / 0 / 0 | 461+167+2 |
| `build\s8\srv-run2-debug.log`（S8 生产专服，短跑重启） | 791814 | **443**（438 唯一） | 72 | 0 / 0 / 0 | 443+162+2 |
| `_s8_srv\logs\latest.log`（S8 生产专服） | 63985 | **0**（见 §16.4 口径陷阱） | 72 | 0 / 0 / 0 | 空 |

结论：**645 = 464（本 mod）+ 179（carpet）+ 2（neoforge）**，是三份 config 的应用行**合计**，不是本 mod 的条数。`RELEASE-NOTES.md` §6 与本文档 §15.4/§15.7 的 645 已全部改成 464 并加说明；§14.7 的 444/453 历史数字与新脚本口径一致，保留原样。464 是**运行期行数**（随类加载增长），不是清单里的 587。
重复应用（同一 mixin 类多 target）：`logger.damage.LivingEntityAndPlayerEntityMixins$ApplyDamageMixin`=2、`...$DamageMixin`=3、`logger.microtiming.tickstages.scarpet.CarpetEventServer_EventMixin`=3 → 464 行 / 459 唯一。

### 16.2 T2 — 发布物改造：单装必崩 → 安装包

`dist\TIS-Addition-NeoForge-1.21.1.zip`（2 932 578 B，SHA-256 `BDE8C0B89DB859C4A06FDA25952ABC29BE2042A963D70BA26A515C96491604E4`），条目（正斜杠路径，`ZipArchive` 手工写，无 backslash 兼容问题）：

```
mods/carpet-tis-addition-1.82.0+neoforge.1.21.1.jar   1804524 B
mods/carpet-1.4.147+v1.0.8-251027.jar                 1498683 B
README.txt                                             5512 B（UTF-8 BOM，中文）
```

`README.txt` 覆盖：必须两个 jar 同放 `mods\`、NeoForge 21.1.x / Java 21 / MC 1.21.1、缺前置报错原文、6 条已知限制、两个 jar 的大小/SHA-256。解压回环校验：三个文件字节数一致，TIS jar SHA-256 仍是 `0B2CA7CB…A3EE9`。

### 16.3 T3 — 三条文本的运行期取证（生产专服 + RCON）

见 `S8-ACCEPT.md` §3 的原文。要点：
- `spawn_reason`：`/summon command` / `Natural spawning` / **`Trans-dimension (from Overworld)`** ✅ 运行期
- `removal_reason`：**`Death (genericKill)`** / **`Trans-dimension (to Nether)`** / `Randomly despawn` ✅ 运行期
- `raid.status`：`Overworld Raid Count: 1` / `Status: ongoing` / `Waves: 1/5` / `Raiders: x5` ✅ 运行期（bad_omen → 村庄触发真袭击）
- `teleport_hint`：hover tooltip，RCON 取不到 → **⚠️ 仅静态验证**，本轮明确不取证，不伪造

跨维度取证方法（踩坑记录）：`/execute in <dim> run tp @e[...]` **不会**给非玩家实体换维度（只改执行位置），必须让它走传送门 —— `fill ... minecraft:nether_portal` + `summon` 到门内，20 s 后到达地狱侧（坐标按 1/8 缩放）。

### 16.4 T4/HUD 归因（旧结论已证伪）

`libs\carpet.jar`（fork，716 class）与上游 `remapped.carpet-fabric-carpet-8f637a66-1.21-1.4.147+v240613.jar`（715 class）同口径扫描：`HUDController` 的引用集合两边**完全相同**（4 个类），`javap` 方法表相同且**没有 render 方法**；`carpet.mixins.json` client 段 14 条同名同集合；`net/fabricmc` 引用 fork 0 / 上游 6（移植去 fabric 依赖的正常差异）。
→ HUD 走的是 **Tab 列表**：`HUDController.update_hud` → `ClientboundTabListPacket(header, footer)` → `player.connection.send`，渲染由原版 `PlayerTabOverlay` 完成。结论 **(B)：两边都没有 HUD 渲染调用点**，"fork 掉了 client mixin"不成立。详见 `S8-ACCEPT.md` §4。

### 16.5 T5 — 干净专服验收

`_s8_srv`（installer 全新装 NeoForge 21.1.264）→ mods 只放两个 release jar → `Done (31.760s)!` → applied 461 / Disabled 72 / ERROR 0 / FATAL 0 / apply-failed 0；`/carpet`、`/lifetime`、`/raid list`、`/raid list full`、`/lifetime zombie spawning`、`/lifetime zombie removal`、`/sleep 1 s` 全部真文本。删 carpet jar 后重启 → `Mod carpet_tis_addition requires carpet 1.4.147 or above / Currently, carpet is not installed` + `Failed to start the minecraft server`，与用户遇到的一致。数据表见 `RELEASE-NOTES.md` §6.1。

### 16.6 S8 自检

```
require=0            : 4     （基线 4）
@Overwrite           : 2     （基线 2）
TODO|FIXME|UnsupOp   : 21    （基线 21）
net.fabricmc (main)  : 8     （基线 8；compat-src 另有 12 行属 shim 本体）
mixins.json          : 587/12/require=true/JAVA_21  md5 63980334F58FB07D1218D070242D62DD
src 16:35 后改动     : 0 个文件；compat-src 同
本轮新增/改动        : tools\s8-count.ps1、dist\*（README.txt + zip）、RELEASE-NOTES.md、PORT-STATE.md、BLOCKERS.md、S8-ACCEPT.md、build\s8\*（日志副本）
```

### 16.7 S8 教训

1. **专服看 `debug.log` 不看 `latest.log`**：生产 NeoForge 把 mixin 的 `Mixing ... into` 打在 DEBUG，`latest.log` 会是 0，别据此判断"mixin 没应用"。
2. **`/execute in <dim> run tp @e[...]` 不换维度**，非玩家实体只能走传送门；玩家（含 carpet 假玩家）才吃 `tp` 的换维。
3. **假玩家不进 lifetime 追踪**（`/lifetime player ...` 永远 `No result yet`），要取证得用普通实体。
4. **`data get entity @e[...]` 的 `in <dim>` 只改执行位置**，不改变选择器的维度作用域，用它判断实体在哪个维度会误判。
5. RCON 拿不到 `/say` 的广播文本；PS 里自定义函数别叫 `R`（撞 `Invoke-History` 别名）。
6. 传送门放置后 20 s 足够非玩家实体完成维度切换；raid 触发要 player **真的落在村庄 bounds 内**（y 高了不算，掉下来才算）。

## 17. S9 归档（拆掉「拍平 conditional-mixin」，消除模块包冲突并重出 dist）

### 17.1 背景（B-S9-1）

用户整合包实例 `D:\PCl2\.minecraft\versions\DIS机械动力航空学6.0.10-1.3.2\`（NeoForge 21.1.252 + Sinytra Connector）启动即崩：

```
logs\latest.log:453 [main/ERROR] [cpw.mods.modlauncher.ModuleLayerHandler/]: Error while resolving modules.
logs\latest.log:454 java.lang.module.ResolutionException: Modules carpet_tis_addition and conditional_mixin export package me.fallenbreath.conditionalmixin.api.mixin to module jecharacters
```

成因：TweakerMoreForge 通过 jar-in-jar 提供 `conditional-mixin-neoforge-0.6.4.jar`（modId `conditional_mixin`），而 S8 及以前的主 jar 把同一批 `me/fallenbreath/**` 拍平在自己身上 → 模块层两个 module 导出同一包。

### 17.2 修法（本轮实际动作）

| 项 | S8 | S9 |
|---|---|---|
| `build.gradle` 任务 | `stripConditionalMixin`（自造 Fabric 产物 + `genConditionalMixinShimMeta` + `compileFabricLoaderShim`） | `packageConditionalMixin`（把 vendored 官方 `libs\conditional-mixin-neoforge-0.6.4.jar` 原样复制到 `build\libs`） |
| 主 jar 内容 | 合并 jar：本 mod + `me/fallenbreath/**` + `net/fabricmc/loader/**` + `net/fabricmc/mapping/**` | 只有本 mod（`exclude 'me/**'` + `exclude 'net/**'`），1645 条目，1 689 654 B |
| 依赖声明 | 仅 carpet / minecraft / neoforge | 新增 `[[dependencies.carpet_tis_addition]] modId="conditional_mixin" type="required" versionRange="[0.6.4,)" ordering="NONE" side="BOTH"` |
| 第三方库发行 | 拍平进主 jar | 独立模块 jar：`mods\tiny-mappings-parser-0.3.0+build.17.jar`（必需）+ `optional\conditional-mixin-neoforge-0.6.4.jar`（按需） |
| dist | 3 条目 / 2 932 578 B | 6 条目 / 2 933 347 B / `37D87A0B…61E1` |

踩坑留档：`packageConditionalMixin` 不能写成 `Copy` 类型（`into build/libs` 会让 Gradle 9.5.1 报
`Task ':compileJava' uses this output of task ':packageConditionalMixin' without declaring an explicit or implicit dependency`），
改普通任务 + `inputs`/`outputs` + `Files.copy` 后通过。

### 17.3 验收（NeoForge 21.1.264 专服，`tools\s9-run-env.ps1`，日志在 `_s9_logs\`）

| 环境 | 结果 | 证据 |
|---|---|---|
| A：只 carpet + 本 mod | ❌ 预期失败 | `S9A.latest.log:21/22/25/26`（`requires conditional_mixin 0.6.4 or above` / `Currently, conditional_mixin is not installed`） |
| B：+ conditional-mixin + tiny-mappings | ✅ `Done (7.536s)!` | applied **437**（唯一 432）、Disabled 21、ERROR/FATAL/apply-failed **0/0/0**（`S9B.debug.log` 773 027 B 全文扫描） |
| B2：故意不给 tiny-mappings | ⚠️ Done 但 stdout 抛异常 | `S9B2.stdout.log:94` `NoClassDefFoundError: net/fabricmc/mapping/reader/v2/TinyVisitor` |
| C1 / C1b：旧拍平 jar + 模块 jar | ❌ 复现冲突 | `S9C1.latest.log:26`（`net.fabricmc.mappings.model`）/ `S9C1b.latest.log:24`（`me.fallenbreath.conditionalmixin.api.annotation`） |
| C2：新包 + 两份相同 conditional-mixin | ✅ `Done (8.094s)!` | `S9C2.latest.log` ResEx = 0，Mod List 只一行 `conditional mixin 0.6.4` |
| C3：新包 + 0.6.4 与 0.6.5 同放 | ✅ `Done (9.028s)!` | `S9C3.latest.log:56` `conditional_mixin (version 0.6.4 -> 0.6.5)`，ResEx = 0 |

主 jar 断言：`tops` = `[META-INF, assets, carpet-tis-addition.mixins.json, carpettisaddition]`；`assert-absent <jar> me net` → `RESULT: PASS`、exit=0。

### 17.4 dist（T4）

```
dist\TIS-Addition-NeoForge-1.21.1.zip   2 933 347 B   SHA-256 37D87A0B00DF62CA70F7039E06F427A21966CEF3B1FC059AC94F86163617F1E1
  mods\carpet-tis-addition-1.82.0+neoforge.1.21.1.jar   1 689 654 B   941E9C8A…A5150228
  mods\carpet-1.4.147+v1.0.8-251027.jar                 1 498 683 B   6A81DB10…A34259F5C
  mods\tiny-mappings-parser-0.3.0+build.17.jar             83 140 B   B00E557A…21F6AFDD3F
  optional\conditional-mixin-neoforge-0.6.4.jar            28 507 B   36A570A4…098F9750
  README.txt                                              11 595 B
  THIRD-PARTY-NOTICES.txt                                  3 921 B
```

打包工具 `tools\make-dist-zip.ps1`（显式条目名；`dist` 内有未登记文件即 exit 2）。三坑留档：`CreateFromDirectory` 会带 `dist\` 前缀、临时 zip 不能写在 `dist` 内、旧 zip 会被当成成员打进去。

### 17.5 S9 自检

```
require\s*=\s*0                  : 4   （基线 4）
@Overwrite                       : 2   （基线 2）
TODO|FIXME|UnsupportedOperation  : 21  （基线 21；不加 -CaseSensitive 会因 mapToDouble 里的 ToDo 误报 22）
net\.fabricmc                    : 8 行 / 4 文件（基线 8）
carpet-tis-addition.mixins.json  : 未改动（required=true / package / JAVA_21 / plugin / mixins 587 / client 12）
src/main/java 业务逻辑改动        : 0（本轮只动 build.gradle、META-INF/neoforge.mods.toml、tools\、dist\、文档）
```

### 17.6 S9 遗留

- **B-S9-2**：`[TISCM] Disabled mixin` 从 72 降到 21，少掉的 51 条全部是 `minecraft@1.21.1 does not matches version predicates` 这一类；applied 集合未变（438→432 的差集全是 headless 场景差异），判定为诊断行丢失而非限制失效，待源码级排查。
- 客户端实机本轮未跑；NeoForge **21.1.252** 上未复测修复后的包（只证明它能加载到模块解析阶段）。
- `util.teleport_hint` 仍为静态验证（S8 遗留）。

完整报告：`S9-ACCEPT.md`。
### 17.7 S9c：换图标重出（覆盖 §17.2/§17.4 的数值）
- 图标：`assets/carpettisaddition/icon.png` 换成像素画版本（青绿地毯 + 琥珀流苏 + `TIS`，512×512，1 827 B / `CACA6017…341ACCBC`），生成脚本 `tools\make-icon2.py`，4 个候选见 `build\icon-variants.png`；旧图标留档 `tools\assets\icon-upstream-tis.png` 与 `tools\assets\icon-v1-card.png`。
- 主 jar：**1 674 134 B** / `722A60C1078DD85E380FB31677F2FD83E20B355747152CE3BC751ADCB193BA31`（原 1 689 654 / `941E9C8A…` 作废），条目数 1645 与顶层段 4 个不变。
- dist：三 jar 版 **2 917 758 B** / `D52FA3B72F7DC1B5A3E2C3E202C3623C76B8948B044971C9AC58C960B3ABB897`；单文件版 **2 913 077 B** / `20039133471AAF1A3F840115B02AF6CC9334B9AEDF94D7EBC6506291FB1628B9`。
- 复验：`_s9_logs\S9ICON.latest.log:73` `Done (3.904s)`，ERROR/FATAL/ResolutionException/Duplicate 全 0。

### 17.8 S9d：图标定稿为变体 D（覆盖 §17.7）（S9d 阶段值，终值见 §17.10）
- 图标：`assets/carpettisaddition/icon.png` = 1 885 B / `B093B4E2FDE93C182992F53D5523BBCD6762B7B2A19780A0232914A1B7F7C3C5`（骨白 `C` + 青色 `TIS`，棋盘底；A/B/C 三候选与生成脚本 `tools\make-icon2.py` 均留档）。
- 主 jar：**1 674 227 B** / `4FE83BAE384266FD6A63FF596E491E0339E210781512116126F88173D556D805`，条目 1645、顶层段 4 个不变。
- dist：三 jar 版 **2 917 847 B** / `E3A830BA…7B75A5FE`；单文件版 **2 913 174 B** / `7C106ABF…9412BACF`。
- 复验：`_s9_logs\S9ICON2.latest.log` `Done (5.632s)`，ERROR/FATAL/ResolutionException/Duplicate 全 0。

### 17.10 S9e：PCL2 识别不到本 mod 的根因与修法（2026-10-27）

- 现象：PCL2 社区版 mod 列表只显示文件名 `carpet-tis-addition-1.82.0+neoforge.1.21.1` + 默认立方体图标。
- 根因：PCL2 CE 的 `ModLocalComp.vb:810` 只 `Jar.GetEntry("META-INF/mods.toml")`（另有 mcmod.info / fabric.mod.json / quilt.mod.json 三支），**无 `neoforge.mods.toml` 分支**；exe UTF-16LE 扫描亦 0 命中。NeoForge 1.20.5 起改用 `neoforge.mods.toml`，故未上架 CF/Modrinth 的自制 jar 只能回落文件名（其它 mod 靠指纹命中在线缓存）。
- 修法：主 jar 增补 `src\main\resources\META-INF\mods.toml`（377 B；根段落 `displayURL`/`authors`，`[[mods]]` 段 `modId`/`version`/`displayName`/`description`/`logoFile`；无 `#` 注释）。FML 仍读 `neoforge.mods.toml`，两份共存无副作用。
- 验收：`_s9_logs\S9B3.latest.log` `Done (5.381s)`、Mod List 单条 `Carpet TIS Addition (NeoForge port) 1.82.0 (carpet_tis_addition)`、`ERROR/FATAL = 0/0`；`tools\pcl-parse-sim.py` 复刻 PCL 解析逻辑：旧 jar `FAIL`（无 mods.toml）、新 jar `RESULT: PCL2 will show 'Carpet TIS Addition (NeoForge port)'`。
- 终值：主 jar **1 674 635 B** / `AF5DB35435006C4A0DC0FEB635B035A179B0CDB7E2D411DB57EFA40CB862CB17`（条目 1646）；单文件版主 jar **1 767 785 B** / `C61AE6DC0867A3EA04B4852170688C15A1781994BC1A641155BF526452792917`；三 jar 版 dist **2 918 691 B** / `4618BB19…D2559D`；单文件版 dist **2 913 744 B** / `57B23673…DEE089E`。
- 边界：PCL2 图标仍为默认（不读 `logoFile`，只从 CF/Modrinth 取）；HMCL 未源码核对。
