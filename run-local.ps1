param(
    [ValidateSet('local', 'prod')]
    [string]$Profile = 'local'
)

$ErrorActionPreference = 'Stop'

# ---------------------------------------------------------------------------
# Carga un archivo .env (KEY=VALUE) a las variables de entorno de la sesion.
# ---------------------------------------------------------------------------
function Load-DotEnv {
    param([string]$Path)

    if (-not (Test-Path $Path)) {
        return
    }

    Get-Content $Path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith('#')) { return }

        $parts = $line.Split('=', 2)
        if ($parts.Count -ne 2) { return }

        $name = $parts[0].Trim()
        $value = $parts[1].Trim().Trim('"')
        if ($name) {
            Set-Item -Path "Env:$name" -Value $value
        }
    }
}

# ---------------------------------------------------------------------------
# Devuelve la version mayor de un java.exe (ej. 8, 11, 21). 0 si no se puede.
# ---------------------------------------------------------------------------
function Get-JavaMajor {
    param([string]$JavaExe)

    if (-not (Test-Path $JavaExe)) { return 0 }

    # java escribe la version por stderr. En PowerShell 5.1, '2>&1' sobre un .exe
    # envuelve el stderr en NativeCommandError (y con EAP=Stop aborta el script),
    # asi que lo redirigimos a un archivo temporal.
    $tmp = [System.IO.Path]::GetTempFileName()
    $output = ''
    try {
        & $JavaExe -version 2> $tmp | Out-Null
        $output = Get-Content $tmp -Raw -ErrorAction SilentlyContinue
    } catch {
        $output = ''
    } finally {
        Remove-Item $tmp -ErrorAction SilentlyContinue
    }

    if ($output -match 'version "(\d+)') {
        $major = [int]$Matches[1]
        if ($major -eq 1 -and $output -match 'version "1\.(\d+)') {
            return [int]$Matches[1]   # formato viejo: 1.8 -> 8
        }
        return $major
    }
    return 0
}

# ---------------------------------------------------------------------------
# Resuelve un JDK 21+. Devuelve:
#   $null        -> el java del PATH ya es 21+, no hay que tocar nada
#   <ruta>       -> usar ese JAVA_HOME
#   'NOT_FOUND'  -> no se encontro ningun JDK 21
# ---------------------------------------------------------------------------
function Resolve-Jdk21Home {
    $current = Get-Command java -ErrorAction SilentlyContinue
    if ($current -and (Get-JavaMajor $current.Source) -ge 21) {
        return $null
    }

    if ($env:JAVA_HOME -and (Get-JavaMajor "$env:JAVA_HOME\bin\java.exe") -ge 21) {
        return $env:JAVA_HOME
    }

    if ($env:JAVA_HOME_21 -and (Test-Path "$env:JAVA_HOME_21\bin\java.exe")) {
        return $env:JAVA_HOME_21
    }

    $dirs = @()
    $dirs += Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -Filter 'jdk-21*' -ErrorAction SilentlyContinue
    $dirs += Get-ChildItem 'C:\Program Files\Java' -Directory -Filter 'jdk-21*' -ErrorAction SilentlyContinue
    $dirs += Get-ChildItem 'C:\Program Files\Microsoft' -Directory -Filter 'jdk-21*' -ErrorAction SilentlyContinue
    $dirs += Get-ChildItem "$env:USERPROFILE\.sdkman\candidates\java" -Directory -Filter '21*' -ErrorAction SilentlyContinue
    foreach ($d in $dirs) {
        if (Test-Path "$($d.FullName)\bin\java.exe") {
            return $d.FullName
        }
    }

    return 'NOT_FOUND'
}

$repoRoot = $PSScriptRoot
$envPath = Join-Path $repoRoot '.env'
$exampleEnvPath = Join-Path $repoRoot '.env.example'
$localExamplePath = Join-Path $repoRoot 'BE/src/main/resources/application-local.example.yml'
$localConfigPath = Join-Path $repoRoot 'BE/src/main/resources/application-local.yml'

# 1) Crear .env desde la plantilla si no existe
if (-not (Test-Path $envPath) -and (Test-Path $exampleEnvPath)) {
    Write-Host 'No se encontro .env. Copiando .env.example a .env...'
    Copy-Item $exampleEnvPath $envPath
    Write-Host 'IMPORTANTE: edita .env y carga MAIL_USERNAME / MAIL_PASSWORD reales para enviar correos.' -ForegroundColor Yellow
}

# 2) Crear application-local.yml desde la plantilla si no existe
if (-not (Test-Path $localConfigPath) -and (Test-Path $localExamplePath)) {
    Write-Host 'No se encontro application-local.yml. Copiando plantilla...'
    Copy-Item $localExamplePath $localConfigPath
}

# 3) Cargar variables del .env
Load-DotEnv -Path $envPath

# 4) Asegurar un JDK 21 (Spring Boot 4 lo requiere)
$jdk = Resolve-Jdk21Home
if ($jdk -eq 'NOT_FOUND') {
    Write-Error @'
No se encontro un JDK 21. Spring Boot 4 lo necesita para arrancar.
Opciones:
  - Instalalo (Eclipse Adoptium / Microsoft OpenJDK 21), o
  - Defini la variable JAVA_HOME_21 apuntando a tu carpeta del JDK 21.
'@
    exit 1
} elseif ($jdk) {
    Write-Host "Usando JDK 21: $jdk"
    $env:JAVA_HOME = $jdk
    $env:Path = "$jdk\bin;$env:Path"
}

# 5) Arrancar el backend con el perfil local
if ($Profile -eq 'prod') {
    Push-Location (Join-Path $repoRoot 'BE')
    try {
        Write-Host 'Levantando PostgreSQL con Docker Compose...'
        & docker compose up -d
    } finally {
        Pop-Location
    }
}

$env:SPRING_PROFILES_ACTIVE = $Profile

Push-Location (Join-Path $repoRoot 'BE')
try {
    if ($Profile -eq 'prod') {
        & .\mvnw.cmd "-Dspring-boot.run.profiles=prod" spring-boot:run
    } else {
        & .\mvnw.cmd spring-boot:run
    }
} finally {
    Pop-Location
}
