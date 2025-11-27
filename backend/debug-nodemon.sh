#!/bin/bash

echo "🔍 Debugging Nodemon Configuration"
echo "=================================="
echo ""

echo "1️⃣ Checking nodemon.json location:"
echo "---"
if [ -f "/opt/fi-thnity/backend/nodemon.json" ]; then
    echo "✅ Found: /opt/fi-thnity/backend/nodemon.json"
    echo "Contents:"
    cat /opt/fi-thnity/backend/nodemon.json
else
    echo "❌ NOT FOUND: /opt/fi-thnity/backend/nodemon.json"
fi
echo ""

echo "2️⃣ Checking PM2 process info:"
echo "---"
pm2 describe fi-thnity-backend | grep -A 5 "script path\|exec cwd\|script"
echo ""

echo "3️⃣ Checking PM2 working directory:"
echo "---"
pm2 jlist | jq '.[] | select(.name=="fi-thnity-backend") | {name, cwd: .pm2_env.pm_cwd, script: .pm2_env.pm_exec_path}'
echo ""

echo "4️⃣ Testing nodemon config manually:"
echo "---"
cd /opt/fi-thnity/backend
echo "Current directory: $(pwd)"
echo "Nodemon config exists here: $(test -f nodemon.json && echo 'YES' || echo 'NO')"
echo ""
echo "Running nodemon with debug to see what it's watching:"
echo "(This will show nodemon's configuration)"
npx nodemon --dump src/server.js 2>&1 | head -30
echo ""

echo "5️⃣ Recommended fix:"
echo "---"
echo "Option A: Update PM2 to use correct working directory"
echo "  pm2 delete fi-thnity-backend"
echo "  cd /opt/fi-thnity/backend"
echo "  pm2 start npm --name 'fi-thnity-backend' -- run dev"
echo ""
echo "Option B: Explicitly pass config to nodemon"
echo "  Change package.json dev script to:"
echo "  \"dev\": \"nodemon --config nodemon.json src/server.js\""
echo ""

echo "✅ Diagnostic complete"

