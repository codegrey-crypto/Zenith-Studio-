import sys

def find_closing_brace(filename, start_line):
    with open(filename, 'r') as f:
        lines = f.readlines()
    
    start_idx = start_line - 1
    depth = 0
    found_start = False
    for i in range(start_idx, len(lines)):
        line = lines[i]
        for char in line:
            if char == '{':
                depth += 1
                found_start = True
            elif char == '}':
                depth -= 1
        
        if found_start and depth == 0:
            print(f"Closing brace found at line {i + 1}")
            return
    print("Closing brace not found")

find_closing_brace('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 9689)
find_closing_brace('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 9822)
