import re

with open('app/src/main/java/com/example/ui/components/BootstartOnboardingDialog.kt', 'r') as f:
    content = f.read()

target = """                )
            }
        }
    }
}"""
replacement = """                )
            }
        }
    }
    } // End CompositionLocalProvider
} // End BootstartOnboardingDialog"""

# We need to make sure we replace the correct closing brace for BootstartOnboardingDialog.
# Let's find where BootstartOnboardingDialog ends.
