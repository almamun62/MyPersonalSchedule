with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'r') as f:
    lines = f.readlines()

depth = 0
for i, line in enumerate(lines):
    depth += line.count('{')
    depth -= line.count('}')
    if depth < 0:
        print(f"Negative depth at line {i+1}: {line.strip()}")
        break
    if depth == 0 and i > 60:
        print(f"Class ended prematurely at line {i+1}: {line.strip()}")
        break
print(f"Final depth: {depth}")
