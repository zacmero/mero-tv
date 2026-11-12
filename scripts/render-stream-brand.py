"""Generate bitmap launcher resources for Fire TV launchers (Pillow required)."""
from pathlib import Path
from PIL import Image, ImageDraw
root=Path(__file__).resolve().parents[1]/"stream/src/main/res/drawable-nodpi"
root.mkdir(parents=True,exist_ok=True)
im=Image.new("RGB",(432,432),"#0A1420")
d=ImageDraw.Draw(im)
d.rectangle((48,48,384,384),fill="#122A40")
for points in [[(108,60),(60,60),(60,108)],[(324,60),(372,60),(372,108)],[(60,324),(60,372),(108,372)],[(324,372),(372,372),(372,324)]]:
 d.line(points,fill="#459CDE",width=8)
d.polygon([(120,120),(236,204),(120,288),(120,240),(172,204),(120,168)],fill="#77BEFF")
d.rectangle((232,268,324,296),fill="#71F7BC")
im.save(root/"launcher_icon.png")
banner=Image.new("RGB",(1280,720),"#0A1420")
banner.paste(im,(424,144))
ImageDraw.Draw(banner).rectangle((2,2,1277,717),outline="#459CDE",width=6)
banner.resize((320,180),Image.Resampling.LANCZOS).save(root/"launcher_banner.png")
