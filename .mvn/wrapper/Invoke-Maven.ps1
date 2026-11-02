param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $MavenArguments
)
$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$ProjectDirectory = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$PropertiesFile = Join-Path $PSScriptRoot 'maven-wrapper.properties'
$UrlLine = Get-Content -LiteralPath $PropertiesFile | Where-Object { $_ -match '^distributionUrl=' }
if (-not $UrlLine) { throw 'distributionUrl is missing.' }
$Url = $UrlLine.Substring('distributionUrl='.Length).Trim()
$ArchiveName = [IO.Path]::GetFileName(([Uri] $Url).AbsolutePath)
$DirectoryName = $ArchiveName -replace '-bin\.zip$', ''
$MavenHome = if ($env:MAVEN_USER_HOME) { $env:MAVEN_USER_HOME } else { Join-Path $HOME '.m2' }
$Cache = Join-Path (Join-Path $MavenHome 'wrapper\dists') $DirectoryName
$MavenCommand = Join-Path (Join-Path $Cache $DirectoryName) 'bin\mvn.cmd'
if (-not (Test-Path -LiteralPath $MavenCommand)) {
    New-Item -ItemType Directory -Path $Cache -Force | Out-Null
    $Archive = Join-Path $Cache $ArchiveName
    $Checksum = "$Archive.sha512"
    Write-Host "Downloading $DirectoryName (first run only)..."
    try {
        Invoke-WebRequest -UseBasicParsing -Uri $Url -OutFile $Archive
        Invoke-WebRequest -UseBasicParsing -Uri "$Url.sha512" -OutFile $Checksum
        $Expected = ((Get-Content -LiteralPath $Checksum -Raw).Trim() -split '\s+')[0]
        $Actual = (Get-FileHash -LiteralPath $Archive -Algorithm SHA512).Hash
        if ($Expected -notmatch '^[0-9a-fA-F]{128}$' -or $Actual -ine $Expected) {
            throw 'Maven archive SHA-512 verification failed.'
        }
        Expand-Archive -LiteralPath $Archive -DestinationPath $Cache -Force
    }
    finally {
        Remove-Item -LiteralPath $Archive, $Checksum -Force -ErrorAction SilentlyContinue
    }
}
if (-not (Test-Path -LiteralPath $MavenCommand)) { throw 'Maven could not be installed.' }
Push-Location $ProjectDirectory
try {
    & $MavenCommand @MavenArguments
    $ResultCode = $LASTEXITCODE
}
finally { Pop-Location }
exit $ResultCode
