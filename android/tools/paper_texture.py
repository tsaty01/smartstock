"""Generate the Android bill-paper tile at app/src/main/res/drawable-nodpi/paper.webp."""
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

SIZE = 512
BASE = np.array([245, 237, 218], dtype=np.float64)  # cream, #F5EDDA
SEED = 553
ANDROID = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUTPUT = os.path.join(ANDROID, "app", "src", "main", "res", "drawable-nodpi", "paper.webp")

rng = np.random.default_rng(SEED)


def band_noise(low, high):
    """Periodic noise keeping spatial frequencies between low and high (cycles per tile), with unit standard deviation."""
    spectrum = np.fft.fft2(rng.standard_normal((SIZE, SIZE)))
    fy = np.fft.fftfreq(SIZE)[:, None] * SIZE
    fx = np.fft.fftfreq(SIZE)[None, :] * SIZE
    radius = np.hypot(fx, fy)
    mask = np.exp(-((radius / high) ** 2)) * (1 - np.exp(-((radius / max(low, 1e-6)) ** 2)))
    field = np.real(np.fft.ifft2(spectrum * mask))
    return field / field.std()


# Luminance variation: soft mottling, paper tooth and fine grain.
shade = 0.004 * band_noise(3, 10) + 0.007 * band_noise(10, 40) + 0.012 * band_noise(80, 400)

# Fibres: short faint strokes, drawn three times over so they wrap across tile edges.
fibres = Image.new("L", (SIZE * 3, SIZE * 3), 128)
draw = ImageDraw.Draw(fibres)
for _ in range(420):
    x, y = rng.uniform(0, SIZE, 2)
    angle = rng.uniform(0, np.pi)
    length = rng.uniform(6, 34)
    bend = rng.normal(0, 0.25)
    tone = 128 + int(rng.choice([-1, 1]) * rng.uniform(40, 80))
    points = [
        (x + t * length * np.cos(angle + bend * t), y + t * length * np.sin(angle + bend * t))
        for t in np.linspace(0, 1, 8)
    ]
    for ox in (0, SIZE, 2 * SIZE):
        for oy in (0, SIZE, 2 * SIZE):
            draw.line([(px + ox, py + oy) for px, py in points], fill=tone, width=1)
fibres = fibres.filter(ImageFilter.GaussianBlur(0.6)).crop((SIZE, SIZE, 2 * SIZE, 2 * SIZE))
shade += (np.asarray(fibres, dtype=np.float64) - 128) / 128 * 0.07

# Darker specks shift slightly warm, as on real paper.
rgb = BASE * (1 + shade[..., None]) + np.array([2.0, 1.0, -2.0]) * shade[..., None] * 20
image = Image.fromarray(np.clip(rgb, 0, 255).astype(np.uint8), "RGB")

os.makedirs(os.path.dirname(OUTPUT), exist_ok=True)
image.save(OUTPUT, "WEBP", lossless=True, method=6)
print(OUTPUT, os.path.getsize(OUTPUT), "bytes")
