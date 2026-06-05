#!/usr/bin/env python3
"""Final QA tests for NeoForge coverage matrix."""
import sys
import os
import time
import ctypes
import ctypes.wintypes
import pyautogui
import pygetwindow as gw
from PIL import Image
import numpy as np

sys.path.insert(0, 'D:/repos/uqlism/RunicInk/scripts/qa')
os.chdir('D:/repos/uqlism/RunicInk')
import mc_qa

def find_mc():
    return mc_qa.find_mc_window()

def _hwnd():
    return find_mc()._hWnd

def wtype(text):
    hwnd = _hwnd()
    for ch in text:
        ctypes.windll.user32.PostMessageW(hwnd, 0x0102, ord(ch), 0)
        time.sleep(0.02)

def wkey(key):
    VK_MAP = {"t": 0x54, "return": 0x0D, "escape": 0x1B, "backspace": 0x08,
              "1": 0x31, "2": 0x32}
    vk = VK_MAP.get(key.lower())
    hwnd = _hwnd()
    ctypes.windll.user32.PostMessageW(hwnd, 0x0100, vk, 0)
    time.sleep(0.05)
    ctypes.windll.user32.PostMessageW(hwnd, 0x0101, vk, 0)

def wscreenshot(path):
    args = type('A', (), {'output': path})()
    mc_qa.cmd_wscreenshot(args)
    return path

def is_menu():
    args = type('A', (), {'output': 'run/qa-screenshots/neoforge-matrix/_chk.png'})()
    mc_qa.cmd_wscreenshot(args)
    img = Image.open('run/qa-screenshots/neoforge-matrix/_chk.png')
    arr = np.array(img)
    cx = 648
    for y in range(220, 300):
        r, g, b = int(arr[y, cx, 0]), int(arr[y, cx, 1]), int(arr[y, cx, 2])
        if r > 240 and g > 240 and b > 240:
            return True
    return False

def close_menu_and_capture():
    win = find_mc()
    win.activate()
    time.sleep(0.3)
    if is_menu():
        back_x = win.left + 648
        back_y = win.top + 248
        pyautogui.click(back_x, back_y)
        time.sleep(0.5)
    # Click center to capture mouse
    cx = win.left + win.width // 2
    cy = win.top + win.height // 2
    pyautogui.click(cx, cy)
    time.sleep(0.3)

def send_command(cmd):
    close_menu_and_capture()
    pyautogui.press('t')
    time.sleep(0.3)
    hwnd = _hwnd()
    ctypes.windll.user32.PostMessageW(hwnd, 0x0100, 0x08, 0)
    time.sleep(0.05)
    ctypes.windll.user32.PostMessageW(hwnd, 0x0101, 0x08, 0)
    time.sleep(0.1)
    wtype(cmd)
    time.sleep(0.1)
    wkey("return")
    time.sleep(0.5)

def make_sign_cmd(x, y, z, content):
    return '/setblock %d %d %d minecraft:oak_sign{front_text:{messages:["[{\\"text\\":\\"%s\\"}]","[\\"\\"]","[\\"\\"]","[\\"\\"]"]}}' % (x, y, z, content)

os.makedirs('run/qa-screenshots/neoforge-matrix', exist_ok=True)

# === Initial setup ===
win = find_mc()
win.activate()
time.sleep(0.3)
close_menu_and_capture()

print("=== SETUP ===")
send_command("/time set day")
send_command("/gamemode creative")
send_command("/kill @e[type=!player]")

# ============================================================
# SIGN TESTS
# ============================================================
print("=== SIGN TESTS ===")

def sign_test(content, name):
    print("  %s: %s" % (name, content))
    send_command("/setblock 10 64 0 air")
    send_command(make_sign_cmd(10, 64, 0, content))
    send_command("/tp @p 10 65.5 -3 0 30")
    time.sleep(0.8)
    wscreenshot("run/qa-screenshots/neoforge-matrix/%s.png" % name)

sign_tests_list = [
    ("#bold[Bold Sign]", "sign-bold"),
    (":item.diamond:", "sign-sprite"),
    ("#rainbow[Rainbow]", "sign-rainbow"),
    ("#size.2[Big]", "sign-size"),
    ("#bold[#color.red[Nest]]", "sign-nest"),
]

for content, name in sign_tests_list:
    sign_test(content, name)

print("  sign-glow")
send_command("/setblock 10 64 0 air")
send_command(make_sign_cmd(10, 64, 0, "#glow[Glow]"))
send_command("/time set midnight")
send_command("/effect give @p minecraft:night_vision 9999 1")
send_command("/tp @p 10 65.5 -3 0 30")
time.sleep(0.8)
wscreenshot("run/qa-screenshots/neoforge-matrix/sign-glow.png")
send_command("/time set day")
send_command("/effect clear @p")

# ============================================================
# ENTITY TESTS
# ============================================================
print("=== ENTITY TESTS ===")
send_command("/kill @e[type=!player]")
time.sleep(0.3)

entity_tests_list = [
    (":item.diamond: NeoX", "entity-sprite"),
    ("#rainbow[RainbowCow]", "entity-rainbow"),
    ("#size.2[BigCow]", "entity-size"),
    ("#underline[U] #strike[S]", "entity-decoration"),
]

for custom_name, name in entity_tests_list:
    print("  %s" % name)
    send_command('/summon minecraft:cow 0 66 4 {NoGravity:1b,CustomName:\'{"text":"%s"}\',CustomNameVisible:1b}' % custom_name)
    send_command("/tp @p 0 66 -1 0 0")
    time.sleep(0.8)
    wscreenshot("run/qa-screenshots/neoforge-matrix/%s.png" % name)
    send_command("/kill @e[type=!player]")
    time.sleep(0.3)

# ============================================================
# GUI TESTS
# ============================================================
print("=== GUI TESTS ===")

actionbar_list = [
    ("#bold[NeoBar]", "gui-actionbar-bold"),
    ("#color.gold[GoldBar]", "gui-actionbar-color"),
    ("#size.2[BigBar]", "gui-actionbar-size"),
    (":item.diamond: Bar", "gui-actionbar-sprite"),
]

for content, name in actionbar_list:
    print("  %s" % name)
    send_command('/title @s actionbar {"text":"%s"}' % content)
    time.sleep(0.8)
    wscreenshot("run/qa-screenshots/neoforge-matrix/%s.png" % name)

title_list = [
    ("#bold[NeoTitle]", "gui-title-bold"),
    ("#rainbow[RainbowTitle]", "gui-title-rainbow"),
    ("#size.2[BigTitle]", "gui-title-size"),
    (":item.diamond: Title", "gui-title-sprite"),
]

for content, name in title_list:
    print("  %s" % name)
    send_command('/title @s title {"text":"%s"}' % content)
    time.sleep(1.0)
    wscreenshot("run/qa-screenshots/neoforge-matrix/%s.png" % name)

# ============================================================
# HOTBAR TESTS
# ============================================================
print("=== HOTBAR TESTS ===")
send_command("/clear @p")
time.sleep(0.3)
send_command('/give @p minecraft:diamond{display:{Name:\'{"text":"#rainbow[Rainbow Gem]"}\'}}')
time.sleep(0.3)
wkey("2")
time.sleep(0.1)
wkey("1")
time.sleep(0.3)
wscreenshot("run/qa-screenshots/neoforge-matrix/hotbar-rainbow.png")
print("  hotbar-rainbow")

send_command("/clear @p")
time.sleep(0.3)
send_command('/give @p minecraft:emerald{display:{Name:\'{"text":"#size.2[BigItem]"}\'}}')
time.sleep(0.3)
wkey("2")
time.sleep(0.1)
wkey("1")
time.sleep(0.3)
wscreenshot("run/qa-screenshots/neoforge-matrix/hotbar-size.png")
print("  hotbar-size")

# ============================================================
# AUTOCOMPLETE TESTS
# ============================================================
print("=== AUTOCOMPLETE TESTS ===")

def autocomplete_test(input_text, name):
    print("  %s: %s" % (name, input_text))
    close_menu_and_capture()
    pyautogui.press('t')
    time.sleep(0.3)
    hwnd = _hwnd()
    ctypes.windll.user32.PostMessageW(hwnd, 0x0100, 0x08, 0)
    time.sleep(0.05)
    ctypes.windll.user32.PostMessageW(hwnd, 0x0101, 0x08, 0)
    time.sleep(0.1)
    wtype(input_text)
    time.sleep(0.8)
    wscreenshot("run/qa-screenshots/neoforge-matrix/%s.png" % name)
    wkey("escape")
    time.sleep(0.5)

autocomplete_test("#b", "auto-decorator")
autocomplete_test(":di", "auto-shortcode")
autocomplete_test("#color.", "auto-color-args")
autocomplete_test(":item.", "auto-item-args")

# ============================================================
# CHAT TESTS
# ============================================================
print("=== CHAT TESTS ===")

chat_tests_list = [
    ("#italic[Hello]", "chat-italic"),
    ("#color.red[Red]", "chat-color"),
    ("#glow[Glowing]", "chat-glow"),
    ("#underline[U] #strike[S]", "chat-decoration"),
    ("#bold[#color.red[Nested]]", "chat-nest"),
    (":block.stone:", "chat-block"),
    ("\\#bold literal", "chat-escape"),
]

for text, name in chat_tests_list:
    print("  %s" % name)
    close_menu_and_capture()
    pyautogui.press('t')
    time.sleep(0.3)
    hwnd = _hwnd()
    ctypes.windll.user32.PostMessageW(hwnd, 0x0100, 0x08, 0)
    time.sleep(0.05)
    ctypes.windll.user32.PostMessageW(hwnd, 0x0101, 0x08, 0)
    time.sleep(0.1)
    wtype(text)
    time.sleep(0.1)
    wkey("return")
    time.sleep(0.8)
    wscreenshot("run/qa-screenshots/neoforge-matrix/%s.png" % name)

print("=== ALL TESTS DONE ===")
