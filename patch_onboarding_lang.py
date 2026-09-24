import re

with open('app/src/main/java/com/example/ui/components/BootstartOnboardingDialog.kt', 'r') as f:
    content = f.read()

target = """    var currentStep by remember { mutableIntStateOf(1) }

    // Step 1 State: Language, Theme & Accent
    var selectedAppLanguage by remember { mutableStateOf(initialAppLanguage) }"""

replacement = """    var currentStep by remember { mutableIntStateOf(1) }

    // Step 1 State: Language, Theme & Accent
    var selectedAppLanguage by remember { mutableStateOf(initialAppLanguage) }
    
    // Crucial: Provide the selected language to the rest of the dialog
    // so that when they tap "Chinese", the UI strings immediately update
    androidx.compose.runtime.CompositionLocalProvider(
        com.example.ui.theme.LocalAppLanguage provides selectedAppLanguage
    ) {"""


target_end = """        )
    }
}"""

replacement_end = """        )
    }
    } // End CompositionLocalProvider
}"""


content = content.replace(target, replacement)
content = content.replace(target_end, replacement_end)


with open('app/src/main/java/com/example/ui/components/BootstartOnboardingDialog.kt', 'w') as f:
    f.write(content)
print("Patched BootstartOnboardingDialog for live language switching")

