import sys

def fix_file(filename):
    with open(filename, 'r') as f:
        lines = f.readlines()
    
    pkg_line = -1
    for i, line in enumerate(lines):
        if line.startswith("package "):
            pkg_line = i
            break
            
    if pkg_line > 0:
        # Move the package line to the top
        pkg_str = lines.pop(pkg_line)
        lines.insert(0, pkg_str)
        
        with open(filename, 'w') as f:
            f.writelines(lines)
        print(f"Fixed {filename}")
    else:
        print(f"No package line found or already at top for {filename}")

fix_file("app/src/main/java/com/aistudio/zenithstudio/rpxwtq/EffectPresetLibraryUI.kt")
