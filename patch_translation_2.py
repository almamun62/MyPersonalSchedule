import re

with open('app/src/main/java/com/example/ui/theme/Translation.kt', 'r') as f:
    content = f.read()

additions = [
    ('"Beginner\'s guide & First time trail"', '"新手指南与首次体验"'),
    ('"100% Offline-First Architecture"', '"100% 离线优先架构"'),
    ('"AI Integration (BYOK)"', '"AI 集成 (BYOK)"'),
    ('"Day Mode"', '"日间模式"'),
    ('"Night Mode"', '"夜间模式"'),
    ('"System Default"', '"系统默认"'),
    ('"Base URL"', '"接口地址 (Base URL)"'),
    ('"API Key"', '"API 密钥"'),
    ('"Zero network calls, analytics, or remote tracking. All course schedules, exams, and tasks reside strictly in your device\'s encrypted Room SQLite database."', '"零网络请求、零分析和零远程追踪。所有课表、考试和任务都严格保存在您设备的加密本地数据库中。"'),
]

new_lines = []
for k, v in additions:
    if k not in content:
        new_lines.append(f"    {k} to {v},")

if new_lines:
    # insert before the last parenthesis or at the end of the map
    # The map ends with: "中文" to "中文"\n)
    
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
        print("Added new translations")
    else:
        print("Could not parse map end")
else:
    print("All additions already present")
