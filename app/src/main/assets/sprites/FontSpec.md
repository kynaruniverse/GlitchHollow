# Glitch Hollow — Bitmap Font Spec
## File: assets/sprites/font.png
## Size: 256×96px, PNG-32, transparent background

### Grid
- 16 columns × 6 rows
- Each cell: 16×16px
- Characters: ASCII 32 (space) to 126 (~)
- Row-major, left-to-right, top-to-bottom

### Character map
Row 0 (y=0):   [space] ! " # $ % & ' ( ) * + , - . /
Row 1 (y=16):  0 1 2 3 4 5 6 7 8 9 : ; < = > ?
Row 2 (y=32):  @ A B C D E F G H I J K L M N O
Row 3 (y=48):  P Q R S T U V W X Y Z [ \ ] ^ _
Row 4 (y=64):  ` a b c d e f g h i j k l m n o
Row 5 (y=80):  p q r s t u v w x y z { | } ~

### Style guidelines
- Pixel size: 7×9px glyph within a 16×16 cell (centred, 4px left margin)
- Weight: 1px stroke, no anti-aliasing
- Colour: white (#FFFFFF) on transparent — tint via SpriteBatch vertex colour
- Style: condensed monospace, slight CRT feel (thin horizontal strokes)
- Special chars to prioritise: 0-9, A-Z, : / . ! % (used most in HUD)

### Quick production method
1. Use a pixel font tool (Pixilart, Aseprite, or LibreSprite)
2. Import any free pixel font (e.g. "Press Start 2P", "04b03", "Silkscreen")
3. Render each char into a 16×16 cell at the positions above
4. Export as 256×96 PNG-32 with transparency
5. Place at: app/src/main/assets/sprites/font.png

### Alternative (no art tools)
Generate programmatically — ask Claude to write a Python/Pillow script
that renders a system monospace font into the grid layout above.