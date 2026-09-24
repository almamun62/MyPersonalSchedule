import re
with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

target = """                Box(modifier = Modifier.fillMaxSize()) {
                    MyScheduleApp(viewModel = viewModel, appLanguage = appLanguage ?: "en")
                    
                    if (!hasCompletedOnboarding) {
                        BootstartOnboardingDialog("""

replacement = """                Box(modifier = Modifier.fillMaxSize()) {
                    if (hasCompletedOnboarding) {
                        MyScheduleApp(viewModel = viewModel, appLanguage = appLanguage ?: "en")
                    } else {
                        BootstartOnboardingDialog("""

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
        f.write(content)
    print("Patched successfully")
else:
    print("Target not found")
