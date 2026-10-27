# 发布到 GitHub / Modrinth（工作区已备好，只差你的账号令牌）

本轮已把「能自动化的部分」全部做完并实测到**上传前一步**：仓库已提交、发布素材已生成、两个上传脚本已跑通到凭据检查。

## 0. 为什么停在这里

上传需要**你自己账号的凭证**，本机环境里没有任何可用的 token（实测）：

```
$ env | grep -i 'TOKEN|MODRINTH|GITHUB'      -> 空
$ tools\publish-modrinth.ps1                -> [publish] MODRINTH_TOKEN is empty.  (exit 2)
$ tools\publish-github.ps1                  -> [publish] GITHUB_TOKEN is empty.    (exit 2)
```

其余都已就绪：

```
git log --oneline -1
a8f24c9 Carpet TIS Addition 1.82.0 NeoForge 1.21.1 port: source, build config and verification records
git status --porcelain                       -> 0（干净）
已跟踪文件 1082 个 / 约 5.5 MB（build、run、_s8_srv、_tmp、dist 已 gitignore）
```

## 1. 路线 A：命令行（拿到 token 后 2 条命令）

### GitHub

1. 打开 <https://github.com/settings/tokens> → Generate new token (classic) → 勾 `repo`（要建公开仓库就够）→ 复制 `ghp_...`
2. 在 PowerShell 里：

```powershell
cd "D:\Minecraft-mod-development\carpet-tis-neoforge移植（已收工）\TIS-NeoForge"
$env:GITHUB_TOKEN = "ghp_xxxxxxxxxxxxxxxx"; $env:GITHUB_OWNER = "你的GitHub用户名"
tools\publish-github.ps1
```

脚本会：建仓库（若不存在）→ `git push` 到 `main` → 建 release `v1.82.0+neoforge.1.21.1` → 上传三个资产（两个 zip + 单文件版主 jar）→ 打印仓库与 release 链接。可重复执行（幂等）。

### Modrinth

1. 打开 <https://modrinth.com/settings/pats> → Create PAT → 勾 **Create projects** 与 **Create versions** → 复制 `mrp_...`
2. 在同一个 PowerShell 里：

```powershell
$env:MODRINTH_TOKEN = "mrp_xxxxxxxxxxxxxxxx"
tools\publish-modrinth.ps1
```

脚本会：`POST /v2/project`（slug `carpet-tis-addition-neoforge`，LGPL-3.0，neoforge / 1.21.1）→ `POST /v2/project/{id}/icon` 上传 512×512 图标 → `POST /v2/version` 上传单文件版 mod jar + `version.json` 里的元数据 → 打印 `https://modrinth.com/mod/carpet-tis-addition-neoforge`。
首次创建成功会把 project id 记到 `dist\publish\modrinth-project-id.txt`，之后重跑直接复用。

> 两个脚本都写成 ASCII 注释 + UTF-8 BOM（Windows PowerShell 5.1 在没有 BOM 时按 ANSI 解析，中文会把脚本读坏——本轮踩过一次，`Unexpected token 'utility'`）。

## 2. 路线 B：纯网页（不碰命令行）

素材都在 `dist\publish\`：

| 文件 | 用在哪 |
|---|---|
| `modrinth-body.md` | Modrinth 项目页 Description（已写好中文说明：安装、依赖、崩服原因、已知限制、许可） |
| `version.json` | 版本元数据（name / version_number / changelog / game_versions / loaders / environment），网页版照着填即可 |
| `github-release.md` | GitHub Release 正文 |
| `icon-512.png` | Modrinth 项目图标（512×512，第一次跑脚本时生成；也可直接用 `..\..\build\icon-D.png`） |
| `..\TIS-Addition-NeoForge-1.21.1-single-jar.zip` | GitHub release 资产（推荐下载项） |
| `..\TIS-Addition-NeoForge-1.21.1.zip` | GitHub release 资产（三 jar 版） |
| `..\_s9_payload\TIS-single-jarjar.jar` | Modrinth 版本文件（Modrinth 只认单个 jar 作为版本主文件） |

Modrinth 网页流程：<https://modrinth.com> → 右上头像 → Create a project → 类型 Mod、名字 `Carpet TIS Addition (NeoForge port)`、slug `carpet-tis-addition-neoforge`、描述一句、分类 Utility/Optimization、客户端与服务端都 required、License `LGPL-3.0` → 粘贴 `modrinth-body.md` → 上传图标 → 保存后进 Versions → Create version → 版本号 `1.82.0+neoforge.1.21.1`、Channel Release、Loaders 勾 NeoForge、Game version 1.21.1、上传 `TIS-single-jarjar.jar`、Changelog 从 `version.json` 粘。

## 3. 上传前建议确认的三件事

1. **署名与合规**：这是**非官方移植**。项目页与 README 里已写明「非官方、上游 LGPL-3.0、本移植版同许可」，未使用上游作者名义发布。若要标为「由 TISUnion 官方认可」需要上游作者同意，本文件没这么写。
2. **Modrinth 审核**：首次发布通常会进人工审核队列，需要项目页信息完整（描述/许可/来源链接）。脚本里 `source_url` / `issues_url` 取 `$env:GITHUB_SOURCE_URL` / `$env:GITHUB_ISSUES_URL`，**先建 GitHub 仓库再传 Modrinth**，这两个变量填上更顺。
3. **别混包**：Modrinth 版本文件传**单文件版**的 mod jar（`TIS-single-jarjar.jar`，1 767 785 B / `C61AE6DC…2917`）；两个 zip 是给 GitHub release 用的整合包。

## 4. 已上传后的自查

```powershell
# Modrinth：版本是否可下载
curl.exe -s -H "User-Agent: dsh-agent" https://api.modrinth.com/v2/project/carpet-tis-addition-neoforge/version
# GitHub：release 资产列表
curl.exe -s -H "User-Agent: dsh-agent" https://api.github.com/repos/OWNER/carpet-tis-addition-neoforge/releases/tags/v1.82.0+neoforge.1.21.1
```

## 5. 本轮实际产出的文件

* `tools\publish-modrinth.ps1`（5 922 B）/ `tools\publish-github.ps1`（4 492 B）—— 两个幂等上传脚本
* `dist\publish\modrinth-body.md`（3 306 B）/ `version.json`（762 B）/ `github-release.md`（1 791 B）
* 仓库根 `README.md`（1 521 B）、`.gitignore`（构建产物与验证台全部排除）
* git 初始提交 `a8f24c9`（1082 文件 / 5.5 MB）

---

## 6. 上传前的离线自检（本轮实测，不需要 token）

```
PS> tools\publish-modrinth.ps1 -DryRun
[dry-run] project payload fields : slug/title/description/body/categories/client_side/server_side/project_type/license_id
[dry-run] body markdown chars   : 2212
[dry-run] version data JSON     : 718 chars
[dry-run] version json valid    : 1.82.0+neoforge.1.21.1
[dry-run] game_versions/loaders : 1.21.1 / neoforge
[dry-run] upload file           : TIS-single-jarjar.jar (1767785 bytes)
[dry-run] icon                  : icon-D.png (1885 bytes)
[dry-run] no request sent; set MODRINTH_TOKEN and drop -DryRun to publish.

PS> tools\publish-github.ps1 -DryRun
[dry-run] planned API calls:
  GET    https://api.github.com/repos/$Owner/$Repo            (existence check)
  POST   https://api.github.com/user/repos                    (create when absent)
  POST   https://api.github.com/repos/$Owner/$Repo/releases   (tag v1.82.0+neoforge.1.21.1)
  POST   .../releases/{id}/assets?name=...                    (3 assets)
[dry-run] local git state:
  branch        : master
  head          : 8c54c3d Add publishing pipeline: GitHub/Modrinth scripts, release metadata, publish checklist
  origin        : (none yet)
  dirty files   : 2
[dry-run] assets that would be uploaded:
  TIS-Addition-NeoForge-1.21.1-single-jar.zip          2,913,744 bytes
  TIS-Addition-NeoForge-1.21.1.zip                     2,918,691 bytes
  TIS-single-jarjar.jar                                1,767,785 bytes
```

（`dirty files: 2` 是本次 DryRun 补丁自身，提交后为 0。）

## 7. 三个已知坑（都已在本轮脚本里规避）

1. **PS 5.1 读 .ps1 的编码**：无 BOM 的 UTF-8 脚本会被当 ANSI，中文字符串把引号吃掉，报 `Unexpected token 'utility'` / `The string is missing the terminator: '`。→ 两个脚本都存成 **UTF-8 with BOM**。
2. **PS 5.1 没有 `if` 表达式**：`-f (if (Test-Path x) {1} else {2})` 会报 `无法将"if"项识别为 cmdlet`。→ 先算变量再格式化。
3. **`Get-Content -Raw` 默认按 ANSI 读**：`version.json` 里的中文被读坏后 `ConvertFrom-Json` 报 `Invalid object passed in, ':' or '}' expected`。→ 全部改成 `-Encoding UTF8`。

---

## 8. 排错：401 未经授权（截图里遇到的那次）

你运行时贴的是占位符，不是真 token：

```
PS> $env:GITHUB_TOKEN = "ghp_xxx"
PS> tools\publish-github.ps1
Invoke-RestMethod : 远程服务器返回错误: (401) 未经授权。
```

`ghp_xxx` 只有 7 个字符，真正 classic PAT 是 **40 字符**、`github_pat_` 开头的 fine-grained 是 **93 字符**。脚本现在会在开工前先做一次 `GET /user` 预检，并直接告诉你这件事：

```
[publish] token: ghp_xxx (7 chars)
[publish] GitHub rejected the token: HTTP 401 未经授权。
[publish] your token is only 7 chars - that looks like the placeholder from the docs, not a real one.
  Classic PAT (40 chars, starts with ghp_):  https://github.com/settings/tokens  -> scope: repo
  Fine-grained PAT: https://github.com/settings/personal-access-tokens/new
     Repository access: All repositories
     Permissions: Contents = Read and write, Administration = Read and write, Metadata = Read
exit=3
```

Modrinth 侧同样预检（`GET /v2/user`），占位符 `mrp_xxx` 会提示「only 7 chars - looks like the placeholder」。

### 还要注意两点

1. **`GITHUB_OWNER` 不能有空格**：你的截图里是 `"xiao huan 114"`。GitHub 用户名不允许空格，实际应该是 `xiaohuang114` 这样的连续串。填错会在 `POST /user/repos` 阶段变成 404/403。
2. **token 别贴进聊天**：写进 `tools\.secrets\github.token`（已 gitignore）最稳妥，脚本会自动读。

---

## 9. 生成 GitHub token 的具体点法（截图那一页）

你贴的是 <https://github.com/settings/tokens> → **Generate new token (classic)** 这一页，红字 `Note can't be blank` 就是唯一卡住你的地方。

1. **Note**：随便填一个用途名，例如 `tis-neoforge-publish`（只是给你自己日后辨认）。
2. **Expiration**：`30 days` 够用；嫌麻烦可选 `90 days`。
3. **Select scopes**：**只勾 `repo`** 这一行（勾父项后它下面的 `repo:status` / `public_repo` 等子项会自动带上，方块变成实心打勾）。
   * 不要勾 `workflow`、`write:packages`、`delete:packages`、`admin:org`、`admin:public_key` 等。
4. 滚到页面**最底部**，点绿色 **Generate token**。
5. 下一个页面顶部会出现 `ghp_` 开头的一串（**只显示这一次**，关掉就再也看不到），点右侧复制图标。
6. 回到 PowerShell：

```powershell
cd "D:\Minecraft-mod-development\carpet-tis-neoforge移植（已收工）\TIS-NeoForge"
tools\save-token.ps1 -Service github        # 隐藏输入粘贴 token，再输入用户名
tools\publish-github.ps1                   # 自动建库 + push + 建 release + 传 3 个资产
```

`save-token.ps1` 会做三件事：校验长度（≥20 且有 `ghp_`/`github_pat_` 前缀）、拒绝带空格的用户名、写入被 gitignore 的 `tools\.secrets\`。

Modrinth 同理：`tools\save-token.ps1 -Service modrinth` → `tools\publish-modrinth.ps1`。

### 如果 token 已经生成过

关掉页面就看不到原值了 —— 直接按上面第 1~6 步重新生成一个（旧的可以在同一页面 Revoke 掉）。
