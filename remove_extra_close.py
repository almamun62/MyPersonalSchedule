import re

with open('app/src/main/java/com/example/ui/components/BootstartOnboardingDialog.kt', 'r') as f:
    content = f.read()

target_end = """        )
    }
    } // End CompositionLocalProvider
}"""

replacement_end = """        )
    }
}"""

content = content.replace(target_end, replacement_end)

with open('app/src/main/java/com/example/ui/components/BootstartOnboardingDialog.kt', 'w') as f:
    f.write(content)
print("Removed extra braces at end of file")
