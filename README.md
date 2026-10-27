# Carpet TIS Addition (NeoForge port)

[Carpet TIS Addition](https://github.com/TISUnion/Carpet-TIS-Addition) 1.82.0 的 **NeoForge 1.21.1** 非官方移植版。

上游是 Fabric 模组；本仓库只做加载器适配（Fabric API → NeoForge / 原版等价实现），
规则、命令、日志器与 mixin 清单与上游逐条对齐（587 条 mixins / 12 条 client mixins）。

## 下载

Releases 页提供两种包装：

| 资产 | 内容 |
|---|---|
| `TIS-Addition-NeoForge-1.21.1-single-jar.zip` | **推荐**：carpet 前置 + 本模组（两个库已用 JarJar 内嵌） |
| `TIS-Addition-NeoForge-1.21.1.zip` | 三 jar 版：本模组 + `conditional-mixin` + `tiny-mappings-parser` 各自独立 |
| `TIS-single-jarjar.jar` | 单文件版里的模组 jar 本身 |

## 安装

1. Minecraft 1.21.1 + NeoForge 21.1.x + Java 21。
2. 把 `carpet-1.4.147+v1.0.8-251027.jar`（[NeoForge 版 Carpet](https://github.com/chililisoup/neoforge-carpet)）与
   `carpet-tis-addition-1.82.0+neoforge.1.21.1.jar` 一起放进 `mods/`。
3. 启动；`/carpet` 应回 `Carpet TIS Addition version: 1.82.0`。

## 构建

```
gradlew.bat --offline --console=plain packageConditionalMixin stripTinyMappings releaseJar
```

产物在 `build/libs/`。需要 JDK 21。

## 与上游的差异 / 已知限制 / 验收记录

见 `RELEASE-NOTES.md`、`PORT-STATE.md`、`BLOCKERS.md`、`S9-ACCEPT.md`。

## 许可

本移植版 LGPL-3.0（与上游一致）。第三方组件许可见发布包内 `THIRD-PARTY-NOTICES.txt`。
