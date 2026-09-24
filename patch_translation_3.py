import re

with open('app/src/main/java/com/example/ui/theme/Translation.kt', 'r') as f:
    content = f.read()

additions = [
    ('"Fall 2026 Term"', '"2026秋季学期"'),
    ('"mins"', '"分钟"'),
    ('"Term Name: "', '"学期名称: "'),
    ('"Semester Start Date: "', '"学期开始日期: "')
]

new_lines = []
for k, v in additions:
    if k not in content:
        new_lines.append(f"    {k} to {v},")

if new_lines:
    parts = content.rsplit(')', 1)
    if len(parts) == 2:
        new_content = parts[0]
        if not new_content.endswith(','):
            if new_content.endswith('\n'):
                new_content = new_content[:-1] + ',\n'
            else:
                new_content += ','
        new_content += '\n' + '\n'.join(new_lines) + '\n)' + parts[1]
        with open('app/src/main/java/com/example/ui/theme/Translation.kt', 'w') as f:
            f.write(new_content)
        print("Added more translations")
