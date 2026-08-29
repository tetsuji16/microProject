import sys, time
import ctypes
from ctypes import wintypes

user32 = ctypes.windll.user32
EnumWindowsProc = ctypes.WINFUNCTYPE(wintypes.BOOL, wintypes.HWND, wintypes.LPARAM)

def find_all(substr):
    found = []
    def cb(hwnd, _):
        n = user32.GetWindowTextLengthW(hwnd)
        if n == 0:
            return True
        b = ctypes.create_unicode_buffer(n + 1)
        user32.GetWindowTextW(hwnd, b, n + 1)
        if substr.lower() in b.value.lower():
            found.append((hwnd, b.value))
        return True
    user32.EnumWindows(EnumWindowsProc(cb), 0)
    return found

def main():
    sub = sys.argv[1] if len(sys.argv) > 1 else "microProject"
    wins = find_all(sub)
    if not wins:
        print("no window")
        return
    VK_ESCAPE = 0x1B
    for hwnd, title in wins:
        user32.ShowWindow(hwnd, 9)
        user32.SetForegroundWindow(hwnd)
        time.sleep(0.2)
        user32.keybd_event(VK_ESCAPE, 0, 0, 0)
        time.sleep(0.05)
        user32.keybd_event(VK_ESCAPE, 0, 2, 0)
        print("sent ESC to", hwnd, title)

main()
