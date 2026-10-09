from pathlib import Path
p = Path(".bak-tmp/fix5a.py")
p.write_text("print('x')\n", encoding="utf-8")
print("probe ok")
