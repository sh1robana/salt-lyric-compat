# FLAC 歌词时间戳兼容模组

Salt Player 桌面版的歌词时间戳兼容模组，版本 `0.1.0`，首个公开测试版。已验证 Windows 1.18.5，其他平台与版本的状态见下文。

作者：**sh1robana 和 Codex**。采用 [MIT 许可证](LICENSE)，见 [作者与致谢](CREDITS.md)。

播放 FLAC 时，将内嵌歌词行首误写的 `[mm:ss:ff]` 或 `[mm:ss:fff]` 转成 `[mm:ss.ff]` / `[mm:ss.fff]`，把转换后的文本交给播放器。音乐文件、标签、封面和音频数据都不写入；无需联网。

## 当前验证状态

- 对本机 Salt Player for Windows **1.18.5** 安装包中提供的真实 API 编译成功。
- 使用真实 API 调用歌词加载回调，605 首 FLAC 和边界测试，共 **624 项断言通过**。
- 43 首歌曲可转换，共 3,664 个时间戳。
- 另有 1 首只有 `[00:00:00]` 及纯音乐提示，由于存在歧义保留原样。
- 已在播放器 1.18.5 中导入、启用并重启验证：原问题歌曲恢复按时间滚动和高亮；正常示例的日文歌词及中文翻译继续正常显示。
- 禁用模组并重新打开原问题歌曲后，恢复为带冒号时间标记的纯文本显示，确认修复来自模组。测试版的支持范围暂限本机 1.18.5。
- 实际 ZIP 产物通过宿主 PF4J 加载、启动、扩展发现及卸载测试。

## 导入与验证

1. 打开播放器的 **设置 → 创意工坊 → 模组管理 → 导入**。
2. 在 **1.18.5** 版选择 `dist/plugin-local.salt.lyriccompat-0.1.0.zip`（不是源码 ZIP）。该版导入器支持 ZIP/JAR；为新版打包规范另提供内容相同的 `.spmod` 文件。
3. 启用 **FLAC Lyric Timestamp Compatibility**。如果播放器提示需要重启，保存当前播放状态后按提示重启。
4. 重新打开一首原本无法同步的 FLAC，跳到有歌词的时间段，观察是否隐藏时间标记并随播放滚动。
5. 打开原本正常的歌曲，检查显示是否保持正常。
6. 禁用模组后重新打开原问题歌曲，检查是否回到播放器原来的处理方式。

菜单名称可能随播放器版本变化。遇到导入失败时，保留错误提示和播放器版本，先禁用模组。

## 范围与保护

- 仅处理原生 FLAC 文件的 `LYRICS` 标签；没有该标签时尝试 `UNSYNCEDLYRICS`。
- 仅替换歌词行首的时间标签，保留歌词正文、换行、时间值及正常的小数点时间戳。
- 支持同一行开头包含多个时间标签。
- 使用曲目时长判断：若标记可能是合理的 `时:分:秒`，且没有明确的“小数秒”证据，整段歌词保留原样。
- 如转换后的时间超出曲目时长 5 秒以上，保留原样。
- 存在同目录、同名 `.lrc` / `.ttml` / `.srt` / `.qrc` / `.yrc` 时，交回播放器默认处理，避免覆盖外置歌词选择。
- 读取失败、格式未知、时长未知和正常歌词均返回 `null`，让播放器按默认逻辑处理。
- 不涉及 MP3、逐字歌词、在线歌词、用户指定的外置歌词搜索目录或其他时间戳问题。

**与其他歌词来源模组共用的限制：** 官方的 `onBeforeLoadLyrics` 返回文本后会优先提供歌词。本模组无法获知所有其他模组或自定义目录的来源优先级。若同时启用其他歌词供应模组，需要单独验证冲突；可先只启用本模组进行测试。

## 从源码构建

需要 Windows、PowerShell、JDK 21 或更新版本，以及已安装的 Salt Player for Windows。本构建不下载依赖，只从安装包读取公开插件 API，产物不包含播放器代码或运行库。

```powershell
.\build.ps1 -PlayerPath '你的播放器安装目录' -JdkPath '你的 JDK 目录'
.\test.ps1 -PlayerPath '你的播放器安装目录' -JdkPath '你的 JDK 目录'
```

如果播放器更新后不再提供 `app/ffmpeg-x64.dll` 这一 API 容器，本地构建脚本需要调整。这个文件名是 1.18.5 版的安装布局，不是通用 API 协议。

测试可选参数 `-CorpusList` 接受 UTF-8 文本文件，每行一个 FLAC 的绝对路径。测试不会写入这些音乐文件。

`src/main/resources/META-INF/extensions.idx` 注册扩展。`.spmod` 采用官方打包逻辑的 `classes/` 布局；没有运行时外部依赖。

兼容说明：较新的 API 文档将 `Plugin-Provider` 视为可选字段，但 1.18.5 的模组列表要求其存在，本包已填写作者，防止空值异常。

## 平台兼容性

| 平台 | 当前状态 | 说明 |
| --- | --- | --- |
| Windows，Salt Player 1.18.5 | 已验证 | 实际播放器启用、禁用、重启及正常歌曲对照均通过。 |
| Linux 桌面版 | 未验证 | 修复代码没有 Windows 原生调用；仍需相同的公开 API、模组加载器及 Java 21 或更新的运行环境。未在 Linux 播放器上测试，暂不承诺可直接使用。 |
| Android | 当前安装包不适用 | 此包面向桌面 JVM / PF4J 模组接口；没有安卓适配或可用性测试，也未找到官方文档声明安卓支持加载此包。 |
| 其他 Windows 版本 | 未验证 | 需再次验证宿主接口、导入器及歌词加载行为。 |

播放器本身支持 Linux，不等于本模组已通过 Linux 验证。官方桌面 API：[spw-workshop-api](https://github.com/Moriafly/spw-workshop-api)；平台信息：[Steam 商店](https://store.steampowered.com/app/3009140/)、[官方 Android 发布仓库](https://github.com/Moriafly/SaltPlayerSource)。

如果需要多个平台读取同一份规范歌词，可另行导出标准 LRC，或在备份后修正音乐副本的内嵌标签；是否能由具体播放器同步读取仍需实测。本模组不会执行这些文件修改。

## 发布准备

此源码包不包含音乐、真实歌词、私人曲库清单或播放器安装文件。

仓库：[sh1robana/salt-lyric-compat](https://github.com/sh1robana/salt-lyric-compat)。采用 MIT 许可证，使用 `salt-player-plugins` topic。安装包见 [Releases](https://github.com/sh1robana/salt-lyric-compat/releases)。当前模组 ID 保持 `local.salt.lyriccompat`，以延续本地安装的身份，不代表官方模组。

首次发布标为预发布测试版，支持范围为上述已验证的 Windows 1.18.5；其他播放器版本、Linux 及模组优先级仍需补测。GitHub Release 提供安装 ZIP、SPMOD、源码包和校验值。发布 GitHub 仓库与上传 Steam 创意工坊是两个步骤；当前没有已发布的创意工坊条目。

参考：[官方创意工坊 API](https://github.com/Moriafly/spw-workshop-api)，尤其是 `PlaybackExtensionPoint.onBeforeLoadLyrics` 和 `WorkshopPluginTask` 的打包布局。
