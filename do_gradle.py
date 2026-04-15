import os
toml_path = r"C:\Users\guest_xnqhrf8\AndroidStudioProjects\ex01\gradle\libs.versions.toml"
with open(toml_path, "r", encoding="utf-8") as f:
    content = f.read()
content = content.replace('[versions]', '[versions]\nkotlinxSerialization = "1.6.3"')
content = content.replace('[libraries]', '[libraries]\nkotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }')
content = content.replace('[plugins]', '[plugins]\nkotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }')
with open(toml_path, "w", encoding="utf-8") as f:
    f.write(content)
gradle_path = r"C:\Users\guest_xnqhrf8\AndroidStudioProjects\ex01\app\build.gradle.kts"
with open(gradle_path, "r", encoding="utf-8") as f:
    gradle = f.read()
gradle = gradle.replace("alias(libs.plugins.google.ksp)", "alias(libs.plugins.google.ksp)\n    alias(libs.plugins.kotlin.serialization)")
gradle = gradle.replace("implementation(libs.androidx.core.ktx)", "implementation(libs.androidx.core.ktx)\n    implementation(libs.kotlinx.serialization.json)")
with open(gradle_path, "w", encoding="utf-8") as f:
    f.write(gradle)
print("Added serialization dependencies to gradle configuration!")
