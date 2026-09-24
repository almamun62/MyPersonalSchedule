with open('app/src/main/java/com/example/ui/components/BootstartOnboardingDialog.kt', 'r') as f:
    content = f.read()

lines = content.split('\n')
for i, line in enumerate(lines):
    if "OnboardingStepLanguageAndTheme" in line or "CompositionLocalProvider" in line:
        print(f"{i}: {line}")
