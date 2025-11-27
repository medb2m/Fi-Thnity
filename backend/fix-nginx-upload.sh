#!/bin/bash

# Fix nginx configuration for file uploads
# This script checks if nginx is installed and updates its configuration

echo "🔍 Checking nginx configuration..."

# Check if nginx is installed
if ! command -v nginx &> /dev/null; then
    echo "❌ nginx is not installed or not in PATH"
    echo "   The Express server might be running directly on port 9090"
    echo "   In that case, the issue might be with PM2 or Express configuration"
    exit 1
fi

echo "✅ nginx is installed"

# Find nginx configuration file
NGINX_CONF="/etc/nginx/nginx.conf"
SITES_AVAILABLE="/etc/nginx/sites-available"
SITES_ENABLED="/etc/nginx/sites-enabled"

# Check if configuration directories exist
if [ ! -d "$SITES_AVAILABLE" ]; then
    echo "⚠️  $SITES_AVAILABLE does not exist"
    echo "   nginx might be using a different configuration structure"
fi

# Look for Fi Thnity configuration
echo ""
echo "📋 Looking for Fi Thnity nginx configuration..."

# Search for configurations that proxy to port 9090
CONFIG_FILES=$(grep -l "9090" /etc/nginx/sites-available/* 2>/dev/null || echo "")

if [ -z "$CONFIG_FILES" ]; then
    echo "❌ No nginx configuration found for port 9090"
    echo ""
    echo "🔧 Creating nginx configuration..."
    
    # Create a new configuration
    cat > /tmp/fi-thnity-nginx.conf << 'EOF'
server {
    listen 9090;
    server_name _;

    # Increase body size limit for file uploads
    client_max_body_size 50M;
    client_body_buffer_size 50M;

    # Increase timeouts
    proxy_connect_timeout 300s;
    proxy_send_timeout 300s;
    proxy_read_timeout 300s;
    send_timeout 300s;

    # Disable buffering for large uploads
    proxy_request_buffering off;

    location / {
        proxy_pass http://localhost:3000;  # Assuming Express runs on 3000
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
    }
}
EOF

    echo "✅ Configuration created at /tmp/fi-thnity-nginx.conf"
    echo ""
    echo "📌 To apply this configuration, run:"
    echo "   sudo cp /tmp/fi-thnity-nginx.conf /etc/nginx/sites-available/fi-thnity"
    echo "   sudo ln -s /etc/nginx/sites-available/fi-thnity /etc/nginx/sites-enabled/"
    echo "   sudo nginx -t"
    echo "   sudo systemctl reload nginx"
else
    echo "✅ Found nginx configuration(s):"
    echo "$CONFIG_FILES"
    echo ""
    echo "🔧 Checking configuration for upload limits..."
    
    for config in $CONFIG_FILES; do
        echo ""
        echo "📄 $config:"
        echo "---"
        
        # Check for client_max_body_size
        if grep -q "client_max_body_size" "$config"; then
            echo "  ✅ client_max_body_size: $(grep client_max_body_size $config | head -1)"
        else
            echo "  ❌ client_max_body_size: NOT SET (default 1MB)"
            echo "     Add: client_max_body_size 50M;"
        fi
        
        # Check for proxy timeouts
        if grep -q "proxy_read_timeout" "$config"; then
            echo "  ✅ proxy_read_timeout: $(grep proxy_read_timeout $config | head -1)"
        else
            echo "  ❌ proxy_read_timeout: NOT SET (default 60s)"
            echo "     Add: proxy_read_timeout 300s;"
        fi
        
        echo "---"
    done
    
    echo ""
    echo "🔧 Creating fixed configuration..."
    
    # Backup and create fixed version
    for config in $CONFIG_FILES; do
        BACKUP="${config}.backup.$(date +%Y%m%d_%H%M%S)"
        echo "  📋 Backing up $config to $BACKUP"
        sudo cp "$config" "$BACKUP" 2>/dev/null || cp "$config" "$BACKUP"
        
        # Add missing directives
        cat > /tmp/nginx-fixes.txt << 'EOF'

    # File upload configuration
    client_max_body_size 50M;
    client_body_buffer_size 50M;
    
    # Timeout configuration
    proxy_connect_timeout 300s;
    proxy_send_timeout 300s;
    proxy_read_timeout 300s;
    send_timeout 300s;
    
    # Disable buffering for large uploads
    proxy_request_buffering off;
EOF
        
        echo "  ✅ Fixes prepared in /tmp/nginx-fixes.txt"
        echo ""
        echo "  📌 To apply fixes, edit $config and add the directives from /tmp/nginx-fixes.txt"
        echo "     inside the server block, then run:"
        echo "     sudo nginx -t"
        echo "     sudo systemctl reload nginx"
    done
fi

echo ""
echo "🔍 Checking if Express is running directly on port 9090..."
if netstat -tuln | grep -q ":9090.*LISTEN"; then
    echo "✅ Port 9090 is in use"
    
    # Check what process is using it
    PROCESS=$(sudo lsof -i :9090 -t 2>/dev/null || lsof -i :9090 -t 2>/dev/null)
    if [ -n "$PROCESS" ]; then
        PROC_NAME=$(ps -p $PROCESS -o comm= 2>/dev/null)
        echo "   Process: $PROC_NAME (PID: $PROCESS)"
        
        if [ "$PROC_NAME" = "node" ]; then
            echo "   ✅ Node.js is listening directly on port 9090"
            echo "   ❌ No nginx reverse proxy detected"
            echo ""
            echo "   The connection reset is likely due to:"
            echo "   1. Express body size limits (already fixed in code)"
            echo "   2. Network issues"
            echo "   3. PM2 configuration"
            echo ""
            echo "   Check PM2 logs:"
            echo "   pm2 logs fi-thnity-backend --lines 50"
        fi
    fi
else
    echo "❌ Port 9090 is not in use"
    echo "   Please start the Fi Thnity backend server"
fi

echo ""
echo "✅ Diagnostic complete"

