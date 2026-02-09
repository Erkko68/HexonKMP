import os, json

for f in os.listdir():
    if f.lower().endswith(".svg"):
        name = os.path.splitext(f)[0]
        svg = open(f, encoding="utf-8").read()
        data = {"id": name, "svg": svg, "color": "", "version": ""}
        json.dump(data, open(name + ".json", "w", encoding="utf-8"), ensure_ascii=False)
        print("✓", name + ".json")
