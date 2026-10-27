#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Generate the Carpet TIS Addition (NeoForge port) mod icon.

Design goals
------------
* keeps TIS identity (name legible down to ~32 px, checker motif from the
  upstream icon as a low-contrast texture)
* marks the port: a NeoForge-orange banner with the loader + MC version
* square 512x512 RGBA, no baked-in background outside the rounded card

Run:  python tools/make-icon.py [out.png]
"""
import os
import sys
from PIL import Image, ImageDraw, ImageFont, ImageFilter

W = H = 512
SS = 4                      # supersampling factor for smooth edges
CW, CH = W * SS, H * SS

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEFAULT_OUT = os.path.join(
    ROOT, "src", "main", "resources", "assets", "carpettisaddition", "icon.png"
)

FONT_CANDIDATES_BOLD = [
    r"C:\Windows\Fonts\segoeuib.ttf",
    r"C:\Windows\Fonts\arialbd.ttf",
    r"C:\Windows\Fonts\bahnschrift.ttf",
]
FONT_CANDIDATES_SEMI = [
    r"C:\Windows\Fonts\segoeuisb.ttf",
    r"C:\Windows\Fonts\segoeuib.ttf",
    r"C:\Windows\Fonts\arialbd.ttf",
]

INK_TOP = (18, 20, 24)          # card gradient, top
INK_BOT = (35, 41, 51)          # card gradient, bottom
WHITE = (243, 246, 250)
GREY = (154, 164, 178)
ORANGE_A = (240, 130, 28)
ORANGE_B = (255, 178, 33)
ORANGE_TEXT = (24, 21, 17)
SHADOW = (0, 0, 0, 110)


def font(paths, size):
    for p in paths:
        if os.path.exists(p):
            return ImageFont.truetype(p, size)
    raise SystemExit("no usable font found: %r" % (paths,))


def rounded_mask(size, radius):
    m = Image.new("L", size, 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, size[0] - 1, size[1] - 1], radius=radius, fill=255)
    return m


def vgrad(size, c0, c1):
    g = Image.new("RGB", (1, size[1]))
    d = ImageDraw.Draw(g)
    for y in range(size[1]):
        t = y / max(1, size[1] - 1)
        d.point((0, y), tuple(round(c0[i] + (c1[i] - c0[i]) * t) for i in range(3)))
    return g.resize(size, Image.BILINEAR)


def tracked_text(draw, xy, text, fnt, fill, tracking=0.0):
    """Draw text centred at xy with extra letter spacing (in px, supersampled)."""
    widths = [draw.textlength(ch, font=fnt) for ch in text]
    total = sum(widths) + tracking * (len(text) - 1)
    x = xy[0] - total / 2
    for ch, w in zip(text, widths):
        draw.text((x, xy[1]), ch, font=fnt, fill=fill, anchor="lm")
        x += w + tracking
    return total


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_OUT
    img = Image.new("RGBA", (CW, CH), (0, 0, 0, 0))

    # ---- card background -------------------------------------------------
    card = vgrad((CW, CH), INK_TOP, INK_BOT).convert("RGBA")
    m = rounded_mask((CW, CH), 88 * SS)

    # subtle checker motif, borrowed from the upstream icon
    tex = Image.new("RGBA", (CW, CH), (0, 0, 0, 0))
    td = ImageDraw.Draw(tex)
    cell = 32 * SS
    for row in range(0, CH, cell):
        for col in range(0, CW, cell):
            if ((row // cell) + (col // cell)) % 2 == 0:
                td.rectangle([col, row, col + cell - 1, row + cell - 1], fill=(255, 255, 255, 9))
    card.alpha_composite(tex)

    # soft orange glow behind the hero text
    glow = Image.new("RGBA", (CW, CH), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse(
        [CW * 0.10, CH * 0.02, CW * 0.90, CH * 0.78], fill=(240, 130, 28, 46)
    )
    glow = glow.filter(ImageFilter.GaussianBlur(60 * SS))
    card.alpha_composite(glow)

    img.paste(card, (0, 0), m)

    d = ImageDraw.Draw(img)

    # ---- hero: TIS -------------------------------------------------------
    f_tis = font(FONT_CANDIDATES_BOLD, 196 * SS)
    # drop shadow so the glyphs stay readable on any mod-list background
    shadow = Image.new("RGBA", (CW, CH), (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    tracked_text(sd, (CW / 2, 168 * SS + 6 * SS), "TIS", f_tis, SHADOW, tracking=-4 * SS)
    shadow = shadow.filter(ImageFilter.GaussianBlur(9 * SS))
    img.alpha_composite(shadow)
    tracked_text(d, (CW / 2, 168 * SS), "TIS", f_tis, WHITE, tracking=-4 * SS)

    # thin divider
    d.rounded_rectangle(
        [CW * 0.28, 286 * SS, CW * 0.72, 286 * SS + 3 * SS], radius=2 * SS, fill=(255, 255, 255, 46)
    )

    # ---- subtitle: Addition ---------------------------------------------
    f_add = font(FONT_CANDIDATES_SEMI, 74 * SS)
    tracked_text(d, (CW / 2, 330 * SS), "Addition", f_add, GREY, tracking=3 * SS)

    # ---- NeoForge banner -------------------------------------------------
    bx0, bx1 = 56 * SS, CW - 56 * SS
    by0, by1 = 396 * SS, 466 * SS
    banner = vgrad((bx1 - bx0, by1 - by0), ORANGE_A, ORANGE_B).convert("RGBA")
    bmask = rounded_mask((bx1 - bx0, by1 - by0), 20 * SS)
    img.paste(banner, (bx0, by0), bmask)

    f_tag = font(FONT_CANDIDATES_BOLD, 46 * SS)
    tracked_text(
        d, (CW / 2, (by0 + by1) / 2 - 1 * SS), "NeoForge 1.21.1", f_tag, ORANGE_TEXT, tracking=1 * SS
    )

    # ---- downscale -------------------------------------------------------
    icon = img.resize((W, H), Image.LANCZOS)
    os.makedirs(os.path.dirname(out), exist_ok=True)
    icon.save(out, "PNG", optimize=True)
    print("wrote %s (%d bytes)" % (out, os.path.getsize(out)))

    # side-by-side legibility preview: 512 / 128 / 64 / 32 px on a MC-ish plate
    plate = Image.new("RGBA", (760, 560), (48, 52, 58, 255))
    pd = ImageDraw.Draw(plate)
    for gx in range(0, 760, 40):
        pd.line([(gx, 0), (gx, 560)], fill=(56, 60, 67, 255))
    for gy in range(0, 560, 40):
        pd.line([(0, gy), (760, gy)], fill=(56, 60, 67, 255))
    plate.alpha_composite(icon, (24, 24))
    small = [(128, 536, 24), (64, 536, 176), (32, 536, 264)]
    for size, x, y in small:
        s = icon.resize((size, size), Image.LANCZOS)
        plate.alpha_composite(s, (x, y))
        pd.text((x, y + size + 8), "%dpx" % size, fill=(190, 196, 205))
    prev = os.path.join(ROOT, "build", "icon-preview.png")
    os.makedirs(os.path.dirname(prev), exist_ok=True)
    plate.save(prev, "PNG")
    print("wrote %s (%d bytes)" % (prev, os.path.getsize(prev)))


if __name__ == "__main__":
    main()
