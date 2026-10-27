# BLOCKERS — S1（NeoForge 骨架 + 依赖闭环） / S2（C 类清零）

- 阶段：**S1 已完成** → **S2 已完成（编译全绿；启动止步 S3 的 mixin 注册）**
- S1 目标：让 `src/main/java` 的 1009 个 .java 在 NeoForge 21.1.209 / MDG 2.0.78 下编译到「只剩 B/C 类错误」→ 达成。A = 0，B = 0，D = 0，剩 37 条 ALL-C（`net.fabricmc.*`）
- S2 目标：C 类清零 + `-PtisFull` 能跑起服务器 → 编译 0 errors 达成；启动见 §11（B-S2-1）
- 编译日志：`_compile_s1.log`、`_compile_s2.log`；运行日志：`_smoke_s1.log`、`_run_s2.log`

---

## 1. 分桶计数表（最终一轮 `-PtisFull compileJava`）

| 桶 | 定义 | 首轮（raw 83 / unique 77） | 终轮（raw 39 / unique 37） | 状态 |
|---|---|---|---|---|
| **A** | 依赖缺失（package X does not exist / 第三方类找不到） | 22 | **0** | ✅ 清零（补 lithium.jar） |
| **A-AT** | 访问权限缺失（Access Widener 的 NeoForge 等价物缺失），单列不记入 D | 23 | **0** | ✅ 清零（accesstransformer.cfg） |
| **B** | carpet fork 成员缺失（`cannot find symbol`，符号应来自 `carpet.*`） | 0 | **0** | ✅ 无 |
| **C** | Fabric 加载器 API（`net.fabricmc.*`） | 31 | **37** | ⏸ 本阶段不动（见 3） |
| **D** | 其它（真语法 / JDK 版本 / 泛型不匹配） | **0** | **0** | ✅ 恒为 0 |

> 说明：C 从 31 涨到 37 不是回退。首轮那 6 条 `method does not override or implement a method from a supertype` 当时被 A-AT 的连带噪声盖住，A/A-AT 清零后它们全部显形，且根因仍是 `net.fabricmc.*` 接口缺失（`ModInitializer` / `PreLaunchEntrypoint` / `MappingVisitor` 的父接口不存在 → 方法自然不构成 override）。

终轮 37 条唯一错误的原始消息分布：
```
cannot find symbol                                        16
package net.fabricmc.loader.api does not exist             7
method does not override or implement a method from ...    6
package net.fabricmc.mapping.reader.v2 does not exist      4
package net.fabricmc.api does not exist                    3
package net.fabricmc.loader.api.entrypoint does not exist   1
```

---

## 2. A 类如何清零（未触碰 `src/main/java` 任何文件）

### 2.1 依赖补全
| 动作 | 来源 | 结果 |
|---|---|---|
| `libs\carpet.jar` | `D:\Minecraft-mod-development\_neoforge_carpet.jar`（1498683 B，本身即 NeoForge mod，`modId=carpet`, `version=1.4.147+v1.0.8-251027`） | `implementation files(...)`，B 类符号（`carpet.api.*` 等）全解析 |
| `libs\conditional-mixin.jar` | `Carpet-TIS-Addition\.gradle\loom-cache\remapped_mods\remapped\me\fallenbreath\conditional-mixin-fabric-2767f698\0.6.4\...jar`（30612 B，含 `me/fallenbreath/conditionalmixin/api/annotation/{Condition,Restriction}`） | `implementation files(...)`；运行期为 Fabric 打包，仅告警 `fml.modloadingissue.brokenfile.fabric`，不影响 S1 |
| `libs\lithium.jar` | `Carpet-TIS-Addition\.gradle\loom-cache\remapped_mods\remapped\curse\maven\lithium-360438-8f637a66\7581453\lithium-360438-8f637a66-7581453.jar`（862533 B，`0.15.2+mc1.21.1`） | `compileOnly files(...)`；A 类 22 条（`net.caffeinemc.mods.lithium.*`）清零 |
| `org.jetbrains:annotations:24.1.0` | mavenCentral | `compileOnly` |

MixinExtras 未声明（NeoForge 自带）。

### 2.2 访问权限（A-AT 23 条）
Fabric 的 `accesswidener` 在 NeoForge 侧等价物是 Access Transformer。已把上游
`Carpet-TIS-Addition\versions\1.21.1\carpet-tis-addition.accesswidener`（`accessWidener v1 named`，896 B）逐条对译成
`src\main\resources\META-INF\accesstransformer.cfg`（958 B），并在 `build.gradle` 中
`accessTransformers.from 'src/main/resources/META-INF/accesstransformer.cfg'`：

```
public net.minecraft.server.commands.FillCommand$Mode
public net.minecraft.server.level.ThreadedLevelLightEngine$TaskType
public net.minecraft.world.entity.raid.Raid$RaidStatus
public net.minecraft.world.level.LocalMobCapCalculator$MobCounts
public net.minecraft.world.level.chunk.PalettedContainer$Data
public net.minecraft.world.level.redstone.CollectingNeighborUpdater$NeighborUpdates
public net.minecraft.world.level.block.entity.CalibratedSculkSensorBlockEntity$VibrationUser
public net.minecraft.world.level.block.entity.SculkSensorBlockEntity$VibrationUser
```
覆盖并清掉的报错：`Mode is not public in FillCommand` ×5、`MobCounts` ×4、`Data` ×3、`TaskType` ×2、`VibrationUser has protected access` ×3、`RaidStatus` ×1、连带 override 失败 ×6。

---

## 3. 余下 C 类明细（本阶段按停止条件不动，留给 S2）

| 文件:行 | 缺失符号 |
|---|---|
| `carpettisaddition\CarpetTISAdditionMod.java:29` | `class ModInitializer`（`implements ModInitializer` 的接口缺失） |
| `carpettisaddition\CarpetTISAdditionMod.java:39` | override 失败（`onInitialize` 无父接口） |
| `carpettisaddition\CarpetTISAdditionMod.java:42` | `variable FabricLoader` |
| `carpettisaddition\CarpetTISAdditionPreLaunch.java:26` | `class PreLaunchEntrypoint` |
| `carpettisaddition\CarpetTISAdditionPreLaunch.java:28` | override 失败（`onPreLaunch` 无父接口） |
| `carpettisaddition\network\TISCMClientPacketHandler.java:106` | `variable FabricLoader` |
| `carpettisaddition\translations\TranslationLoader.java:67` | `variable FabricLoader` |
| `carpettisaddition\utils\mixin\AutoMixinAuditExecutor.java:32` | `variable FabricLoader` |
| `carpettisaddition\utils\mixin\testers\DevelopmentEnvironmentTester.java:31` | `variable FabricLoader` |
| `carpettisaddition\utils\compat\LithiumConfigAccess.java:39` | `variable FabricLoader` |
| `carpettisaddition\utils\compat\GameUtils.java:98` | `variable EnvType`（`net.fabricmc.api.EnvType`） |
| `carpettisaddition\utils\mapping\TinyMappingV2Reader.java:43` | `TinyV2Factory` |
| `...TinyMappingV2Reader.java:47` | `TinyVisitor` |
| `...TinyMappingV2Reader.java:58 / 73 / 79 / 85` | `MappingGetter`（4 处） |
| `...TinyMappingV2Reader.java:66` | `TinyMetadata` |
| `...TinyMappingV2Reader.java:65 / 72 / 78 / 84` | override 失败（`MappingVisitor` 的父接口缺失连带） |

涉及包：`net.fabricmc.loader.api`、`net.fabricmc.loader.api.entrypoint`、`net.fabricmc.api`、`net.fabricmc.mapping.reader.v2`。

**为什么不在 S1 处理**：任务书停止条件为「A=0 且 D=0 即停，不许碰 B/C」，且铁律 6 禁止改 `src\main\java` 任何 .java；其中 `net.fabricmc.mapping.reader.v2` 与 `FabricLoader` 语义基本无 NeoForge 直接对应物，属于典型 S2「加载器 API 替换」工作量，需改源码才能消。

---

## 4. 已知偏差 / 放弃项

1. **modId 由 `carpet-tis-addition` 改为 `carpet_tis_addition`**（平台硬约束，非本阶段选择）。
   NeoForge loader 4.0.41 校验 `^[a-z][a-z0-9_]{1,63}$`，连字符非法，首轮冒烟直接 FATAL：
   ```
   [FATAL] [ne.ne.fm.lo.mo.ModInfo/FATAL]: Invalid modId found in file ...\build\resources\main - carpet-tis-addition does not match the standard: ^[a-z][a-z0-9_]{1,63}$
   net.neoforged.neoforgespi.locating.InvalidModFileException: Invalid modId found : carpet-tis-addition (main)
   ```
   已在 `neoforge.mods.toml` 的 `[[mods]]` 与 3 个 `[[dependencies.carpet_tis_addition]]` 同步改名，并在文件头注释说明。`@Mod("carpet_tis_addition")` 同步。
2. **`conditional-mixin.jar` 是 Fabric 打包**：运行期 FML 报 `Skipping jar. ... is a Fabric mod and cannot be loaded` + `Mods loaded with 1 issues (fml.modloadingissue.brokenfile.fabric)`。compileOnly 用途不受影响，S1 不追。若 S2 起 `@ConditionalMixin` 注解需要在运行期生效，需改换 NeoForge 版的 conditional-mixin 或自带移植。
3. **`[[mixins]] config="carpet-tis-addition.mixins.json"` 仍为注释态**（任务书「本阶段先注释掉」）：`carpet-tis-addition.mixins.json` 未注册到 mods.toml，因此本阶段 1009 个 mixin 类即便编译通过也不会被应用。留 S3。
4. **冒烟用 `src/smoke/java` 单文件**（`carpettisaddition\neoforge\TISNeoForgeMod.java`）走通全链路，`-PtisFull` 才切到 `src/main/java`；切换开关在 `build.gradle` 的 `// TIS-SWITCH` 标注处。
5. **`build\classes\java\main` 类文件数 = 0**：javac 因 37 条 C 类错误整体失败，未产出 class。这是预期，不是构建配置问题（MC 5364 个源文件与 MDG 管线本身全部成功）。

---

## 5. S1 边界外但已确认的环境事实（供后续复用）

- 所有 `maven.neoforged.net` / `libraries.minecraft.net` 依赖经本机 Clash Verge 代理 `127.0.0.1:7897` 拿取（`gradle.properties` 里 `systemProp.http(s).proxyHost/Port`）。直连 `maven.neoforged.net` 因 DNS 污染 + TLS RST 不可用；BMCLAPI 未镜像 `neoform-runtime*`。
- 拉一次完整 NeoForge 管线 ≈ 9m37s（含 vineflower 反编译 113.99s、MC 重编译 83.91s）；增量编译 26s ~ 2m9s。
- 全量编译日志必须用 `cmd /c "... > log 2>&1"` 或 `Out-File` 直接重定向；`pwsh ... | Tee-Object` 会按控制台宽度断行 javac stderr 并重复输出。

---

## 11. S2 阻塞项

### B-S2-1（真阻塞，属 S3 范围）TISCM 自己的 accessor mixin 未注册 → 命令注册期 ClassCastException

- 现象（`_run_s2.log`）：`java.util.concurrent.ExecutionException: java.lang.ExceptionInInitializerError`
  `Caused by: java.lang.ClassCastException: class net.minecraft.network.chat.contents.TranslatableContents cannot be cast to class carpettisaddition.mixins.utils.messenger.TranslatableTextAccessor`
  `at carpettisaddition.utils.Messenger.copy(Messenger.java:595)` ← `Messenger.fancy(Messenger.java:211/:229)` ← `DyeCounter.<init>(DyeCounter.java:58)` ← `SupplierCounter.<init>(SupplierCounter.java:38)` ← `SupplierCounterCommand.createCounterForColor(SupplierCounterCommand.java:47)` ← `DyeCounterCommand.<init>(DyeCounterCommand.java:59)` ← `SupplierCounterCommand.<clinit>(SupplierCounterCommand.java:30)` ← `carpettisaddition.CarpetTISAdditionServer.registerCommands(CarpetTISAdditionServer.java:178)` ← `carpet.CarpetServer.lambda$registerCarpetCommands$7(CarpetServer.java:162)`
- 根因：`TranslatableTextAccessor` 是 TISCM 自己的 `@Mixin(TranslatableContents.class) ... implements` accessor，需要 `carpet-tis-addition.mixins.json` 被注册后才会真正织入 `TranslatableContents`。S2 铁律明令**不许打开** `neoforge.mods.toml` 的 `[[mixins]]`，也不许改 mixins.json，所以这条必然出现。日志可佐证：`Mixin apply failed` 计数 = 0（没有任何 mixin 在跑）。
- 处置：**S3** 打开 `[[mixins]]`（并按需处理 conditional-mixin 的 mixin plugin），本条自然消失。S2 未做任何绕过（没改 `Messenger`、没改 `CarpetTISAdditionServer`、没加 try/catch）。

### B-S2-2（已解决，留档）conditional-mixin / FabricLoader / tiny-mappings-parser 在 dev 运行期不可见

- 现象：`Skipping jar. File ...\libs\conditional-mixin.jar is a Fabric mod and cannot be loaded`；随后
  `NoClassDefFoundError: [Lme/fallenbreath/conditionalmixin/api/annotation/Restriction;`（`CarpetRuleRegistrar.java:68` 读 `@Rule` 注解时触发）、
  `NoClassDefFoundError: net/fabricmc/loader/api/FabricLoader`（conditional-mixin 的 `VersionCheckerImpl`）、
  `NoClassDefFoundError: net/fabricmc/mapping/reader/v2/TinyVisitor`（`StackTraceDeobfuscator.java:63`，异步 `TISCM Mapping` 线程）。
- 机制：gradle `runtimeClasspath` 上的普通 jar 不在 FML 的 `ModuleClassLoader` 可见范围；`run\mods\` 里手搓 jar 会报 `is not a valid mod file`；`configurations.detachedConfiguration` 里依赖 `sourceSets.main.compileClasspath` 会与 `stripConditionalMixin` 形成 `Circular dependency between the following tasks`。
- 处置（均已落地，见 PORT-STATE §10.2 第 4/5/6 条）：把三者做成带 `META-INF/neoforge.mods.toml` 的 dev 库 mod（`conditional_mixin_lib` / `tiny_mappings_parser_lib`）挂在运行期 classpath 上。当前 `_run_s2.log` 里三处 `NoClassDefFoundError` 全部为 0，Mod List 也能看到这两个 shim，`TISCM Mapping` 线程成功 `Yarn mapping file yarn-1.21.1+build.3-v2.tiny loaded`。

### B-S2-3（口径说明，非阻塞）`net\.fabricmc` 仍有 8 行

- 4 行是 fabric-api compat 反射/`@Mixin(targets=...)`（`LargeBarrelFabricAPIHack.java:55,60`、`ItemStorageMixin.java:39`、`ItemStorageMixin_Old.java:39`），是 S3 的 compat 开关，任务书明令不许动。
- 4 行是 `TinyMappingV2Reader.java:24-27` 的 import，该文件不在 S2 可改名单，只能靠依赖（tiny-mappings-parser）消错 —— 已消，编译期 0 errors。
- 结论：C 类**编译错误**清零；`net.fabricmc` 字面残留见上，无需（也不允许）在 S2 处理。

## 12. S3 阻塞项

### B-S3-1（**已于 S4 关闭**）24 条 mixin 注入在 `defaultRequire=1` 下必失败

- 现象：正式配置 `injectors.defaultRequire=1` 时 `runServer` 在第一条命中处抛
  `MixinApplyError` → `InvalidInjectionException: Critical injection failure: …` / `Invalid descriptor on …:<条目>`，
  FML 侧表现为 `java.lang.reflect.InvocationTargetException` + `Caused by: org.spongepowered.asm.mixin.throwables.MixinApplyError`。
- 已定位的完整清单：**24 条 / 15 个文件**，见 `MIXIN-LOG.md` §5（含每条原始 fatal 摘要）、`build\mixin-defect-table.tsv`、`build\mixin-seed.txt`。
- 两类根因：
  - **A 类 15 条（intermediary 名残留）**：注解值 / `@Shadow` 字段名仍是 Fabric 的 `method_13389`、`field_27208` 之类。上游靠 refmap 在构建期重映射；NeoForge 开发环境是 Mojmap 且**不加载 refmap**，于是 0 候选。全仓库此类硬编码共 21 处（`build\intermediary-all.txt`），A 类条目与之逐条对应。
  - **B 类 9 条（真实 API 漂移）**：`carpet` 是 NeoForge fork（`onServerLoaded(ServerAboutToStartEvent)` 等签名已变），叠加 1.21.1 自身重构（`CrashReport`、`BucketItem`、`EnchantCommand`、`PhantomSpawner` 等），导致 handler 描述符或 `@At` 目标失效。
- 为什么 S3 不修：修 A 类要把注解字符串改成 `javap -p` 实测到的 Mojmap/合成名（`lambda$…$n`），修 B 类要改 handler 参数表或换 `@At` 目标——**都在 mixin `.java` 里**，S3 铁律"1009 个 .java 一字节不动"禁止。
- 处置（S4 第一条任务）：按 `MIXIN-LOG.md` §5 的表逐条改；改完用 `tools\mixin-fatal-loop3.ps1` 同一流程复跑（`defaultRequire=1`，直到 `Done` 且清单为空）。不许把条目从 config 里删 / 注释，也不许长期依赖 `require=0`。
- 影响面：A 类覆盖 `opPlayerNoCheat`(5)、`tickCommandPermission`、`tickCommandEnhance`、`command.stop`、`command.lifetime.{playerlogin,playerlogout,mobthrow}`、`keepMobInLazyChunks`、`creativeNoClip`(1/2)、`vaultBlacklistDisabled`、`largeBarrel`；B 类覆盖 `deobfuscateCrashReportStackTrace`、`carpet.hooks`（服务器对象获取）、`command.lifetime.{mobpickup,slime}`、`creativeNetherWaterPlacement`、`creativeNoClip`(xpOrb)、`gazeDisguiseEquipmentExtended`、`enchantCommandNoRestriction`、`logger.phantom`。

### B-S3-2（已证伪的死路，留档）refmap 在 NeoForge 开发环境不可用

- 手写 `tis-refmap.json`（`mappings: {…: "field_6757": "this$0"}`）+ config 里挂 `refmap` 字段，三轮实测都不命中：Mixin 日志恒定 `No refMap loaded.`。
- 原因：refmap 只在存在 obfuscation context 的环境生效；NeoForge dev 是 Mojmap，`MixinEnvironment.getObfuscationContext()` 为空；Fabric 的 `mixin.env.remapRefMap` / `refMapRemappingFile` 在 NeoForge 无对应实现。
- 结论：**不要再用 refmap 补偿**；A 类只能改源码里的名字。

### B-S3-3（口径阻塞，非阻塞）`@Restriction` 跳过名单运行期不可枚举

- `RestrictiveMixinConfigPlugin` 在条件不满足时只走 `LOGGER.debug`，而 Mixin 自建 log4j2 context 会覆盖外部配置（`-Dlog4j.configurationFile`、自定义 `log4j2-survey.xml`、`-Dlog4j.debug` 三种都试过），DEBUG 行一律引不出来。
- 因此 P-c 只能按"静态判定 + 期望行为"处理：这批条目被跳过是**正确行为**，不进排除清单、不算缺陷。
- 若 S4 需要精确名单：在 `CarpetTISAdditionMixinPlugin` 里把 `debug` 换成 `info`（属 .java 改动，与 B-S3-1 一起做）。

## 13. 下一步（仅一条）

**S4：修 B-S3-1 的 24 条**（先 A 类 15 条：按 `build\intermediary-all.txt` 的文件:行，用 `javap -p` 实测目标类的 Mojmap/合成名替换；再 B 类 9 条：对齐 fork 与 1.21.1 签名），修完把 `tools\mixin-fatal-loop3.ps1` 跑到 `Done` 且缺陷清单为空。

## 14. S4 关闭记录 + S5 阻塞项

### 14.1 B-S3-1 关闭（S4 完成，S5 复核）

- 24 条全部修完：A 类 15 条把 `method_\d+` / `field_\d+` 换成 `javap -p` 实测的 Mojmap/合成名，B 类 9 条对齐 fork 与 1.21.1 签名。
- 复核证据（S5 阶段实测，非重新核对）：`_run_s4_final.log` = `_run_s4_iter12.log` 原件，`Done (4.044s)!`，`Mixin apply failed` / `Critical injection failure` / `InvalidInjectionException` / `Invalid descriptor on` 全为 0；全仓库 `(method|field)_\d+` 命中 **0**；`_run_s5.log` 本轮再次跑到 `Done` 且同样 4 类致命串 0 条。
- 状态：**CLOSED**。原处置口径（不许 require=0 / 不许删条目）在本阶段继续遵守。

### B-S5-1（真阻塞）`CustomPayloadC2SPacketMixin` 的 `@ModifyArg` 在 1.21.1 下 0 候选 → `GameProtocols` clinit 炸 → 假玩家全链路失效

- 现象（`run\logs\latest.log:942`，`_run_s5.log` 同）：
  ```
  [Server thread/ERROR] [mixin/]: <!-- /* InjectionError: Critical injection failure: Argument modifier method
  registerTISCMPayload_c2s(Ljava/util/List;)Ljava/util/List; in carpet-tis-addition.mixins.json:network.CustomPayloadC2SPacketMixin
  from mod carpet_tis_addition failed injection check, (0/1) succeeded. Scanned 0 target(s). No refMap loaded. */ -->
  ```
- 运行时形态（`_run_s5.log:1016`，`require=1` 下按设计抛异常而非只打日志）：
  ```
  Caused by: org.spongepowered.asm.mixin.injection.throwables.InjectionError: Critical injection failure: Argument
  modifier method registerTISCMPayload_c2s(Ljava/util/List;)Ljava/util/List; in carpet-tis-addition.mixins.json:network.
  CustomPayloadC2SPacketMixin from mod carpet_tis_addition failed injection check, (0/1) succeeded. Scanned 0 target(s).
  No refMap loaded.
    at ...MinecraftServer.lambda$spin$2(MinecraftServer.java:267)
  ```
- 为什么 S4 的四个致命串计数是 0：S4 的验收只跑到 `Done` 就退出，全程无玩家/payload 交互，`GameProtocols`（`CustomPacketPayload.codec` 的宿主类）从未被加载，该 mixin 的注入器从未执行 → 缺陷处于潜伏态。S5 发 `/player Steve spawn` 后 `GameProtocols.<clinit>` 首次触发，注入器第一次执行即 0 目标。**S4 的 0 = 未触发，不是通过。**
- 影响链：`net.minecraft.network.protocol.game.GameProtocols` 的 `<clinit>` 依赖 `CustomPacketPayload.codec(...)` 的注入 → clinit 抛 `ExceptionInInitializerError` → TISCM 自定义 payload 注册表不完整 → `/player Steve spawn` 的假玩家 `createFake` 任务失败（响应体为空，`/list` 仍 0 玩家），日志三处 `createFake delayed task error`（L901 / L1029 / L1520）。
- 根因方向：目标方法锚在 `CustomPayloadC2SPacket` 的 `<clinit>` 内 `INVOKE CustomPacketPayload;codec(...)`，1.21.1 Mojmap 下该方法体的 codec 调用形态已变（`StreamCodec` 泛型擦除 + `codec` 变为 `CustomPacketPayload.Type` 相关的静态辅助），`@ModifyArg` 找不到可修饰的参数。
- 为什么不现场修：修它必须改 mixin `.java`（换 `@At` 目标或改 handler 描述符），S5 铁律只允许跑命令看行为 → 记 B-S5-x 并跳过该条用例。

### B-S5-2（真阻塞）`LithiumConfigAccess` 静态初始化在 mixin 阶段调用 `ModList.get()` → `NoClassDefFoundError`

- 现象：
  - 启动期 5 条 ERROR（L39/L40/L54/L86/L87）：`Failed to instantiate a ConditionTester from class carpettisaddition.utils.mixin.testers.LithiumEntityWorldTickSchedulerTester[ $Inverted] / LithiumBlockHopperTester / LithiumEntityMovementOptimizationTester: java.lang.reflect.InvocationTargetException`。这些 tester 是 conditional-mixin 的 `@Restriction(require = @ConditionType(...))` 判定器，构造失败 → 相关 mixin 的跳过判定退化为"最保守"。
  - 运行期：`/carpet hopperXpCounters true` → `NoClassDefFoundError: Could not initialize class carpettisaddition.utils.mixin.LithiumConfigAccess`，`BlockableEventLoop` 打 `[FATAL] Error executing task on Server`（L1411）。
- 根因：S2 把该类的 `isModLoaded(x)` 改成 `ModList.get().isLoaded(x)`（`LithiumConfigAccess.java:39`），但静态字段初始化（`:35`）在 Mixin `PREPARE` 阶段就可能触发，此时 `ModList.get()` 仍为 null；S3 给 fabric-loader-shim 加的 early-bootstrap 分支没覆盖这条直连 NeoForge API 的路径。
- 为什么不现场修：属 .java 改动（把静态初始化改成惰性 + null 守卫），S5 禁改。

### B-S5-3（口径阻塞，非代码缺陷）验收清单与 1.82 实际命令树不符

- `/tis`（含 `help/info/raid/speedtest`）、`/counter`（含 `redstone/dye`）、`/man`、`/stat` 共 10 条在 1.82 源码中**没有对应根节点**（`literal("tis"|"counter"|"man"|"stat")` 全 0 命中，`/help <root>` 一律 Unknown command）。
- `/carpet tisAdditionEnabled` 四个分支同理：1.82 的 `CarpetTISAdditionSettings` 里没有 `tisAdditionEnabled` 这个规则。
- 处置：按任务书判定口径记为 MISSING / SPEC-MISMATCH，**不当作移植缺陷**；后续验收清单应按真实命令树重写（见 `S5-ACCEPT.md` §2.1）。

## 15. 下一步（仅一条）

**S6：修 B-S5-1 与 B-S5-2 两处 .java（`network\CustomPayloadC2SPacketMixin.java` 换 `@At` 锚点；`utils\compat\LithiumConfigAccess.java` 静态初始化改惰性 + 空守卫），修完重跑 `/player <name> spawn`、`/carpet hopperXpCounters true` 两条用例，并确认 `latest.log` 的 9 ERROR + 1 FATAL 归零。**


## 16. S6 结论（B-S5-1 / B-S5-2 / B-S6-1 关闭，新增 B-S6-2 已修、B-S6-3 遗留）

### B-S6-1 — 已修复（关闭）：7 条翻译键名尾部多一个 `.`

- 修法：`build.gradle:237` `flattenTranslationNode` 识别子键 `"."` 时把前缀保持为父键（不再拼出一个空段）。
- 验收：`build/resources/main/assets/carpettisaddition/lang/{en_us,zh_cn}.json` 与上游 jar 双向键集合 diff = **0/0**（各 1142 键），7 条键值逐字一致。
- 回归：`lang/*.yml` 未改；下次构建自动重建，不会被覆盖问题反复。

### B-S5-2 — 已修复（关闭）：`LithiumConfigAccess` 静态初始化期调 `ModList.get()`

- 修法：`src/main/java/carpettisaddition/utils/mixin/LithiumConfigAccess.java:41-81` 改惰性双检缓存 + `isModListReady()` / `isLithiumLoaded()` 的 null 守卫，PREPARE 阶段返回 null 且不缓存。
- 验收：`_s6_server2.log` 里 `ConditionalMixinTester` 0 条、`NoClassDefFoundError` 0 条、`FATAL` 0 条、`ERROR` 0 条；`/carpet hopperXpCounters` 路径不再炸。

### B-S5-1 — 已修复（关闭）：`CustomPayloadC2SPacketMixin` 的 `@ModifyArg` 0 候选

- 走**路线 A**（修描述符），依据：`ServerboundCustomPayloadPacket.<clinit>` 结构未变，只是 `CustomPacketPayload.codec` 在 1.21.1 变成 4 参数（多了 `ConnectionProtocol` + `PacketFlow`）。javap 原文见 `MIXIN-LOG.md` §12。
- 验收：`/player Steve spawn` 有正常响应、`kill` 后再 `spawn` 不报错；日志 `Error while creating fake player` 0 条，`GameProtocols` 的 `ExceptionInInitializerError` 0 条。mixin 应用数 444 → 453。

### B-S6-2 — 新发现并已修复（关闭）：NeoForge 未注册 custom payload → 客户端连服被踢

- 现象：客户端进世界即 `Connection lost`，服务端 `Unknown or unaccepted channel: tiscm:network/v1`。
- 根因：1.21.1 NeoForge 的 `ServerCommonPacketListenerImpl.handleCustomPayload` 对不在 **NeoForge payload 注册表**里的 channel 直接 disconnect；上游 Fabric 版没有这张表（靠 mixin 改静态 codec 列表即可）。
- 修法：新建 `src/main/java/carpettisaddition/network/TISCMNetworkRegistry.java`，用 `RegisterPayloadHandlersEvent` + `PayloadRegistrar` 双向注册 `tiscm:network/v1`；T3 的 mixin 保留（职责不同）。
- 验收：`[TISCM] registered payload channel tiscm:network/v1` → 建世界 → `Dev joined the game`，全程不掉线，`/carpet`、`/speedtest`、`/info world weather`、`/log` 均可用。

### B-S6-3 — **S8 部分关闭（T3 运行期取证完成 3/4，剩 teleport_hint）**：三组文本的运行期验证

- 涉及：`command.raid.status`、`tracker.lifetime.{spawn_reason,removal_reason}.*`、`util.teleport_hint`。
- **S8 结果（生产专服 `_s8_srv` + `tools\s5-rcon.ps1`，原文见 `S8-ACCEPT.md` §3）**：
  - ✅ `raid.status`：`/effect give <fakeplayer> bad_omen` → 落进村庄 bounds → `/raid list` 回 `Overworld Raid Count: 1` / `- Raid #2` / `Status: ongoing` / `Center: [-319, 71, -288]` / `Bad omen level: 1` / `Waves: 1/5` / `Raiders: x5`，`/raid list full` 还列出 5 个 pillager 坐标。
  - ✅ `spawn_reason.trans_dimension`：僵尸走传送门进地狱 → `/lifetime zombie spawning` 的地狱段 `Trans-dimension (from Overworld): 1, (17.4/h) 100.0% [S]`。
  - ✅ `removal_reason.trans_dimension` / `removal_reason.death`：`/lifetime zombie removal` 的 Overworld 段 `Trans-dimension (to Nether): 1`、`Death (genericKill): 4`（含 min/max/avg lifetime 921/976/943.25 gt）、`Randomly despawn: 1`。
  - ⚠️ `util.teleport_hint`：客户端 hover tooltip，RCON 通道拿不到 → **本轮明确不取证**，仅静态验证（`src\main\java\carpettisaddition\utils\Messenger.java:307` `tr("teleport_hint", dest)`、`:317` `tr("teleport_hint.dimension")`，键值双向 diff = 0）。
- 剩余：仅 `teleport_hint` 一条，需要**客户端**截图 hover 才闭环；不阻塞发布。
- 历史（S6/S7）结果保留：`/raid list` → `No raid exists`、`/raid start` 被权限挡下、`/lifetime pig` → `Lifetime Tracker has not started`；兜底 `en_us`/`zh_cn` 与上游双向 diff = 0。

### B-S6-4 — 记录（非缺陷）：客户端 12 条 client mixin 的真实分布 —— **S7 已按实测修正，关闭**

- 10 条 applied；`carpet.hooks.onServerLoadedWorlds.IntegratedServerMixin` 被上游 `@Restriction(<1.16)` 合法跳过；`carpet.tweaks.robustness.network.ClientNetworkHandlerMixin` 是 `@Mixin(DummyClass.class)` 空壳（1.21.1 无实现）。
- 因此「12 条全部 applied」这个验收口径本身需要按此重述：**applied 10 + 合法跳过 1 + 空壳 1 = 12，apply 失败 0**。
- **S7 修正（以真机 + dev 双份日志逐条实测为准）**：实际是 **9 applied + 2 合法跳过 + 1 空壳 = 12**。被 `@Restriction(<1.16)` 跳过的有**两条**：`carpet.hooks.onServerLoadedWorlds.IntegratedServerMixin` **和** `rule.structureBlockOutlineDistance.StructureBlockBlockEntityMixin`（真机 `build\shots\s7\client-latest.log` L106/L107；dev `run\logs\latest.log` L101/L102，reason 均为 `does not matches version predicates [<1.16]`）。真机与 dev 完全同构 → 非移植回归。口径以 S7 为准。

## 17. S7 结论（出发布包 + 真机验收）：产物已交付，新增 B-S7-1..B-S7-3

发布包：`D:\Minecraft-mod-development\TIS-NeoForge\build\libs\carpet-tis-addition-1.82.0+neoforge.1.21.1.jar`
（1 804 524 bytes，SHA-256 `0B2CA7CB21854208F88FF3ED773DBF6672A8EA7C9A124FCD02846433AB2A3EE9`，包体检 8 组全 PASS，真机 0 ERROR / 0 FATAL）。

### B-S7-1 — 记录（上游行为，非缺陷）：`/speedtest` 默认关闭且客户端侧不支持

- `CarpetTISAdditionSettings.java:139` `commandSpeedTest = "false"` → 不做 `/carpet commandSpeedTest true` 时 `/speedtest` 直接 `Unknown or incomplete command`。
- 开了规则后：裸 `/speedtest` 打印用法（`Test the network connection speed and ping. Test size limit: 10MiB; Client supported: no`）；`/speedtest ping 1` 在 `tiscmNetworkProtocol=true` 下回 `Client does not support TISCM speed test. Please install or update the Carpet TIS Addition mod`。
- 根因（上游设计）：`src/main/java/carpettisaddition/mixins/carpet/tweaks/robustness/network/ClientNetworkHandlerMixin.java` 是 `@Mixin(DummyClass.class)` + 注释 `// impl in [mc1.15, mc1.20.2)`，即 1.20.2 之后上游就没有客户端侧 TISCM 网络处理器。
- 处置：**不修**（S7 不许改 587 条清单/业务逻辑）；在 `RELEASE-NOTES.md` §5.4/§5.5 里写明用法与限制。

### B-S7-2 — **已关闭（S8-T4 证伪原结论）**：不是"HUD 面板不渲染"，是 HUD 走 Tab 列表

- 原 S7 结论（**错**）：「该 carpet fork 本身没有客户端 HUD 渲染入口」。
- S8 复核（同口径字节扫描 + `javap`，fork `libs\carpet.jar` 716 class vs 上游 `remapped.carpet-fabric-carpet-8f637a66-1.21-1.4.147+v240613.jar` 715 class）：
  - `carpet/logging/HUDController` 的引用集合两边**完全相同**（`carpet/CarpetServer.class`、`carpet/logging/HUDController.class`、`carpet/logging/HUDLogger.class`、`carpet/script/external/Carpet.class`）。
  - 两边 `javap -p carpet.logging.HUDController` 方法表一致：`register` / `resetScarpetHUDs` / `addMessage` / `clearPlayer` / `update_hud(MinecraftServer, List<ServerPlayer>)` + 私有 `send_tps_display` / `send_counter_info` / `packetCounter` —— **没有任何 render 方法**。
  - 两边 `carpet.mixins.json` client 段 **14 条同名同集合**（含 `Gui_tablistMixin`、`PlayerTabOverlayMixin`），真机日志两条都在应用（`16:45:02.220` / `16:45:02.298`）。
  - 上游源码 `update_hud`（`build\s8\HUDController.upstream.java:105-114`）：`Set<ServerPlayer> targets = new HashSet<>(player_huds.keySet());` → `ClientboundTabListPacket packet = new ClientboundTabListPacket(scarpet_headers..., Messenger.c(player_huds...));` → `player.connection.send(packet);`
- 结论：**carpet 的 HUD 就是 Tab（玩家列表）的表头/表尾**，渲染由原版 `PlayerTabOverlay` 完成，两边都没有独立叠加层渲染入口。所以 `/log mobcaps` 之后要按 **Tab 键**看列表表尾，屏幕上本来就没有浮层 → 上游行为，非移植缺陷（结论 (B)，见 `S8-ACCEPT.md` §4）。
- 处置：`RELEASE-NOTES.md` §5.6 已重写；不再把它当"已知缺陷/依赖侧限制"记录。改清单去加 Gui 渲染钩子仍是禁止项（S7 铁律）。

### B-S7-3 — 记录（环境偏差，非阻塞）：真机环境与交接描述的三处不一致

1. 本机**没有** `%APPDATA%\.minecraft`：真机是 PCL2（`D:\PCl2\.minecraft`），版本隔离目录 `versions\1.21.1-NeoForge_21.1.264` 就是 gameDir，`mods` 在其下。
2. 真机 NeoForge 是 **21.1.264**（交接写 21.1.209），FML 4.0.45；`neoforge.mods.toml` 的 `neoforge [21.1.0,)` 满足，验收有效。
3. PCL2 无法脚本化 → 自建 `tools\s7_launch.ps1`（读版本 json、classpath 去重、natives 只取 `:natives-windows$`、拼 `--launchTarget forgeclient`）。验收结论仍成立：启动链路就是 NeoForge 21.1.264 官方 profile。

### 17.x 环境已恢复（可复核）

- `versions\1.21.1-NeoForge_21.1.264\mods`：测期放 2 个 jar，测完用 `mods.bak` 还原 → 现 **0 文件**（`mods.bak` 保留作凭据）。
- `saves`：测试世界 `New World` 已移到 `D:\Minecraft-mod-development\TIS-NeoForge\build\s7\saves-evidence\` → 实例 `saves` 现 0 项。
- `crash-reports`：本次 `-Dmixin.debug.countInjections=true` 实验产生的 `crash-2026-10-25_16.42.38-client.txt` 已移到 `build\s7\crash-evidence\` → 实例 `crash-reports` 现 0 项。

## 18. S8 结论（T1 口径 / T2 安装包 / T3 取证 / T4 HUD 重查 / T5 专服）

### 18.1 关闭与新增一览

| 编号 | 状态 | 一句话 |
|------|------|--------|
| B-S7-2（HUD） | **关闭（证伪）** | 不是"HUD 不渲染"，carpet HUD 走 Tab 列表表尾，两边都没有独立渲染入口，见上 |
| B-S6-3（三文本） | **部分关闭** | `raid.status` + `spawn/removal_reason.{trans_dimension,death}` 已运行期取证；仅 `teleport_hint` 需客户端 hover，仍开 |
| B-S8-1 | **新增（已记录，非缺陷）** | 生产专服的 `Mixing ... into` 只在 `logs\debug.log`（DEBUG 级），`latest.log` 数出 0 → 口径陷阱 |
| B-S8-2 | **新增（已根治）** | 单装本 jar 必崩（缺 carpet 前置）→ 已改为发 `dist\TIS-Addition-NeoForge-1.21.1.zip` 双 jar 安装包 |
| B-S8-3 | **新增（遗留，不阻塞）** | `util.teleport_hint` 的 hover tooltip 无法用 RCON 取证，需客户端截图 |
| B-S7-1 / B-S7-3 | 维持 | `/speedtest` 上游行为；真机环境偏差（21.1.264 / PCL2），均非缺陷 |

### 18.2 B-S8-1 — 口径陷阱：专服 mixin 行只在 debug.log

- 现象：`_s8_srv\logs\latest.log`（63985 B / 357 行）里 `Mixing ... from carpet-tis-addition.mixins.json into` **0 条**；同一轮 `_s8_srv\logs\debug.log`（832898 B / 3447 行）里 **461 条**，`[TISCM] Disabled mixin` 72 条，ERROR/FATAL 0。
- 原因：生产 NeoForge 把 mixin 的应用日志按 DEBUG 输出；dev（`gradlew runServer`/`runClient`）与 S7 真机客户端因为日志配置不同，落到了 INFO 的 `latest.log`（这就是 464 的来源）。
- 处置：新增 `tools\s8-count.ps1`，一次可喂多个日志；`RELEASE-NOTES.md` §6.2 明写「专服 applied = 0 是日志级别假象」。**不改日志配置**（改 verifier 侧配置等于改验收口径，不允许）。

### 18.3 B-S8-2 — 单装本 jar 必崩 → 发安装包（已根治）

- 现象（用户首次装机）：`Mod carpet_tis_addition requires carpet 1.4.147 or above / Currently, carpet is not installed`，`Failed to start the minecraft server`，进程退出。
- S8 复现（生产专服，移走 `mods\carpet-*.jar` 后重启）：日志原文与用户完全一致，记录在 `build\s8\nocarpet-latest.log`。
- 处置：`dist\TIS-Addition-NeoForge-1.21.1.zip` 内 `mods\` 直接放两个 jar + 中文 `README.txt`（含报错原文与"必须两个都放"），`RELEASE-NOTES.md` §2 顶部加 ⚠ 警告块。校验：zip 条目 3 个、解压回环字节数/SHA-256 一致。

### 18.4 B-S8-3 — teleport_hint 仍未动态取证（明确标注，不伪造）

- `util.teleport_hint` 只出现在客户端聊天组件的 hover tooltip 里，RCON 只能拿聊天栏纯文本，拿不到 tooltip 结构。
- 本轮**明确标注为"需客户端手动 hover，本轮不取证"**，按 ⚠️ 仅静态验证 记录：`Messenger.java:307` / `:317` 两个调用点 + `en_us`/`zh_cn` 键值 diff = 0。
- 不阻塞发布；若要闭环，需要一次真机客户端 hover 截图（`build\shots\s8\` 目前无此截图）。

## 19. S9 结论（拆掉拍平 conditional-mixin）：B-S9-1 已修，新增 B-S9-2 记录待查

### 19.1 关闭与新增一览

| 编号 | 状态 | 一句话 |
|------|------|--------|
| B-S9-1（模块包冲突 / `ResolutionException`） | **已修** | 主 jar 不再拍平 `me/fallenbreath/**` 与 `net/fabricmc/**`，conditional-mixin 改为独立模块 jar + `neoforge.mods.toml` 显式依赖；C1b 复现 → C2/C3 同机验证消失 |
| B-S9-2（51 条版本谓词 `Disabled mixin` 诊断丢失） | **新增（记录，待查）** | Disabled 72 → 21，缺的全部是 `minecraft@1.21.1 does not matches version predicates` 类；applied 集合未变，mixin 行为等价，**诊断信息缺失** |
| B-S8-1 / B-S8-2 / B-S8-3 | 维持 | 口径陷阱 / 安装包根治 / teleport_hint 待客户端截图 |

### 19.2 B-S9-1 — 模块解析冲突（已修）

- 现象（用户实例）：`D:\PCl2\.minecraft\versions\DIS机械动力航空学6.0.10-1.3.2\logs\latest.log:453` `[main/ERROR] [cpw.mods.modlauncher.ModuleLayerHandler/]: Error while resolving modules.`；`:454` `java.lang.module.ResolutionException: Modules carpet_tis_addition and conditional_mixin export package me.fallenbreath.conditionalmixin.api.mixin to module jecharacters`。
- 根因：TweakerMoreForge 的 jar-in-jar 提供 modId `conditional_mixin`（官方 `conditional-mixin-neoforge-0.6.4.jar`），而我们的发布 jar 把同一批类拍平在 `me/fallenbreath/**`（外加 `net/fabricmc/loader/**` shim 与 `net/fabricmc/mapping/**`）→ 两个 module 导出同一包。
- 本地复现（NeoForge 21.1.264 专服）：`_s9_logs\S9C1.latest.log:26` `ResolutionException: Modules carpet_tis_addition and tiny_mappings_parser_lib export package net.fabricmc.mappings.model to module carpet`；去掉 tiny 后再现 `S9C1b.latest.log:24` `ResolutionException: Module conditional_mixin contains package me.fallenbreath.conditionalmixin.api.annotation, module carpet_tis_addition exports package … to conditional_mixin`。
- 修法摘要：
  1. `build.gradle:90-153` 用 `packageConditionalMixin`（普通任务，`inputs.file` + `outputs.file` + `Files.copy`）取代 `stripConditionalMixin` / `genConditionalMixinShimMeta` / `compileFabricLoaderShim`，产物 = vendored 官方 `libs\conditional-mixin-neoforge-0.6.4.jar` 原样复制到 `build\libs`（28 507 B，modId `conditional_mixin`，version 0.6.4，license `LGPL-3.0-only`，18 个 `me/fallenbreath/**` class，无 `net/**`）。
  2. `releaseJar` 只留 `from sourceSets.main.output` + `exclude 'me/**'` + `exclude 'net/**'`（主 jar 1645 条目 / 1 689 654 B）。
  3. 主 jar `neoforge.mods.toml` 新增 `[[dependencies.carpet_tis_addition]] modId="conditional_mixin" type="required" versionRange="[0.6.4,)" ordering="NONE" side="BOTH"`。
  4. 编译依赖整体切到官方 neo jar（`VersionChecker` 公开签名与 Fabric 产物逐方法一致，唯一调用点 `settings\TISCMRule.java:85` 不受影响）。
  5. dist 改为独立模块发行：`mods\tiny-mappings-parser-0.3.0+build.17.jar`（必需，缺则 stdout 抛 `NoClassDefFoundError: net/fabricmc/mapping/reader/v2/TinyVisitor`）+ `optional\conditional-mixin-neoforge-0.6.4.jar`。
- 验收：`tools\s9-run-env.ps1` 七环境跑完 —— A 预期失败（`S9A.latest.log:25/26` `requires conditional_mixin 0.6.4 or above` / `Currently, conditional_mixin is not installed`）；B `Done (7.536s)!` + applied 437 + ERROR/FATAL/apply-failed 0/0/0；C2 `Done (8.094s)!`、C3 `Done (9.028s)!`，两者 `ResolutionException` 均为 0。主 jar `java tools\ZipArchive.java assert-absent <jar> me net` → `RESULT: PASS`。
- 证据边界：修复后的包**尚未在用户实例的 NeoForge 21.1.252 上复测**；21.1.252 只被证明能加载到模块解析阶段（`RELEASE-NOTES.md` §2 兼容性表）。

### 19.3 B-S9-2 — 版本谓词类 `Disabled mixin` 诊断行消失（记录，待查）

- 现象：`[TISCM] Disabled mixin` 计数 S8 专服 72 / 客户端 74 → S9 专服 21。
- 按 reason 分组：S8 的 72 条里 53 条是 `due to mod minecraft@1.21.1 does not matches version predicates [...]`（46 个 `<1.x`、4 个 `<=`、2 个 `>=26.2`、1 个 `>1`），其余 19 条是 required mod 缺失（lithium/malilib/fabric-transfer-api-v1/mctester/minihud/tic_tacs）+ required mixin class disabled + tester result=false；S9 的 21 条全部落在后一类，版本谓词 0 条。
- 行为等价证据：①applied 唯一类集合 S8(srv-run2) 438 vs S9B 432，only-in-S8 8 条全部是「需要玩家/指令才加载目标类」的项（raid、spawning.breeding、microtiming ChainRestrictedNeighborUpdaterMixins$StateReplacementEntryMixin、chatMessageLengthLimitUnlocked.StringHelperMixin 等），only-in-S9 2 条；②`carpet.tweaks.command.tickWarpMaximumDuration.TickCommandMixin`（目标 `TickCommand` 启动期必加载）在 S8 有 Disabled 行、S9 既无 Disabled 行也无 `Mixing ... into TickCommand`，而同目标类的另外 5 条 mixin 正常 applied → **仍被跳过，仅诊断行丢失**。
- 初判：`RestrictiveMixinConfigPlugin.shouldApplyMixin` → `RestrictionCheckers.memorized().checkRestriction(...)` → `SimpleRestrictionChecker.checkConditions` / `onRestrictionCheckFailure` 这条链上，S8 用的是 Fabric 产物 `api/util/fabric/VersionCheckerImpl` + 自造 fabric-loader shim，S9 换成官方 neo 产物 `api/util/neoforge/VersionCheckerImpl`（`doesVersionSatisfyPredicate(Object,String)` 走 `org.apache.maven.artifact.versioning.ArtifactVersion`），失败回调没被调用。
- 影响：不影响 mixin 应用结果（等价已证），只影响排障可读性。下一轮做源码级排查（解包 `SimpleRestrictionChecker` 看 `ClassNotFoundException` / `setFailureCallback` 分支）。
- 不阻塞发布。

### 19.4 下一步（仅一条）

S10：源码级定位 B-S9-2（conditional-mixin neo 产物的注解读取 / 失败回调路径），并把 S9 交付在客户端真机复验一次。
### 19.5 S9c 变更（不新增阻塞，仅同步数值）
图标换成像素画版并重出 dist 后：主 jar 1 674 134 B / `722A60C1…B193BA31`；三 jar 版 dist 2 917 758 B / `D52FA3B7…B3ABB897`；单文件版 2 913 077 B / `20039133…FB1628B9`。B-S9-1 仍是「已修」，B-S9-2 仍是「记录，待查」——换图标不影响 mixin 诊断行为（`_s9_logs\S9ICON.latest.log` 复验 `Done`、ERROR 0）。

### 19.6 S9d 变更（仅数值同步）
图标定稿为变体 D（用户选定）并重出：主 jar 1 674 227 B / `4FE83BAE…D556D805`；三 jar 版 dist 2 917 847 B / `E3A830BA…7B75A5FE`；单文件版 2 913 174 B / `7C106ABF…9412BACF`。阻塞状态不变（B-S9-1 已修、B-S9-2 记录待查）；`_s9_logs\S9ICON2.latest.log` 复验 `Done`、ERROR 0。

### 19.7 PCL2 / HMCL 显示文件名（S9e，状态：**已修**）

- 现象：PCL2 社区版 mod 列表显示 `carpet-tis-addition-1.82.0+neoforge.1.21.1` 两行 + 默认立方体图标。
- 根因：PCL2 CE `ModLocalComp.vb:810` 仅认 `META-INF/mods.toml`（配合 `mcmod.info` / `fabric.mod.json` / `quilt.mod.json`），无 `neoforge.mods.toml` 分支；exe 字符串扫描 0 命中 `neoforge.mods.toml`，`logoFile` 亦 0 命中（故图标不读 jar 内资源）。
- 修法：主 jar 增补 `META-INF/mods.toml`（377 B，`displayURL`/`authors` 置于根段落以命中 `TomlData(0)` 读取；`[[mods]]` 提供 modId/version/displayName/description/logoFile）。不改 NeoForge 侧 `neoforge.mods.toml`。
- 验收：`S9B3.latest.log` `Done (5.381s)` + 单条 mod 条目 + `ERROR/FATAL 0/0`；`tools\pcl-parse-sim.py` 对旧 jar 输出 `FAIL … falls back to filename`、对新 jar 输出 `RESULT: PCL2 will show 'Carpet TIS Addition (NeoForge port)'`。
- 残留：启动器图标需上架 CurseForge/Modrinth 才能自定义显示；HMCL 未取源码验证（按同一 mini-parser 行为推断）。
