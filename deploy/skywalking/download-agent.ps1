$agentVersion = "9.5.0"
$agentDir = "apache-skywalking-java-agent-${agentVersion}"
$agentTar = "${agentDir}.tgz"
$downloadUrl = "https://archive.apache.org/dist/skywalking/java-agent/${agentVersion}/${agentTar}"

[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

if (Test-Path $agentDir) {
    Write-Host "SkyWalking Agent already exists: $agentDir"
    exit 0
}

Write-Host "Downloading SkyWalking Agent ${agentVersion}..."
Invoke-WebRequest -Uri $downloadUrl -OutFile $agentTar

Write-Host "Extracting..."
tar -xzf $agentTar

Write-Host "Cleaning up..."
Remove-Item $agentTar

$finalPath = Join-Path (Get-Location) $agentDir
Write-Host "Done! Agent path: $finalPath"