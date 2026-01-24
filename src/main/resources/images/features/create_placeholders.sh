#!/bin/bash
# Create simple placeholder images for PDFin features
# These are temporary placeholders - replace with proper illustrations from unDraw

# Check if ImageMagick is installed
if ! command -v convert &> /dev/null; then
    echo "ImageMagick not installed. Creating empty files as placeholders."
    echo "Please download proper images from https://undraw.co/"
    touch merge.png split.png compress.png protect.png
    exit 0
fi

# Create merge placeholder - files coming together
convert -size 80x80 xc:#eff1ff -fill #5b65ea -draw "rectangle 15,20 35,60" -draw "rectangle 45,15 65,55" -draw "translate 40,35 line 0,-10 0,10" -draw "translate 40,35 line 0,0 10,0" merge.png

# Create split placeholder - file being divided
convert -size 80x80 xc:#eff1ff -fill #5b65ea -draw "rectangle 20,15 40,65" -draw "rectangle 45,15 65,65" -draw "line 42,15 42,65" split.png

# Create compress placeholder - arrows pointing inward
convert -size 80x80 xc:#eff1ff -fill #5b65ea -draw "rectangle 25,25 55,55" -draw "polygon 15,40 25,35 25,45" -draw "polygon 65,40 55,35 55,45" compress.png

# Create protect placeholder - lock icon
convert -size 80x80 xc:#eff1ff -fill #5b65ea -draw "roundrectangle 25,35 55,65 5,5" -draw "arc 30,20 50,40 0,180" -stroke #5b65ea -strokewidth 3 -draw "line 32,20 32,35" -draw "line 48,20 48,35" protect.png

echo "Placeholder images created successfully!"
echo "Replace these with proper illustrations from https://undraw.co/ for better visuals."
