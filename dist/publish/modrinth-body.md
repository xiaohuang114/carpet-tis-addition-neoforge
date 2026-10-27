# Carpet TIS Addition (NeoForge port) 1.82.0

把 [Carpet TIS Addition](https://github.com/TISUnion/Carpet-TIS-Addition) 1.82.0（原本是 Fabric 模组）移植到 **NeoForge 1.21.1** 的非官方版本。

- 规则、命令、日志器与上游**逐条对齐**：587 条 mixin 与 12 条 client mixin 全部保留，与官方 Fabric jar 逐条比对过。
- 只做「加载器适配」这一层改动：Fabric API 调用换成 NeoForge / 原版等价实现，业务逻辑不动。

## 安装

1. 装好 **Minecraft 1.21.1 + NeoForge 21.1.x + Java 21**。
2. 把下面两个 jar 放进 `mods/`（客户端与服务端都要）：
   - `carpet-1.4.147+v1.0.8-251027.jar` —— Carpet 前置（必需，[NeoForge 移植版](https://github.com/chililisoup/neoforge-carpet)）
   - `carpet-tis-addition-1.82.0+neoforge.1.21.1.jar` —— 本模组

本文件下载的是**单文件版**：`conditional-mixin` 与 `tiny-mappings-parser` 已经用 JarJar 嵌在 jar 内部，不用再单独放。

## 依赖说明（重要）

| 组件 | 状态 |
|---|---|
| `conditional_mixin` 0.6.4+ | 已内嵌。如果你的整合包里已经有别的 mod 提供了它（例如 TweakerMore 的 jar-in-jar），NeoForge 会自动跳过内嵌那份，日志里只有一行 WARN，不会报错 |
| `tiny_mappings_parser_lib` 0.3.0 | 已内嵌。它是运行期必需的（`TISCM Mapping` 线程解析 yarn 映射），缺了会在 stdout 抛 `NoClassDefFoundError: net/fabricmc/mapping/reader/v2/TinyVisitor`，但**不会**写进 `latest.log` |
| `carpet` 1.4.147+ | 需要你另外安装，缺失时会在 mod 解析阶段直接拒绝启动：`Mod carpet_tis_addition requires carpet 1.4.147 or above` |

## 从旧版升级（很可能就是你崩服的原因）

1.82.0 之前的包装方式把 `conditional-mixin` 的类**拍平**进了主 jar，与整合包里 jar-in-jar 提供的同一份 `conditional_mixin` 在模块层导出同一个包，启动直接崩：

```
java.lang.module.ResolutionException: Modules carpet_tis_addition and conditional_mixin
export package me.fallenbreath.conditionalmixin.api.mixin to module ...
```

本版把第三方库全部改为独立模块（JarJar 嵌套 / 单独 jar），**请删掉 mods 里所有旧的 `carpet-tis-addition-*.jar` 再放入本文件**。

## 已知限制

- 只在**服务端**做过完整自动化验收（`Done`、`ERROR 0`、`apply failed 0`、applied 437 条 mixin），客户端只做了加载与 mixin 应用层面的核对。
- `util.teleport_hint` 的鼠标悬停文本需要真机 hover 才能验证，未取证。
- 少数依赖可选 mod 的 mixin（lithium / malilib / minihud / mctester / tic_tacs）在这些 mod 缺席时按设计被跳过，`debug.log` 里会看到 21 条 `Disabled mixin`，属预期。
- 启动器（PCL2 等）里显示的是 jar 里的 `META-INF/mods.toml` 兼容名；自定义图标需要上架平台后才会显示。

## 许可与来源

- 本移植版：**LGPL-3.0**（与上游一致）。
- 上游：Carpet TIS Addition by Fallen_Breath / TISUnion（LGPL-3.0）。
- Carpet：gnembon 上游 MIT；NeoForge 端口 by chililisoup，MIT。
- conditional-mixin by Fallen_Breath，**LGPL-3.0-only**。
- tiny-mappings-parser by FabricMC，Apache-2.0。

完整第三方声明见发布包内 `THIRD-PARTY-NOTICES.txt`。
