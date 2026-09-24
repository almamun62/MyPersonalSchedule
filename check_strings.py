import os
import re

for root, dirs, files in os.walk("app/src/main/java/com/example/ui"):
    for file in files:
        if file.endswith(".kt"):
            with open(os.path.join(root, file), 'r') as f:
                content = f.read()
                matches = re.findall(r'Text\(\s*"([^"]+)"(?!\.tr)', content)
                for match in matches:
                    print(f"{file}: {match}")
