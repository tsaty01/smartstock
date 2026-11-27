"""Regenerates the demo product barcodes: python docs/demo-barcodes/generate.py (needs python-barcode, Pillow)."""
from pathlib import Path

from barcode import Code128
from barcode.writer import ImageWriter, SVGWriter
from PIL import Image, ImageDraw, ImageFont

HERE = Path(__file__).parent
# Matches the seeded products in server/src/main/resources/db/demo/R__demo_data.sql.
PRODUCTS = {
    '8900000000011': 'Basmati Rice 5 kg',
    '8900000000028': 'Whole Wheat Atta 5 kg',
    '8900000000035': 'Toor Dal 1 kg',
    '8900000000042': 'Masala Tea 250 g',
    '8900000000059': 'Orange Juice 1 L',
    '8900000000066': 'Mineral Water 1 L',
    '8900000000073': 'Paracetamol 500 mg (10)',
    '8900000000080': 'Cough Syrup 100 ml (expired)',
    '8900000000097': 'Herbal Shampoo 340 ml',
    '8900000000103': 'Toothpaste 150 g',
    '8900000000110': 'Salted Chips 90 g',
    '8900000000127': 'Peanut Cookies 200 g',
    '8900000000134': 'Toned Milk 1 L',
    '8900000000141': 'Paneer 200 g',
    '8900000000158': 'Rolled Oats 1 kg',
    '8900000000165': 'Cold Brew Coffee 250 ml',
    '8900000000172': 'Dark Chocolate 100 g',
    '8900000000189': 'Hand Wash 500 ml',
}
# The demo codes are not valid EAN-13 numbers, so they are encoded as Code 128.
OPTIONS = {'module_height': 12, 'quiet_zone': 4, 'font_size': 8, 'text_distance': 4}

for code in PRODUCTS:
    Code128(code, writer=SVGWriter()).save(str(HERE / code), OPTIONS)

columns, cell_w, cell_h = 2, 620, 300
rows = (len(PRODUCTS) + columns - 1) // columns
sheet = Image.new('RGB', (columns * cell_w, rows * cell_h + 60), 'white')
draw = ImageDraw.Draw(sheet)
font = ImageFont.load_default(size=22)
draw.text((20, 18), 'SmartStock demo barcodes (Code 128)', fill='black', font=font)
for i, (code, name) in enumerate(PRODUCTS.items()):
    image = Code128(code, writer=ImageWriter()).render({**OPTIONS, 'dpi': 200, 'font_size': 12})
    image.thumbnail((cell_w - 60, cell_h - 70))
    x, y = (i % columns) * cell_w + 30, (i // columns) * cell_h + 70
    draw.text((x, y), name, fill='black', font=font)
    sheet.paste(image, (x, y + 34))
sheet.save(HERE / 'sheet.png', optimize=True)
