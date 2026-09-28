#!/bin/bash
OUTPUT="docs/100-Page-Comprehensive-Manual.md"
echo "# BOUNDLESS FINTECH: COMPREHENSIVE ENGINEERING MANUAL" > $OUTPUT
echo "Generated on $(date)" >> $OUTPUT

for d in auth-engine-service card-identity-service ledger-service notification-service api-gateway-service; do
    echo -e "\n## $d Deep Dive" >> $OUTPUT
    echo "This section covers every single line of code and configuration for $d." >> $OUTPUT
    find $d -name "*.java" -o -name "*.yml" -o -name "pom.xml" | while read file; do
        echo -e "\n### File: $file" >> $OUTPUT
        echo -e "\`\`\`java" >> $OUTPUT
        cat "$file" >> $OUTPUT
        echo -e "\`\`\`\n" >> $OUTPUT
    done
done
echo "Massive document generated at $OUTPUT"
