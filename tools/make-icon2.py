#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Carpet TIS Addition (NeoForge port) mod icon - pixel-art variants v2.

Style brief: Minecraft-native pixel art, flat palette, 32x32 logical canvas
integer-upscaled to 512. No UI card, no version banner, at most 3 elements.

Run:  python tools/make-icon2.py            -> build/icon-variants.png (contact sheet)
      python tools/make-icon2.py A out.png  -> one variant
"""
import os
import sys
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GRID = 32
SCALE = 16
SIZE = GRID * SCALE

VOID = (22, 25, 31)
CELL_A = (30, 35, 43)
CELL_B = (38, 44, 54)
BONE = (240, 243, 248)
STEEL = (146, 156, 170)
TEAL = (63, 190, 168)
TEAL_D = (32, 122, 110)
TEAL_L = (110, 214, 194)
AMBER = (238, 176, 63)
AMBER_D = (168, 116, 30)

F35 = {
    "T": ["###", ".#.", ".#.", ".#.", ".#."],
    "I": ["###", ".#.", ".#.", ".#.", "###"],
    "S": [".##", "#..", ".#.", "..#", "##."],
    "A": [".#.", "#.#", "###", "#.#", "#.#"],
}

F57 = {
    "T": ["#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#.."],
    "I": ["#####", "..#..", "..#..", "..#..", "..#..", "..#..", "#####"],
    "S": [".####", "#....", "#....", ".###.", "....#", "....#", "####."],
    "A": [".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"],
}


class Grid(dict):
    def __missing__(self, key):
        return VOID


def glyph_w(fnt):
    return len(next(iter(fnt.values()))[0])


def text_size(fnt, text, scale=1, gap=1):
    w = (len(text) * glyph_w(fnt) + (len(text) - 1) * gap) * scale
    h = len(next(iter(fnt.values()))) * scale
    return w, h


def blit_text(px, text, fnt, ox, oy, color, scale=1, gap=1):
    gw = glyph_w(fnt)
    for ci, ch in enumerate(text):
        g = fnt[ch]
        for y, row in enumerate(g):
            for x, c in enumerate(row):
                if c == "#":
                    bx = ox + (ci * (gw + gap) + x) * scale
                    by = oy + y * scale
                    for dy in range(scale):
                        for dx in range(scale):
                            px[bx + dx, by + dy] = color


def centre_text(px, text, fnt, y, color, scale=1, gap=1):
    w, _ = text_size(fnt, text, scale, gap)
    blit_text(px, text, fnt, (GRID - w) // 2, y, color, scale, gap)


def checker(px, cells=8):
    for y in range(GRID):
        for x in range(GRID):
            px[x, y] = CELL_A if ((x // cells) + (y // cells)) % 2 == 0 else CELL_B


def frame(px, color=(255, 255, 255, 24)):
    for i in range(GRID):
        px[i, 0] = color
        px[i, GRID - 1] = color
        px[0, i] = color
        px[GRID - 1, i] = color


def rect(px, x0, y0, w, h, color):
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            px[x, y] = color


# ---------------------------------------------------------------- variants
def variant_a(px):
    """Monogram: pixel TIS on a woollen checker + teal rule."""
    checker(px, cells=8)
    frame(px)
    centre_text(px, "TIS", F35, 9, BONE, scale=2)
    rect(px, 9, 23, 14, 2, TEAL)
    px[9, 23] = TEAL_D


def variant_b(px):
    """Carpet swatch: flat teal rug, woven stripes, amber fringe."""
    rect(px, 0, 0, GRID, GRID, (17, 20, 25))
    x0, y0, w, h = 2, 6, 28, 16
    rect(px, x0, y0, w, h, TEAL)
    rect(px, x0, y0, w, 2, TEAL_L)          # lit top edge
    rect(px, x0, y0 + h - 2, w, 2, TEAL_D)  # shaded bottom edge
    for y in range(y0 + 3, y0 + h - 3, 4):  # weave
        rect(px, x0 + 1, y, w - 2, 1, (52, 168, 149))
    centre_text(px, "TIS", F35, y0 + (h - 10) // 2, BONE, scale=2)
    for x in range(x0 + 1, x0 + w, 2):      # fringe
        px[x, y0 + h] = AMBER_D
        px[x, y0 + h + 1] = AMBER
    frame(px, (255, 255, 255, 18))


def variant_c(px):
    """Prompt: chevron + cursor (command / logger identity)."""
    rect(px, 0, 0, GRID, GRID, (16, 19, 24))
    cx, cy = 5, 6                                  # chevron, 3px stroke, 10 tall
    for i in range(9):
        for t in range(3):
            px[cx + i, cy + i + t] = BONE
            px[cx + i, cy + 15 - i + t] = BONE
    rect(px, 21, cy + 3, 6, 9, AMBER)              # cursor block
    rect(px, 22, cy + 4, 4, 7, AMBER_D)
    rect(px, 22, cy + 4, 3, 2, AMBER)
    centre_text(px, "TIS", F35, 24, STEEL, scale=1)
    frame(px, (255, 255, 255, 20))


def variant_d(px):
    """Upstream glyph restyle: heavy pixel C in bone + teal TIS underneath."""
    checker(px, cells=8)
    frame(px)
    x0, y0, w, h, t = 8, 2, 16, 15, 3
    rect(px, x0, y0, w, t, BONE)                   # top bar
    rect(px, x0, y0 + h - t, w, t, BONE)           # bottom bar
    rect(px, x0, y0, t, h, BONE)                   # spine
    rect(px, x0 + w - t, y0, t, 4, BONE)           # upper mouth
    rect(px, x0 + w - t, y0 + h - 4, t, 4, BONE)   # lower mouth
    for cx_, cy_ in ((x0, y0), (x0 + w - 1, y0), (x0, y0 + h - 1), (x0 + w - 1, y0 + h - 1)):
        px[cx_, cy_] = VOID                        # knock the hard corners off
    centre_text(px, "TIS", F57, 20, TEAL, scale=1)


VARIANTS = {"A": variant_a, "B": variant_b, "C": variant_c, "D": variant_d}


def render(key):
    px = Grid()
    VARIANTS[key](px)
    small = Image.new("RGB", (GRID, GRID))
    p = small.load()
    for y in range(GRID):
        for x in range(GRID):
            p[x, y] = px[x, y]
    return small.resize((SIZE, SIZE), Image.NEAREST)


def main():
    if len(sys.argv) >= 2 and sys.argv[1] in VARIANTS:
        key = sys.argv[1]
        out = sys.argv[2] if len(sys.argv) > 2 else os.path.join(ROOT, "build", "icon-%s.png" % key)
        img = render(key)
        os.makedirs(os.path.dirname(out), exist_ok=True)
        img.save(out, "PNG", optimize=True)
        print("wrote %s (%d bytes)" % (out, os.path.getsize(out)))
        return

    cellw, cellh = 300, 470
    sheet = Image.new("RGB", (4 * cellw + 30, cellh + 40), (44, 48, 55))
    d = ImageDraw.Draw(sheet)
    for i, key in enumerate("ABCD"):
        img = render(key)
        x = 30 + i * cellw
        d.text((x, 12), "variant %s" % key, fill=(210, 216, 224))
        sheet.paste(img.resize((256, 256), Image.LANCZOS), (x, 32))
        for size, sx in ((128, x), (64, x + 140), (32, x + 220)):
            sheet.paste(img.resize((size, size), Image.LANCZOS), (sx, 300))
            d.text((sx, 300 + size + 6), "%d" % size, fill=(150, 158, 170))
    out = os.path.join(ROOT, "build", "icon-variants.png")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    sheet.save(out, "PNG")
    print("wrote %s (%d bytes)" % (out, os.path.getsize(out)))


if __name__ == "__main__":
    main()
