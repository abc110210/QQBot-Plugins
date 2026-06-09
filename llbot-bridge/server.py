#!/usr/bin/env python3
"""LLBOT bridge: receive Halo push JSON, render Blog渲染 HTML, send via LLOneBot."""

from __future__ import annotations

import html
import json
import logging
import os
import re
import tempfile
import threading
from pathlib import Path
from typing import Any

import requests
import yaml
from flask import Flask, jsonify, request

app = Flask(__name__)
BASE_DIR = Path(__file__).resolve().parent
CONFIG: dict[str, Any] = {}
ROUTES: dict[str, Any] = {}

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
log = logging.getLogger("llbot-bridge")


def load_config() -> None:
    global CONFIG, ROUTES
    with open(BASE_DIR / "config.yaml", encoding="utf-8") as f:
        CONFIG = yaml.safe_load(f)
    with open(BASE_DIR / "routes.yaml", encoding="utf-8") as f:
        ROUTES = yaml.safe_load(f).get("routes", {})


def auth_ok() -> bool:
    token = CONFIG.get("listen", {}).get("authToken")
    if not token:
        return True
    header = request.headers.get("Authorization", "")
    if header == f"Bearer {token}":
        return True
    return request.headers.get("X-Halo-Token") == token


def template_dir() -> Path:
    rel = CONFIG.get("templatesDir", "templates/Blog渲染")
    return (BASE_DIR / rel).resolve()


def read_template(name: str) -> str:
    path = template_dir() / name
    return path.read_text(encoding="utf-8")


def replace_css_url(content: str, selector: str, url: str) -> str:
    if not url:
        return content
    pattern = rf"({re.escape(selector)}\s*\{{[^}}]*background:\s*url\(\")[^\"]*(\")"
    return re.sub(pattern, rf"\1{url}\2", content, count=1)


def replace_text(content: str, selector: str, text: str, index: int = 0) -> str:
    safe = html.escape(text or "")
    if selector == "span.name":
        parts = content.split('<span class="name">', 1)
        if len(parts) == 2:
            rest = parts[1].split("</span>", 1)
            if len(rest) == 2:
                return parts[0] + f'<span class="name">{safe}</span>' + rest[1]
        return content
    if selector == ".timedata span.time":
        return re.sub(
            r'(<div class="timedata"><span class="time">)[^<]*(</span></div>)',
            rf"\1{safe}\2",
            content,
            count=1,
        )
    if selector == ".wzbt":
        return re.sub(r'(<span class="wzbt">)[^<]*(</span>)', rf"\1{safe}\2", content, count=1)
    if selector == ".wzzy":
        return re.sub(r'(<span class="wzzy">)[^<]*(</span>)', rf"\1{safe}\2", content, count=1)
    if selector == ".wzzt":
        return re.sub(r'(<span class="wzzt">)[^<]*(</span>)', rf"\1{safe}\2", content, count=1)
    if selector == ".dzwz":
        matches = list(re.finditer(r'(<span class="dzwz">)[^<]*(</span>)', content))
        if index < len(matches):
            m = matches[index]
            return content[: m.start()] + f'{m.group(1)}{safe}{m.group(2)}' + content[m.end() :]
    return content


def build_fl_chips(categories: list, tags: list) -> str:
    chips = []
    for item in (categories or []) + (tags or []):
        label = item.get("displayName") if isinstance(item, dict) else str(item)
        chips.append(f'<div class="flbj"><span class="flzt">{html.escape(label)}</span></div>')
    return "".join(chips)


def render_post(data: dict[str, Any]) -> str:
    content = read_template("文章渲染.html")
    content = replace_css_url(content, ".fm", data.get("cover", ""))
    content = replace_text(content, ".wzbt", data.get("title", ""))
    content = replace_text(content, ".wzzy", data.get("excerpt", ""))
    content = replace_text(content, "span.name", data.get("ownerDisplayName", ""))
    content = replace_text(content, ".timedata span.time", data.get("publishTime", ""))
    if data.get("ownerAvatar"):
        content = re.sub(
            r'(<img class="tx" src=")[^"]*(")',
            rf'\1{html.escape(data["ownerAvatar"])}\2',
            content,
            count=1,
        )
    chips = build_fl_chips(data.get("categories"), data.get("tags"))
    content = re.sub(r'(<div class="fl">)[\s\S]*?(</div>)', rf"\1{chips}\2", content, count=1)
    return content


def render_moment(data: dict[str, Any]) -> str:
    content = read_template("瞬间渲染.html")
    content = replace_css_url(content, ".tp", data.get("firstImage", ""))
    content = replace_text(content, ".wzzt", data.get("contentPreview", ""))
    content = replace_text(content, "span.name", data.get("ownerDisplayName", ""))
    content = replace_text(content, ".timedata span.time", data.get("publishTime", ""))
    content = replace_text(content, ".dzwz", str(data.get("upvote", 0)), 0)
    content = replace_text(content, ".dzwz", str(data.get("totalComment", 0)), 1)
    if data.get("ownerAvatar"):
        content = re.sub(
            r'(<img class="tx" src=")[^"]*(")',
            rf'\1{html.escape(data["ownerAvatar"])}\2',
            content,
            count=1,
        )
    if not data.get("firstImage"):
        content = re.sub(r"\.tpbox\s*\{[^}]*\}", ".tpbox{display:none;}", content)
    return content


def html_to_png(html_content: str) -> str | None:
    try:
        from playwright.sync_api import sync_playwright
    except ImportError:
        log.warning("playwright 未安装，跳过截图")
        return None

    tdir = template_dir()
    tmp_html = tdir / "_render_preview.html"
    tmp_png = tempfile.NamedTemporaryFile(suffix=".png", delete=False)
    tmp_png.close()

    tmp_html.write_text(html_content, encoding="utf-8")
    html_path = tmp_html.resolve().as_uri()
    try:
        with sync_playwright() as p:
            browser = p.chromium.launch()
            page = browser.new_page(viewport={"width": 800, "height": 1200})
            page.goto(html_path)
            page.wait_for_timeout(800)
            page.screenshot(path=tmp_png.name, full_page=True)
            browser.close()
        return tmp_png.name
    except Exception as e:
        log.error("HTML 截图失败: %s", e)
        return None
    finally:
        if tmp_html.exists():
            tmp_html.unlink(missing_ok=True)


def send_onebot(target_type: str, target_id: str, message: Any) -> None:
    cfg = CONFIG.get("llonebot", {})
    api_url = cfg.get("apiUrl", "http://127.0.0.1:3000").rstrip("/")
    token = cfg.get("accessToken", "")

    if target_type == "private":
        endpoint = f"{api_url}/send_private_msg"
        body = {"user_id": int(target_id), "message": message}
    else:
        endpoint = f"{api_url}/send_group_msg"
        body = {"group_id": int(target_id), "message": message}

    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"

    resp = requests.post(endpoint, json=body, headers=headers, timeout=30)
    resp.raise_for_status()
    log.info("LLOneBot 发送成功 type=%s id=%s", target_type, target_id)


def fallback_text(data: dict[str, Any]) -> str:
    category = data.get("category", "")
    if category == "post":
        return f"{data.get('title', '')}\n{data.get('excerpt', '')}\n{data.get('permalink', '')}"
    if category == "moment":
        return f"{data.get('contentPreview', '')}\n👍 {data.get('upvote', 0)} 💬 {data.get('totalComment', 0)}"
    return json.dumps(data, ensure_ascii=False)


def dispatch_push(payload: dict[str, Any]) -> None:
    event = payload.get("event", "")
    data = payload.get("data") or {}
    test = payload.get("test") is True or str(event).startswith("test.")

    if test and payload.get("testTarget"):
        targets = [payload["testTarget"]]
        if event.endswith("moment"):
            template = "瞬间渲染.html"
        else:
            template = "文章渲染.html"
        route_list = [{"type": targets[0]["type"], "id": targets[0]["id"], "template": template, "outputMode": "image"}]
    else:
        route_list = ROUTES.get(event, [])

    if not route_list:
        log.warning("未找到路由 event=%s", event)
        return

    for route in route_list:
        tpl = route.get("template", "")
        if tpl.endswith("瞬间渲染.html") or "moment" in event:
            rendered = render_moment(data)
        else:
            rendered = render_post(data)

        target_type = route.get("type", "group")
        target_id = str(route.get("id", ""))
        output_mode = route.get("outputMode", "image")

        if output_mode == "image":
            png = html_to_png(rendered)
            if png:
                send_onebot(target_type, target_id, [{"type": "image", "data": {"file": f"file:///{png.replace(chr(92), '/')}"}}])
                continue
        send_onebot(target_type, target_id, fallback_text(data))


@app.post("/halo-push")
def halo_push():
    if not auth_ok():
        return jsonify({"ok": False, "message": "unauthorized"}), 401

    payload = request.get_json(force=True, silent=True) or {}
    threading.Thread(target=dispatch_push, args=(payload,), daemon=True).start()
    return jsonify({"ok": True})


def main():
    load_config()
    host = CONFIG.get("listen", {}).get("host", "0.0.0.0")
    port = int(CONFIG.get("listen", {}).get("port", 8787))
    app.run(host=host, port=port)


if __name__ == "__main__":
    main()
