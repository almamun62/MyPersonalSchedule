import re

with open('gradle/libs.versions.toml', 'r') as f:
    toml = f.read()

if "workRuntimeKtx =" not in toml:
    toml = toml.replace('[versions]', '[versions]\nworkRuntimeKtx = "2.9.1"')
    toml = toml.replace('[libraries]', '[libraries]\nandroidx-work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "workRuntimeKtx" }')
    
    with open('gradle/libs.versions.toml', 'w') as f:
        f.write(toml)

with open('app/build.gradle.kts', 'r') as f:
    build = f.read()

if "work-runtime-ktx" not in build:
    build = build.replace('dependencies {', 'dependencies {\n    implementation(libs.androidx.work.runtime.ktx)')
    
    with open('app/build.gradle.kts', 'w') as f:
        f.write(build)
