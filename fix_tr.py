import re

def fix_screen(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    # Replace com.example.ui.theme.tr("XYZ") with "XYZ".tr
    content = re.sub(r'com\.example\.ui\.theme\.tr\("(.*?)"\)', r'"\1".tr', content)

    with open(file_path, 'w') as f:
        f.write(content)

fix_screen('app/src/main/java/com/example/ui/screens/DashboardScreen.kt')
fix_screen('app/src/main/java/com/example/ui/screens/SettingsScreen.kt')
