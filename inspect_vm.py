import re

with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'r') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if "fun " in line and "ourse" in line:
        print(f"Line {i+1}: {line.strip()}")

