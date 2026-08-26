with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt") as f:
    lines = f.readlines()

count = 0
for i, line in enumerate(lines):
    if i < 27111: continue
    
    # Simple heuristic to avoid counting inside strings
    line_clean = line.split('"')[0]
    
    count += line_clean.count('{')
    count -= line_clean.count('}')
    
    if count == 0 and i >= 27111:
        print(f"Block ends at {i+1}")
        break
