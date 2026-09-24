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
} // End BootstartOnboardingDialog
"""

# Let's actually look at the end of the file.
