# 开发与维护

普通用户的下载安装步骤见 [首页](../README.md)。本页供需要修改源码或重新构建模组的人使用。

## 构建与测试

需要 Windows、PowerShell 7、JDK 21 或更新版本，以及已安装的 Salt Player for Windows。本构建不下载依赖，只从安装包读取公开插件 API；产物不包含播放器代码或运行库。

在仓库根目录执行：

```powershell
.\scripts\build.ps1 -PlayerPath '你的播放器安装目录' -JdkPath '你的 JDK 目录'
.\scripts\test.ps1 -PlayerPath '你的播放器安装目录' -JdkPath '你的 JDK 目录'
.\scripts\package-source.ps1
```

`build.ps1` 编译 Java 类并生成安装 ZIP 和相同内容的 SPMOD。`test.ps1` 会先构建，再验证边界行为及实际安装包的加载。`package-source.ps1` 将当前源码、说明和开发脚本打包为 `dist/salt-lyric-compat-source.zip`，并生成本地校验值；它不会上传或替换已发布的 v0.1.0 附件。

测试可选参数 `-CorpusList` 接受 UTF-8 文本文件，每行一个 FLAC 的绝对路径。测试不会写入音乐文件。私人曲库清单应存放于被忽略的 `local-validation/`，不提交、不加入发布包。

如果播放器更新后不再提供 `app/ffmpeg-x64.dll` 这一 API 容器，本地构建脚本需要调整。这是 1.18.5 版的安装布局，不是通用 API 协议。

## 处理范围与回退

- 仅读取原生 FLAC 的 `LYRICS`；缺少该标签时尝试 `UNSYNCEDLYRICS`。
- 仅转换歌词行首的两位或三位小数时间标签，支持同一行多个时间标签，保留歌词正文、换行及正常的小数点标签。
- 若曲目时长未知、时间标记可能是合理的时分秒且缺少小数秒证据，或转换后超出曲目时长 5 秒以上，整段歌词保持原样。
- 同目录存在同名 `.lrc` / `.ttml` / `.srt` / `.qrc` / `.yrc` 时，交回播放器默认处理。
- 读取失败、格式未知和正常歌词均返回 `null`，让播放器继续默认处理。
- 不涉及 MP3、逐字歌词、在线歌词或自定义外置歌词搜索目录。

官方的 `onBeforeLoadLyrics` 返回文本后会优先提供歌词。本模组无法获知所有其他模组及自定义目录的来源优先级，组合使用需要单独验证。避免将“正常歌词不转换”扩大表述为“所有歌词来源均不受影响”。

## 注册与发布

`src/main/resources/META-INF/extensions.idx` 注册扩展；安装包采用官方的 `classes/` 布局，没有额外的运行时依赖。较新的文档将 `Plugin-Provider` 视为可选字段，但 1.18.5 的模组列表要求其存在，须保留作者字段。

模组 ID 为 `local.salt.lyriccompat`，保持本地试包与首个发布版的身份一致。发布前核对版本、安装包内容、校验值及平台范围。发布附件不得包含音乐、完整真实歌词、私人曲库清单或宿主安装文件。

当前公开版本 v0.1.0 是测试版，只将 Windows 1.18.5 列为已验证；Linux、其他播放器版本及其他歌词模组组合仍需补测。安装包为桌面 JVM / PF4J 模组，未适配 Android。源码没有 Windows 原生调用，但这不等同于 Linux 实测通过。

仓库使用官方推荐的主题标签 `salt-player-plugins`。GitHub 发布不等同于 Steam 创意工坊上传，目前没有已发布的 Steam 创意工坊条目。

参考：[官方创意工坊接口](https://github.com/Moriafly/spw-workshop-api)，尤其是 `PlaybackExtensionPoint.onBeforeLoadLyrics` 与 `WorkshopPluginTask`。实际测试结果见 [验证记录](VALIDATION.md)。
