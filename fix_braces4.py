with open('app/src/main/java/com/example/ui/components/BootstartOnboardingDialog.kt', 'r') as f:
    content = f.read()

lines = content.split('\n')
new_lines = []
for i, line in enumerate(lines):
    if i == 315: # Line 316 is     } // End CompositionLocalProvider
        continue
    new_lines.append(line)

with open('app/src/main/java/com/example/ui/components/BootstartOnboardingDialog.kt', 'w') as f:
    f.write('\n'.join(new_lines))
