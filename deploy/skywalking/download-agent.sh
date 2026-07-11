#!/bin/bash
agentVersion="9.5.0"
agentDir="apache-skywalking-java-agent-${agentVersion}"
agentZip="${agentDir}.zip"
downloadUrl="https://archive.apache.org/dist/skywalking/${agentVersion}/${agentZip}"

if [ -d "$agentDir" ]; then
    echo "SkyWalking Agent already exists: $agentDir"
    exit 0
fi

echo "Downloading SkyWalking Agent ${agentVersion}..."
curl -L -O "$downloadUrl"

echo "Extracting..."
unzip "$agentZip"

echo "Cleaning up..."
rm "$agentZip"

echo "Done! Agent path: $(pwd)/${agentDir}"