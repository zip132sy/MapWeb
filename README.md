# MapWeb

在服务器上启动内置网页服务，浏览器打开网页即可查看服务器**当前已加载区块**的实时俯视地图。
支持网页配置刷新频率、多世界切换，以及**网页命令行工具**（需登录管理员账号）。

- 兼容版本：Minecraft 1.12.x（Spigot / Paper）
- 作者：zip132sy
- 版本：1.0.0

---

## 功能一览

| 功能 | 说明 |
| --- | --- |
| 实时地图 | 渲染当前已加载区块，每个区块取最高方块代表色，并按高度做阴影 |
| 2D / 3D 切换 | 顶部按钮切换俯视（2D）与真 3D（Three.js 立体地形，可见实体 Z 轴高度） |
| 方块材质 | 3D 视图支持方块贴图，材质包可自动下载或手动放置 |
| 自定义材质包 | 玩家可在网页上传自己的材质包（仅本地生效，存浏览器 IndexedDB） |
| 多世界切换 | 网页顶部下拉框切换世界 |
| 刷新频率 | 网页上直接修改，提交后立即生效（前端定时轮询） |
| 端口自动检测 | 默认端口被占用时，自动向后寻找可用端口 |
| 网页命令行 | 登录管理员账号后，可执行白名单内的服务器命令 |
| 在线玩家列表 | 网页顶部显示在线人数，点击展开玩家头像列表（含世界/坐标/血量/饱食度，可自动排除假玩家） |
| 玩家搜索 | 玩家列表顶部支持按名字搜索过滤 |
| 查看背包 | 点击玩家头像可查看其背包（仅登录管理员，只读，支持假玩家） |
| 生物显示 | 地图上以彩色圆点标记生物（友好绿/中立黄/敌对红），默认关闭，三类可独立开关 |
| 手机端适配 | 响应式布局，顶部控件可折叠，支持双指缩放与拖动 |

---

## 安装

1. 将编译好的 `MapWeb.jar` 放入服务器 `plugins/` 目录。
2. 启动服务器，插件会自动生成 `plugins/MapWeb/config.yml`。
3. 打开浏览器访问：`http://<服务器IP>:8123/`（端口以控制台输出为准）。

> 注意：网页服务需要服务器**处于运行状态**，且防火墙放行对应端口。

---

## 命令

| 命令 | 说明 | 权限 |
| --- | --- | --- |
| `/mapweb status` | 查看运行状态 | `mapweb.admin` |
| `/mapweb port` | 查看当前网页端口 | `mapweb.admin` |
| `/mapweb reload` | 重载配置 | `mapweb.admin` |
| `/mapweb admin add <用户名> <密码>` | 创建/更新网页管理员账号（**仅控制台**） | 控制台 |
| `/mapweb admin remove <用户名>` | 删除网页管理员账号（**仅控制台**） | 控制台 |
| `/mapweb admin list` | 列出所有网页管理员账号（**仅控制台**） | 控制台 |
| `/mapweb downloadtextures <url>` | 手动下载材质包 | `mapweb.admin` |

别名：`/mw`

---

## 网页命令行工具

### 安全说明（重要）

网页命令行是**高危功能**，请务必注意：

- 账号密码**只在服务端存储**，使用 SHA-256 + 随机盐 + 1000 次迭代哈希，绝不保存明文。
- 登录后发放会话 token，默认 30 分钟滑动过期。
- 命令执行受**白名单**限制，只有 `config.yml` 中 `web-console.command-whitelist` 列出的命令才允许执行。
- 所有通过网页执行的命令都会记录到服务器日志，格式：`[MapWeb] 网页管理员 <用户名> 执行命令: /<命令>`。
- **强烈建议**：仅在可信网络环境下开启，并严格限制白名单。

### 使用步骤

1. 在服务器控制台创建管理员账号：

   ```
   /mapweb admin add admin 你的密码
   ```

2. 打开网页，点击右下角键盘图标打开命令行面板。
3. 输入用户名和密码登录。
4. 在命令输入框中输入命令（无需斜杠），点击执行。

---

## 配置文件说明（config.yml）

```yaml
config-version: 7          # 配置版本，请勿手动修改

web:
  port: 8123               # 网页服务端口
  auto-port: true          # 端口被占用时是否自动向后寻找可用端口
  refresh-interval: 5      # 默认刷新间隔（秒）
  min-refresh-interval: 1  # 刷新间隔最小值
  max-refresh-interval: 300 # 刷新间隔最大值

map:
  show-all-worlds: true    # 是否显示所有已加载的世界
  default-world: world     # 默认显示的世界
  resolution: 8            # 渲染分辨率：每区块渲染成 resolution × resolution 像素
  show-chunk-border: true  # 是否绘制区块边界线

three-d:
  enabled: true            # 是否启用 3D 视图
  radius: 3                # 渲染半径（区块数），3 表示 7×7 区块
  min-y: 0                 # 扫描最低高度
  max-y: 128               # 扫描最高高度
  max-voxels: 120000       # 单次返回方块数量上限
  texture-download-urls:   # 材质包下载地址列表（依次尝试）
    - 'https://github.com/InventivetalentDev/minecraft-assets/archive/refs/heads/1.12.2.zip'

player-list:
  enabled: true            # 是否启用在线玩家列表
  show-world: true         # 是否显示玩家所在世界
  show-coords: true        # 是否显示玩家坐标
  show-health: true        # 是否显示玩家血量
  show-food: true          # 是否显示玩家饱食度
  exclude-fake-players: true # 是否排除假玩家（NPC / 机器人）

entity-display:
  enabled: false           # 总开关：是否启用生物显示（默认关闭）
  show-friendly: false     # 是否显示友好生物（动物）
  show-neutral: false      # 是否显示中立生物（末影人、蜘蛛、狼等）
  show-hostile: false      # 是否显示敌对生物（僵尸、骷髅、苦力怕等）

web-console:
  enabled: true            # 是否启用网页命令行
  session-timeout: 30      # 登录会话有效期（分钟）
  command-whitelist:       # 允许执行的命令白名单（不含斜杠）
    - say
    - list
    - tps
    - time
    - weather
```

> 若配置结构发生变更（`config-version` 提升），OP 进服时会收到旧配置提示。
> 建议删除旧 `config.yml` 后重启，让插件重新生成。

---

## 性能说明

- 地图渲染在**异步线程**中完成，不会阻塞服务器主线程。
- 渲染范围为**当前已加载的区块**，区块越多，单次渲染耗时越长。
- 若服务器区块数量庞大，建议适当调大刷新间隔（如 10~30 秒），避免频繁渲染。
- 单次渲染的区块数量上限为 400 万，超过则跳过渲染（防止内存溢出）。

---

## 常见问题

**Q：网页打不开？**
- 确认服务器正在运行，且插件成功启动（控制台会输出网页地址）。
- 确认防火墙放行了对应端口。
- 若端口被占用，查看控制台输出的实际端口，或执行 `/mapweb port`。

**Q：地图是空白的？**
- 说明该世界当前没有已加载的区块。让玩家进入该世界走动一下即可。

**Q：编译报错找不到 `org.bukkit` 类？**
- 请使用 Spigot 或 Paper 这类标准服务端，并确保开发环境已引入对应版本的 API 依赖。
