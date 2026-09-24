import re

with open('app/src/main/java/com/example/data/local/AppDatabase.kt', 'r') as f:
    content = f.read()

content = content.replace("version = 2,", "version = 3,")
content = content.replace("fallbackToDestructiveMigration()", "fallbackToDestructiveMigration(dropAllTables = true)")

with open('app/src/main/java/com/example/data/local/AppDatabase.kt', 'w') as f:
    f.write(content)
