$agentVersion = "9.5.0"
$agentDir = "apache-skywalking-java-agent-${agentVersion}"
$agentZip = "${agentDir}.zip"
$downloadUrl = "https://archive.apache.org/dist/skywalking/${agentVersion}/${agentZip}"

if (Test-Path $agentDir) {
    Write-Host "SkyWalking Agent already exists: $agentDir"
    exit 0
}

Write-Host "Downloading SkyWalking Agent ${agentVersion}..."
Invoke-WebRequest -Uri $downloadUrl -OutFile $agentZip

Write-Host "Extracting..."
Expand-Archive -Path $agentZip -DestinationPath .

Write-Host "Cleaning up..."
Remove-Item $agentZip

Write-Host "Done! Agent path: $(Get-Location)/${agentDir}"