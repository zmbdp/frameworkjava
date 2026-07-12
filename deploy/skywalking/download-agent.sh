#!/bin/bash
agentVersion="9.5.0"
agentDir="apache-skywalking-java-agent-${agentVersion}"
agentTar="${agentDir}.tgz"
downloadUrl="https://archive.apache.org/dist/skywalking/java-agent/${agentVersion}/${agentTar}"

if [ -d "$agentDir" ]; then
    echo "SkyWalking Agent already exists: $agentDir"
    exit 0
fi

echo "Downloading SkyWalking Agent ${agentVersion}..."
curl -L -O "$downloadUrl"

echo "Extracting..."
tar -xzf "$agentTar"

echo "Cleaning up..."
rm "$agentTar"

echo "Done! Agent path: $(pwd)/${agentDir}"