# MIXIN-LOG — S3（mixin 容器接入 + conditional-mixin 落地）

- 项目：Carpet TIS Addition v1.82.0 → NeoForge 21.1.209 / 1.21.1（Mojmap，JDK 21）
- 阶段：S3（只做 mixin 容器；1009 个 .java 一字节未动）
- 结论时间：2026-10-25

---

## 1. 结论速览

| 项 | 结果 |
|---|---|
| `CarpetTISAdditionMixinPlugin` 能否实例化 | ✅ 可以。`me.fallenbreath.conditionalmixin` 与其 `RestrictiveMixinConfigPlugin` 经 `conditional_mixin_lib` dev mod 可见，`[[dependencies]] required` 生效，无 `NoClassDefFoundError: me/fallenbreath/...` |
| 599 条是否都被解析/注册 | ✅ 正式配置 587 + client 12 全部进入 Mixin 注册表；无 `IllegalClassLoadError`（说明 package 归属唯一、无第二份 config 抢包） |
| 正式配置 `injectors.defaultRequire = 1` 下能否启动 | ❌ 失败，且**不是容器问题**：24 条 mixin 的注入元数据与 NeoForge/1.21.1 实际目标不一致，Mixin 在第一条命中时抛 `MixinApplyError` 中断启动 |
| 排掉这 24 条后 | ✅ `runServer` 走到 `Done (4.044s)!`（`_run_s4_iter12.log`，S4 定稿原件另存为 `_run_s4_final.log`），`Mixin apply failed` = 0 |
| 这 24 条能否在 S3 内修好 | ❌ 需要改 15 个 mixin `.java`（改注释值/签名），S3 铁律禁改 → 已逐条落到 §5 + BLOCKERS.md（B-S3-1） |
| 因条件跳过（P-c）的条数 | ⚠️ 静态可判，运行时不可观测（原因见 §7） |

一句话：**容器接上了；剩下的 24 条是 mixin 源码自身的移植债，不是容器债。**

---

## 2. 阶段步骤（按执行顺序）

| # | 动作 | 结果 |
|---|---|---|
| S3.0 | `carpet-tis-addition.mixins.json` 的 `"{{COMPATIBILITY_LEVEL}}"` → `"JAVA_21"` | ✅ 全资源目录 `\{\{` 复核：只剩 `icon.png` 二进制误命中；`COMPATIBILITY_LEVEL` 命中 0 |
| S3.1 | 小刀冒烟：新建 `tis-mixin-smoke.json`（只放 `carpet.access.MessengerInvoker`），toml 只开它 | ⚠️ 死路，见 §8-1：`IllegalClassLoadError` 证明"同 package 只能有一个 config"，冒烟配置会把 587 条的同包类全部判为"未注册" |
| S3.2 | conditional-mixin 可见性验证 | ✅ `conditional_mixin_lib`（`build\libs\conditional-mixin-lib.jar`）+ `tiny_mappings_parser_lib` + `fabric-loader-shim` 三个 dev mod 均按 S2 结论工作；未走 C 分支（`mergeMixinPluginClasses` 不需要） |
| S3.3 | 兼容层加固（不改判定逻辑）：`FabricLoader`/`Version` shim 增加 early-bootstrap 分支 | ✅ mixin `PREPARE` 阶段 `ModList.get()==null` 时改走 `LoadingModList.getMods()` + `FMLLoader.versionInfo()`，lithium/conditional-mixin 的 `VersionChecker` 不再 NPE |
| S3.4 | `tis-mixin-survey.json`（`defaultRequire=0`）一次性勘测 599 条 | ✅ `Done (4.569s)!`，`Mixin apply failed`=0，applied 525；但这**不是**验收，见 §6 的判定口径 |
| S3.5 | 正式语义（`defaultRequire=1`）逐条定位首个致命项，一次一条，直到 `Done` | ✅ 24 条，见 §5；`_run_s3_fatal2_4.log` 达 `Done` |

---

## 3. 容器验收证据

正式语义最终运行（`_run_s4_iter12.log` = `_run_s4_final.log`，587 + 12 条注册；下面的 Mixing 行摘自同族运行 `_run_s3_fatal2_4.log`，Done 时间以 S4 定稿运行为准）：

```
[13:34:03] [Server thread/INFO] [mixin/]: Mixing command.spawn.natualSpawning.NatualSpawnerMixin from tis-mixin-survey.json into net.minecraft.world.level.NaturalSpawner
[13:34:03] [Server thread/INFO] [mixin/]: Mixing carpet.tweaks.rule.creativeNoClip.IWorldOverridesMixin from tis-mixin-survey.json into carpettisaddition.helpers.mixin.IWorldOverrides
...
[14:28:37] [Server thread/INFO] [minecraft/DedicatedServer]: Done (4.044s)! For help, type "help"
```

> S5 勘误：原文写作 `Done (4.743s)`，与 `_run_s4_iter12.log:892` 实测不符，实际为 `Done (4.044s)`（第 892 行原文：
> `[14:28:37] [Server thread/INFO] [minecraft/DedicatedServer]: Done (4.044s)! For help, type "help"`）。

计数核对（同一次运行）：

| 指标 | 值 | 命令 |
|---|---|---|
| `Mixing … from tis-mixin-survey.json into …` | 415 | `Select-String _run_s3_fatal2_4.log -Pattern 'Mixing .+? from tis-mixin-survey\.json into'` |
| `Mixin apply failed` | **0** | 同上 `-Pattern 'Mixin apply failed'` |
| `NoClassDefFoundError: me/fallenbreath` | 0 | `-Pattern 'NoClassDefFoundError: me/fallenbreath'` |
| `IllegalClassLoadError` | 0 | `-Pattern 'IllegalClassLoadError'` |
| `Compatibility level` 告警 | 3 | 仅 carpet.jar / conditional_mixin_lib / tiny_mappings 三个第三方 config 的提示，非本模组 |

---

## 4. 599 条的账目（require=1，最终运行）

```
599 = 587 (mixins) + 12 (client)
    = 24  (硬失败，§5 清单)                     ← 必须在 java 侧修
    + 415 (本次运行真实 applied)
    + 148 (未 applied 服务端条目)
    + 12  (client-only，dedicated server 不会加载)
```

`148` 的成因：Mixin 只在**目标类第一次被 transform 时**打印 `Mixing …` 行。裸 `runServer` 从 `Done` 到退出只有几秒，`command.*`/`logger.*`/`rule.*` 里大量目标类（各种 `*Command`、`*Hud`、只在特定规则路径才加载的类）根本不会被加载，因此它们的注入既不会 applied 也不会报错。

**判定口径结论**：`applied + 条件跳过 = 599` 这一条在"裸 runServer + Mixin verbose"下**不可测**——它既不是失败也不是成功，而是"目标类未被加载"。可测的等价证据是：

- `Mixin apply failed` = 0；
- `defaultRequire=1` 下把 24 条硬失败移出后能 `Done`；
- 剩余 563 条中每一条要么 applied，要么因目标类未加载，要么被 `@Restriction` 跳过（§7）。

---

## 5. 24 条硬失败清单（require=1 下必中断启动）

分类口径：
- **A = intermediary 名残留**：注解/字段里写的是 Fabric 的 `method_xxxxx`/`field_xxxxx`，NeoForge（Mojmap，无 refmap）解析不到。
- **B = 目标或签名漂移**：目标方法/字段还在，但签名、注入点或 handler 描述符与 fork/1.21.1 不一致。

| # | 条目（config 相对名） | 类 | 失败摘要（原始日志截断） | 类 |
|---|---|---|---|---|
| 1 | `rule.opPlayerNoCheat.GameModeCommandMixin` | `GameModeCommandMixin.java:40` | `@ModifyReturnValue … could not find any targets matching 'method_13389'` | A |
| 2 | `rule.opPlayerNoCheat.GiveCommandMixin` | `GiveCommandMixin.java:40` | `… matching 'method_13404'` | A |
| 3 | `rule.opPlayerNoCheat.SetBlockCommandMixin` | `SetBlockCommandMixin.java:40` | `… matching 'method_13627'` | A |
| 4 | `rule.opPlayerNoCheat.SummonCommandMixin` | `SummonCommandMixin.java:40` | `… matching 'method_13693'` | A |
| 5 | `rule.opPlayerNoCheat.TeleportCommandMixin` | `TeleportCommandMixin.java:41-42` | `… matching 'method_13763' or 'method_13764'` | A |
| 6 | `rule.tickCommandPermission.TickCommandMixin` | `TickCommandMixin.java:45` | `@Inject … matching 'method_54709'` | A |
| 7 | `command.stop.StopCommandMixin` | `StopCommandMixin.java:36` | `@Inject … matching 'method_13676'` | A |
| 8 | `command.lifetime.removal.playerlogout.PlayerManagerMixin` | `...\playerlogout\PlayerManagerMixin.java:35` | `@ModifyVariable … matching 'method_31441'` | A |
| 9 | `command.lifetime.spawning.playerlogin.PlayerManagerMixin` | `...\playerlogin\PlayerManagerMixin.java:40` | `@ModifyReturnValue … matching 'method_18241'` | A |
| 10 | `rule.tickCommandEnhance.ServerPlayNetworkHandlerMixin` | `ServerPlayNetworkHandlerMixin.java:73` | `@ModifyVariable … matching 'method_14365'` | A |
| 11 | `rule.keepMobInLazyChunks.ServerWorldMixin` | `ServerWorldMixin.java:43` | `@WrapOperation … matching 'method_31420'` | A |
| 12 | `carpet.tweaks.rule.creativeNoClip.EntityPredicatesMixin` | `EntityPredicatesMixin.java:42` | `@ModifyReturnValue … could not find any …`（源内为 `method_24517`） | A |
| 13 | `rule.vaultBlacklistDisabled.VaultSharedDataMixin` | `VaultSharedDataMixin.java:37` | `@ModifyExpressionValue … could not find any …`（源内为 `method_56791`） | A |
| 14 | `command.lifetime.spawning.mobthrow.DolphinEntityPlayWithItemsGoalMixin` | `DolphinEntityPlayWithItemsGoalMixin.java:40,51` | `@Shadow field field_6757 was not located in Dolphin$PlayWithItemsGoal. No refMap loaded.` | A |
| 15 | `rule.largeBarrel.BarrelBlockEntityViewerCountManagerMixin` | `BarrelBlockEntityViewerCountManagerMixin.java:46,65` | `@Shadow field field_27208 was not located in BarrelBlockEntity$1. No refMap loaded.` | A |
| 16 | `rule.deobfuscateCrashReportStackTrace.CrashReportMixin` | `CrashReportMixin.java` | `Variable modifier method deobfuscateCrashReportStackTrace_applyCrashReport2(Ljava/lang/Throwable;)…` → 0 候选 | B |
| 17 | `carpet.hooks.CarpetServerMixin` | `CarpetServerMixin.java` | `Invalid descriptor on …:carpet.hooks.CarpetServerMixin`（fork 的 `onServerLoaded` 已改吃 `ServerAboutToStartEvent`） | B |
| 18 | `command.lifetime.removal.mobpickup.ItemEntityMixin` | `...\mobpickup\ItemEntityMixin.java` | `Callback method lifetimeTracker_recordRemoval_mobPickup_playerPickupItem(Player,CallbackInfo,ItemStack,…)` 注入检查 0/1 | B |
| 19 | `command.lifetime.spawning.slime.SlimeEntityMixin` | `...\slime\SlimeEntityMixin.java` | `Argument modifier method lifetimeTracker_recordSpawning_slimDivision(Entity)Entity` 0/1 | B |
| 20 | `rule.creativeNetherWaterPlacement.BucketItemMixin` | `BucketItemMixin.java` | `Callback method creativeNetherWaterPlacement_impl(Z, Player)Z` 0/1 | B |
| 21 | `carpet.tweaks.rule.creativeNoClip.ExperienceOrbEntityMixin` | `ExperienceOrbEntityMixin.java` | `Callback method …_xpOrb_enter(CallbackInfo)V` 0/1 | B |
| 22 | `rule.gazeDisguiseEquipmentExtended.EnderManMixin` | `EnderManMixin.java` | `Callback method gazeDisguiseEquipmentExtended_pretendThisIsACarvedPumpkin(ItemStack, Item)ItemStack` 0/1 | B |
| 23 | `rule.enchantCommandNoRestriction.EnchantCommandMixin` | `EnchantCommandMixin.java` | `Callback method enchantCommandNoRestriction_removeAcceptableCheck(Z)Z` 0/1 | B |
| 24 | `logger.phantom.PhantomSpawnerMixin` | `PhantomSpawnerMixin.java` | `Variable modifier method resetFlag_phantomLogger(ServerLevel, LocalBooleanRef)ServerLevel` 0/1 | B |

统计：**A 类 15 条**（纯元数据/名字问题），**B 类 9 条**（真实 API 漂移），合计 24 条、涉及 15 个文件。

### 5.1 A 类存在的根源（重要）

全仓库 `method_\d+` / `field_\d+` 硬编码只有 **21 处 / 15 文件**（`build\intermediary-all.txt`），与 A 类的 15 条完全对应（差额来自 client-only 与 `@Restriction` 提前跳过的条目）：

```
src\main\java\carpettisaddition\mixins\...\GameModeCommandMixin.java:40  method = "method_13389",  // lambda method
src\main\java\carpettisaddition\mixins\...\TickCommandMixin.java:45      @Inject(method = "method_54709", ...)
src\main\java\carpettisaddition\mixins\...\BarrelBlockEntityViewerCountManagerMixin.java:46  field_27208;
...（完整 21 行见 build\intermediary-all.txt）
```

上游 TIS 用 Fabric 的 refmap 在构建期把这些名字重映射到生产环境；NeoForge 开发环境是 Mojmap 且 **不会加载 refmap**（详见 §8-2），于是这些名字原样进了解析器 → 0 候选。

---

## 6. 为什么 `defaultRequire=0` 的"Done"不能算验收

`injectors.defaultRequire=0` 只是把注入失败从 **throw** 降级为 **warning**，启动能过，但：

- 本次勘测里 `Mixin apply failed` 计数仍为 0（Mixin 用 `InvalidInjectionException` 打日志，不用这句话）；
- 真正的失败信号在 `... [main/FATAL] [MixinProcessor/]: /* org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException: Critical injection failure: ... */` 这种 FATAL 块里；
- 所以 require=0 下"启动成功"完全不能证明注入成功。**验收必须用 require=1**，这也正是 S3 用 require=1 逐条定位的原因。

---

## 7. P-c（`@Restriction` 条件跳过）的处置口径

`CarpetTISAdditionMixinPlugin` 继承 `RestrictiveMixinConfigPlugin`，判定发生在 `onRestrictionCheckFailed`：

- 已知 147 个 mixin 文件带 `@Restriction`（`ModIds` / `DevelopmentEnvironment` / `OS` / `GameEnvironment` / `MCVersion` 及其 `Inverted` 变体）；
- 判定不满足时条目被静默剔除，**不产生任何 INFO 级日志**；
- conditional-mixin 的日志点在 `LOGGER.debug`，而 Mixin 在 log4j2 自建 context（`log4j.configurationFile` 被 Mixin 覆盖）里初始化，三次尝试都没能把 DEBUG 行引出来（§8-3）。

因此 P-c 的运行时名单在 S3 内**不可枚举**，只给静态口径：这批条目"按条件正确跳过"属于期望行为，不是缺陷，不需要也不允许把它们写进正式配置的排除清单。

`ModIds` 测试器在 NeoForge 下的取值已由 S2 的 shim 提供（`ModList.get().isLoaded` / `ModList.getModContainerById`），语义与原 Fabric 一致；`DevelopmentEnvironment` 已被改成 `!FMLEnvironment.production`（S2 授权改动）。

---

## 8. 死路记录（别再重试）

1. **小刀冒烟配置**：单条 `MessengerInvoker` 进独立 config → `IllegalClassLoadError: Illegal classload request for accessor mixin carpettisaddition.mixins.utils.messenger.TranslatableTextAccessor. The mixin is missing from tis-mixin-smoke.json which owns package carpettisaddition.mixins.*`。**同一个 package 只能由一份 config 拥有**，所以任何"只放几条"的勘测配置都会把其余同包类判死。勘测必须整包（589/599 条）跑。
2. **给 `@Shadow field_xxxxx` 加 refmap**：NeoForge 开发环境 `MixinEnvironment.getObfuscationContext()` 为空，refmap 不参与解析；手写 `tis-refmap.json` 并挂 `refmap` 字段对 `field_6757` / `field_27208` 无效（实测三次：dotted 键、`null` 键、带 namespace 键全不命中）。Fabric 的 `mixin.env.remapRefMap` 在 NeoForge 不存在对应实现。
3. **`-Dlog4j.configurationFile` 引 DEBUG**：Mixin 会自己初始化 log4j2 context，外部配置文件被覆盖；`-Dmixin.debug.verbose=true` 只放大 Mixin 自身的 INFO，不放开 conditional-mixin 的 `LOGGER.debug`。
4. **`pwsh` 的 `Tee-Object` 重定向 gradle**：会断开 javac 的 stderr 行，必须 `cmd /c ".\gradlew.bat ... > log 2>&1"`（S2 结论，本轮继续遵守）。

---

## 9. 证据文件

| 文件 | 内容 |
|---|---|
| `_run_s3_fatal2_4.log` | require=1 最终 `Done` 运行（563+12 注册、applied 415、0 apply failed） |
| `_run_s3_fatal2_1..23.log` | 逐条定位过程，每条日志的首个 `Critical injection failure` 就是清单里的一条 |
| `_run_s3_req1_15.log` | 早期"噪声排除集"运行（88 条剔除后可 `Done`，用于证伪更宽的范围） |
| `_run_s3_survey*.log` | require=0 勘测运行 |
| `build\mixin-defect-table.tsv` | 24 条 + 原始 fatal 摘要 |
| `build\intermediary-all.txt` | 全仓库 21 处 intermediary 硬编码（文件:行） |
| `build\mixin-seed.txt` | 24 条条目名（直接可用于 S4 的修复清单） |
| `tools\mixin-fatal-loop3.ps1` | 逐条定位脚本（一次一条，直到 `Done`） |

---

## 10. S4 定稿账目（补记）

| 项 | 值 | 证据 |
|---|---|---|
| 正式配置注册条目 | 587 + client 12 | `carpet-tis-addition.mixins.json` |
| `Done` 时间 | **4.044s** | `_run_s4_iter12.log`（原件另存 `_run_s4_final.log`） |
| applied | 439 条 | 同上 |
| `Mixin apply failed` / `Critical injection failure` / `InvalidInjectionException` / `Invalid descriptor on` | 0 / 0 / 0 / 0 | 同上 |
| 全仓库 `(method|field)_\d+` | 0 命中 | S5 复核 |
| S3 的 `TranslatableTextAccessor` ClassCastException | 消失 | 同上 |

> §1 / §3 原先写的 `Done (4.743s)` 是笔误，已在本轮按原件订正为 `4.044s`。

---

## 11. `@Restriction` 跳过名单判读（`build\restriction-skipped.txt`，71 条）

生成方式：`CarpetTISAdditionMixinPlugin.onRestrictionCheckFailed` 的日志级别在 S4 已由 `LOGGER.debug` 改为 `LOGGER.info`，本轮带 `-Dmixin.debug.verbose=true` 跑一次 `runServer`，抓全部 `[TISCM] Disabled mixin ... due to ...` 行。

统计：**71 条 = 53 条版本谓词 + 17 条依赖 mod 缺失 + 1 条级联**；**判为"条件写错（缺陷）"的：0 条**。

口径：
- **Fabric 专属（合规）**：`required mod X not found` —— 依赖的是 Fabric 生态 mod（lithium / tic_tacs / mctester / fabric-transfer-api-v1 / malilib / minihud），NeoForge 侧不存在这些 mod id；`libs\lithium.jar` 只是编译期 classpath，不是 mod，故判"找不到"是**正确行为**。
- **版本谓词（合规）**：`mod minecraft@1.21.1 does not matches version predicates [...]` —— 上游为其它 MC 版本写的兼容分支（`<1.15` / `<1.16` / `<1.16.2` / `<1.17` / `<1.18` / `<1.19` / `<=1.19` / `<=1.19.4-pre3` / `<=1.20.2` / `<1.20.3` / `<1.20.4` / `<1.20.2-alpha.0` / `>=1.16 <1.17` / `>1.21.1` / `>=26.2`），1.21.1 不满足是设计意图。
- **级联（合规）**：`required mixin class ... disabled` —— 依赖的 `ItemStorageMixin` 已因 fabric-transfer-api-v1 缺失被跳过。

> 注意：本阶段真正暴露的两处**缺陷**（`CustomPayloadC2SPacketMixin` 注入 0 候选、`LithiumConfigAccess` 静态初始化炸）**不在**这份跳过名单里 —— 它们是"没被跳过但注入/初始化失败"，见 `BLOCKERS.md` B-S5-1 / B-S5-2。

| # | mixin 条目 | 跳过原因 | 判读 |
|---|---|---|---|
| 1 | `carpet.access.LoggerRegistryInvoker` | mod minecraft@1.21.1 does not matches version predicates [<1.15] | 版本谓词（合规）—— `<1.15` 不含 1.21.1 |
| 2 | `carpet.access.LoggerRegistryMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.15] | 版本谓词（合规）—— `<1.15` 不含 1.21.1 |
| 3 | `carpet.access.MessengerInvoker` | mod minecraft@1.21.1 does not matches version predicates [<1.15] | 版本谓词（合规）—— `<1.15` 不含 1.21.1 |
| 4 | `carpet.shape.texthack.ShapesRendererRenderedTextGlyphVisitorMixin` | mod minecraft@1.21.1 does not matches version predicates [>=26.2] | 版本谓词（合规）—— `>=26.2` 不含 1.21.1 |
| 5 | `carpet.tweaks.bugfixes.fakePlayerNoKnockBack.PlayerEntityMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.16] | 版本谓词（合规）—— `<1.16` 不含 1.21.1 |
| 6 | `carpet.tweaks.command.tickWarpMaximumDuration.TickCommandMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.20.3] | 版本谓词（合规）—— `<1.20.3` 不含 1.21.1 |
| 7 | `carpet.tweaks.logger.projectile.ThrownEntityMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.16] | 版本谓词（合规）—— `<1.16` 不含 1.21.1 |
| 8 | `carpet.tweaks.messenger.MessengerMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.16] | 版本谓词（合规）—— `<1.16` 不含 1.21.1 |
| 9 | `carpet.tweaks.rule.creativeNoClip.EndGatewayBlockEntityMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.16] | 版本谓词（合规）—— `<1.16` 不含 1.21.1 |
| 10 | `carpet.tweaks.rule.creativeNoClip.PressurePlateBlockMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.20] | 版本谓词（合规）—— `<1.20` 不含 1.21.1 |
| 11 | `carpet.tweaks.rule.creativeNoClip.WeightedPressurePlateBlockMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.20] | 版本谓词（合规）—— `<1.20` 不含 1.21.1 |
| 12 | `carpet.tweaks.rule.creativeNoClip.compat.lithium.WorldHelperMixin` | required mod lithium not found | Fabric 专属（合规）—— `lithium` 不是 NeoForge mod |
| 13 | `command.info.entity.EntityInfoMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.17] | 版本谓词（合规）—— `<1.17` 不含 1.21.1 |
| 14 | `command.lifetime.deathdamage.AbstractMinecartEntityMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.20.4] | 版本谓词（合规）—— `<1.20.4` 不含 1.21.1 |
| 15 | `command.lifetime.deathdamage.BoatEntityMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.20.4] | 版本谓词（合规）—— `<1.20.4` 不含 1.21.1 |
| 16 | `command.lifetime.deathdamage.VehicleEntityMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.20.4] | 版本谓词（合规）—— `<1.20.4` 不含 1.21.1 |
| 17 | `command.lifetime.removal.conversion.ZombieEntityMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.16] | 版本谓词（合规）—— `<1.16` 不含 1.21.1 |
| 18 | `command.lifetime.removal.conversion.ZombieVillagerEntityMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.16] | 版本谓词（合规）—— `<1.16` 不含 1.21.1 |
| 19 | `command.lifetime.removal.despawn.HostileEntityMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.15] | 版本谓词（合规）—— `<1.15` 不含 1.21.1 |
| 20 | `command.lifetime.spawning.breeding.AnimalMateGoalMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.16] | 版本谓词（合规）—— `<1.16` 不含 1.21.1 |
| 21 | `command.lifetime.spawning.conversion.ZombieEntityMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.16] | 版本谓词（合规）—— `<1.16` 不含 1.21.1 |
| 22 | `command.lifetime.spawning.conversion.ZombieVillagerEntityMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.16] | 版本谓词（合规）—— `<1.16` 不含 1.21.1 |
| 23 | `command.lifetime.spawning.playerlogin.ServerPlayerEntityMixin` | mod minecraft@1.21.1 does not matches version predicates [>1.21.1] | 版本谓词（合规）—— `>1.21.1` 不含 1.21.1 |
| 24 | `command.lifetime.spawning.summon.VillagerEntityMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.19] | 版本谓词（合规）—— `<1.19` 不含 1.21.1 |
| 25 | `command.tick.warp.TickCommandMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.20.3] | 版本谓词（合规）—— `<1.20.3` 不含 1.21.1 |
| 26 | `command.xcounter.compat.lithium.HopperBlockEntityMixin` | required mod lithium not found | Fabric 专属（合规）—— `lithium` 不是 NeoForge mod |
| 27 | `logger.lightqueue.compat.tic_tacs.LightingExecutorMixin` | required mod tic_tacs not found | Fabric 专属（合规）—— `tic_tacs` 不是 NeoForge mod |
| 28 | `logger.microtiming.events.blockupdate.BlockMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.19] | 版本谓词（合规）—— `<1.19` 不含 1.21.1 |
| 29 | `logger.microtiming.events.compat.lithium.LithiumTileTickListMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.18] | 版本谓词（合规）—— `<1.18` 不含 1.21.1 |
| 30 | `logger.microtiming.tickstages.spawning.WanderingTraderManagerMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.16] | 版本谓词（合规）—— `<1.16` 不含 1.21.1 |
| 31 | `logger.microtiming.tickstages.tiletick.ServerWorld_TileTickDetailMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.18] | 版本谓词（合规）—— `<1.18` 不含 1.21.1 |
| 32 | `misc.devqol.mctester.McTesterConfigMixin` | required mod mctester not found | Fabric 专属（合规）—— `mctester` 不是 NeoForge mod |
| 33 | `misc.devqol.mctester.TestCommandMixin` | required mod mctester not found | Fabric 专属（合规）—— `mctester` 不是 NeoForge mod |
| 34 | `network.CustomPayloadC2SPacketAccessor` | mod minecraft@1.21.1 does not matches version predicates [<1.20.2-alpha.0] | 版本谓词（合规）—— `<1.20.2-alpha.0` 不含 1.21.1 |
| 35 | `network.CustomPayloadS2CPacketAccessor` | mod minecraft@1.21.1 does not matches version predicates [<1.20.2-alpha.0] | 版本谓词（合规）—— `<1.20.2-alpha.0` 不含 1.21.1 |
| 36 | `rule.cauldronBlockItemInteractFix.CauldronBlockMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.17] | 版本谓词（合规）—— `<1.17` 不含 1.21.1 |
| 37 | `rule.chunkUpdatePacketThreshold.ChunkHolderMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.16] | 版本谓词（合规）—— `<1.16` 不含 1.21.1 |
| 38 | `rule.entityBrainMemoryUnfreedFix.BrainAccessor` | mod minecraft@1.21.1 does not matches version predicates [<=1.19.4-pre3] | 版本谓词（合规）—— `<=1.19.4-pre3` 不含 1.21.1 |
| 39 | `rule.entityBrainMemoryUnfreedFix.EntityMixin` | mod minecraft@1.21.1 does not matches version predicates [<=1.19.4-pre3] | 版本谓词（合规）—— `<=1.19.4-pre3` 不含 1.21.1 |
| 40 | `rule.hopperNoItemCost.compat.lithium.HopperBlockEntityMixin` | required mod lithium not found | Fabric 专属（合规）—— `lithium` 不是 NeoForge mod |
| 41 | `rule.hopperNoItemCost.compat.lithium.HopperHelperMixin` | required mod lithium not found | Fabric 专属（合规）—— `lithium` 不是 NeoForge mod |
| 42 | `rule.largeBarrel.compat.fabricapi.ItemStorageMixin` | required mod fabric-transfer-api-v1 not found | Fabric 专属（合规）—— `fabric-transfer-api-v1` 不是 NeoForge mod |
| 43 | `rule.largeBarrel.compat.fabricapi.DoubleInventoryAccessor` | required mixin class carpettisaddition.mixins.rule.largeBarrel.compat.fabricapi.ItemStorageMixin disabled | 级联（合规）—— 依赖条目已被跳过 |
| 44 | `rule.largeBarrel.compat.fabricapi.ItemStorageMixin` | required mod fabric-transfer-api-v1 not found | Fabric 专属（合规）—— `fabric-transfer-api-v1` 不是 NeoForge mod |
| 45 | `rule.largeBarrel.compat.fabricapi.ItemStorageMixin_Old` | required mod fabric-transfer-api-v1 not found | Fabric 专属（合规）—— `fabric-transfer-api-v1` 不是 NeoForge mod |
| 46 | `rule.largeBarrel.compat.lithium.BarrelBlockMixin` | required mod lithium not found | Fabric 专属（合规）—— `lithium` 不是 NeoForge mod |
| 47 | `rule.largeBarrel.compat.malilib.BlockEntityTypeMixin` | required mod malilib not found | Fabric 专属（合规）—— `malilib` 不是 NeoForge mod |
| 48 | `rule.largeBarrel.compat.malilib.IDataSyncerMixin` | required mod malilib not found | Fabric 专属（合规）—— `malilib` 不是 NeoForge mod |
| 49 | `rule.largeBarrel.compat.malilib.IInventoryOverlayHandlerMixin` | required mod malilib not found | Fabric 专属（合规）—— `malilib` 不是 NeoForge mod |
| 50 | `rule.largeBarrel.compat.malilib.InventoryUtilsMixin` | required mod malilib not found | Fabric 专属（合规）—— `malilib` 不是 NeoForge mod |
| 51 | `rule.largeBarrel.compat.minihud.EntitiesDataManagerMixin` | required mod minihud not found | Fabric 专属（合规）—— `minihud` 不是 NeoForge mod |
| 52 | `rule.lightEngineMaxBatchSize.MinecraftServerMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.16] | 版本谓词（合规）—— `<1.16` 不含 1.21.1 |
| 53 | `rule.lightEngineMaxBatchSize.ServerLightingProviderMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.16] | 版本谓词（合规）—— `<1.16` 不含 1.21.1 |
| 54 | `rule.minecartFullDropBackport.AbstractMinecartEntityMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.19] | 版本谓词（合规）—— `<1.19` 不含 1.21.1 |
| 55 | `rule.minecartFullDropBackport.MinecartVariablesMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.19] | 版本谓词（合规）—— `<1.19` 不含 1.21.1 |
| 56 | `rule.oakBalloonPercent.OakSaplingGeneratorMixin` | mod minecraft@1.21.1 does not matches version predicates [<=1.20.2] | 版本谓词（合规）—— `<=1.20.2` 不含 1.21.1 |
| 57 | `rule.optimizedFastEntityMovement.compat.lithium.EntityMixin` | required mod lithium not found | Fabric 专属（合规）—— `lithium` 不是 NeoForge mod |
| 58 | `rule.optimizedHardHitBoxEntityCollision.EntityTrackingSectionMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.17] | 版本谓词（合规）—— `<1.17` 不含 1.21.1 |
| 59 | `rule.optimizedHardHitBoxEntityCollision.WorldChunkMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.17] | 版本谓词（合规）—— `<1.17` 不含 1.21.1 |
| 60 | `rule.railDupingFix.AbstractRailBlockMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.16.2] | 版本谓词（合规）—— `<1.16.2` 不含 1.21.1 |
| 61 | `rule.structureBlockLimit.StructureBlockBlockEntityMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.16] | 版本谓词（合规）—— `<1.16` 不含 1.21.1 |
| 62 | `rule.structureBlockLimit.UpdateStructureBlockC2SPacketMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.16] | 版本谓词（合规）—— `<1.16` 不含 1.21.1 |
| 63 | `rule.tileTickLimit.ServerTickSchedulerMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.18] | 版本谓词（合规）—— `<1.18` 不含 1.21.1 |
| 64 | `rule.tileTickLimit.compat.lithium.LithiumServerTickSchedulerMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.18] | 版本谓词（合规）—— `<1.18` 不含 1.21.1 |
| 65 | `rule.totallyNoBlockUpdate.BlockMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.19] | 版本谓词（合规）—— `<1.19` 不含 1.21.1 |
| 66 | `rule.totallyNoBlockUpdate.RedstoneWireBlockMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.19] | 版本谓词（合规）—— `<1.19` 不含 1.21.1 |
| 67 | `rule.totallyNoBlockUpdate.WorldMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.19] | 版本谓词（合规）—— `<1.19` 不含 1.21.1 |
| 68 | `rule.voidRelatedAltitude.AbstractDecorationEntityMixin` | mod minecraft@1.21.1 does not matches version predicates [>=1.16 <1.17] | 版本谓词（合规）—— `>=1.16 <1.17` 不含 1.21.1 |
| 69 | `rule.voidRelatedAltitude.AbstractMinecartEntityMixin` | mod minecraft@1.21.1 does not matches version predicates [<1.17] | 版本谓词（合规）—— `<1.17` 不含 1.21.1 |
| 70 | `settings.ParsedRuleAccessor` | mod minecraft@1.21.1 does not matches version predicates [<=1.19] | 版本谓词（合规）—— `<=1.19` 不含 1.21.1 |
| 71 | `translations.ClientSettingsC2SPacketAccessor` | mod minecraft@1.21.1 does not matches version predicates [<1.18] | 版本谓词（合规）—— `<1.18` 不含 1.21.1 |


---

## 12. S6 取证：`ServerboundCustomPayloadPacket` / `CustomPacketPayload`（B-S5-1）与 NeoForge payload 注册（B-S6-2）

### 12.1 javap 原文（路线 A 的依据）

工具与输入：

```
javap = C:\Program Files\Java\jdk-21.0.11\bin\javap.exe
jar   = C:\Users\xiao_\.gradle\caches\neoformruntime\intermediate_results\compiledWithNeoForge_0166c820696cc2f2876873532bdc8ee0a3eded51_output.jar
javap -p -c -classpath <jar> net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket
javap -p    -classpath <jar> net.minecraft.network.protocol.common.custom.CustomPacketPayload
```

（原始输出已落盘：`build/s6-javap-1.txt`、`build/s6-javap-2.txt`）

字段（两个 codec 都在这个类里，都由 `<clinit>` 初始化，没有别处）：

```
public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf,
    net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket> STREAM_CODEC;
public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf,
    net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket> CONFIG_STREAM_CODEC;
```

`static {}`（`<clinit>`）里对 `CustomPacketPayload.codec` 的调用点 —— **两处，PLAY 与 CONFIGURATION 各一**：

```
static {};
 ...
 45: invokestatic  #96   // InterfaceMethod net/minecraft/network/protocol/common/custom/CustomPacketPayload.codec:
                        //   (Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload$FallbackProvider;
                        //    Ljava/util/List;
                        //    Lnet/minecraft/network/ConnectionProtocol;
                        //    Lnet/minecraft/network/protocol/PacketFlow;)
                        //   Lnet/minecraft/network/codec/StreamCodec;
 ...
 63: putstatic     #112  // Field STREAM_CODEC:Lnet/minecraft/network/codec/StreamCodec;
 ...
111: invokestatic  #96   // InterfaceMethod net/minecraft/network/protocol/common/custom/CustomPacketPayload.codec:(...同上 4 参数...)
129: putstatic     #118  // Field CONFIG_STREAM_CODEC:Lnet/minecraft/network/codec/StreamCodec;
```

`CustomPacketPayload` 当前只有两个 `codec` 重载（没有旧的 2 参数 `(FallbackProvider, List)` 形式）：

```
public static <B extends io.netty.buffer.ByteBuf, T extends CustomPacketPayload>
    StreamCodec<B, T> codec(StreamMemberEncoder<B, T>, StreamDecoder<B, T>);

public static <B extends FriendlyByteBuf> StreamCodec<B, CustomPacketPayload>
    codec(CustomPacketPayload$FallbackProvider<B>,
          java.util.List<CustomPacketPayload$TypeAndCodec<? super B, ?>>,
          net.minecraft.network.ConnectionProtocol,
          net.minecraft.network.protocol.PacketFlow);
```

**结论（路线 A）**：宿主类、`<clinit>`、List 参数位置都没变，唯一变化是描述符多了 `ConnectionProtocol` + `PacketFlow`。旧 target
`...codec(...FallbackProvider;Ljava/util/List;)L.../StreamCodec;` 在 1.21.1 里**不存在**，所以 `@ModifyArg` 扫 0 目标。
修法只需把 `@At.target` 换成上面的 4 参数描述符；`<clinit>` 里两处调用都被修饰，于是 `STREAM_CODEC` 与 `CONFIG_STREAM_CODEC`
都带上 TISCM payload（这正是假玩家 `createFake` 需要的握手阶段 codec）。参数列表中 `List` 唯一，不需要显式 `index`。

### 12.2 B-S6-2 取证：NeoForge 为什么还会踢「未注册通道」

`build/mc-src/ServerCommonPacketListenerImpl.java`（NeoForge patched source，`handleCustomPayload`）：

```java
public void handleCustomPayload(ServerboundCustomPayloadPacket p_294276_) {
    // Neo: Unconditionally handle register/unregister payloads.
    if (p_294276_.payload() instanceof ...MinecraftRegisterPayload minecraftRegisterPayload) { ... return; }
    if (p_294276_.payload() instanceof ...MinecraftUnregisterPayload minecraftUnregisterPayload) { ... return; }
    if (p_294276_.payload() instanceof ...CommonVersionPayload commonVersionPayload) { ... return; }
    if (p_294276_.payload() instanceof ...CommonRegisterPayload commonRegisterPayload) { ... return; }

    // Neo: Handle modded payloads. Vanilla payloads do not get sent to the modded handling pass.
    // Additional payloads cannot be registered in the minecraft domain.
    if (net.neoforged.neoforge.network.registration.NetworkRegistry.isModdedPayload(p_294276_.payload())) {
        net.neoforged.neoforge.network.registration.NetworkRegistry.handleModdedPayload(this, p_294276_);
        return;
    }
}
```

配套事实：`isModdedPayload` 只认「在 NeoForge 注册表里登记过、且 domain 不是 `minecraft`」的 payload；
`NetworkRegistry.handleModdedPayload` 对未登记通道走 `disconnect("Unknown or unaccepted channel ...")`。

**结论**：Fabric 侧「mixin 往静态度 codec 列表里塞自己的 payload」这套（= B-S5-1 修好的那条）在 NeoForge 下只解决
「编解码认识它」，解决不了「注册表承认它」。必须额外用 `RegisterPayloadHandlersEvent` + `PayloadRegistrar` 注册，
于是新增 `src/main/java/carpettisaddition/network/TISCMNetworkRegistry.java`（两个都是必须的，不是二选一）。

## 13. S6 改动表（只列本轮动过的文件）

| 文件 | 动作 | 关键点 | 验证 |
|------|------|--------|------|
| `build.gradle:227-244` | 改 | `flattenTranslationNode`：`childKey == '.'` 时 `childPrefix = prefix`（子键 `.` = 节点自身的值） | `en_us`/`zh_cn` 与上游 jar 双向键 diff 0/0（1142 键） |
| `src/main/java/carpettisaddition/utils/mixin/LithiumConfigAccess.java` | 改（B-S5-2 授权例外） | 静态初始化 → 惰性 `configCache`/`configLoaded` 双检；`isModListReady()` + `isLithiumLoaded()` null 守卫；PREPARE 阶段返 null 且不缓存 | `ConditionalMixinTester` 0、`NoClassDefFoundError` 0、FATAL 0、ERROR 0 |
| `src/main/java/carpettisaddition/mixins/network/CustomPayloadC2SPacketMixin.java` | 改（仅元数据） | `@At.target` → 4 参数 `codec(FallbackProvider,List,ConnectionProtocol,PacketFlow)StreamCodec`；`@Mixin` 目标/handler 不变 | 假玩家 `spawn`/`kill`/再 `spawn` 均正常；`Error while creating fake player` 0 |
| `src/main/java/carpettisaddition/network/TISCMNetworkRegistry.java` | 新建 | `@EventBusSubscriber(MOD)` + `RegisterPayloadHandlersEvent` → `registrar("1").playToServer/playToClient(TISCMCustomPayload.KEY, TISCMCustomPayload.CODEC, handler)` | `[TISCM] registered payload channel tiscm:network/v1`；客户端进世界不掉线 |
| `tools/ui.ps1` / `tools/uikey.ps1` / `tools/uipost.ps1` | 新建（测试工具） | 窗口内截帧 + `mouse_event` 绝对坐标点击；`PostMessage(WM_KEYDOWN/WM_CHAR)` 驱动聊天框（`SendKeys` 对 GLFW 无效） | 见 `build/shots/*.png` 与 `_s6_client3/4.log` |

未动：`lang/*.yml`、`carpet-tis-addition.mixins.json`（587/12/require=true/JAVA_21）、`neoforge.mods.toml`、`libs/*`、
`run/mods/carpet.jar`、以及 `src/main/java` 下的业务逻辑。

mixin 应用数（`Mixing ... from carpet-tis-addition.mixins.json into`）：S5 `_run_s5.log` = 444 → S6 `_s6_server2.log` = **453**。
