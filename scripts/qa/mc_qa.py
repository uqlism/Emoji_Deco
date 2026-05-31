#!/usr/bin/env python3
"""
mc_qa.py — Minecraft dev-client QA automation utility for RunicInk.

Working directory must be the project root (D:/repos/uqlism/RunicInk).
Coordinates are absolute screen pixels.

Usage:
  python scripts/qa/mc_qa.py screenshot <output_path>
  python scripts/qa/mc_qa.py bounds
  python scripts/qa/mc_qa.py focus
  python scripts/qa/mc_qa.py click <x> <y>
  python scripts/qa/mc_qa.py double-click <x> <y>
  python scripts/qa/mc_qa.py right-click <x> <y>
  python scripts/qa/mc_qa.py move <x> <y>
  python scripts/qa/mc_qa.py type <text>
  python scripts/qa/mc_qa.py key <key> [<key> ...]
  python scripts/qa/mc_qa.py scroll <x> <y> <amount>
  python scripts/qa/mc_qa.py wait-log <pattern> [--timeout N]
  python scripts/qa/mc_qa.py world-exists <name>
  python scripts/qa/mc_qa.py sleep <seconds>
"""

import argparse
import ctypes
import ctypes.wintypes
import json
import os
import re
import struct
import sys
import time
from pathlib import Path

try:
    import pyautogui
    import pygetwindow as gw
    import pyperclip
except ImportError:
    print(
        "ERROR: Missing dependencies.\n"
        "Run: pip install pyautogui pygetwindow pyperclip",
        file=sys.stderr,
    )
    sys.exit(1)

pyautogui.FAILSAFE = True   # Move mouse to top-left corner to abort
pyautogui.PAUSE = 0.05

# QA_RUN_DIR: Minecraft の run/ ディレクトリへのパス（デフォルト: "run"）
# NeoForge 用: QA_RUN_DIR=neoforge/run
_RUN_DIR = Path(os.environ.get("QA_RUN_DIR", "run"))


# ---------------------------------------------------------------------------
# Window helpers
# ---------------------------------------------------------------------------

def _find_window_by_pid(pid):
    """指定 PID のウィンドウを返す。"""
    result = []
    WNDENUMPROC = ctypes.WINFUNCTYPE(ctypes.c_bool, ctypes.c_int, ctypes.c_int)

    def callback(hwnd, _):
        wpid = ctypes.c_ulong()
        ctypes.windll.user32.GetWindowThreadProcessId(hwnd, ctypes.byref(wpid))
        if wpid.value == pid:
            rect = ctypes.wintypes.RECT()
            ctypes.windll.user32.GetWindowRect(hwnd, ctypes.byref(rect))
            w = rect.right - rect.left
            if w > 200:  # 小さいウィンドウを除外
                result.append(hwnd)
        return True

    ctypes.windll.user32.EnumWindows(WNDENUMPROC(callback), 0)
    return result[0] if result else None


def find_cf_window():
    """Find CurseForge Electron window."""
    wins = [w for w in gw.getAllWindows() if "CurseForge" in w.title and w.width > 200]
    # タイトルが完全一致する（Electron アプリ本体）を優先
    exact = [w for w in wins if w.title.strip() == "CurseForge"]
    if exact:
        return exact[0]
    return wins[0] if wins else None


def _focus_cf():
    win = find_cf_window()
    if not win:
        print("ERROR: CurseForge window not found", file=sys.stderr)
        sys.exit(1)
    hwnd = win._hWnd
    try:
        win.activate()
    except Exception:
        pass
    time.sleep(0.3)
    if ctypes.windll.user32.GetForegroundWindow() != hwnd:
        SW_MINIMIZE, SW_RESTORE = 6, 9
        ctypes.windll.user32.ShowWindow(hwnd, SW_MINIMIZE)
        time.sleep(0.2)
        ctypes.windll.user32.ShowWindow(hwnd, SW_RESTORE)
        time.sleep(0.4)
    return win


def find_mc_window():
    # PID ファイルが指定されている場合はそれを使う
    pid_file = os.environ.get("QA_PID_FILE")
    if pid_file and Path(pid_file).exists():
        try:
            pid = int(Path(pid_file).read_text().strip())
            hwnd = _find_window_by_pid(pid)
            if hwnd:
                wins = [w for w in gw.getAllWindows() if w._hWnd == hwnd]
                return wins[0] if wins else None
        except Exception:
            pass
    # 既存のフォールバック
    if os.environ.get("QA_WINDOW_TITLE"):
        target = os.environ["QA_WINDOW_TITLE"]
        wins = [w for w in gw.getAllWindows() if target in w.title and w.width > 0]
        return wins[0] if wins else None
    all_wins = [w for w in gw.getAllWindows() if w.width > 0]
    # Prefer Minecraft game window — must contain "Minecraft" to exclude "CurseForge"
    mc_forge = [w for w in all_wins if "Minecraft" in w.title and "Forge" in w.title]
    if mc_forge:
        return mc_forge[0]
    mc_wins = [w for w in all_wins if "Minecraft" in w.title
               and "Launcher" not in w.title and "CurseForge" not in w.title]
    return mc_wins[0] if mc_wins else None


def _save_focus_state():
    """現在のフォアグラウンドウィンドウとマウス位置を記録して返す。"""
    prev_hwnd = ctypes.windll.user32.GetForegroundWindow()
    prev_mouse = pyautogui.position()
    return prev_hwnd, prev_mouse


def _restore_focus_state(prev_hwnd, prev_mouse):
    """フォアグラウンドウィンドウとマウス位置を元に戻す。"""
    try:
        if prev_hwnd:
            ctypes.windll.user32.SetForegroundWindow(prev_hwnd)
    except Exception:
        pass
    try:
        pyautogui.moveTo(prev_mouse.x, prev_mouse.y, duration=0.05)
    except Exception:
        pass


def _focus():
    win = find_mc_window()
    if not win:
        print("ERROR: Minecraft window not found", file=sys.stderr)
        sys.exit(1)
    hwnd = win._hWnd

    # まず通常の activate を試みる
    try:
        win.activate()
    except Exception:
        pass
    time.sleep(0.3)

    # フォーカスが取れていなければ最小化→復元で強制取得
    if ctypes.windll.user32.GetForegroundWindow() != hwnd:
        SW_MINIMIZE, SW_RESTORE = 6, 9
        ctypes.windll.user32.ShowWindow(hwnd, SW_MINIMIZE)
        time.sleep(0.2)
        ctypes.windll.user32.ShowWindow(hwnd, SW_RESTORE)
        time.sleep(0.4)

    return win


# ---------------------------------------------------------------------------
# Commands
# ---------------------------------------------------------------------------

def cmd_screenshot(args):
    out = Path(args.output)
    out.parent.mkdir(parents=True, exist_ok=True)

    # VM モード (QA_WINDOW_TITLE 設定時): ウィンドウ領域だけ切り取る。
    # これでホスト側の他のウィンドウが映り込まず、vision 判定の精度が上がる。
    if os.environ.get("QA_WINDOW_TITLE"):
        win = find_mc_window()
        if win:
            from PIL import ImageGrab
            img = ImageGrab.grab(bbox=(win.left, win.top, win.right, win.bottom))
        else:
            img = pyautogui.screenshot()
    else:
        img = pyautogui.screenshot()

    img.save(str(out))
    print(str(out.resolve()))


def cmd_bounds(args):
    win = find_mc_window()
    if not win:
        print("ERROR: Minecraft window not found", file=sys.stderr)
        sys.exit(1)
    print(json.dumps({"x": win.left, "y": win.top, "w": win.width, "h": win.height}))


def cmd_focus(args):
    win = _focus()
    print(json.dumps({"x": win.left, "y": win.top, "w": win.width, "h": win.height}))


def cmd_click(args):
    prev = _save_focus_state()
    _focus()
    pyautogui.moveTo(args.x, args.y, duration=0.1)
    time.sleep(0.05)
    pyautogui.click(args.x, args.y)
    time.sleep(0.2)  # Minecraft がクリックを処理するまで待つ
    _restore_focus_state(*prev)


def cmd_double_click(args):
    prev = _save_focus_state()
    _focus()
    pyautogui.moveTo(args.x, args.y, duration=0.1)
    time.sleep(0.05)
    pyautogui.doubleClick(args.x, args.y)
    time.sleep(0.2)
    _restore_focus_state(*prev)


def _send_mouse_button(flags):
    """SendInput でマウスボタンイベントを送信する（フォーカス不要、マウスキャプチャ対応）。"""
    class _MI(ctypes.Structure):
        _fields_ = [('dx', ctypes.c_long), ('dy', ctypes.c_long),
                    ('mouseData', ctypes.c_ulong), ('dwFlags', ctypes.c_ulong),
                    ('time', ctypes.c_ulong), ('dwExtraInfo', ctypes.POINTER(ctypes.c_ulong))]
    class _INP(ctypes.Structure):
        class _U(ctypes.Union):
            _fields_ = [('mi', _MI)]
        _anonymous_ = ('_u',)
        _fields_ = [('type', ctypes.c_ulong), ('_u', _U)]
    i = _INP()
    i.type = 0
    i.mi.dx = i.mi.dy = i.mi.mouseData = i.mi.time = 0
    i.mi.dwFlags = flags
    i.mi.dwExtraInfo = None
    ctypes.windll.user32.SendInput(1, ctypes.byref(i), ctypes.sizeof(_INP))


def cmd_right_click(args):
    """in-game 右クリック: PostMessage WM_RBUTTONDOWN/UP をウィンドウ中央に送信。
    フォーカス不要。クロスヘアが向いているブロックに作用する。
    """
    WM_RBUTTONDOWN = 0x0204
    WM_RBUTTONUP   = 0x0205
    MK_RBUTTON     = 0x0002

    hwnd = _hwnd()
    rect = ctypes.wintypes.RECT()
    ctypes.windll.user32.GetClientRect(hwnd, ctypes.byref(rect))
    cx = (rect.right - rect.left) // 2
    cy = (rect.bottom - rect.top) // 2
    lparam = (cy << 16) | (cx & 0xFFFF)

    ctypes.windll.user32.PostMessageW(hwnd, WM_RBUTTONDOWN, MK_RBUTTON, lparam)
    time.sleep(0.1)
    ctypes.windll.user32.PostMessageW(hwnd, WM_RBUTTONUP, 0, lparam)
    time.sleep(0.2)
    print(f"Right-click sent (PostMessage WM_RBUTTON center={cx},{cy})")


def cmd_move(args):
    pyautogui.moveTo(args.x, args.y, duration=0.1)


def cmd_type(args):
    prev = _save_focus_state()
    _focus()
    pyperclip.copy(args.text)
    time.sleep(0.1)
    pyautogui.hotkey("ctrl", "v")
    _restore_focus_state(*prev)


def cmd_key(args):
    prev = _save_focus_state()
    _focus()
    if len(args.keys) == 1:
        pyautogui.press(args.keys[0])
    else:
        pyautogui.hotkey(*args.keys)
    _restore_focus_state(*prev)


def cmd_scroll(args):
    prev = _save_focus_state()
    _focus()
    pyautogui.scroll(args.amount, x=args.x, y=args.y)
    _restore_focus_state(*prev)


def cmd_wait_log(args):
    log_path = _RUN_DIR / "logs/latest.log"
    deadline = time.time() + args.timeout

    # --fresh: ファイルが新規作成されるまで待ってから検索する（前セッションのログを拾わない）
    if getattr(args, "fresh", False):
        start_mtime = log_path.stat().st_mtime if log_path.exists() else 0
        while time.time() < deadline:
            if log_path.exists() and log_path.stat().st_mtime > start_mtime + 1:
                break
            time.sleep(0.5)
        initial_size = 0  # 新しいファイルなので最初から読む
    else:
        # ファイルが縮小した場合（ローテーション）は先頭から読む
        initial_size = log_path.stat().st_size if log_path.exists() else 0

    while time.time() < deadline:
        if log_path.exists():
            try:
                text = log_path.read_text(encoding="utf-8", errors="ignore")
                offset = initial_size if len(text) >= initial_size else 0
                if re.search(args.pattern, text[offset:]):
                    print("FOUND")
                    return
            except Exception:
                pass
        time.sleep(1)

    print(f"TIMEOUT: '{args.pattern}' not found within {args.timeout}s", file=sys.stderr)
    sys.exit(1)


def cmd_world_exists(args):
    path = _RUN_DIR / "saves" / args.name
    print("true" if path.exists() else "false")


def cmd_copy_world(args):
    """run/saves/<src>/ を run/saves/<dst>/ にコピーする（session.lock を除外）。"""
    import shutil
    src = _RUN_DIR / "saves" / args.src
    dst = _RUN_DIR / "saves" / args.dst
    if not src.exists():
        print(f"ERROR: {src} not found", file=sys.stderr)
        sys.exit(1)
    if dst.exists():
        shutil.rmtree(dst)
    shutil.copytree(src, dst, ignore=shutil.ignore_patterns("session.lock"))
    print(f"Copied {src} -> {dst}")


def cmd_sleep(args):
    time.sleep(args.seconds)
    print(f"Slept {args.seconds}s")


def cmd_sequence(args):
    """複数のコマンドを1プロセス内で連続実行する。
    Minecraft をフォーカスしたまま操作を続けられるため、
    各コマンド間でフォーカスが外れる問題を回避できる。

    書式: python mc_qa.py sequence <cmd1>:<arg1>,<arg2> <cmd2>:<arg> ...
    例:   python mc_qa.py sequence focus key:t sleep:0.5 wscreenshot:out.png
    """
    import argparse as _ap

    # フォーカスを取得して保持
    _focus()

    for step in args.steps:
        parts = step.split(":", 1)
        cmd_name = parts[0]
        cmd_args_str = parts[1] if len(parts) > 1 else ""

        print(f"[seq] {step}")

        if cmd_name == "sleep":
            time.sleep(float(cmd_args_str))
        elif cmd_name == "key":
            keys = cmd_args_str.split(",")
            if len(keys) == 1:
                pyautogui.press(keys[0])
            else:
                pyautogui.hotkey(*keys)
        elif cmd_name == "type":
            pyperclip.copy(cmd_args_str)
            time.sleep(0.1)
            pyautogui.hotkey("ctrl", "v")
        elif cmd_name == "click":
            x, y = map(int, cmd_args_str.split(","))
            pyautogui.click(x, y)
        elif cmd_name == "rclick":
            # in-game 右クリック: SendInput でボタンのみ送信（クロスヘア対象に作用）
            _send_mouse_button(0x0008)  # MOUSEEVENTF_RIGHTDOWN
            time.sleep(0.1)
            _send_mouse_button(0x0010)  # MOUSEEVENTF_RIGHTUP
        elif cmd_name == "wclick":
            x, y = map(int, cmd_args_str.split(","))
            WM_LBUTTONDOWN, WM_LBUTTONUP = 0x0201, 0x0202
            win = find_mc_window()
            hwnd = win._hWnd
            pt = ctypes.wintypes.POINT(x, y)
            ctypes.windll.user32.ScreenToClient(hwnd, ctypes.byref(pt))
            lp = (pt.y << 16) | (pt.x & 0xFFFF)
            ctypes.windll.user32.PostMessageW(hwnd, WM_LBUTTONDOWN, 0x0001, lp)
            time.sleep(0.05)
            ctypes.windll.user32.PostMessageW(hwnd, WM_LBUTTONUP, 0, lp)
        elif cmd_name == "wkey":
            vk = _VK_MAP.get(cmd_args_str.lower())
            if vk is None:
                vk = int(cmd_args_str, 0)
            WM_KEYDOWN, WM_KEYUP = 0x0100, 0x0101
            win = find_mc_window()
            ctypes.windll.user32.PostMessageW(win._hWnd, WM_KEYDOWN, vk, 0)
            time.sleep(0.05)
            ctypes.windll.user32.PostMessageW(win._hWnd, WM_KEYUP, vk, 0)
        elif cmd_name == "wtype":
            WM_CHAR = 0x0102
            win = find_mc_window()
            for ch in cmd_args_str:
                ctypes.windll.user32.PostMessageW(win._hWnd, WM_CHAR, ord(ch), 0)
                time.sleep(0.02)
        elif cmd_name == "wscreenshot":
            # wscreenshot と同じロジック (フォーカスなし)
            mock = type("A", (), {"output": cmd_args_str})()
            cmd_wscreenshot(mock)
        elif cmd_name == "refocus":
            # Minecraft のフォーカスを再取得
            _focus()
        elif cmd_name == "openchat":
            pyautogui.press("t")
            time.sleep(0.3)
            _win = find_mc_window()
            ctypes.windll.user32.PostMessageW(_win._hWnd, 0x0100, 0x08, 0)
            time.sleep(0.05)
            ctypes.windll.user32.PostMessageW(_win._hWnd, 0x0101, 0x08, 0)
            time.sleep(0.1)
        elif cmd_name == "cmd":
            # openchat + wtype + return を1ステップで
            pyautogui.press("t")
            time.sleep(0.3)
            _win_c = find_mc_window()
            _hwnd_c = _win_c._hWnd
            ctypes.windll.user32.PostMessageW(_hwnd_c, 0x0100, 0x08, 0)
            time.sleep(0.05)
            ctypes.windll.user32.PostMessageW(_hwnd_c, 0x0101, 0x08, 0)
            time.sleep(0.05)
            _chat_send(_hwnd_c, cmd_args_str)
            time.sleep(0.3)
        elif cmd_name == "wrclick":
            # GUI 右クリック: PostMessage WM_RBUTTONDOWN/UP（座標指定、フォーカス不要）
            x, y = map(int, cmd_args_str.split(","))
            WM_RBUTTONDOWN, WM_RBUTTONUP = 0x0204, 0x0205
            win = find_mc_window()
            hwnd = win._hWnd
            pt = ctypes.wintypes.POINT(x, y)
            ctypes.windll.user32.ScreenToClient(hwnd, ctypes.byref(pt))
            lp = (pt.y << 16) | (pt.x & 0xFFFF)
            ctypes.windll.user32.PostMessageW(hwnd, WM_RBUTTONDOWN, 0x0002, lp)
            time.sleep(0.05)
            ctypes.windll.user32.PostMessageW(hwnd, WM_RBUTTONUP, 0, lp)
        # ── CurseForge 操作 ────────────────────────────────────────────────
        elif cmd_name == "cfscreenshot":
            mock = type("A", (), {"output": cmd_args_str})()
            cmd_cf_wscreenshot(mock)
        elif cmd_name == "cfclick":
            x, y = map(int, cmd_args_str.split(","))
            _focus_cf()
            pyautogui.moveTo(x, y, duration=0.1)
            time.sleep(0.05)
            pyautogui.click(x, y)
            time.sleep(0.3)
        elif cmd_name == "cfdblclick":
            x, y = map(int, cmd_args_str.split(","))
            _focus_cf()
            pyautogui.moveTo(x, y, duration=0.1)
            time.sleep(0.05)
            pyautogui.doubleClick(x, y)
            time.sleep(0.3)
        elif cmd_name == "cfrclick":
            x, y = map(int, cmd_args_str.split(","))
            _focus_cf()
            pyautogui.moveTo(x, y, duration=0.1)
            time.sleep(0.05)
            pyautogui.rightClick(x, y)
            time.sleep(0.2)
        elif cmd_name == "cftype":
            _focus_cf()
            pyperclip.copy(cmd_args_str)
            time.sleep(0.1)
            pyautogui.hotkey("ctrl", "v")
            time.sleep(0.1)
        elif cmd_name == "cfkey":
            keys = cmd_args_str.split(",")
            _focus_cf()
            if len(keys) == 1:
                pyautogui.press(keys[0])
            else:
                pyautogui.hotkey(*keys)
            time.sleep(0.1)
        else:
            print(f"[seq] WARN: unknown command '{cmd_name}', skipping")


# ---------------------------------------------------------------------------
# CurseForge 操作コマンド (focus 取得 + pyautogui)
# ---------------------------------------------------------------------------

def cmd_cf_screenshot(args):
    """CurseForge ウィンドウ領域を ImageGrab でキャプチャ（フォーカス不要だが遮蔽に注意）。"""
    win = find_cf_window()
    out = Path(args.output)
    out.parent.mkdir(parents=True, exist_ok=True)
    if win:
        from PIL import ImageGrab
        img = ImageGrab.grab(bbox=(win.left, win.top, win.right, win.bottom))
    else:
        img = pyautogui.screenshot()
    img.save(str(out))
    print(str(out.resolve()))


def cmd_cf_wscreenshot(args):
    """PrintWindow で CurseForge をキャプチャ（遮蔽しても取れる）。"""
    from PIL import Image
    win = find_cf_window()
    if not win:
        print("ERROR: CurseForge window not found", file=sys.stderr)
        sys.exit(1)
    hwnd = win._hWnd
    rect = ctypes.wintypes.RECT()
    ctypes.windll.user32.GetWindowRect(hwnd, ctypes.byref(rect))
    w = rect.right  - rect.left
    h = rect.bottom - rect.top
    hwnd_dc = ctypes.windll.user32.GetWindowDC(hwnd)
    mem_dc  = ctypes.windll.gdi32.CreateCompatibleDC(hwnd_dc)
    bitmap  = ctypes.windll.gdi32.CreateCompatibleBitmap(hwnd_dc, w, h)
    ctypes.windll.gdi32.SelectObject(mem_dc, bitmap)
    ctypes.windll.user32.PrintWindow(hwnd, mem_dc, 0x2)  # PW_RENDERFULLCONTENT
    bmi = struct.pack("IiiHHIIiiII", 40, w, -h, 1, 32, 0, w * h * 4, 0, 0, 0, 0)
    buf = ctypes.create_string_buffer(w * h * 4)
    ctypes.windll.gdi32.GetDIBits(mem_dc, bitmap, 0, h, buf, bmi, 0)
    ctypes.windll.gdi32.DeleteObject(bitmap)
    ctypes.windll.gdi32.DeleteDC(mem_dc)
    ctypes.windll.user32.ReleaseDC(hwnd, hwnd_dc)
    img = Image.frombytes("RGBA", (w, h), bytes(buf), "raw", "BGRA")
    out = Path(args.output)
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(str(out))
    print(str(out.resolve()))


def cmd_cf_click(args):
    prev = _save_focus_state()
    _focus_cf()
    pyautogui.moveTo(args.x, args.y, duration=0.1)
    time.sleep(0.05)
    pyautogui.click(args.x, args.y)
    time.sleep(0.3)
    _restore_focus_state(*prev)


def cmd_cf_double_click(args):
    prev = _save_focus_state()
    _focus_cf()
    pyautogui.moveTo(args.x, args.y, duration=0.1)
    time.sleep(0.05)
    pyautogui.doubleClick(args.x, args.y)
    time.sleep(0.3)
    _restore_focus_state(*prev)


def cmd_cf_right_click(args):
    prev = _save_focus_state()
    _focus_cf()
    pyautogui.moveTo(args.x, args.y, duration=0.1)
    time.sleep(0.05)
    pyautogui.rightClick(args.x, args.y)
    time.sleep(0.2)
    _restore_focus_state(*prev)


def cmd_cf_type(args):
    prev = _save_focus_state()
    _focus_cf()
    pyperclip.copy(args.text)
    time.sleep(0.1)
    pyautogui.hotkey("ctrl", "v")
    time.sleep(0.1)
    _restore_focus_state(*prev)


def cmd_cf_key(args):
    prev = _save_focus_state()
    _focus_cf()
    if len(args.keys) == 1:
        pyautogui.press(args.keys[0])
    else:
        pyautogui.hotkey(*args.keys)
    time.sleep(0.1)
    _restore_focus_state(*prev)


def cmd_rcon(args):
    """RCON 経由でコマンドを送信 — フォーカス完全不要。
    run/server.properties に enable-rcon=true が必要。
    """
    import socket

    host     = getattr(args, "host", "localhost")
    port     = getattr(args, "port", 25575)
    password = getattr(args, "password", "qatest123")
    command  = args.mc_command

    def _pack(req_id: int, ptype: int, payload: str) -> bytes:
        data = payload.encode("utf-8") + b"\x00\x00"
        header = struct.pack("<iii", 4 + 4 + len(data), req_id, ptype)
        return header + data

    def _unpack(sock) -> tuple[int, int, str]:
        raw_len = sock.recv(4)
        length = struct.unpack("<i", raw_len)[0]
        data = b""
        while len(data) < length:
            data += sock.recv(length - len(data))
        req_id, ptype = struct.unpack("<ii", data[:8])
        payload = data[8:-2].decode("utf-8", errors="replace")
        return req_id, ptype, payload

    with socket.create_connection((host, port), timeout=5) as sock:
        # 認証
        sock.sendall(_pack(1, 3, password))
        rid, _, _ = _unpack(sock)
        if rid == -1:
            print("ERROR: RCON auth failed (wrong password?)", file=sys.stderr)
            sys.exit(1)
        # コマンド送信
        sock.sendall(_pack(2, 2, command))
        _, _, response = _unpack(sock)

    print(response if response else "(no response)")


# ---------------------------------------------------------------------------
# Win32 フォーカスなし操作 (PostMessage / PrintWindow)
# ---------------------------------------------------------------------------

_VK_MAP = {
    "t": 0x54, "return": 0x0D, "enter": 0x0D, "escape": 0x1B, "space": 0x20,
    "backspace": 0x08,
    "e": 0x45, "f": 0x46, "f3": 0x72, "f5": 0x74,
    "1": 0x31, "2": 0x32, "3": 0x33, "4": 0x34, "5": 0x35,
    "6": 0x36, "7": 0x37, "8": 0x38, "9": 0x39, "0": 0x30,
}

def _hwnd():
    """Minecraft ウィンドウの HWND を返す。find_mc_window() と同じウィンドウを使う。"""
    win = find_mc_window()
    if not win:
        print("ERROR: Minecraft window not found", file=sys.stderr)
        sys.exit(1)
    return win._hWnd


def cmd_wclick(args):
    """PostMessage WM_LBUTTONDOWN/UP — フォーカスなしでメニューをクリック。
    座標はスクリーン絶対座標で指定、内部でクライアント座標に変換する。
    """
    WM_LBUTTONDOWN = 0x0201
    WM_LBUTTONUP   = 0x0202
    MK_LBUTTON     = 0x0001

    hwnd = _hwnd()
    pt = ctypes.wintypes.POINT(args.x, args.y)
    ctypes.windll.user32.ScreenToClient(hwnd, ctypes.byref(pt))
    lparam = (pt.y << 16) | (pt.x & 0xFFFF)

    ctypes.windll.user32.PostMessageW(hwnd, WM_LBUTTONDOWN, MK_LBUTTON, lparam)
    time.sleep(0.05)
    ctypes.windll.user32.PostMessageW(hwnd, WM_LBUTTONUP, 0, lparam)
    print(f"WM_LBUTTON sent: screen({args.x},{args.y}) → client({pt.x},{pt.y}) hwnd={hwnd}")


def cmd_wrclick(args):
    """PostMessage WM_RBUTTONDOWN/UP — フォーカスなしで GUI 右クリック。
    座標はスクリーン絶対座標で指定、内部でクライアント座標に変換する。
    GUI ボタン右クリックに使用。in-game インタラクションには right-click を使うこと。
    """
    WM_RBUTTONDOWN = 0x0204
    WM_RBUTTONUP   = 0x0205
    MK_RBUTTON     = 0x0002

    hwnd = _hwnd()
    pt = ctypes.wintypes.POINT(args.x, args.y)
    ctypes.windll.user32.ScreenToClient(hwnd, ctypes.byref(pt))
    lparam = (pt.y << 16) | (pt.x & 0xFFFF)

    ctypes.windll.user32.PostMessageW(hwnd, WM_RBUTTONDOWN, MK_RBUTTON, lparam)
    time.sleep(0.05)
    ctypes.windll.user32.PostMessageW(hwnd, WM_RBUTTONUP, 0, lparam)
    print(f"WM_RBUTTON sent: screen({args.x},{args.y}) → client({pt.x},{pt.y}) hwnd={hwnd}")


def cmd_wkey(args):
    """PostMessage WM_KEYDOWN/UP — フォーカスなしでキーを送る。"""
    WM_KEYDOWN, WM_KEYUP = 0x0100, 0x0101
    hwnd = _hwnd()
    vk = _VK_MAP.get(args.key.lower())
    if vk is None:
        try:
            vk = int(args.key, 0)
        except ValueError:
            print(f"ERROR: unknown key '{args.key}'", file=sys.stderr)
            sys.exit(1)
    ctypes.windll.user32.PostMessageW(hwnd, WM_KEYDOWN, vk, 0)
    time.sleep(0.05)
    ctypes.windll.user32.PostMessageW(hwnd, WM_KEYUP, vk, 0)
    print(f"WM_KEYDOWN/UP sent: {args.key} (0x{vk:02X}) → hwnd={hwnd}")


def cmd_wtype(args):
    """PostMessage WM_CHAR — フォーカスなしでテキストを送る。"""
    WM_CHAR = 0x0102
    hwnd = _hwnd()
    for ch in args.text:
        ctypes.windll.user32.PostMessageW(hwnd, WM_CHAR, ord(ch), 0)
        time.sleep(0.02)
    print(f"WM_CHAR sent: {len(args.text)} chars → hwnd={hwnd}")


def cmd_wscreenshot(args):
    """PrintWindow (PW_RENDERFULLCONTENT) — フォーカスなしで OpenGL ウィンドウをキャプチャ。"""
    from PIL import Image
    hwnd = _hwnd()
    rect = ctypes.wintypes.RECT()
    ctypes.windll.user32.GetWindowRect(hwnd, ctypes.byref(rect))
    w = rect.right  - rect.left
    h = rect.bottom - rect.top

    hwnd_dc  = ctypes.windll.user32.GetWindowDC(hwnd)
    mem_dc   = ctypes.windll.gdi32.CreateCompatibleDC(hwnd_dc)
    bitmap   = ctypes.windll.gdi32.CreateCompatibleBitmap(hwnd_dc, w, h)
    ctypes.windll.gdi32.SelectObject(mem_dc, bitmap)

    PW_RENDERFULLCONTENT = 0x2
    ctypes.windll.user32.PrintWindow(hwnd, mem_dc, PW_RENDERFULLCONTENT)

    # BITMAPINFOHEADER + ピクセルデータ取得
    bmi = struct.pack("IiiHHIIiiII", 40, w, -h, 1, 32, 0, w * h * 4, 0, 0, 0, 0)
    buf = ctypes.create_string_buffer(w * h * 4)
    ctypes.windll.gdi32.GetDIBits(mem_dc, bitmap, 0, h, buf, bmi, 0)

    ctypes.windll.gdi32.DeleteObject(bitmap)
    ctypes.windll.gdi32.DeleteDC(mem_dc)
    ctypes.windll.user32.ReleaseDC(hwnd, hwnd_dc)

    img = Image.frombytes("RGBA", (w, h), bytes(buf), "raw", "BGRA")
    out = Path(args.output)
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(str(out))
    print(str(out.resolve()))


def cmd_openchat(args):
    """チャットを開いて 't' を削除し入力待機状態にする。以降は wtype/wkey でフォーカス不要。"""
    prev = _save_focus_state()
    _focus()
    pyautogui.press("t")
    time.sleep(0.3)
    _restore_focus_state(*prev)
    time.sleep(0.1)
    hwnd = _hwnd()
    ctypes.windll.user32.PostMessageW(hwnd, 0x0100, 0x08, 0)  # WM_KEYDOWN BACKSPACE
    time.sleep(0.05)
    ctypes.windll.user32.PostMessageW(hwnd, 0x0101, 0x08, 0)  # WM_KEYUP BACKSPACE
    print("Chat opened")


def _chat_send(hwnd, text):
    """チャットフィールドにテキストを入力して送信する（フォーカス不要）。"""
    for ch in text:
        ctypes.windll.user32.PostMessageW(hwnd, 0x0102, ord(ch), 0)  # WM_CHAR
        time.sleep(0.02)
    time.sleep(0.1)
    ctypes.windll.user32.PostMessageW(hwnd, 0x0100, 0x0D, 0)  # WM_KEYDOWN RETURN
    time.sleep(0.05)
    ctypes.windll.user32.PostMessageW(hwnd, 0x0101, 0x0D, 0)  # WM_KEYUP RETURN


def cmd_sendcmd(args):
    """チャットを開いてテキストを送信し、オプションでスクリーンショットを撮る。
    openchat + wtype + return を1コマンドで実行する。
    """
    prev = _save_focus_state()
    _focus()
    pyautogui.press("t")
    time.sleep(0.3)
    _restore_focus_state(*prev)
    time.sleep(0.1)
    hwnd = _hwnd()
    ctypes.windll.user32.PostMessageW(hwnd, 0x0100, 0x08, 0)  # BACKSPACE で "t" を削除
    time.sleep(0.05)
    ctypes.windll.user32.PostMessageW(hwnd, 0x0101, 0x08, 0)
    time.sleep(0.05)
    _chat_send(hwnd, args.text)
    if args.sleep > 0:
        time.sleep(args.sleep)
    if args.screenshot:
        mock = type("A", (), {"output": args.screenshot})()
        cmd_wscreenshot(mock)
    else:
        print("Sent")


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(description="Minecraft QA automation utility")
    sub = parser.add_subparsers(dest="command", required=True)

    p = sub.add_parser("screenshot")
    p.add_argument("output")

    sub.add_parser("bounds")
    sub.add_parser("focus")

    for name, help_ in [
        ("click", "Click at coordinates"),
        ("double-click", "Double-click at coordinates"),
        ("move", "Move mouse to coordinates"),
    ]:
        p = sub.add_parser(name, help=help_)
        p.add_argument("x", type=int)
        p.add_argument("y", type=int)

    sub.add_parser("right-click",
        help="In-game right-click via SendInput (works with mouse capture; no coordinates needed)")

    p = sub.add_parser("type", help="Paste text via clipboard")
    p.add_argument("text")

    p = sub.add_parser("key", help="Press key or key combination")
    p.add_argument("keys", nargs="+")

    p = sub.add_parser("scroll")
    p.add_argument("x", type=int)
    p.add_argument("y", type=int)
    p.add_argument("amount", type=int, help="Positive=up, negative=down")

    p = sub.add_parser("wait-log", help="Wait for regex in run/logs/latest.log")
    p.add_argument("pattern")
    p.add_argument("--timeout", type=int, default=120)
    p.add_argument("--fresh", action="store_true", help="Wait for log file to be recreated first (prevents old session interference)")

    p = sub.add_parser("world-exists", help="Check run/saves/<name>/ exists")
    p.add_argument("name")

    p = sub.add_parser("copy-world", help="Copy run/saves/<src> to run/saves/<dst>, excluding session.lock")
    p.add_argument("src")
    p.add_argument("dst")

    p = sub.add_parser("sleep")
    p.add_argument("seconds", type=float)

    p = sub.add_parser("wclick", help="PostMessage WM_LBUTTONDOWN (no focus needed, client coords auto-converted)")
    p.add_argument("x", type=int)
    p.add_argument("y", type=int)

    p = sub.add_parser("wrclick", help="PostMessage WM_RBUTTONDOWN (no focus, GUI right-click with coordinates)")
    p.add_argument("x", type=int)
    p.add_argument("y", type=int)

    p = sub.add_parser("wkey", help="PostMessage WM_KEYDOWN (no focus needed)")
    p.add_argument("key", help="Key name (t/return/escape/space/...) or hex VK code")

    p = sub.add_parser("wtype", help="PostMessage WM_CHAR (no focus needed)")
    p.add_argument("text")

    p = sub.add_parser("wscreenshot", help="PrintWindow screenshot (no focus, captures OpenGL)")
    p.add_argument("output")

    sub.add_parser("openchat", help="Open chat and clear 't' (focus steal once, then wtype/wkey work without focus)")

    p = sub.add_parser("sendcmd", help="Open chat, type text, send, optionally screenshot (openchat+wtype+return in one command)")
    p.add_argument("text")
    p.add_argument("--screenshot", default=None, metavar="PATH")
    p.add_argument("--sleep", type=float, default=0.5, metavar="SEC")

    p = sub.add_parser("sequence", help="Run multiple commands in one process (Minecraft stays focused)")
    p.add_argument("steps", nargs="+", help="cmd:arg1,arg2 ...")

    p = sub.add_parser("cf-screenshot",   help="Capture CurseForge window (PrintWindow, no focus needed)")
    p.add_argument("output")
    p = sub.add_parser("cf-wscreenshot",  help="Alias for cf-screenshot")
    p.add_argument("output")
    for name, help_ in [
        ("cf-click",        "Click in CurseForge (focus + pyautogui)"),
        ("cf-double-click", "Double-click in CurseForge"),
        ("cf-right-click",  "Right-click in CurseForge"),
    ]:
        p = sub.add_parser(name, help=help_)
        p.add_argument("x", type=int)
        p.add_argument("y", type=int)
    p = sub.add_parser("cf-type", help="Type text in CurseForge (focus + clipboard paste)")
    p.add_argument("text")
    p = sub.add_parser("cf-key", help="Press key in CurseForge (focus + pyautogui)")
    p.add_argument("keys", nargs="+")

    p = sub.add_parser("rcon", help="Send command via RCON (no focus needed)")
    p.add_argument("mc_command", help="Minecraft command (e.g. 'say hello')")
    p.add_argument("--host",     default="localhost")
    p.add_argument("--port",     type=int, default=25575)
    p.add_argument("--password", default="qatest123")

    args = parser.parse_args()

    dispatch = {
        "screenshot":   cmd_screenshot,
        "bounds":       cmd_bounds,
        "focus":        cmd_focus,
        "click":        cmd_click,
        "double-click": cmd_double_click,
        "right-click":  cmd_right_click,
        "move":         cmd_move,
        "type":         cmd_type,
        "key":          cmd_key,
        "scroll":       cmd_scroll,
        "wait-log":     cmd_wait_log,
        "world-exists":  cmd_world_exists,
        "copy-world":    cmd_copy_world,
        "sleep":         cmd_sleep,
        "wclick":        cmd_wclick,
        "wrclick":       cmd_wrclick,
        "wkey":          cmd_wkey,
        "wtype":         cmd_wtype,
        "wscreenshot":   cmd_wscreenshot,
        "openchat":      cmd_openchat,
        "sendcmd":       cmd_sendcmd,
        "sequence":      cmd_sequence,
        "rcon":           cmd_rcon,
        "cf-screenshot":  cmd_cf_wscreenshot,
        "cf-wscreenshot": cmd_cf_wscreenshot,
        "cf-click":       cmd_cf_click,
        "cf-double-click":cmd_cf_double_click,
        "cf-right-click": cmd_cf_right_click,
        "cf-type":        cmd_cf_type,
        "cf-key":         cmd_cf_key,
    }
    dispatch[args.command](args)


if __name__ == "__main__":
    main()
