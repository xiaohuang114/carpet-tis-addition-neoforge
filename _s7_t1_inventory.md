# S7 / T1 — 三个输入的完整条目清点 + 白名单

取证命令（本机无 unzip，用 JDK 21 的 `jar tf`，等价于 `unzip -l` 的条目树）：

```
& 'C:\Program Files\Java\jdk-21.0.11\bin\jar.exe' tf build\libs\conditional-mixin-lib.jar   → _s7_cm_entries.txt
& 'C:\Program Files\Java\jdk-21.0.11\bin\jar.exe' tf build\libs\tiny-mappings-lib.jar       → _s7_tm_entries.txt
Get-ChildItem build\classes\fabric-loader-shim -Recurse -File                                → 9 个 .class
```

## 输入 1：`build\libs\conditional-mixin-lib.jar`（54 条目 = 21 目录 + 33 文件）

完整条目树：

```
META-INF/
META-INF/MANIFEST.MF
LICENSE_conditional-mixin
architectury_inject_conditionalmixin_common_2db8ded5260d42dc832031cee1979f43_21157910afb3f855270e2dcc759ab71e3deab59e64fc40cb3b41da2c24d3f42fconditionalmixincommon064devjar/
architectury_inject_conditionalmixin_common_2db8ded5260d42dc832031cee1979f43_21157910afb3f855270e2dcc759ab71e3deab59e64fc40cb3b41da2c24d3f42fconditionalmixincommon064devjar/PlatformMethods.class
assets/
assets/conditionalmixin/
assets/conditionalmixin/icon.png
me/
me/fallenbreath/
me/fallenbreath/conditionalmixin/
me/fallenbreath/conditionalmixin/ConditionalMixinMod.class
me/fallenbreath/conditionalmixin/api/
me/fallenbreath/conditionalmixin/api/annotation/
me/fallenbreath/conditionalmixin/api/annotation/Condition$Type.class
me/fallenbreath/conditionalmixin/api/annotation/Condition.class
me/fallenbreath/conditionalmixin/api/annotation/Restriction.class
me/fallenbreath/conditionalmixin/api/checker/
me/fallenbreath/conditionalmixin/api/checker/RestrictionChecker.class
me/fallenbreath/conditionalmixin/api/checker/RestrictionCheckers.class
me/fallenbreath/conditionalmixin/api/mixin/
me/fallenbreath/conditionalmixin/api/mixin/AnnotationCleaner.class
me/fallenbreath/conditionalmixin/api/mixin/ConditionTester.class
me/fallenbreath/conditionalmixin/api/mixin/RestrictionCheckFailureCallback.class
me/fallenbreath/conditionalmixin/api/mixin/RestrictiveMixinConfigPlugin.class
me/fallenbreath/conditionalmixin/api/util/
me/fallenbreath/conditionalmixin/api/util/VersionChecker.class
me/fallenbreath/conditionalmixin/api/util/fabric/
me/fallenbreath/conditionalmixin/api/util/fabric/VersionCheckerImpl.class
me/fallenbreath/conditionalmixin/fabric/
me/fallenbreath/conditionalmixin/fabric/ConditionalMixinFabric.class
me/fallenbreath/conditionalmixin/impl/
me/fallenbreath/conditionalmixin/impl/AnnotationCleanerImpl.class
me/fallenbreath/conditionalmixin/impl/MemorizedRestrictionChecker.class
me/fallenbreath/conditionalmixin/impl/SimpleRestrictionChecker$1.class
me/fallenbreath/conditionalmixin/impl/SimpleRestrictionChecker$Result.class
me/fallenbreath/conditionalmixin/impl/SimpleRestrictionChecker.class
META-INF/neoforge.mods.toml
net/
net/fabricmc/
net/fabricmc/loader/
net/fabricmc/loader/api/
net/fabricmc/loader/api/FabricLoader$FixedModContainer.class
net/fabricmc/loader/api/FabricLoader$Impl.class
net/fabricmc/loader/api/FabricLoader$NeoModContainer.class
net/fabricmc/loader/api/FabricLoader.class
net/fabricmc/loader/api/ModContainer.class
net/fabricmc/loader/api/Version.class
net/fabricmc/loader/api/metadata/
net/fabricmc/loader/api/metadata/ModMetadata.class
net/fabricmc/loader/api/metadata/version/
net/fabricmc/loader/api/metadata/version/VersionPredicate$Parsed.class
net/fabricmc/loader/api/metadata/version/VersionPredicate.class
previous-compilation-data.bin
```

**关键事实（T1 要的“别猜”）**：`net/**` 下面**不是** conditional-mixin 的上游依赖，而是 `stripConditionalMixin`
把 `compileFabricLoaderShim` 的输出并进来的结果 —— 即 **9 个 shim 类本身就在这里**：
`FabricLoader`（+ `$Impl` / `$FixedModContainer` / `$NeoModContainer`）、`ModContainer`、`Version`、
`metadata/ModMetadata`、`metadata/version/VersionPredicate`（+ `$Parsed`）。
所以坑 B 的类在 `conditional-mixin-lib.jar` 里是**第二份**，与 `build\classes\fabric-loader-shim` 逐字节同源。

## 输入 2：`build\libs\tiny-mappings-lib.jar`（101 条目 = 12 目录 + 86 .class + 3 META-INF）

- `net/fabricmc/mapping/reader/v2/` **18** 项（`TinyV2Factory` + 6 个内部类 + `TinyVisitor` + `MappingGetter` …）
- `net/fabricmc/mapping/tree/` **25** 项
- `net/fabricmc/mapping/util/` **7** 项
- `net/fabricmc/mappings/` 顶层 10 个类 + `helpers/` 7 项 + `model/` 23 项
- `META-INF/`（目录）、`META-INF/MANIFEST.MF`、`META-INF/neoforge.mods.toml`

TISCM 运行期只用 `net.fabricmc.mapping.reader.v2.*`（`TinyMappingV2Reader` → `TinyV2Factory`/`TinyVisitor`），
其余 `tree/` `util/` `mappings/` `model/` 是它的传递依赖（同一个 jar 里不可拆）→ 整包类全进。

## 输入 3：`compat-src\fabric-loader-shim` 的编译输出

`compileFabricLoaderShim`（`build.gradle:132-140`）→ `destinationDirectory = build/classes/fabric-loader-shim`，9 个 .class：

```
net/fabricmc/loader/api/FabricLoader.class                          823
net/fabricmc/loader/api/FabricLoader$Impl.class                    5875
net/fabricmc/loader/api/FabricLoader$FixedModContainer.class       1662
net/fabricmc/loader/api/FabricLoader$NeoModContainer.class         2390
net/fabricmc/loader/api/ModContainer.class                          202
net/fabricmc/loader/api/Version.class                               170
net/fabricmc/loader/api/metadata/ModMetadata.class                  195
net/fabricmc/loader/api/metadata/version/VersionPredicate.class     707
net/fabricmc/loader/api/metadata/version/VersionPredicate$Parsed.class 4054
```

源文件 5 个：`FabricLoader.java` / `ModContainer.java` / `Version.java` / `metadata/ModMetadata.java` /
`metadata/version/VersionPredicate.java`。

## 白名单表（条目前缀 → 进包 / 不进包 → 理由）

| # | 输入 | 条目前缀 | 进包 | 理由 |
|---|------|----------|------|------|
| 1 | main output | `carpettisaddition/**`（class） | ✅ | 本 mod 全部实现 |
| 2 | main output | `META-INF/neoforge.mods.toml` | ✅ | 唯一合法 mod 元数据（`modId="carpet_tis_addition"`，1 个 `[[mods]]` + 1 个 `[[mixins]]`） |
| 3 | main output | `META-INF/accesstransformer.cfg` | ✅ | AT 已在 `neoForge.accessTransformers` 引用，随包必需 |
| 4 | main output | `carpet-tis-addition.mixins.json` | ✅ | 587/12 的 mixin 容器入口 |
| 5 | main output | `assets/carpettisaddition/**`（含 `lang/*.json`） | ✅ | 翻译 + 图标（`logoFile` 指向 `assets/carpettisaddition/icon.png`） |
| 6 | shim dir | `net/fabricmc/loader/api/**`（9 类） | ✅ | **坑 B**：`RestrictiveMixinConfigPlugin`→`VersionCheckerImpl`→`FabricLoader` 的实现在这里；release 缺它 100% `NoClassDefFoundError` |
| 7 | conditional-mixin-lib | `me/fallenbreath/conditionalmixin/**`（18 类） | ✅ | `@Restriction` 注解本体 + `RestrictiveMixinConfigPlugin` + `SimpleRestrictionChecker` |
| 8 | conditional-mixin-lib | `net/fabricmc/loader/api/**`（9 类，与 #6 重复） | ⚠️ 保留一次 | `duplicatesStrategy=EXCLUDE` + shim 先 `from` → 生效的是 #6 那份，包内只有一份 |
| 9 | conditional-mixin-lib | `LICENSE_conditional-mixin` | ✅ | LGPL-3.0 许可要求随分发 |
| 10 | conditional-mixin-lib | `META-INF/**`（含 `neoforge.mods.toml`） | ❌ | dev-only 假 modId `conditional_mixin_lib`，会把库装成**独立 mod**并与 #2 冲突；发布包只留一份 toml |
| 11 | conditional-mixin-lib | `architectury_inject_conditionalmixin_common_*`（目录 + `PlatformMethods.class`） | ❌ | Architectury Loom 的转换缓存，非 mod 内容 |
| 12 | conditional-mixin-lib | `previous-compilation-data.bin` | ❌ | Gradle 编译缓存 |
| 13 | conditional-mixin-lib | `assets/conditionalmixin/icon.png` | ❌ | dev shim 图标，无任何引用 |
| 14 | tiny-mappings-lib | `net/fabricmc/mapping/reader/v2/**` | ✅ | `TinyMappingV2Reader` 运行期硬依赖 |
| 15 | tiny-mappings-lib | `net/fabricmc/mapping/tree/**`、`util/**`、`mappings/**`、`model/**` | ✅ | 上者的传递依赖，同 jar 不可拆 |
| 16 | tiny-mappings-lib | `META-INF/**`（含 `neoforge.mods.toml`） | ❌ | 同 #10：dev-only 假 modId `tiny_mappings_parser_lib` |
| 17 | — | `*.SF` / `*.DSA` / `*.RSA` | ❌ | 签名残留（`strip*` 任务已排，release 侧再兜一层） |
| 18 | — | `compat-src/**`（源文件）、`build.gradle` | ❌ | 只有 shim 的**编译产物**该进包（#6），源码树不进包 |
