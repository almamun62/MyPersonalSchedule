with open('app/src/main/java/com/example/ui/components/BootstartOnboardingDialog.kt', 'r') as f:
    content = f.read()

lines = content.split('\n')
for i, line in enumerate(lines):
    if i > 305 and i < 325:
        print(f"{i+1}: {line}")
