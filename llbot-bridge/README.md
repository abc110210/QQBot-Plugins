# llbot-bridge

部署在 **LLBOT 服务器**，接收 Halo 插件 `xlingran-shan` 的 HTTP POST，渲染 `Blog渲染/` 模板后通过 LLOneBot 发 QQ。

**环境要求：Python 3.10.0+**（已在 3.10.0 验证；不建议 3.9 及以下）

## 部署

1. 复制仓库根目录 `Blog渲染/` 到 `llbot-bridge/templates/Blog渲染/`（含 `ttf/`、`img/`）
2. 安装依赖：

**Windows：**

```bash
cd llbot-bridge
py -3.10 -m pip install -r requirements.txt
# playwright install chromium   # 首次或升级 Playwright 后需要；已安装可跳过
```

**Linux / macOS：**

```bash
cd llbot-bridge
python3.10 -m pip install -r requirements.txt
# playwright install chromium   # 首次或升级 Playwright 后需要；已安装可跳过
```

3. 编辑 `config.yaml`（鉴权 token、LLOneBot 地址）和 `routes.yaml`（群号/好友）
4. 启动：

```bash
# Windows
py -3.10 server.py

# Linux / macOS
python3.10 server.py
```

默认监听 `http://0.0.0.0:8787/halo-push`，在 Halo 插件设置中填写 `pushScriptUrl` 为 `http://你的服务器:8787/halo-push`（生产环境建议 Nginx + HTTPS）。

## Halo 插件测试

在 Halo Console → 插件 → 博客动态推送 → **测试推送** Tab，点击按钮即可向 `settings` 里配置的测试 QQ 发送卡片。

## 模板

- 文章：`templates/Blog渲染/文章渲染.html`（封面 `.fm`）
- 瞬间：`templates/Blog渲染/瞬间渲染.html`（首图 `.tp`）

分类/标签会生成完整 `<div class="flbj"><span class="flzt">...</span></div>` 块。
