<#
publish.ps1 — скрипт для помощи в создании репозитория на GitHub и пуше от имени пользователя.

Требования:
- PowerShell (Windows)
- git установлен и доступен в PATH
- (опционально) GitHub CLI `gh` для автоматического создания репозитория
#>

param(
    [string]$RepoName = "HologramsFSMP",
    [string]$RemoteUrl = "",
    [string]$Description = "Paper plugin: HologramsFSMP",
    [switch]$Private
)

Push-Location -Path $PSScriptRoot
try {
    if (-not (Test-Path .git)) {
        Write-Host "Initializing local git repository..."
        git init
    }

    Write-Host "Adding files and trying to commit..."
    git add .
    # Попытка создать коммит; в PowerShell 5.1 нельзя использовать ||. Проверяем $LASTEXITCODE.
    git commit -m "Initial commit" -q 2>$null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Коммит не создан (возможно, нет изменений)."
    }

    if ($RemoteUrl -ne "") {
        Write-Host "Adding remote origin: $RemoteUrl"
        git remote remove origin 2>$null
        git remote add origin $RemoteUrl
    }

    if ($RemoteUrl -eq "") {
        if (Get-Command gh -ErrorAction SilentlyContinue) {
            Write-Host "Creating GitHub repository via gh..."
            $ghArgs = @("repo", "create", $RepoName, "--description", $Description, "--source=.")
            if ($Private) { $ghArgs += "--private" }
            $ghArgs += "--push"
            gh @ghArgs
            Write-Host "Repository created and pushed."
        }
        else {
            Write-Host "GitHub CLI (gh) not found. Provide -RemoteUrl or install gh."
            Write-Host "Manual commands to create remote and push:"
            Write-Host "    git remote add origin https://github.com/USERNAME/REPO.git"
            Write-Host "    git branch -M main"
            Write-Host "    git push -u origin main"
        }
    }
    else {
        Write-Host "Setting branch to main and pushing to origin..."
        git branch -M main
        git push -u origin main
    }
}
finally {
    Pop-Location
}
