# Demo barcodes

Barcodes for the products seeded by the `demo` profile, for testing the scanner in the Android app.
Open `sheet.png` on another screen or print it, then use the scan button in Sell, Products or the product editor.
Each `<barcode>.svg` holds one product.

The demo codes are not valid EAN-13 numbers, so they are encoded as Code 128.
Regenerate with `python docs/demo-barcodes/generate.py` (needs `python-barcode` and Pillow).
