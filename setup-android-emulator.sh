#!/bin/bash
# Setup script for Android emulator to access host services via localhost
# This maps the emulator's localhost:8080 to the host's localhost:8080

echo "Setting up Android emulator port forwarding..."
adb reverse tcp:8080 tcp:8080

if [ $? -eq 0 ]; then
    echo "✓ Port 8080 forwarded successfully"
    echo "  Emulator can now access host's localhost:8080"
else
    echo "✗ Failed to setup port forwarding"
    echo "  Make sure:"
    echo "  1. Android emulator is running"
    echo "  2. adb is in your PATH"
    exit 1
fi

