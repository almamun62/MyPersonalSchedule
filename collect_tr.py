import os
import re

strings = set()
for root, dirs, files in os.walk("app/src/main/java/com/example/ui"):
    for file in files:
        if file.endswith(".kt"):
            with open(os.path.join(root, file), 'r') as f:
                content = f.read()
                # matches "something".tr
                matches = re.findall(r'"([^"\\]*(?:\\.[^"\\]*)*)"\.tr', content)
                for match in matches:
                    strings.add(match)

with open('app/src/main/java/com/example/ui/theme/Translation.kt', 'r') as f:
    translation_content = f.read()

missing = []
for s in strings:
    if f'"{s}" to' not in translation_content:
        missing.append(s)

print("Missing translations:")
for m in missing:
    print(m)
