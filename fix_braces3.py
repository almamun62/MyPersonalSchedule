with open('app/src/main/java/com/example/ui/components/BootstartOnboardingDialog.kt', 'r') as f:
    content = f.read()

lines = content.split('\n')
new_lines = []
for i, line in enumerate(lines):
    if i == 312 or i == 316 or i == 317:
        continue
    if i == 313:
        new_lines.append("    } // End CompositionLocalProvider")
        new_lines.append("} // End BootstartOnboardingDialog")
        continue
    
    new_lines.append(line)

with open('app/src/main/java/com/example/ui/components/BootstartOnboardingDialog.kt', 'w') as f:
    f.write('\n'.join(new_lines))
